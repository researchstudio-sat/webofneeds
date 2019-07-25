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
package won.protocol.util.linkeddata.impl;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.modify.UpdateProcessRemote;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.util.linkeddata.CrawlerCallback;

import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Iterator;

/**
 * Crawler callback implementation that writes crawled data to a predefined
 * sparql endpoint.
 */
public class SparqlUpdateCrawlerCallback implements CrawlerCallback {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    String sparqlEndpoint = null;

    public void setSparqlEndpoint(final String sparqlEndpoint) {
        this.sparqlEndpoint = sparqlEndpoint;
    }

    @Override
    public void onDatasetCrawled(final URI uri, final Dataset dataset) {
        if (null == sparqlEndpoint) {
            logger.warn("no SPARQL endpoint defined");
            return;
        }
        Iterator<String> graphNames = dataset.listNames();
        while (graphNames.hasNext()) {
            StringBuilder quadpatterns = new StringBuilder();
            String graphName = graphNames.next();
            Model model = dataset.getNamedModel(graphName);
            StringWriter sw = new StringWriter();
            RDFDataMgr.write(sw, model, Lang.NTRIPLES);
            quadpatterns.append("\nINSERT DATA { GRAPH <").append(graphName).append("> { ").append(sw).append("}};\n");
            logger.info(quadpatterns.toString());
            UpdateRequest update = UpdateFactory.create(quadpatterns.toString());
            UpdateProcessRemote riStore = (UpdateProcessRemote) UpdateExecutionFactory.createRemote(update,
                            sparqlEndpoint);
            riStore.execute();
        }
    }
}
