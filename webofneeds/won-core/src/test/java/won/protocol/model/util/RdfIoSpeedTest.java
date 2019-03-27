/*
 * Copyright 2012 Research Studios Austria Forschungsges.m.b.H. Licensed under
 * the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package won.protocol.model.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.Ignore;
import org.junit.Test;

import won.protocol.util.RdfUtils;

public class RdfIoSpeedTest {
    @Test
    @Ignore
    public void compareTrigRdfthriftNquads() throws IOException {
        Dataset dataset = DatasetFactory.createGeneral();
        Path infile = Paths.get("won-core/src/test/resources/speedtest/dataset.trig");
        byte[] dataTrig = Files.readAllBytes(infile);
        infile = Paths.get("won-core/src/test/resources/speedtest/dataset.rdft");
        byte[] dataRdft = Files.readAllBytes(infile);
        infile = Paths.get("won-core/src/test/resources/speedtest/dataset.nq");
        byte[] dataRdfq = Files.readAllBytes(infile);
        StopWatch watch = new StopWatch();
        watch.start();
        for (int i = 0; i < 10000; i++) {
            RDFDataMgr.read(dataset, new ByteArrayInputStream(dataTrig), Lang.TRIG);
        }
        watch.stop();
        System.out.println("trig reading took: " + watch.getTime());
        watch = new StopWatch();
        watch.start();
        for (int i = 0; i < 10000; i++) {
            RDFDataMgr.write(new ByteArrayOutputStream(), dataset, Lang.TRIG);
        }
        watch.stop();
        System.out.println("trig writing took: " + watch.getTime());
        watch = new StopWatch();
        watch.start();
        for (int i = 0; i < 10000; i++) {
            RDFDataMgr.read(dataset, new ByteArrayInputStream(dataRdfq), Lang.NQUADS);
        }
        watch.stop();
        System.out.println("nquads reading took: " + watch.getTime());
        watch = new StopWatch();
        watch.start();
        for (int i = 0; i < 10000; i++) {
            RDFDataMgr.write(new ByteArrayOutputStream(), dataset, Lang.NQUADS);
        }
        watch.stop();
        System.out.println("nquads writing took: " + watch.getTime());
        watch = new StopWatch();
        watch.start();
        for (int i = 0; i < 10000; i++) {
            RDFDataMgr.read(dataset, new ByteArrayInputStream(dataRdft), Lang.RDFTHRIFT);
        }
        watch.stop();
        System.out.println("rdf-thrift reading took: " + watch.getTime());
        watch = new StopWatch();
        watch.start();
        for (int i = 0; i < 10000; i++) {
            RDFDataMgr.write(new ByteArrayOutputStream(), dataset, Lang.RDFTHRIFT);
        }
        watch.stop();
        System.out.println("rdf-thrift writing took: " + watch.getTime());
    }

    @Test
    @Ignore
    public void compareCollectBytesVsCollectDatasets() throws IOException {
        Dataset dataset = DatasetFactory.createGeneral();
        Path infile = Paths.get("won-core/src/test/resources/speedtest/dataset.trig");
        byte[] dataTrig = Files.readAllBytes(infile);
        infile = Paths.get("won-core/src/test/resources/speedtest/dataset.rdft");
        byte[] dataRdft = Files.readAllBytes(infile);
        infile = Paths.get("won-core/src/test/resources/speedtest/dataset.nq");
        byte[] dataRdfq = Files.readAllBytes(infile);
        StopWatch watch = new StopWatch();
        watch.start();
        for (int i = 0; i < 10000; i++) {
            Dataset additionalDataset = DatasetFactory.createGeneral();
            RDFDataMgr.read(additionalDataset, new ByteArrayInputStream(dataRdfq), Lang.NQUADS);
            RdfUtils.addDatasetToDataset(dataset, additionalDataset);
        }
        watch.stop();
        System.out.println("collecting in dataset took: " + watch.getTime());
        watch = new StopWatch();
        watch.start();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < 10000; i++) {
            out.write(dataRdfq);
        }
        Dataset additionalDataset = DatasetFactory.createGeneral();
        RDFDataMgr.read(additionalDataset, new ByteArrayInputStream(out.toByteArray()), Lang.NQUADS);
        watch.stop();
        System.out.println("collecting in byteArrays took: " + watch.getTime());
        watch = new StopWatch();
        watch.start();
        Vector<InputStream> streams = new Vector<>(10000);
        for (int i = 0; i < 10000; i++) {
            streams.add(new ByteArrayInputStream(dataRdfq));
        }
        additionalDataset = DatasetFactory.createGeneral();
        RDFDataMgr.read(additionalDataset, new SequenceInputStream(streams.elements()), Lang.NQUADS);
        watch.stop();
        System.out.println("collecting in vector -> sequenceInputStream took: " + watch.getTime());
        watch = new StopWatch();
        watch.start();
        ArrayList<InputStream> streamsList = new ArrayList<>(10000);
        for (int i = 0; i < 10000; i++) {
            streamsList.add(new ByteArrayInputStream(dataRdfq));
        }
        additionalDataset = DatasetFactory.createGeneral();
        RDFDataMgr.read(additionalDataset, new SequenceInputStream(Collections.enumeration(streamsList)), Lang.NQUADS);
        watch.stop();
        System.out.println("collecting in arrayList -> sequenceInputStream took: " + watch.getTime());
        watch = new StopWatch();
        watch.start();
        InputStream[] streamsArr = new InputStream[10000];
        for (int i = 0; i < 10000; i++) {
            streamsArr[i] = new ByteArrayInputStream(dataRdfq);
        }
        additionalDataset = DatasetFactory.createGeneral();
        RDFDataMgr.read(additionalDataset, new SequenceInputStream(new Enumeration() {
            int index = 0;

            @Override
            public boolean hasMoreElements() {
                return index < streamsArr.length;
            }

            @Override
            public Object nextElement() {
                return streamsArr[index++];
            }
        }), Lang.NQUADS);
        watch.stop();
        System.out.println("collecting in array -> sequenceInputStream took: " + watch.getTime());
    }
}
