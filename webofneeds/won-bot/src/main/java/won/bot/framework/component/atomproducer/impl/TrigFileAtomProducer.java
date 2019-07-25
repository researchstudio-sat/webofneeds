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
package won.bot.framework.component.atomproducer.impl;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.component.atomproducer.FileBasedAtomProducer;
import won.protocol.util.AtomModelWrapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * User: fkleedorfer Date: 17.12.13
 */
public class TrigFileAtomProducer implements FileBasedAtomProducer {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public synchronized Dataset readAtomFromFile(final File file) throws IOException {
        logger.debug("processing as turtle file: {} ", file);
        try (FileInputStream fis = new FileInputStream(file)) {
            Dataset dataset = DatasetFactory.createGeneral();
            RDFDataMgr.read(dataset, fis, RDFFormat.TRIG.getLang());
            AtomModelWrapper atomModelWrapper = new AtomModelWrapper(dataset);
            return atomModelWrapper.copyDataset();
        } catch (Exception e) {
            logger.error("could not parse trig from file {} ", file, e);
            throw e;
        }
    }
}
