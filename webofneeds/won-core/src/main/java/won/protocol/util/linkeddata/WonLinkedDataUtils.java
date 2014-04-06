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

package won.protocol.util.linkeddata;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.sparql.path.Path;
import com.hp.hpl.jena.sparql.path.PathParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.util.RdfUtils;
import won.protocol.vocabulary.WON;

import java.net.URI;

/**
 * Utilitiy functions for common linked data lookups.
 */
public class WonLinkedDataUtils
{
  private static final Logger logger = LoggerFactory.getLogger(WonLinkedDataUtils.class);

  public static URI getRemoteConnectionURIforConnectionURI(URI connectionURI, LinkedDataSource linkedDataSource) {
    Model model = getModelForResource(connectionURI, linkedDataSource);
    Path propertyPath = PathParser.parse("<" + WON.HAS_REMOTE_CONNECTION + ">", PrefixMapping.Standard);
    return RdfUtils.getURIPropertyForPropertyPath(model, connectionURI, propertyPath);
  }

  public static URI getRemoteNeedURIforConnectionURI(URI connectionURI, LinkedDataSource linkedDataSource) {
    Model model = getModelForResource(connectionURI, linkedDataSource);
    Path propertyPath = PathParser.parse("<" + WON.HAS_REMOTE_NEED + ">", PrefixMapping.Standard);
    return RdfUtils.getURIPropertyForPropertyPath(model, connectionURI, propertyPath);
  }

  public static Model getModelForResource(final URI connectionURI, final LinkedDataSource linkedDataSource) {
    assert linkedDataSource != null : "linkedDataSource must not be null";
    assert connectionURI != null : "connection URI must not be null";
    Model model = null;
    logger.debug("loading model for connection {}", connectionURI);
    model = linkedDataSource.getModelForResource(connectionURI);
    if (model == null) {
      throw new IllegalStateException("failed to load model for Connection " + connectionURI);
    }
    return model;
  }

}
