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
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;
import org.junit.Assert;
import org.junit.Test;
import won.protocol.util.RdfUtils;

import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class WonMessageBuilderTest
{
  private static final URI MSG_URI_1 = URI.create("http://example.com/msg/1234");
  private static final URI MSG_URI_2 = URI.create("http://example.com/msg/5678");
  private static final URI CONTENT_GRAPH_URI_1 = URI.create("http://example.com/content/1");
  private static final URI CONTENT_GRAPH_URI_2 = URI.create("http://example.com/content/2");
  private static final URI TYPE_URI_1 = URI.create("http://example.com/type/1");
  private static final URI TYPE_URI_2 = URI.create("http://example.com/type/2");
  private static final URI CONNECTION_URI_1 = URI.create("http://example.com/won/res/con/1");
  private static final URI CONNECTION_URI_2 = URI.create("http://example.com/won/res/con/2");

  @Test
  public void test_content_is_referenced_from_envelope(){
    WonMessage msg1 = createMessageWithContent().build();
    List<String> contentGraphUris = msg1.getContentGraphURIs();
    Assert.assertFalse("envelope graph does not contain any content graph URI", contentGraphUris.isEmpty());
    Assert.assertEquals(1, msg1.getContentGraphURIs().size());
    for (String cgu : msg1.getContentGraphURIs() ){
      Assert.assertTrue("message does not contain content graph " + cgu +" referenced from envelope",
        msg1.getCompleteDataset().containsNamedModel(cgu));
    }
  }

  @Test
  public void test_wrap_retains_envelope_graph_properties(){
    WonMessage msg2 = wrapMessage(createMessageWithContent().build()).build();
    Assert.assertEquals(WonMessageType.HINT_MESSAGE, msg2.getMessageType());
  }

  @Test
  public void test_wrap_allows_new_envelope_graph_properties(){
    WonMessage msg2 = wrapMessage(createMessageWithContent().build()).build();
    Assert.assertEquals(CONNECTION_URI_1, msg2.getReceiverURI());
  }

  @Test
  public void test_wrap_retains_content_graphs(){
    WonMessage msg2 = wrapMessage(createMessageWithContent().build()).build();
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
    WonMessage msg2 = addContentWithDifferentURI(wrapMessage(createMessageWithoutContent().build())).build();
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
  public void test_wrapped_message_contains_correct_number_of_envelope_graphs() {
    WonMessage msg2 = addContent(wrapMessage(createMessageWithContent().build())).build();
    Assert.assertEquals(2, msg2.getEnvelopeGraphs().size());
  }






  @Test
  public void test_copy_yields_correct_number_of_content_graphs(){
    WonMessage msg = copyEnvelopeAndContent(createMessageWithContent().build()).build();
    Assert.assertEquals(2, Iterators.size(msg.getMessageContent().listNames()));
  }

  @Test
  public void test_copy_replaces_messageURI(){
    final WonMessage msg = copyEnvelopeAndContent(createMessageWithContent().build()).build();
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


  @Test
  public void test_get_content_in_message_without_content(){
    final WonMessage msg = this.createMessageWithoutContent().build();
    check_get_content_in_message_without_content(msg);
  }

  @Test
  public void test_get_content_in_wrapped_message_without_content(){
    final WonMessage msg = wrapMessage(createMessageWithoutContent().build()).build();
    check_get_content_in_message_without_content(msg);
  }


  public void check_get_content_in_message_without_content(final WonMessage msg) {
    Dataset content = msg.getMessageContent();
    Assert.assertTrue("messageContent dataset of message without content has non-empty default graph",
      content.getDefaultModel().isEmpty());
    Assert.assertFalse("messageContent dataset of message without content has named graphs",
      content.listNames().hasNext());
  }

  @Test
  public void test_get_content_in_message_with_content(){
    final WonMessage msg = this.createMessageWithContent().build();
    check_get_content_in_message_with_content(msg);
  }

  @Test
  public void test_get_content_in_wrapped_message_with_content(){
    final WonMessage msg = wrapMessage(createMessageWithContent().build()).build();
    check_get_content_in_message_with_content(msg);
  }

  public void check_get_content_in_message_with_content(final WonMessage msg) {
    Dataset actualContentDataset = msg.getMessageContent();
    Assert.assertTrue("messageContent dataset of message with content has non-empty default graph",
      actualContentDataset.getDefaultModel().isEmpty());
    Assert.assertTrue("messageContent dataset of message with content has no named graphs",
      actualContentDataset.listNames().hasNext());
    Set<String> names = new HashSet<String>();
    Iterators.addAll(names, actualContentDataset.listNames());
    Assert.assertEquals("incorrect number of named graphs", names.size(), 1);
    Assert.assertTrue("content different from the expected content", findContentGraphInMessage(msg, createContent()));
  }

  @Test
  public void test_get_content_in_message_with_two_content_graphs(){
    final WonMessage msg = this.createMessageWithTwoContentGraphs().build();
    check_get_content_in_message_with_two_content_graphs(msg);
  }

  @Test
  public void test_get_content_in_wrapped_message_with_two_content_graphs(){
    WonMessage msg = this.createMessageWithTwoContentGraphs().build();
    msg = wrapMessage(msg).build();
    check_get_content_in_message_with_two_content_graphs(msg);
  }

  @Test
  public void test_copyInboundWonMessageForLocalStorage(){
    WonMessageBuilder msg = this.createMessageWithContent();
    WonMessage msg2 = WonMessageBuilder.copyInboundNodeToNodeMessageAsNodeToOwnerMessage(MSG_URI_2, CONNECTION_URI_2,
                                                                                         msg.build());

  }
  @Test
  public void test_envelope_type_exists(){
    WonMessageBuilder msgbuilder = this.createMessageWithEnvelopeType();
    WonMessage msg =  msgbuilder.build();
    Assert.assertEquals(WonMessageDirection.FROM_EXTERNAL, msg.getEnvelopeType());

  }

  public void check_get_content_in_message_with_two_content_graphs(final WonMessage msg) {
    Dataset actualContentDataset = msg.getMessageContent();
    Assert.assertTrue("messageContent dataset of message with content has non-empty default graph",
      actualContentDataset.getDefaultModel().isEmpty());
    Set<String> names = new HashSet<String>();
    Iterators.addAll(names, actualContentDataset.listNames());
    Assert.assertEquals("incorrect number of named graphs", names.size(), 2);
    Assert.assertTrue("content different from the expected content", findContentGraphInMessage(msg, createContent()));
    Assert.assertTrue("content different from the expected 'different' content", findContentGraphInMessage(msg,
      createDifferentContent()));

  }

  public boolean findContentGraphInMessage(final WonMessage msg, final Model expectedContent) {
    Dataset actualContentDataset = msg.getMessageContent();
    boolean foundIt = false;
    for (Iterator<String> nameit = actualContentDataset.listNames(); nameit.hasNext();) {
      foundIt = expectedContent.isIsomorphicWith(
        actualContentDataset.getNamedModel(nameit.next()));
      if (foundIt) break;
    }
    return foundIt;
  }

  private WonMessageBuilder createMessageWithEnvelopeType(){
    return new WonMessageBuilder()
      .setMessageURI(MSG_URI_1)
      .setWonMessageType(WonMessageType.CLOSE)
      .setWonMessageDirection(WonMessageDirection.FROM_EXTERNAL);
  }
  private WonMessageBuilder createMessageWithoutContent(){
    return new WonMessageBuilder()
      .setMessageURI(MSG_URI_1)
      .setWonMessageType(WonMessageType.HINT_MESSAGE)
      .setWonMessageDirection(WonMessageDirection.FROM_OWNER);
  }


  private WonMessageBuilder addContent(WonMessageBuilder builder) {
    return builder.addContent(createDifferentContent(), null);
  }

  private WonMessageBuilder addContentWithDifferentURI(WonMessageBuilder builder) {
    return builder.addContent(createDifferentContent(), null);
  }

  private WonMessageBuilder wrapMessage(final WonMessage msg1) {
    return new WonMessageBuilder()
      .wrap(msg1)
      .setReceiverURI(CONNECTION_URI_1)
      .setWonMessageDirection(WonMessageDirection.FROM_EXTERNAL);
  }

  private WonMessageBuilder createMessageWithContent(){
      return new WonMessageBuilder()
        .setMessageURI(MSG_URI_1)
        .addContent(createContent(), null)
        .setWonMessageType(WonMessageType.HINT_MESSAGE)
        .setWonMessageDirection(WonMessageDirection.FROM_OWNER);
  }

  private WonMessageBuilder createMessageWithTwoContentGraphs(){
    return new WonMessageBuilder()
      .setMessageURI(MSG_URI_1)
      .addContent(createContent(), null)
      .addContent(createDifferentContent(), null)
      .setWonMessageType(WonMessageType.HINT_MESSAGE)
      .setWonMessageDirection(WonMessageDirection.FROM_OWNER);
  }

  private WonMessageBuilder copyEnvelopeAndContent(WonMessage msg) {
    return new WonMessageBuilder()
      .setMessageURI(MSG_URI_2)
      .copyEnvelopeFromWonMessage(msg)
      .copyContentFromMessageReplacingMessageURI(msg)
      .setReceiverURI(CONNECTION_URI_1)
      .addContent(createDifferentContent(), null)
      .setWonMessageDirection(WonMessageDirection.FROM_EXTERNAL);
  }

  private Model createContent(){
    Model Content = ModelFactory.createDefaultModel();
    Content.createResource(MSG_URI_1.toString(), Content.createResource(TYPE_URI_1.toString()));
    return Content;
  }

  private Model createDifferentContent(){
    Model Content = ModelFactory.createDefaultModel();
    Content.createResource(MSG_URI_2.toString(), Content.createResource(TYPE_URI_2.toString()));
    return Content;
  }

}
