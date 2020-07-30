package won.protocol.vocabulary;

import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;

import java.net.URI;

public final class ResourceWrapper {
    private final String resourceString;
    private final Resource resource;
    private final URI resourceURI;

    private ResourceWrapper(String resourceString) {
        this.resourceString = resourceString;
        this.resourceURI = URI.create(resourceString);
        this.resource = ModelFactory.createDefaultModel().createResource(resourceString);
    }

    private ResourceWrapper(Resource resource) {
        this.resourceString = resource.toString();
        this.resourceURI = URI.create(this.resourceString);
        this.resource = resource;
    }

    private ResourceWrapper(URI resourceURI) {
        this.resourceString = resourceURI.toString();
        this.resourceURI = resourceURI;
        this.resource = ModelFactory.createDefaultModel().createResource(resourceString);
    }

    public static ResourceWrapper create(String resourceString) {
        return new ResourceWrapper(resourceString);
    }

    static ResourceWrapper create(Resource resource) {
        return new ResourceWrapper(resource);
    }

    static ResourceWrapper create(URI resourceURI) {
        return new ResourceWrapper(resourceURI);
    }

    public String toString() {
        return resourceString;
    }

    public URI getUri() {
        return resourceURI;
    }

    public Resource getResource() {
        return resource;
    }
}
