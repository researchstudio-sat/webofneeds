package won.protocol.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFWriterRegistry;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.shared.impl.PrefixMappingImpl;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import won.protocol.util.pretty.ConversationDatasetWriterFactory;
import won.protocol.util.pretty.Lang_WON;
import won.protocol.vocabulary.CERT;
import won.protocol.vocabulary.SCHEMA;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONCON;
import won.protocol.vocabulary.WONMATCH;
import won.protocol.vocabulary.WONMSG;
import won.protocol.vocabulary.WXBUDDY;
import won.protocol.vocabulary.WXCHAT;
import won.protocol.vocabulary.WXGROUP;
import won.protocol.vocabulary.WXHOLD;
import won.protocol.vocabulary.WXREVIEW;

/**
 * Tests if it is viable to set exactly those prefixes that are used in a a
 * dataset to improve readablilty. Result: for a standard atom dataset, the
 * overhead is 30ms - not justifiable.
 * 
 * @author fkleedorfer
 */
public class Prefixer {
    private static final Pattern PREFIX_PATTERN = Pattern.compile("[^/#]+$");
    static {
        if (!RDFWriterRegistry.contains(Lang_WON.TRIG_WON_CONVERSATION)) {
            RDFWriterRegistry.register(Lang_WON.TRIG_WON_CONVERSATION,
                            new ConversationDatasetWriterFactory());
        }
    }

    private static Dataset loadDatasetFromFile(String file) {
        try {
            Dataset dataset;
            dataset = DatasetFactory.createGeneral();
            RDFDataMgr.read(dataset, file);
            dataset.commit();
            return dataset;
        } catch (Exception e) {
            throw new IllegalStateException("could not load dataset " + file, e);
        }
    }

    private static void setLogLevel() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
    }

    public static PrefixMapping getPrefixes() {
        PrefixMapping prefixMapping = new PrefixMappingImpl();
        prefixMapping.setNsPrefix(WON.DEFAULT_PREFIX, WON.getURI());
        prefixMapping.setNsPrefix(WONMSG.DEFAULT_PREFIX, WONMSG.getURI());
        prefixMapping.setNsPrefix(WONCON.DEFAULT_PREFIX, WONCON.getURI());
        prefixMapping.setNsPrefix(WONMATCH.DEFAULT_PREFIX, WONMATCH.getURI());
        prefixMapping.setNsPrefix(WXCHAT.DEFAULT_PREFIX, WXCHAT.BASE_URI);
        prefixMapping.setNsPrefix(WXGROUP.DEFAULT_PREFIX, WXGROUP.BASE_URI);
        prefixMapping.setNsPrefix(WXHOLD.DEFAULT_PREFIX, WXHOLD.BASE_URI);
        prefixMapping.setNsPrefix(WXREVIEW.DEFAULT_PREFIX, WXREVIEW.BASE_URI);
        prefixMapping.setNsPrefix(WXBUDDY.DEFAULT_PREFIX, WXBUDDY.BASE_URI);
        prefixMapping.setNsPrefix("demo", "https://w3id.org/won/ext/demo#");
        prefixMapping.setNsPrefix("wx-bot", "https://w3id.org/won/ext/bot#");
        prefixMapping.setNsPrefix("wxs", "https://w3id.org/won/ext/schema#");
        prefixMapping.setNsPrefix("pogo", "https://w3id.org/won/ext/pogo#");
        prefixMapping.setNsPrefix(CERT.DEFAULT_PREFIX, CERT.getURI());
        prefixMapping.setNsPrefix("rdf", RDF.getURI());
        prefixMapping.setNsPrefix("rdfs", RDFS.getURI());
        prefixMapping.setNsPrefix("xsd", XSD.getURI());
        prefixMapping.setNsPrefix("dc", DC.getURI());
        prefixMapping.setNsPrefix("dct", DCTerms.getURI());
        prefixMapping.setNsPrefix("schema", SCHEMA.getURI());
        prefixMapping.setNsPrefix("sh", "http://www.w3.org/ns/shacl#");
        prefixMapping.setNsPrefix("rdfg", "http://www.w3.org/2004/03/trix/rdfg-1/");
        prefixMapping.setNsPrefix("owl", OWL.getURI());
        prefixMapping.setNsPrefix("vann", "http://purl.org/vocab/vann/");
        return prefixMapping;
    }

    private static Set<String> getBlacklist() {
        return Stream.of("https://w3id.org/won/ext/", "https://w3id.org/won/").collect(Collectors.toSet());
    }

    public static Dataset setPrefixes(Dataset ds) {
        Dataset result = RdfUtils.cloneDataset(ds);
        Model dm = result.getDefaultModel();
        StopWatch sw = new StopWatch();
        sw.start();
        dm.getNsPrefixMap().keySet().stream().forEach(dm::removeNsPrefix);
        Set<String> prefixes = RdfUtils.toStatementStream(result).flatMap(Prefixer::getPrefixes)
                        .collect(Collectors.toSet());
        Map<String, String> defaultPrefixes = getPrefixes().getNsPrefixMap();
        defaultPrefixes.entrySet().stream()
                        .forEach(entry -> {
                            if (prefixes.contains(entry.getValue())) {
                                dm.setNsPrefix(entry.getKey(), entry.getValue());
                                prefixes.remove(entry.getValue());
                            }
                        });
        final AtomicInteger cnt = new AtomicInteger(0);
        Map<String, String> nodeSpecificPrefixes = new HashMap<>();
        nodeSpecificPrefixes.put("event", "https://node.matchat.org/won/resource/event/");
        nodeSpecificPrefixes.put("conn", "https://node.matchat.org/won/resource/connection/");
        nodeSpecificPrefixes.put("atom", "https://node.matchat.org/won/resource/atom/");
        nodeSpecificPrefixes.put("node", "https://node.matchat.org/won/");
        nodeSpecificPrefixes.entrySet().stream().forEach(entry -> {
            if (prefixes.contains(entry.getValue())) {
                dm.setNsPrefix(entry.getKey(), entry.getValue());
                prefixes.remove(entry.getValue());
            }
        });
        prefixes.removeAll(getBlacklist());
        prefixes.stream()
                        .forEach(prefix -> dm.setNsPrefix("p" + cnt.getAndIncrement(), prefix));
        sw.stop();
        return result;
    }

    private static Stream<String> getPrefixes(Statement stmt) {
        Set<String> prefixes = new HashSet<>();
        getUriPrefix(stmt.getSubject()).map(prefixes::add);
        getUriPrefix(stmt.getPredicate()).map(prefixes::add);
        getUriPrefix(stmt.getObject()).map(prefixes::add);
        return prefixes.stream();
    }

    private static Optional<String> getUriPrefix(RDFNode node) {
        if (node.isURIResource()) {
            return Optional.ofNullable(toPrefix(node.asResource().getURI()));
        }
        if (node.isLiteral()) {
            if (node.asLiteral().getDatatypeURI() != null) {
                return Optional.ofNullable(toPrefix(node.asLiteral().getDatatypeURI()));
            }
        }
        return Optional.empty();
    }

    private static String toPrefix(String uri) {
        URI asURI = URI.create(uri);
        String fragment = asURI.getRawFragment();
        if (fragment != null) {
            return uri.substring(0, uri.length() - fragment.length());
        }
        String ssp = asURI.getSchemeSpecificPart();
        if (ssp != null) {
            int lastSlash = ssp.lastIndexOf('/');
            if (lastSlash > 0) {
                try {
                    return new URI(asURI.getScheme(), ssp.substring(0, lastSlash + 1), null).toString();
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
            int lastColon = ssp.lastIndexOf(':');
            if (lastColon > 0) {
                try {
                    return new URI(asURI.getScheme(), ssp.substring(0, lastColon + 1), null).toString();
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("usage: Prefixer {rdf-dataset}");
            System.err.println("   reads the {rdf-dataset}, replaces/adds prefixes, and ");
            System.err.println("   prints the result as TriG to stdout");
            System.exit(1);
        }
        setLogLevel();
        String filename = args[0];
        Dataset ds = Prefixer.loadDatasetFromFile(filename);
        ds = Prefixer.setPrefixes(ds);
        RDFDataMgr.write(System.out, ds, Lang_WON.TRIG_WON_CONVERSATION);
    }
}
