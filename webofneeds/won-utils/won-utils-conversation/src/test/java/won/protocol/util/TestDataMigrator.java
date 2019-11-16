package won.protocol.util;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.graph.Node;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.rdf.model.impl.StatementImpl;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.core.Quad;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import won.protocol.util.RdfUtils.Pair;
import won.protocol.util.pretty.Lang_WON;
import won.protocol.vocabulary.WONMSG;

public class TestDataMigrator {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private File inputDirectory;
    private File outputDirectory;
    private String commonPathPrefix;
    private SparqlSelectFunction<Pair<String>> findRemoteMsg = new SparqlSelectFunction<Pair<String>>(
                    "/won/protocol/util/migrate/remote-message-graph.rq",
                    solution -> new Pair<String>(solution.get("msg").asResource().getURI(),
                                    solution.get("rem").asResource().getURI()));
    private SparqlSelectFunction<String> findRemoteResponse = new SparqlSelectFunction<String>(
                    "/won/protocol/util/migrate/remote-response.rq",
                    solution -> ((QuerySolution) solution).get("msg").asResource().getURI());
    private SparqlSelectFunction<String> listFromExternalMsgs = new SparqlSelectFunction<String>(
                    "/won/protocol/util/migrate/list-fromExternal-msgs.rq",
                    solution -> solution.get("msg").asResource().getURI());

    public TestDataMigrator(File inputDirectory, File outputDirectory) {
        super();
        if (inputDirectory.equals(outputDirectory))
            throw new IllegalArgumentException("Input and output directoy have to be different");
        this.inputDirectory = inputDirectory;
        this.outputDirectory = outputDirectory;
        this.commonPathPrefix = getCommonPathPrefix();
    }

    private String getCommonPathPrefix() {
        char[] inPath = this.inputDirectory.getParentFile().getAbsolutePath().toCharArray();
        char[] outPath = this.outputDirectory.getParentFile().getAbsolutePath().toCharArray();
        int len = Math.min(inPath.length, outPath.length);
        StringBuilder commonPrefix = new StringBuilder();
        for (int i = 0; i < len; i++) {
            if (inPath[i] == outPath[i]) {
                commonPrefix.append(inPath[i]);
            } else {
                break;
            }
        }
        return commonPrefix.toString();
    }

    public TestDataMigrator(String inputDirectory, String outputDirectory) {
        this(new File(inputDirectory), new File(outputDirectory));
    }

    public void listInputFiles() {
        for (File infile : this.inputDirectory.listFiles()) {
            System.out.println(infile);
        }
    }

    /**
     * Deletes all 'remote messages', changes msg:isRemoteResponseTo to
     * msg:respondingTo references.
     * 
     * @param filename
     * @throws Exception
     */
    public void migrateFile(File inFile, File outFile) throws Exception {
        Objects.requireNonNull(inFile);
        Objects.requireNonNull(outFile);
        if (!inFile.isFile()) {
            throw new IllegalArgumentException("input file must be a file, but this isnt one: " + inFile);
        }
        if (!inFile.canRead()) {
            throw new IllegalArgumentException("cannot read input file " + inFile);
        }
        if (inFile.getAbsolutePath().equals(outFile.getAbsolutePath())) {
            throw new IllegalArgumentException("Input file and output file are the same, aborting.");
        }
        logger.debug("migrating {} from {} to {}", new Object[] { inFile.getName(),
                        removeBase(inFile.getParentFile().getAbsolutePath()),
                        removeBase(outFile.getParentFile().getAbsolutePath()) });
        Dataset ds = DatasetFactory.createGeneral();
        RDFDataMgr.read(ds, inFile.getAbsolutePath());
        final Dataset finalDs = ds;
        List<Pair<String>> remoteMessages = this.findRemoteMsg
                        .apply(ds);
        Map<String, String> remoteToOrig = remoteMessages.stream().distinct()
                        .collect(Collectors.toMap(Pair::getSecond, Pair::getFirst));
        Map<String, List<Statement>> statementsToMoveFromRemoteMsgToOrig = new HashMap<>();
        Set<String> remoteResponses = findRemoteResponse.apply(ds).stream().collect(Collectors.toSet());
        // remove the msg:isResponseTo triple from the remote responses (in the next
        // step, we'll rename the msg:isRemoteResponseTo triples
        ds = RdfUtils.toQuadStream(ds).filter(q -> {
            if (!q.getSubject().isURI()) {
                return true;
            }
            if (!remoteResponses.contains(q.getSubject().getURI())) {
                return true;
            }
            if ((WONMSG.getURI() + "isResponseTo").equals(q.getPredicate().getURI())) {
                return false;
            }
            return true;
        }).collect(RdfUtils.collectToDataset());
        ds = RdfUtils.toQuadStream(ds).map(quad -> {
            Node pred = quad.getPredicate();
            if (pred.isURI() && pred.getURI().equals(WONMSG.getURI() + "isRemoteResponseTo")) {
                pred = WONMSG.respondingTo.asNode();
            }
            if (pred.isURI() && pred.getURI().equals(WONMSG.getURI() + "isResponseTo")) {
                pred = WONMSG.respondingTo.asNode();
            }
            if (pred.isURI() && pred.getURI().equals(WONMSG.getURI() + "isResponseToMessageType")) {
                pred = WONMSG.respondingToMessageType.asNode();
            }
            return new Quad(quad.getGraph(), quad.getSubject(), pred, quad.getObject());
        }).collect(RdfUtils.collectToDataset());
        // remove the correspondingRemoteMessage triples
        ds = RdfUtils.toQuadStream(ds).filter(
                        q -> !(WONMSG.getURI() + "correspondingRemoteMessage").equals(q.getPredicate().getURI()))
                        .collect(RdfUtils.collectToDataset());
        // replace remote message uris in prev references with the org msgs and move
        // those triples to the orig msg envelopes
        RdfUtils.toQuadStream(ds).forEach(q -> {
            Node pred = q.getPredicate();
            if (pred.isURI() && pred.getURI().equals(WONMSG.previousMessage.getURI())) {
                pred = WONMSG.respondingTo.asNode();
                Node obj = q.getObject();
                if (obj.isURI()) {
                    String orig = remoteToOrig.get(obj.getURI());
                    if (orig != null) {
                        Statement stmt = new StatementImpl(new ResourceImpl(q.getSubject().getURI()),
                                        new PropertyImpl(q.getPredicate().getURI()), new ResourceImpl(orig));
                        List<Statement> stmts = statementsToMoveFromRemoteMsgToOrig.get(orig);
                        if (stmts == null) {
                            stmts = new ArrayList<>();
                        }
                        stmts.add(stmt);
                        statementsToMoveFromRemoteMsgToOrig.put(orig, stmts);
                    }
                }
            }
        });
        // remove remote messages (their graphs)
        Set<String> toRemove = remoteMessages
                        .stream()
                        .map(p -> p.getSecond())
                        .collect(Collectors.toSet());
        ds = removeMessages(ds, toRemove);
        // now add the triples we saved from the remote messages to the original
        // messages:
        // for each original message: find the quad that holds the [msg] msg:messageType
        // [msgType] triple. insert the set of statements in that graph
        // this creates a stream of quads, which in the end we add to the ds
        final Dataset finalDsNow = ds;
        List<Quad> moveddata = statementsToMoveFromRemoteMsgToOrig.entrySet().stream().flatMap(entry -> {
            Optional<Quad> quad = RdfUtils.toQuadStream(finalDsNow)
                            .filter(q -> q.getSubject().isURI()
                                            && entry.getKey().equals(q.getSubject().getURI())
                                            && WONMSG.messageType.getURI().equals(q.getPredicate().getURI()))
                            .findFirst();
            if (!quad.isPresent())
                return null;
            return entry.getValue().stream()
                            .map(stmt -> new Quad(quad.get().getGraph(), new ResourceImpl(entry.getKey()).asNode(),
                                            stmt.getPredicate().asNode(), stmt.getObject().asNode()));
        }).filter(x -> x != null)
                        .collect(Collectors.toList());
        ds = Stream.concat(RdfUtils.toQuadStream(ds), moveddata.stream()).collect(RdfUtils.collectToDataset());
        ds = removeMessages(ds, listFromExternalMsgs.apply(ds).stream().collect(Collectors.toSet()));
        logger.debug("writing rsult to {}", outFile);
        RDFDataMgr.write(new FileOutputStream(outFile), Prefixer.setPrefixes(ds), Lang_WON.TRIG_WON_CONVERSATION);
        // RDFDataMgr.write(System.out, ds, Lang.TRIG);
    }

    private Dataset removeMessages(final Dataset ds, Set<String> toRemove) {
        toRemove.forEach(prefix -> removeGraphsWithPrefix(ds, prefix));
        // delete all traces of remote messages from statements anywhere else:
        return RdfUtils.toQuadStream(ds).filter(q -> {
            if (!q.getSubject().isURI()) {
                return true;
            }
            if (toRemove.contains(q.getSubject().getURI())) {
                return false;
            }
            if (!q.getObject().isURI()) {
                return true;
            }
            if (toRemove.contains(q.getObject().getURI())) {
                return false;
            }
            return true;
        }).collect(RdfUtils.collectToDataset());
    }

    private void migrate(File inputFileOrDir, File outputFileOrDir) throws Exception {
        if (inputFileOrDir.isFile()) {
            migrateFile(inputFileOrDir, outputFileOrDir);
            return;
        }
        logger.debug("processing direcory: migrating from {} to {}", indicateBase(inputFileOrDir.getAbsolutePath()),
                        removeBase(outputFileOrDir.getAbsolutePath()));
        if (!inputFileOrDir.exists()) {
            throw new IllegalArgumentException("Input directoy doese not exist: " + inputFileOrDir);
        }
        if (!inputFileOrDir.canRead()) {
            throw new IllegalArgumentException("Can't read input directoy " + inputFileOrDir);
        }
        if (!outputFileOrDir.exists()) {
            logger.debug("creating output directory " + outputFileOrDir);
            outputFileOrDir.mkdirs();
        }
        for (File file : inputFileOrDir.listFiles((dir, name) -> name.matches("^.*\\.trig$"))) {
            migrate(file, new File(outputFileOrDir, file.getName()));
        }
    }

    public void migrate() throws Exception {
        migrate(inputDirectory, outputDirectory);
    }

    public String removeBase(String path) {
        return path.replaceFirst(Pattern.quote(commonPathPrefix), "[base]");
    }

    public String indicateBase(String path) {
        if (path.startsWith(commonPathPrefix)) {
            return "[base=" + commonPathPrefix + "]" + path.substring(commonPathPrefix.length());
        }
        return path;
    }

    private void removeGraphsWithPrefix(Dataset ds, String prefix) {
        logger.debug("removing graphs with prefix {} " + prefix);
        AtomicInteger count = new AtomicInteger(0);
        Set<String> toRemove = new HashSet<String>();
        Iterator<String> it = ds.listNames();
        while (it.hasNext()) {
            String name = it.next();
            if (name.startsWith(prefix)) {
                toRemove.add(name);
            }
        }
        toRemove.forEach(g -> {
            ds.removeNamedModel(g);
            count.incrementAndGet();
        });
        logger.debug("removed {} graphs", count.get());
    }

    public static void setLogLevel() {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory
                        .getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
    }

    public static void main(String[] args) throws Exception {
        setLogLevel();
        migrateFolder("src/test/resources/won/protocol/highlevel/agreements/input",
                        "src/test/resources/won/protocol/highlevel/agreements/input.messagerefactoring");
        migrateFolder("src/test/resources/won/protocol/petrinet",
                        "src/test/resources/won/protocol/petrinet.messagerefactoring");
        migrateFolder("src/test/resources/won/protocol/highlevel/proposals/input",
                        "src/test/resources/won/protocol/highlevel/proposals/input.messagerefactoring");
        migrateFolder("src/test/resources/won/utils/getagreements/input",
                        "src/test/resources/won/utils/getagreements/input.messagerefactoring");
    }

    private static void migrateFolder(String from, String to) throws Exception {
        TestDataMigrator migrator = new TestDataMigrator(from, to);
        migrator.migrate();
    }
}
