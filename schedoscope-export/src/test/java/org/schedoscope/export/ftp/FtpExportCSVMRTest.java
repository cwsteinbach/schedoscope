/**
 * Copyright 2016 Otto (GmbH & Co KG)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.schedoscope.export.ftp;

import static org.junit.Assert.assertTrue;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hive.hcatalog.mapreduce.HCatInputFormat;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.schedoscope.export.HiveUnitBaseTest;
import org.schedoscope.export.ftp.outputformat.CSVOutputFormat;
import org.schedoscope.export.ftp.upload.FileCompressionCodec;
import org.schedoscope.export.writables.TextPairArrayWritable;

public class FtpExportCSVMRTest extends HiveUnitBaseTest {

	private static final String OUTPUT_DIR = "/tmp/putput";

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
	}

	@Test
	public void testFtpCSVExport() throws Exception {
		setUpHiveServer("src/test/resources/test_map_data.txt", "src/test/resources/test_map.hql", "test_map");

		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.INFO);

		conf.set("io.compression.codecs", "org.apache.hadoop.io.compress.GzipCodec,org.apache.hadoop.io.compress.BZip2Codec");

		Job job = Job.getInstance(conf);

		Path outfile = new Path(OUTPUT_DIR);

		CSVOutputFormat.setOutputPath(job, outfile);
		CSVOutputFormat.setOutput(job, true, FileCompressionCodec.none, "ftp://192.168.56.101:21/", "vagrant", "vagrant", null, "testing");

		job.setMapperClass(FtpExportCSVMapper.class);
		job.setReducerClass(Reducer.class);
		job.setNumReduceTasks(2);
		job.setInputFormatClass(HCatInputFormat.class);
		job.setOutputFormatClass(CSVOutputFormat.class);

		job.setOutputKeyClass(LongWritable.class);
		job.setOutputValueClass(TextPairArrayWritable.class);

		assertTrue(job.waitForCompletion(true));

		FileSystem fs = outfile.getFileSystem(conf);

		RemoteIterator<LocatedFileStatus> stat = fs.listFiles(outfile, true);

		while (stat.hasNext()) {
			System.out.println(stat.next());
		}
	}

	@Test
	public void testSftpCSVExportUserPassAuth() throws Exception {
		setUpHiveServer("src/test/resources/test_map_data.txt", "src/test/resources/test_map.hql", "test_map");

		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.INFO);

		conf.set("io.compression.codecs", "org.apache.hadoop.io.compress.GzipCodec,org.apache.hadoop.io.compress.BZip2Codec");

		Job job = Job.getInstance(conf);

		Path outfile = new Path(OUTPUT_DIR);

		CSVOutputFormat.setOutputPath(job, outfile);
		CSVOutputFormat.setOutput(job, true, FileCompressionCodec.gzip, "sftp://192.168.56.101:22/", "vagrant", "vagrant", null, "testing");

		job.setMapperClass(FtpExportCSVMapper.class);
		job.setReducerClass(Reducer.class);
		job.setNumReduceTasks(2);
		job.setInputFormatClass(HCatInputFormat.class);
		job.setOutputFormatClass(CSVOutputFormat.class);

		job.setOutputKeyClass(LongWritable.class);
		job.setOutputValueClass(TextPairArrayWritable.class);

		assertTrue(job.waitForCompletion(true));

		FileSystem fs = outfile.getFileSystem(conf);

		RemoteIterator<LocatedFileStatus> stat = fs.listFiles(outfile, true);

		while (stat.hasNext()) {
			System.out.println(stat.next());
		}
	}

	@Test
	public void testSftpCSVExportPubKeyAuthNoEnc() throws Exception {
		setUpHiveServer("src/test/resources/test_map_data.txt", "src/test/resources/test_map.hql", "test_map");

		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.INFO);

		conf.set("io.compression.codecs", "org.apache.hadoop.io.compress.GzipCodec,org.apache.hadoop.io.compress.BZip2Codec");

		Job job = Job.getInstance(conf);

		Path outfile = new Path(OUTPUT_DIR);

		CSVOutputFormat.setOutputPath(job, outfile);
		CSVOutputFormat.setOutput(job, true, FileCompressionCodec.bzip2, "sftp://192.168.56.101:22/", "vagrant", null, "/Users/mac/work/otto/schedoscope/schedoscope-export/src/test/resources/keys/id_rsa_not_encrypted", "testing");

		job.setMapperClass(FtpExportCSVMapper.class);
		job.setReducerClass(Reducer.class);
		job.setNumReduceTasks(2);
		job.setInputFormatClass(HCatInputFormat.class);
		job.setOutputFormatClass(CSVOutputFormat.class);

		job.setOutputKeyClass(LongWritable.class);
		job.setOutputValueClass(TextPairArrayWritable.class);

		assertTrue(job.waitForCompletion(true));

		FileSystem fs = outfile.getFileSystem(conf);

		RemoteIterator<LocatedFileStatus> stat = fs.listFiles(outfile, true);

		while (stat.hasNext()) {
			System.out.println(stat.next());
		}
	}

	@Test
	public void testSftpCSVExportPubKeyAuthEnc() throws Exception {
		setUpHiveServer("src/test/resources/test_map_data.txt", "src/test/resources/test_map.hql", "test_map");

		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.INFO);

		conf.set("io.compression.codecs", "org.apache.hadoop.io.compress.GzipCodec,org.apache.hadoop.io.compress.BZip2Codec");

		Job job = Job.getInstance(conf);

		Path outfile = new Path(OUTPUT_DIR);

		CSVOutputFormat.setOutputPath(job, outfile);
		CSVOutputFormat.setOutput(job, true, FileCompressionCodec.gzip, "sftp://192.168.56.101:22/", "vagrant", "12345", "/Users/mac/work/otto/schedoscope/schedoscope-export/src/test/resources/keys/id_rsa_encrypted", "testing");

		job.setMapperClass(FtpExportCSVMapper.class);
		job.setReducerClass(Reducer.class);
		job.setNumReduceTasks(2);
		job.setInputFormatClass(HCatInputFormat.class);
		job.setOutputFormatClass(CSVOutputFormat.class);

		job.setOutputKeyClass(LongWritable.class);
		job.setOutputValueClass(TextPairArrayWritable.class);

		assertTrue(job.waitForCompletion(true));

		FileSystem fs = outfile.getFileSystem(conf);

		RemoteIterator<LocatedFileStatus> stat = fs.listFiles(outfile, true);

		while (stat.hasNext()) {
			System.out.println(stat.next());
		}
	}
}
