/**
 * Copyright 2015 Otto (GmbH & Co KG)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.schedoscope.export.bigquery.outputformat;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.TableDefinition;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.storage.Storage;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.output.FileOutputCommitter;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.schedoscope.export.bigquery.outputschema.PartitioningScheme;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.schedoscope.export.bigquery.outputformat.BigQueryOutputConfiguration.*;
import static org.schedoscope.export.bigquery.outputschema.HCatSchemaToBigQuerySchemaConverter.convertSchemaToTableDefinition;
import static org.schedoscope.export.bigquery.outputschema.PartitioningScheme.DAILY;
import static org.schedoscope.export.bigquery.outputschema.PartitioningScheme.NONE;
import static org.schedoscope.export.utils.BigQueryUtils.*;
import static org.schedoscope.export.utils.CloudStorageUtils.*;

/**
 * Hadoop output format to write JSON formatted records to GCP Cloud Storage and then forward them to BigQuery.
 *
 * @param <K> we do not care about this type parameter.
 */
public class BigQueryOutputFormat<K> extends OutputFormat<K, Text> {


    private static void setProxies(Configuration conf) {
        if (getBigQueryProxyHost(conf) != null && getBigQueryProxyPort(conf) != null) {
            System.setProperty("https.proxyHost", getBigQueryProxyHost(conf));
            System.setProperty("https.proxyPort", getBigQueryProxyPort(conf));
        }
    }

    /**
     * Given a Hadoop configuration with the BigQuery output format configuration values, create an equivalent BigQuery
     * table. It HCatSchema passed in the configuration is considered, as well as a potentially given partition date
     * to decide about daily partitioning of the table (or not).
     *
     * @param conf the BigQuery augmented Hadoop configuration (see {@link BigQueryOutputConfiguration})
     * @throws IOException in case the table could not be created.
     */
    public static void prepareBigQueryTable(Configuration conf) throws IOException {

        setProxies(conf);

        PartitioningScheme partitioning = getBigQueryTablePartitionDate(conf) != null ? DAILY : NONE;

        TableDefinition outputSchema = convertSchemaToTableDefinition(getBigQueryHCatSchema(conf), partitioning);

        BigQuery bigQueryService = bigQueryService(getBigQueryProject(conf), getBigQueryGcpKey(conf));

        retry(3, () -> {
            dropTable(bigQueryService, getBigQueryTableId(conf, true));
        });

        retry(3, () -> {
            createTable(bigQueryService, getBigQueryTableId(conf), outputSchema, getBigQueryDatasetLocation(conf));
        });

    }

    /**
     * After the output format has written a Hive table's data to GCP cloud storage, commit the export by loading
     * the data into the prepared BigQuery table and then delete the data in the storage bucket afterwards.
     *
     * @param conf the BigQuery augmented Hadoop configuration (see {@link BigQueryOutputConfiguration})
     * @throws IOException
     * @throws TimeoutException
     * @throws InterruptedException
     */
    public static void commit(Configuration conf) throws IOException, TimeoutException, InterruptedException {

        setProxies(conf);

        BigQuery bigQueryService = bigQueryService(getBigQueryProject(conf), getBigQueryGcpKey(conf));
        Storage storageService = storageService(getBigQueryProject(conf), getBigQueryGcpKey(conf));

        List<String> blobsToLoad = listBlobs(storageService, getBigQueryExportStorageBucket(conf), getBigQueryExportStorageFolder(conf));
        TableId tableId = getBigQueryTableId(conf, true);

        retry(3, () -> loadTable(bigQueryService, tableId, blobsToLoad));

        try {
            rollbackStorage(conf);
        } catch (Throwable t) {
            t.printStackTrace();
        }

    }

    /**
     * Call the method in case of a problem. It drops the BigQuery table or table partition and deletes all data
     * on cloud storage.
     *
     * @param conf the BigQuery augmented Hadoop configuration (see {@link BigQueryOutputConfiguration})
     */
    public static void rollback(Configuration conf) {

        setProxies(conf);

        try {
            rollbackBigQuery(conf);
        } catch (Throwable t) {
            t.printStackTrace();
        }

        try {
            rollbackStorage(conf);
        } catch (Throwable t) {
            t.printStackTrace();
        }

    }

    private static void rollbackBigQuery(Configuration conf) throws IOException {
        BigQuery bigQueryService = bigQueryService(getBigQueryProject(conf), getBigQueryGcpKey(conf));
        TableId tableId = getBigQueryTableId(conf, true);

        retry(3, () -> {
            dropTable(bigQueryService, tableId);
        });
    }

    private static void rollbackStorage(Configuration conf) throws IOException {
        Storage storageService = storageService(getBigQueryProject(conf), getBigQueryGcpKey(conf));
        String bucket = getBigQueryExportStorageBucket(conf);
        String blobPrefix = getBigQueryExportStorageFolder(conf);

        retry(3, () -> {
            deleteBlob(storageService, bucket, blobPrefix);
        });
    }


    @Override
    public RecordWriter<K, Text> getRecordWriter(TaskAttemptContext context) throws IOException {
        Configuration conf = context.getConfiguration();

        setProxies(conf);

        Storage storageService = storageService(getBigQueryProject(conf), getBigQueryGcpKey(conf));

        return new BiqQueryJsonRecordWriter<>(
                storageService,
                getBigQueryExportStorageBucket(conf),
                getBigQueryExportStorageFolder(conf) + "/" + context.getTaskAttemptID().toString() + ".gz",
                getBigQueryExportStorageRegion(conf),
                getBigQueryFlushInterval(conf)
        );
    }

    @Override
    public void checkOutputSpecs(JobContext context) throws IOException, InterruptedException {
        // do nothing
    }

    @Override
    public OutputCommitter getOutputCommitter(TaskAttemptContext context) throws IOException, InterruptedException {
        return new FileOutputCommitter(FileOutputFormat.getOutputPath(context), context);
    }

}
