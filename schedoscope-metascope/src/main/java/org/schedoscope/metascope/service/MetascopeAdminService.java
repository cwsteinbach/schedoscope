/**
 * Copyright 2017 Otto (GmbH & Co KG)
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
package org.schedoscope.metascope.service;

import org.schedoscope.metascope.index.SolrFacade;
import org.schedoscope.metascope.model.MetascopeTable;
import org.schedoscope.metascope.repository.MetascopeTableRepository;
import org.schedoscope.metascope.task.MetascopeTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MetascopeAdminService {

  @Autowired
  private MetascopeTask metascopeTask;

  @Autowired
  private MetascopeTableRepository metascopeTableRepository;

  @Autowired
  @Lazy
  private SolrFacade solr;

  @Async
  @Transactional
  public void schedule() {
    metascopeTask.run();
  }

  @Async
  @Transactional
  public void syncIndex() {
    Iterable<MetascopeTable> tables = metascopeTableRepository.findAll();
    for (MetascopeTable table : tables) {
      solr.updateTableEntityAsync(table, false);
    }
    solr.commit();
  }

}
