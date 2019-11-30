package won.protocol.util.linkeddata;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import won.protocol.message.WonMessageUtils;

public class IncludedWonOntologies {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String baseUriMain = "https://w3id.org/won";
    private static final String baseUriExt = "https://w3id.org/won/ext";
    private static Map<URI, String> ontoResources = new HashMap<>();
    static {
        ontoResources.put(URI.create(baseUriMain + "/core"), "ontology/won-core.ttl");
        ontoResources.put(URI.create(baseUriMain + "/agreement"), "ontology/won-agreement.ttl");
        ontoResources.put(URI.create(baseUriMain + "/content"), "ontology/won-content.ttl");
        ontoResources.put(URI.create(baseUriMain + "/matching"), "ontology/won-matching.ttl");
        ontoResources.put(URI.create(baseUriMain + "/message"), "ontology/won-message.ttl");
        ontoResources.put(URI.create(baseUriMain + "/modification"), "ontology/won-modification.ttl");
        ontoResources.put(URI.create(baseUriExt + "/bot"), "ontology/ext/won-ext-bot.ttl");
        ontoResources.put(URI.create(baseUriExt + "/buddy"), "ontology/ext/won-ext-buddy.ttl");
        ontoResources.put(URI.create(baseUriExt + "/chat"), "ontology/ext/won-ext-chat.ttl");
        ontoResources.put(URI.create(baseUriExt + "/demo"), "ontology/ext/won-ext-demo.ttl");
        ontoResources.put(URI.create(baseUriExt + "/group"), "ontology/ext/won-ext-group.ttl");
        ontoResources.put(URI.create(baseUriExt + "/hold"), "ontology/ext/won-ext-hold.ttl");
        ontoResources.put(URI.create(baseUriExt + "/review"), "ontology/ext/won-ext-review.ttl");
        ontoResources.put(URI.create(baseUriExt + "/schema"), "ontology/ext/won-ext-schema.ttl");
        ontoResources.put(URI.create(baseUriExt + "/template"), "ontology/ext/won-ext-template.ttl");
    }

    public IncludedWonOntologies() {
    }

    /**
     * Returns the ontology with the specified URI if it has been loaded. Any
     * trailing fragment identifier is stripped off the URI before fetching the
     * ontology, so it is safe to use this method for a term in the ontology (if
     * term is identified by a fragments).
     * 
     * @param ontologyURI
     * @return
     */
    public static Optional<Model> get(URI ontologyURI) {
        logger.debug("Checking if we have an included ontology for resource {} ", ontologyURI);
        ontologyURI = WonMessageUtils.stripFragment(ontologyURI);
        logger.debug("Using ontology URI {} ", ontologyURI);
        if (ontoResources.containsKey(ontologyURI)) {
            logger.debug("Ontology present for URI {}, loading...", ontologyURI);
            Model m = loadFromClasspathResource(ontoResources.get(ontologyURI));
            if (logger.isDebugEnabled() && m != null) {
                logger.debug("Loaded ontology for URI {} (result contains {} triples)", ontologyURI, m.size());
            }
            return Optional.ofNullable(m);
        } else {
            logger.debug("No ontology included for URI {}", ontologyURI);
        }
        return Optional.empty();
    }

    public static Optional<Model> get(String uri) {
        return get(URI.create(uri));
    }

    private static Model loadFromClasspathResource(String resource) {
        logger.debug("Loading ontology from classpath resource {} ", resource);
        Model model = ModelFactory.createDefaultModel();
        Resource res = new ClassPathResource(resource);
        InputStream in;
        try {
            in = res.getInputStream();
            RDFDataMgr.read(model, in, Lang.TTL);
        } catch (IOException e) {
            logger.error("Error reading ontology from classpath resource {}", resource, e);
            return null;
        }
        return model;
    }
}
