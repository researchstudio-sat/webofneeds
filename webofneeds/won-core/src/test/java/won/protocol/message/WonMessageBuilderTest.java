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

package won.protocol.message;

import com.google.common.collect.Iterators;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;
import org.junit.Assert;
import org.junit.Test;
import won.protocol.util.RdfUtils;

import java.net.URI;
import java.util.Iterator;

public class WonMessageBuilderTest
{
  private static final URI MSG_URI_1 = URI.create("http://example.com/msg/1234");
  private static final URI MSG_URI_2 = URI.create("http://example.com/msg/5678");
  private static final URI CONTENT_GRAPH_URI_1 = URI.create("http://example.com/content/1");
  private static final URI TYPE_URI_1 = URI.create("http://example.com/type/1");
  private static final URI TYPE_URI_2 = URI.create("http://example.com/type/2");
  private static final URI RECEIVER_URI_1 = URI.create("http://example.com/receiver/1");


  @Test
  public void test_wrap_retains_envelope_graph_properties(){
    WonMessage msg2 = createMessageAndWrapIt();
    Assert.assertEquals(WonMessageType.HINT_MESSAGE, msg2.getMessageType());
  }

  @Test
  public void test_wrap_allows_new_envelope_graph_properties(){
    WonMessage msg2 = createMessageAndWrapIt();
    Assert.assertEquals(RECEIVER_URI_1, msg2.getReceiverURI());
  }

  @Test
  public void test_wrap_retains_content_graphs(){
    WonMessage msg2 = createMessageAndWrapIt();
    Assert.assertEquals(MSG_URI_1.toString(), RdfUtils.findOne(msg2.getMessageContent(),
      new RdfUtils.ModelVisitor<String>()
    {
      @Override
      public String visit(final Model model) {
        StmtIterator it = model.listStatements(null, RDF.type, model.getResource(TYPE_URI_1.toString()));
        if (it.hasNext()) return it.nextStatement().getSubject().asResource().toString();
        return null;
      }
    }, false));

    Assert.assertEquals(TYPE_URI_1.toString(), RdfUtils.findOnePropertyFromResource(
      msg2.getMessageContent(),
      MSG_URI_1,
      RDF.type).asResource().getURI().toString());
  }

  @Test
  public void test_wrap_allows_new_content_graphs(){
    WonMessage msg2 = createMessageAndWrapIt();
    Assert.assertEquals(MSG_URI_2.toString(), RdfUtils.findOne(msg2.getMessageContent(),
      new RdfUtils.ModelVisitor<String>()
      {
        @Override
        public String visit(final Model model) {
          StmtIterator it = model.listStatements(null, RDF.type, model.getResource(TYPE_URI_2.toString()));
          if (it.hasNext()) return it.nextStatement().getSubject().asResource().toString();
          return null;
        }
      }, false));

    Assert.assertEquals(TYPE_URI_2.toString(), RdfUtils.findOnePropertyFromResource(
      msg2.getMessageContent(),
      MSG_URI_2,
      RDF.type).asResource().getURI().toString());
  }

  @Test
  public void test_wraped_message_contains_correct_number_of_envelope_graphs() {
    WonMessage msg2 = createMessageAndWrapIt();
    Assert.assertEquals(2, msg2.getEnvelopeGraphs().size());
  }

  private WonMessage createMessageAndWrapIt() {
    WonMessage msg1 = new WonMessageBuilder()
      .setMessageURI(MSG_URI_1)
      .addContent(CONTENT_GRAPH_URI_1, createDummyContent(), null)
      .setWonMessageType(WonMessageType.HINT_MESSAGE)
      .build();
    return new WonMessageBuilder()
      .wrap(msg1)
      .setReceiverURI(RECEIVER_URI_1)
      .addContent(CONTENT_GRAPH_URI_1, createDifferentDummyContent(), null)
      .build();
  }

  @Test
  public void test_copy_yields_correct_number_of_content_graphs(){
    WonMessage msg = createMessageAndCopyEnvelopeAndContent();
    Assert.assertEquals(2, Iterators.size(msg.getMessageContent().listNames()));
  }

  @Test
  public void test_copy_replaces_messageURI(){
    final WonMessage msg = createMessageAndCopyEnvelopeAndContent();
    Iterator<RDFNode> subjectIt = RdfUtils.visit(
      msg.getMessageContent(), new RdfUtils.ModelVisitor<RDFNode>()
      {
        @Override
        public RDFNode visit(final Model model) {
          StmtIterator it = model.listStatements(model.getResource(msg.getMessageURI().toString()), RDF.type,
            (RDFNode) null);
          if (!it.hasNext()) return null;
          return it.nextStatement().getSubject();
        }
      }
    );
    //this finds 3 triples: one in each of the two content graphs, 1 in the default graph.
    Assert.assertEquals(3, Iterators.size(subjectIt));
  }


  private WonMessage createMessageAndCopyEnvelopeAndContent() {
    WonMessage msg1 = new WonMessageBuilder()
      .setMessageURI(MSG_URI_1)
      .addContent(CONTENT_GRAPH_URI_1, createDummyContent(), null)
      .setWonMessageType(WonMessageType.HINT_MESSAGE)
      .build();
    return new WonMessageBuilder()
      .setMessageURI(MSG_URI_2)
      .copyEnvelopeFromWonMessage(msg1)
      .copyContentFromMessageReplacingMessageURI(msg1)
      .setReceiverURI(RECEIVER_URI_1)
      .addContent(CONTENT_GRAPH_URI_1, createDifferentDummyContent(), null)
      .build();
  }

  private Model createDummyContent(){
    Model dummyContent = ModelFactory.createDefaultModel();
    dummyContent.createResource(MSG_URI_1.toString(), dummyContent.createResource(TYPE_URI_1.toString()));
    return dummyContent;
  }

  private Model createDifferentDummyContent(){
    Model dummyContent = ModelFactory.createDefaultModel();
    dummyContent.createResource(MSG_URI_2.toString(), dummyContent.createResource(TYPE_URI_2.toString()));
    return dummyContent;
  }

}
