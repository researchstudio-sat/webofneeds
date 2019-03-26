/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package won.protocol.model;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;

/**
 * Aggregates the datasets wrapped by a number of dataset holders. As soon as
 * the aggregate() function is called, all datasetHolders added so far are read
 * (via their getDatasetBytes() method) and an aggregated dataset is created.
 * All subsequent calls to aggregate just yield the already aggregated dataset.
 */
public class DatasetHolderAggregator {
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private List<InputStream> inputStreams = null;
  private Lang rdfLanguage = null;
  private static final Lang DEFAULT_RDF_LANGUAGE = Lang.NQUADS;
  private Dataset aggregatedDataset = null;

  public DatasetHolderAggregator() {
    this(null, null);
  }

  public DatasetHolderAggregator(final List<InputStream> inputStreams, final Lang rdfLanguage) {
    this.inputStreams = inputStreams == null ? new LinkedList<>() : inputStreams;
    this.rdfLanguage = rdfLanguage == null ? DEFAULT_RDF_LANGUAGE : rdfLanguage;
  }

  public DatasetHolderAggregator(final Lang rdfLanguage) {
    this(null, rdfLanguage);
  }

  public DatasetHolderAggregator(final List<InputStream> inputStreams) {
    this(inputStreams, null);
  }

  public void appendDataset(DatasetHolder datasetHolder) {
    if (this.aggregatedDataset != null)
      throw new IllegalStateException("Cannot append a dataset after the aggregate" + "() function was called");
    this.inputStreams.add(new ByteArrayInputStream(datasetHolder.getDatasetBytes()));
  }

  public Dataset aggregate() {
    if (this.aggregatedDataset != null) {
      return this.aggregatedDataset;
    }
    synchronized (this) {
      if (this.aggregatedDataset != null)
        return this.aggregatedDataset;
      StopWatch stopWatch = new StopWatch();
      stopWatch.start();
      Dataset result = DatasetFactory.createGeneral();
      stopWatch.stop();
      logger.debug("init dataset: " + stopWatch.getLastTaskTimeMillis());
      stopWatch.start();
      this.aggregatedDataset = result;
      if (this.inputStreams == null || this.inputStreams.size() == 0) {
        return this.aggregatedDataset;
      }
      RDFDataMgr.read(result,
          new SequenceInputStream(Collections.enumeration(Collections.unmodifiableCollection(this.inputStreams))),
          this.rdfLanguage);
      stopWatch.stop();
      logger.debug("read dataset: " + stopWatch.getLastTaskTimeMillis());
      return this.aggregatedDataset;
    }
  }
}
