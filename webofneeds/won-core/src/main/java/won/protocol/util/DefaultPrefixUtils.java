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

package won.protocol.util;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;

import won.protocol.vocabulary.CERT;
import won.protocol.vocabulary.GEO;
import won.protocol.vocabulary.LDP;
import won.protocol.vocabulary.SCHEMA;
import won.protocol.vocabulary.SFSIG;
import won.protocol.vocabulary.SIOC;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONCRYPT;
import won.protocol.vocabulary.WONMSG;

/**
 * User: fkleedorfer Date: 27.09.13
 */
public class DefaultPrefixUtils {
  /**
   * Sets the default prefixes on the specified model.
   *
   * @param model
   */
  public static void setDefaultPrefixes(Model model) {
    setDefaultPrefixes(model.getGraph().getPrefixMapping());
  }

  /**
   * * Sets the default prefixes on the specified prefixMapping.
   *
   * @param prefixMapping
   */
  public static void setDefaultPrefixes(PrefixMapping prefixMapping) {
    prefixMapping.setNsPrefix("won", WON.getURI());
    prefixMapping.setNsPrefix(WONMSG.DEFAULT_PREFIX, WONMSG.getURI());
    prefixMapping.setNsPrefix(WONCRYPT.DEFAULT_PREFIX, WONCRYPT.getURI());
    prefixMapping.setNsPrefix(SFSIG.DEFAULT_PREFIX, SFSIG.getURI());
    prefixMapping.setNsPrefix(CERT.DEFAULT_PREFIX, CERT.getURI());
    prefixMapping.setNsPrefix("sioc", SIOC.getURI());
    prefixMapping.setNsPrefix("rdf", RDF.getURI());
    prefixMapping.setNsPrefix("ldp", LDP.getURI());
    prefixMapping.setNsPrefix("rdfs", RDFS.getURI());
    prefixMapping.setNsPrefix("geo", GEO.getURI());
    prefixMapping.setNsPrefix("xsd", XSD.getURI());
    prefixMapping.setNsPrefix("dc", DC.getURI());
    prefixMapping.setNsPrefix(SCHEMA.DEFAULT_PREFIX, SCHEMA.getURI());
    prefixMapping.setNsPrefix("sh", "http://www.w3.org/ns/shacl#");
  }

  public static PrefixMapping getDefaultPrefixes() {
    PrefixMapping ret = PrefixMapping.Factory.create();
    setDefaultPrefixes(ret);
    return ret;
  }
}
