package won.protocol.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.shared.impl.PrefixMappingImpl;
import org.apache.jena.vocabulary.*;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;
import won.protocol.vocabulary.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Tests if it is viable to set exactly those prefixes that are used in a a
 * dataset to improve readablilty. Result: for a standard atom dataset, the
 * overhead is 30ms - not justifiable.
 * 
 * @author fkleedorfer
 */
public class Prefixer {
    private static final Pattern PREFIX_PATTERN = Pattern.compile("[^/#]+$");

    private Dataset loadDatasetFromFile(String file) {
        try {
            Dataset dataset = null;
            dataset = DatasetFactory.createGeneral();
            RDFDataMgr.read(dataset, file);
            dataset.commit();
            return dataset;
        } catch (Exception e) {
            throw new IllegalStateException("could not load dataset " + file, e);
        }
    }

    public static void setLogLevel() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
    }

    public PrefixMapping getPrefixes() {
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
        prefixMapping.setNsPrefix("wxs", "https://w3id.org/won/ext/schema#");
        prefixMapping.setNsPrefix("pogo", "https://w3id.org/won/ext/pogo#");
        prefixMapping.setNsPrefix(SFSIG.DEFAULT_PREFIX, SFSIG.getURI());
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

    public Set<String> getBlacklist() {
        return Stream.of("https://w3id.org/won/ext/", "https://w3id.org/won/").collect(Collectors.toSet());
    }

    public void setUsedPrefixes(Dataset ds) {
        Model dm = ds.getDefaultModel();
        StopWatch sw = new StopWatch();
        sw.start();
        dm.getNsPrefixMap().keySet().stream().forEach(dm::removeNsPrefix);
        Set<String> prefixes = RdfUtils.toStatementStream(ds).flatMap(this::getPrefixes).collect(Collectors.toSet());
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
    }

    public Stream<String> getPrefixes(Statement stmt) {
        Set<String> prefixes = new HashSet<>();
        getUriPrefix(stmt.getSubject()).map(prefixes::add);
        getUriPrefix(stmt.getPredicate()).map(prefixes::add);
        getUriPrefix(stmt.getObject()).map(prefixes::add);
        return prefixes.stream();
    }

    public Optional<String> getUriPrefix(RDFNode node) {
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

    public String toPrefix(String uri) {
        Matcher m = PREFIX_PATTERN.matcher(uri);
        if (!m.find()) {
            return null;
        }
        return m.replaceAll("");
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
        Prefixer app = new Prefixer();
        Dataset ds = app.loadDatasetFromFile(filename);
        app.setUsedPrefixes(ds);
        RDFDataMgr.write(System.out, ds, Lang.TRIG);
    }
}
