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

package won.protocol.model.util;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.Ignore;
import org.junit.Test;
import won.protocol.util.RdfUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class RdfIoSpeedTest
{

  @Test
  @Ignore
  public void compareTrigRdfthriftNquads() throws IOException {

    Dataset dataset = DatasetFactory.createMem();
    Path infile = Paths.get("won-core/src/test/resources/speedtest/dataset.trig");
    byte[] dataTrig = Files.readAllBytes(infile);

    infile = Paths.get("won-core/src/test/resources/speedtest/dataset.rdft");
    byte[] dataRdft = Files.readAllBytes(infile);

    infile = Paths.get("won-core/src/test/resources/speedtest/dataset.nq");
    byte[] dataRdfq = Files.readAllBytes(infile);


    StopWatch watch = new StopWatch();
    watch.start();
    for (int i = 0; i < 10000; i++){
      RDFDataMgr.read(dataset, new ByteArrayInputStream(dataTrig), Lang.TRIG);
    }
    watch.stop();
    System.out.println("trig reading took: " + watch.getTime());

    watch = new StopWatch();
    watch.start();
    for (int i = 0; i < 10000; i++){
      RDFDataMgr.write(new ByteArrayOutputStream(), dataset, Lang.TRIG);
    }
    watch.stop();
    System.out.println("trig writing took: " + watch.getTime());

    watch = new StopWatch();
    watch.start();
    for (int i = 0; i < 10000; i++){
      RDFDataMgr.read(dataset, new ByteArrayInputStream(dataRdfq), Lang.NQUADS);
    }
    watch.stop();
    System.out.println("nquads reading took: " + watch.getTime());

    watch = new StopWatch();
    watch.start();
    for (int i = 0; i < 10000; i++){
      RDFDataMgr.write(new ByteArrayOutputStream(), dataset, Lang.NQUADS);
    }
    watch.stop();
    System.out.println("nquads writing took: " + watch.getTime());

    watch = new StopWatch();
    watch.start();
    for (int i = 0; i < 10000; i++){
      RDFDataMgr.read(dataset, new ByteArrayInputStream(dataRdft), Lang.RDFTHRIFT);
    }
    watch.stop();
    System.out.println("rdf-thrift reading took: " + watch.getTime());

    watch = new StopWatch();
    watch.start();
    for (int i = 0; i < 10000; i++){
      RDFDataMgr.write(new ByteArrayOutputStream(), dataset,  Lang.RDFTHRIFT);
    }
    watch.stop();
    System.out.println("rdf-thrift writing took: " + watch.getTime());
  }

  @Test
  @Ignore
  public void compareCollectBytesVsCollectDatasets() throws IOException {
    Dataset dataset = DatasetFactory.createMem();
    Path infile = Paths.get("won-core/src/test/resources/speedtest/dataset.trig");
    byte[] dataTrig = Files.readAllBytes(infile);

    infile = Paths.get("won-core/src/test/resources/speedtest/dataset.rdft");
    byte[] dataRdft = Files.readAllBytes(infile);

    infile = Paths.get("won-core/src/test/resources/speedtest/dataset.nq");
    byte[] dataRdfq = Files.readAllBytes(infile);


    StopWatch watch = new StopWatch();
    watch.start();

    for (int i = 0; i < 10000; i++){
      Dataset additionalDataset = DatasetFactory.createMem();
      RDFDataMgr.read(additionalDataset, new ByteArrayInputStream(dataRdfq), Lang.NQUADS);
      RdfUtils.addDatasetToDataset(dataset, additionalDataset);
    }
    watch.stop();
    System.out.println("collecting in dataset took: " + watch.getTime());

    watch = new StopWatch();
    watch.start();

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    for (int i = 0; i < 10000; i++){
      out.write(dataRdfq);
    }
    Dataset additionalDataset = DatasetFactory.createMem();
    RDFDataMgr.read(additionalDataset, new ByteArrayInputStream(out.toByteArray()), Lang.NQUADS);
    watch.stop();
    System.out.println("collecting in byteArrays took: " + watch.getTime());
  }
}
