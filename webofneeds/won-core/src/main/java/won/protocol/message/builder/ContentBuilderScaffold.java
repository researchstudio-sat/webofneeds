package won.protocol.message.builder;

import org.apache.jena.graph.Graph;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;

abstract class ContentBuilderScaffold<THIS extends ContentBuilderScaffold<THIS, PARENT>, PARENT extends BuilderScaffold<PARENT, ?>>
                extends BuilderScaffold<THIS, PARENT> {
    public ContentBuilderScaffold(PARENT parent) {
        super(parent);
    }

    /**
     * Adds the specified model as a content graph to the message.
     *
     * @param content
     * @return the parent builder
     */
    public PARENT model(Model content) {
        builder.content(content);
        return parent.get();
    }

    /**
     * Adds the specified graph as a content graph to the message.
     *
     * @param content
     * @return the parent builder
     */
    public PARENT graph(Graph content) {
        builder.content(content);
        return parent.get();
    }

    /**
     * Adds the specified acl graph to the message.
     *
     * @param aclGraph
     * @return the parent builder
     */
    public PARENT aclGraph(Graph aclGraph) {
        builder.aclGraph(aclGraph);
        return parent.get();
    }

    /**
     * <p>
     * Exposes the RDF resource that identifies the message itself. The specified
     * Consumer is given the resource that identifies the message. Any data added to
     * the resource is put in one of the message's content graphs. If there already
     * is one content graph, that one is used. If there is none yet, a new one is
     * created. If there are more than one, another one is added.
     * </p>
     * <p>
     * For example, to add a text message this way::
     * 
     * <pre>
     * .withMessageResource(r -> r.addProperty(WONCON.text, "Hello, World!"))
     * </pre>
     * 
     * To propose the content of previous message with URI <code>prevURI</code>
     * 
     * <pre>
     * .withMessageResource(r -> r.addProperty(WONAGR.proposes, r.getResource(prevURI))
     * </pre>
     * </p>
     * 
     * @return the parent builder
     */
    public PARENT withMessageResource(Consumer<Resource> dataAdder) {
        builder.withMessageResource(dataAdder);
        return parent.get();
    }

    /**
     * Exposes the content graph, which is passed to the specified Consumer. If
     * there already is one content graph, that one is used. If there is none yet, a
     * new one is created. If there are more than one, another one is added, because
     * we cannot decide for any of the existing ones.
     */
    public PARENT withContentGraph(Consumer<Model> dataAdder) {
        builder.withContentGraph(dataAdder);
        return parent.get();
    }

    /**
     * <p>
     * Adds a Text message to one of the message's content graphs. If only one graph
     * is present, the text message is added to that graph. If more than one graph
     * is present, and hence we cannot decide for any one of them, a new content
     * graph is created for the text. If no content graphs are present, a new one is
     * created. If the text is null or empty, this call has no effect.
     * </p>
     * <p>
     *
     * <pre>
     * .text("Hello, World!")
     * </pre>
     *
     * is equivalent to:
     *
     * <pre>
     * .withMessageResource(r -> r.addProperty(WONCON.text, "Hello, World!"))
     * </pre>
     * </p>
     *
     * @param text
     * @return the parent builder
     */
    public PARENT text(String text) {
        builder.textMessage(text);
        return parent.get();
    }

    /**
     * Adds all models in the dataset as content graphs to the message, including
     * the default model.
     * 
     * @param content
     * @return
     */
    public PARENT dataset(Dataset content) {
        builder.content(content);
        return parent.get();
    }

    /**
     * Adds the specified property once with each value.
     * 
     * @param property the property to add
     * @param uriValues the values to add as objects
     * @return
     */
    public PARENT addToMessageResource(Property property, URI... uriValues) {
        builder.addToMessageResource(property, Arrays.asList(uriValues));
        return parent.get();
    }

    /**
     * Adds the specified property once with each value.
     * 
     * @param property the property to add
     * @param uriValues the values to add as objects
     * @return
     */
    public PARENT addToMessageResource(Property property, Collection<URI> uriValues) {
        builder.addToMessageResource(property, uriValues);
        return parent.get();
    }
}