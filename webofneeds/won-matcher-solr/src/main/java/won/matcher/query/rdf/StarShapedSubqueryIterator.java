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

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.sparql.algebra.*;
import com.hp.hpl.jena.sparql.algebra.op.OpBGP;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * User: fkleedorfer
 * Date: 30.08.13
 */
public class StarShapedSubqueryIterator implements Iterator<Op>
{
  private Logger logger = LoggerFactory.getLogger(getClass());
  private Op mainOp;
  private Iterator<Node> subjectIterator;

  public StarShapedSubqueryIterator(final Op mainOp)
  {
    this.mainOp = mainOp;
    setup();
  }

  private void setup()
  {
    SubjectCollectingVisitor visitor = new SubjectCollectingVisitor();
    OpWalker.walk(this.mainOp, visitor);
    this.subjectIterator = visitor.getSubjectIterator();
  }


  @Override
  public boolean hasNext()
  {
    return this.subjectIterator.hasNext();
  }

  @Override
  public Op next()
  {
    return extractStarShapedSubquery(this.mainOp, this.subjectIterator.next());
  }

  private Op extractStarShapedSubquery(final Op mainOp, final Node node)
  {
    return Transformer.transform( new StarShapedSubqueryTransformer(node),mainOp );
  }

  @Override
  public void remove()
  {
    throw new UnsupportedOperationException("remove() not implemented by this iterator");
  }

  private class SubjectCollectingVisitor extends OpVisitorBase
  {
    private Set<Node> collectedNodes = new HashSet<Node>();
    @Override
    public void visit(final OpBGP opBGP)
    {
      logger.debug("visiting bgp: {}", opBGP);
      for(Triple triple: opBGP.getPattern().getList()) {
        logger.debug("processing bgp triple: {}",triple);
        collectedNodes.add(triple.getSubject());
      }
    }

    public Iterator<Node> getSubjectIterator(){
      return collectedNodes.iterator();
    }
  }

  private class StarShapedSubqueryTransformer extends TransformCopy
  {
    private Node center;
    public StarShapedSubqueryTransformer(final Node center)
    {
      this.center = center;
    }


    @Override
    public Op transform(final OpBGP opBGP)
    {
      List<Triple> inTriples = opBGP.getPattern().getList();
      BasicPattern toKeep = new BasicPattern();
      for(Triple inTriple: inTriples){
        if (center.equals(inTriple.getSubject())){
          toKeep.add(new Triple(inTriple.getSubject(), inTriple.getPredicate(), inTriple.getObject()));
        }
      }
      return new OpBGP(toKeep);
    }

  }
}
