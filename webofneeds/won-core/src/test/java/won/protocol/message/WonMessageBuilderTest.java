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
package won.protocol.message;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.google.common.collect.Iterators;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import won.protocol.util.RdfUtils;

import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static won.protocol.message.WonMessageBuilder.wrap;

public class WonMessageBuilderTest {
    private static final URI MSG_URI_1 = URI.create("http://example.com/msg/1234");
    private static final URI MSG_URI_2 = URI.create("http://example.com/msg/5678");
    private static final URI CONTENT_GRAPH_URI_1 = URI.create("http://example.com/content/1");
    private static final URI CONTENT_GRAPH_URI_2 = URI.create("http://example.com/content/2");
    private static final URI TYPE_URI_1 = URI.create("http://example.com/type/1");
    private static final URI TYPE_URI_2 = URI.create("http://example.com/type/2");
    private static final URI CONNECTION_URI_1 = URI.create("http://example.com/won/res/con/1");
    private static final URI CONNECTION_URI_2 = URI.create("http://example.com/won/res/con/2");
    private static final URI ATOM_URI_1 = URI.create("http://example.com/atom/1");
    private static final URI ATOM_URI_2 = URI.create("http://example.com/atom/2");

    @BeforeClass
    public static void setLogLevel() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
    }

    @Test
    public void test_content_is_referenced_from_envelope() {
        WonMessage msg1 = createMessageWithContent().build();
        List<String> contentGraphUris = msg1.getContentGraphURIs();
        Assert.assertFalse("envelope graph does not contain any content graph URI", contentGraphUris.isEmpty());
        Assert.assertEquals(1, msg1.getContentGraphURIs().size());
        for (String cgu : msg1.getContentGraphURIs()) {
            Assert.assertTrue("message does not contain content graph " + cgu + " referenced from envelope",
                            msg1.getCompleteDataset().containsNamedModel(cgu));
        }
    }

    @Test
    public void test_wrap_retains_envelope_graph_properties() {
        WonMessage msg2 = wrapMessage(createMessageWithContent().build()).build();
        Assert.assertEquals(WonMessageType.ATOM_HINT_MESSAGE, msg2.getMessageType());
    }

    @Test
    public void test_wrap_allows_new_envelope_graph_properties() {
        WonMessage msg2 = wrapMessage(createMessageWithContent().build()).build();
        Assert.assertEquals(CONNECTION_URI_1, msg2.getRecipientURI());
    }

    @Test
    public void test_wrap_retains_content_graphs() {
        WonMessage msg2 = wrapMessage(createMessageWithContent().build()).build();
        Assert.assertEquals(MSG_URI_1.toString(),
                        RdfUtils.findOne(msg2.getMessageContent(), new RdfUtils.ModelVisitor<String>() {
                            @Override
                            public String visit(final Model model) {
                                StmtIterator it = model.listStatements(null, RDF.type,
                                                model.getResource(TYPE_URI_1.toString()));
                                if (it.hasNext())
                                    return it.nextStatement().getSubject().asResource().toString();
                                return null;
                            }
                        }, false));
        Assert.assertEquals(TYPE_URI_1.toString(),
                RdfUtils.findOnePropertyFromResource(msg2.getMessageContent(), MSG_URI_1, RDF.type).asResource()
                                .getURI());
    }

    @Test
    public void test_wrap_allows_new_content_graphs() {
        WonMessage msg2 = addContentWithDifferentURI(wrapMessage(createMessageWithoutContent().build())).build();
        Assert.assertEquals(MSG_URI_2.toString(),
                        RdfUtils.findOne(msg2.getMessageContent(), new RdfUtils.ModelVisitor<String>() {
                            @Override
                            public String visit(final Model model) {
                                StmtIterator it = model.listStatements(null, RDF.type,
                                                model.getResource(TYPE_URI_2.toString()));
                                if (it.hasNext())
                                    return it.nextStatement().getSubject().asResource().toString();
                                return null;
                            }
                        }, false));
        Assert.assertEquals(TYPE_URI_2.toString(),
                RdfUtils.findOnePropertyFromResource(msg2.getMessageContent(), MSG_URI_2, RDF.type).asResource()
                                .getURI());
    }

    @Test
    public void test_wrapped_message_contains_correct_number_of_envelope_graphs() {
        WonMessage msg2 = addContent(wrapMessage(createMessageWithContent().build())).build();
        Assert.assertEquals(2, msg2.getEnvelopeGraphs().size());
    }

    @Test
    public void test_get_content_in_message_without_content() {
        final WonMessage msg = this.createMessageWithoutContent().build();
        check_get_content_in_message_without_content(msg);
    }

    @Test
    public void test_get_content_in_wrapped_message_without_content() {
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
    public void test_get_content_in_message_with_content() {
        final WonMessage msg = this.createMessageWithContent().build();
        check_get_content_in_message_with_content(msg);
    }

    @Test
    public void test_get_content_in_wrapped_message_with_content() {
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
        Assert.assertTrue("content different from the expected content",
                        findContentGraphInMessage(msg, createContent()));
    }

    @Test
    public void test_get_content_in_message_with_two_content_graphs() {
        final WonMessage msg = this.createMessageWithTwoContentGraphs().build();
        check_get_content_in_message_with_two_content_graphs(msg);
    }

    @Test
    public void test_get_content_in_message_with_content_dataset() {
        final WonMessage msg = this.createMessageWithContentDataset().build();
        check_get_content_in_message_with_content_dataset(msg);
    }

    @Test
    public void test_get_content_in_wrapped_message_with_two_content_graphs() {
        WonMessage msg = this.createMessageWithTwoContentGraphs().build();
        msg = wrapMessage(msg).build();
        check_get_content_in_message_with_two_content_graphs(msg);
    }

    @Test
    public void test_envelope_type_exists() {
        WonMessageBuilder msgbuilder = this.createMessageWithEnvelopeType();
        WonMessage msg = msgbuilder.build();
        Assert.assertEquals(WonMessageDirection.FROM_EXTERNAL, msg.getEnvelopeType());
    }

    public void check_get_content_in_message_with_two_content_graphs(final WonMessage msg) {
        Dataset actualContentDataset = msg.getMessageContent();
        Assert.assertTrue("messageContent dataset of message with content has non-empty default graph",
                        actualContentDataset.getDefaultModel().isEmpty());
        Set<String> names = new HashSet<String>();
        Iterators.addAll(names, actualContentDataset.listNames());
        Assert.assertEquals("incorrect number of named graphs", names.size(), 2);
        Assert.assertTrue("content different from the expected content",
                        findContentGraphInMessage(msg, createContent()));
        Assert.assertTrue("content different from the expected 'different' content",
                        findContentGraphInMessage(msg, createDifferentContent()));
    }

    public void check_get_content_in_message_with_content_dataset(final WonMessage msg) {
        Dataset actualContentDataset = msg.getMessageContent();
        Assert.assertTrue("messageContent dataset of message with content has non-empty default graph",
                        actualContentDataset.getDefaultModel().isEmpty());
        Set<String> names = new HashSet<String>();
        Iterators.addAll(names, actualContentDataset.listNames());
        Assert.assertEquals("incorrect number of named graphs", names.size(), 2);
        RdfUtils.toNamedModelStream(actualContentDataset, false).forEach(namedModel -> {
            String graphUri = namedModel.getName();
            Model model = namedModel.getModel();
            Assert.assertTrue("model does not contain its own graph uri",
                            model.containsResource(model.getResource(graphUri)));
            Statement stmt = model.getResource(graphUri).getProperty(SKOS.related);
            Assert.assertNotNull(stmt);
            RDFNode otherGraph = stmt.getObject();
            Assert.assertTrue("Reference to other model not found",
                            actualContentDataset.containsNamedModel(otherGraph.asResource().getURI()));
        });
    }

    public boolean findContentGraphInMessage(final WonMessage msg, final Model expectedContent) {
        Dataset actualContentDataset = msg.getMessageContent();
        boolean foundIt = false;
        for (Iterator<String> nameit = actualContentDataset.listNames(); nameit.hasNext();) {
            foundIt = expectedContent.isIsomorphicWith(actualContentDataset.getNamedModel(nameit.next()));
            if (foundIt)
                break;
        }
        return foundIt;
    }

    private WonMessageBuilder createMessageWithEnvelopeType() {
        return new WonMessageBuilder(MSG_URI_1).setWonMessageType(WonMessageType.CLOSE)
                        .setWonMessageDirection(WonMessageDirection.FROM_EXTERNAL);
    }

    private WonMessageBuilder createMessageWithoutContent() {
        return new WonMessageBuilder(MSG_URI_1).setWonMessageType(WonMessageType.ATOM_HINT_MESSAGE)
                        .setHintTargetAtomURI(ATOM_URI_1).setHintScore(0.2)
                        .setWonMessageDirection(WonMessageDirection.FROM_OWNER);
    }

    private WonMessageBuilder addContent(WonMessageBuilder builder) {
        return builder.addContent(createDifferentContent());
    }

    private WonMessageBuilder addContentWithDifferentURI(WonMessageBuilder builder) {
        return builder.addContent(createDifferentContent());
    }

    private WonMessageBuilder wrapMessage(final WonMessage msg1) {
        return wrap(msg1).setRecipientURI(CONNECTION_URI_1).setWonMessageDirection(WonMessageDirection.FROM_EXTERNAL);
    }

    private WonMessageBuilder createMessageWithContent() {
        return new WonMessageBuilder(MSG_URI_1).addContent(createContent())
                        .setWonMessageType(WonMessageType.ATOM_HINT_MESSAGE).setHintTargetAtomURI(ATOM_URI_1)
                        .setHintScore(0.5).setWonMessageDirection(WonMessageDirection.FROM_OWNER);
    }

    private WonMessageBuilder createMessageWithTwoContentGraphs() {
        return new WonMessageBuilder(MSG_URI_1).addContent(createContent()).addContent(createDifferentContent())
                        .setWonMessageType(WonMessageType.ATOM_HINT_MESSAGE).setHintTargetAtomURI(ATOM_URI_1)
                        .setHintScore(0.2).setWonMessageDirection(WonMessageDirection.FROM_OWNER);
    }

    private WonMessageBuilder createMessageWithContentDataset() {
        return new WonMessageBuilder(MSG_URI_1).addContent(createContentDataset())
                        .setWonMessageType(WonMessageType.ATOM_HINT_MESSAGE).setHintTargetAtomURI(ATOM_URI_1)
                        .setHintScore(0.2).setWonMessageDirection(WonMessageDirection.FROM_OWNER);
    }

    private Model createContent() {
        Model Content = ModelFactory.createDefaultModel();
        Content.createResource(MSG_URI_1.toString(), Content.createResource(TYPE_URI_1.toString()));
        return Content;
    }

    private Model createDifferentContent() {
        Model Content = ModelFactory.createDefaultModel();
        Content.createResource(MSG_URI_2.toString(), Content.createResource(TYPE_URI_2.toString()));
        return Content;
    }

    private Dataset createContentDataset() {
        Dataset contentDataset = DatasetFactory.createGeneral();
        Model contentGraph = createContent();
        contentGraph.getResource(CONTENT_GRAPH_URI_1.toString()).addProperty(SKOS.related,
                        contentGraph.getResource(CONTENT_GRAPH_URI_2.toString()));
        contentDataset.addNamedModel(CONTENT_GRAPH_URI_1.toString(), contentGraph);
        contentGraph = createDifferentContent();
        contentGraph.getResource(CONTENT_GRAPH_URI_2.toString()).addProperty(SKOS.related,
                        contentGraph.getResource(CONTENT_GRAPH_URI_1.toString()));
        contentDataset.addNamedModel(CONTENT_GRAPH_URI_2.toString(), contentGraph);
        return contentDataset;
    }
}
