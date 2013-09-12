/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package won.matcher.query.rdf;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.GraphExtract;
import com.hp.hpl.jena.graph.TripleBoundary;
import com.hp.hpl.jena.rdf.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.spin.util.SPTextUtil;
import org.topbraid.spin.vocabulary.SP;
import won.protocol.vocabulary.WON;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * User: fkleedorfer
 * Date: 09.09.13
 */
public class SPINUtils
{
  private static final Logger logger = LoggerFactory.getLogger(SPINUtils.class);
  /**
   * Extracts all subgraphs from the specified model that are attached to it via won:attachSpinWhereClause
   * and returns a ModelWithRoot collection mapping the subject of these triples to the subgraphs attached to them.
   * @param triplesQueryFactory
   * @param model
   * @return
   */
  static Collection<SubModel> extractSPINSubgraphs(final TriplesQueryFactory triplesQueryFactory, final Model model){
    StmtIterator triplesWithWhere = model.listStatements(null, WON.EMBED_SPIN_ASK, (RDFNode) null);
    List<SubModel> spinWhereClauses = new ArrayList<SubModel>();
    while(triplesWithWhere.hasNext()) {
      Statement attachingTriple = triplesWithWhere.next();
      //TODO we run into trouble if the SPIN expression references back into the non-SPIN graph when we use stopNowhere!
      GraphExtract graphExtract = new GraphExtract(TripleBoundary.stopNowhere);
      Graph subGraph = graphExtract.extract(attachingTriple.getObject().asNode(),model.getGraph());
      Model m = ModelFactory.createModelForGraph(subGraph);
      m.setNsPrefixes(model.getNsPrefixMap());
      spinWhereClauses.add(new SubModel(m, attachingTriple));
    }
    return spinWhereClauses;
  }

  /**
   * Replaces all occurrences of sp:text with their respecitve equivalent in spin RDF.
   * @param model
   * @return
   */
  public static void replaceSpinTextWithSpinRdf(Model model) {
    //use SPIN API to add SPIN RDF wherever sp:text is present
    try {
    SPTextUtil.ensureSPINRDFExists(model);
    } catch (Throwable t){
      t.printStackTrace();
    }
    //delete sp:text triples
    StmtIterator it = model.listStatements(null, SP.text, (RDFNode) null);
    while(it.hasNext()){
      Statement stmt = it.next();
      logger.debug("deleting statement: {}", stmt);
      it.remove();
    }
  }
}
