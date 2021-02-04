package won.matcher.solr.evaluation;

import com.github.jsonldjava.core.JsonLdError;
import org.apache.commons.io.FilenameUtils;
import org.apache.jena.query.Dataset;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import won.matcher.solr.hints.HintBuilder;
import won.matcher.solr.index.AtomIndexer;
import won.matcher.solr.query.TestMatcherQueryExecutor;
import won.matcher.solr.query.factory.BasicAtomQueryFactory;
import won.matcher.solr.query.factory.TestAtomQueryFactory;
import won.matcher.utils.tensor.TensorMatchingData;
import won.protocol.exception.IncorrectPropertyCountException;
import won.protocol.util.DefaultAtomModelWrapper;
import won.protocol.util.RdfUtils;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by hfriedrich on 05.08.2016. This class can be used to do evaluation
 * of the quality of matching of Solr querying. It reads atoms mail files from
 * supply and demand directories on the hard drive. Subject will be mapped to
 * title and content will be mapped to description. These atoms can be written
 * to the Solr index and queried. The class uses a solr query executor that
 * defines the Solr query to test for matching. The class can build to tensors
 * that can be used by the "wonpreprocessing" project to evaluate the quality of
 * the matching. The connection tensor has all ground truth connections between
 * all atoms (read from the connections file). The prediction tensor has all
 * computed matches between all atoms using the solr querying. These tensor
 * slices can be compared by the "wonpreprocessing" project to compute
 * statistical evaluation measures like precision, recall, accuracy and f-score.
 */
@Component
public class SolrMatcherEvaluation {
    @Autowired
    TestMatcherQueryExecutor queryExecutor;
    @Autowired
    AtomIndexer atomIndexer;
    @Autowired
    HintBuilder hintBuilder;
    @Autowired
    private MailDirAtomProducer seeksAtomProducer;
    @Autowired
    private MailDirAtomProducer isAtomProducer;
    private String outputDir;
    private String connectionsFile;
    private Map<String, Dataset> atomFileDatasetMap;
    private TensorMatchingData matchingDataConnections;
    private TensorMatchingData matchingDataPredictions;

    public SolrMatcherEvaluation() {
        matchingDataConnections = new TensorMatchingData();
        matchingDataPredictions = new TensorMatchingData();
        atomFileDatasetMap = new HashMap<>();
    }

    public static String createAtomId(Dataset atom) {
        String title = "";
        String description = "";
        try {
            DefaultAtomModelWrapper atomModelWrapper = new DefaultAtomModelWrapper(atom);
            title = atomModelWrapper.getAllTitles().iterator().next();
            title = title.replaceAll("[^A-Za-z0-9 ]", "_");
            title = title.replaceAll("NOT", "_");
            title = title.replaceAll("AND", "_");
            title = title.replaceAll("OR", "_");
            description = atomModelWrapper.getSomeDescription();
        } catch (IncorrectPropertyCountException e) {
            // do nothing
        }
        if (title.isEmpty()) {
            throw new IllegalArgumentException("atom has no title!!");
        }
        return title + "_" + (title + description).hashCode();
    }

    public void setSeeksAtomProducer(final MailDirAtomProducer seeksAtomProducer) {
        this.seeksAtomProducer = seeksAtomProducer;
    }

    public void setIsAtomProducer(final MailDirAtomProducer isAtomProducer) {
        this.isAtomProducer = isAtomProducer;
    }

    @PostConstruct
    public void init() throws IOException {
        initAtomDir(seeksAtomProducer);
        initAtomDir(isAtomProducer);
    }

    private void initAtomDir(MailDirAtomProducer atomProducer) throws IOException {
        // read the atom files and add atoms to the tensor
        if (atomProducer.getDirectory() == null || !atomProducer.getDirectory().isDirectory()) {
            throw new IOException("Input folder not a directory: "
                            + ((atomProducer.getDirectory() != null) ? atomProducer.getDirectory().toString() : null));
        }
        while (!atomProducer.isExhausted()) {
            String atomFileName = atomProducer.getCurrentFileName();
            Dataset ds = atomProducer.create();
            String atomId = createAtomId(ds);
            if (atomProducer == seeksAtomProducer) {
                matchingDataConnections.addAtomAttribute("atomtype", atomId, "WANT");
                matchingDataPredictions.addAtomAttribute("atomtype", atomId, "WANT");
            } else if (atomProducer == isAtomProducer) {
                matchingDataConnections.addAtomAttribute("atomtype", atomId, "OFFER");
                matchingDataPredictions.addAtomAttribute("atomtype", atomId, "OFFER");
            }
            atomFileDatasetMap.put(FilenameUtils.removeExtension(atomFileName), ds);
        }
    }

    public void indexAtoms() throws IOException, JsonLdError {
        for (Dataset atom : atomFileDatasetMap.values()) {
            atomIndexer.indexAtomModel(atom.getDefaultModel(), createAtomId(RdfUtils.cloneDataset(atom)), true);
        }
    }

    public void buildConnectionTensor() throws IOException {
        // read the connection file and add connections to the tensor
        BufferedReader reader = new BufferedReader(new FileReader(connectionsFile));
        String line = "";
        List<String> atoms = new LinkedList<String>();
        while ((line = reader.readLine()) != null) {
            if (line.length() == 0) {
                // add a connection between the first atom and all following atoms until empty
                // line
                addConnection(atoms, false);
                atoms = new LinkedList<String>();
            } else {
                Dataset ds = atomFileDatasetMap.get(line.trim());
                if (ds == null) {
                    throw new IOException("Dataset is null for atom file entry: " + line.trim());
                }
                String atomId = createAtomId(ds);
                if (atomId == null) {
                    throw new IOException("Atom from connection file not found in atom directory: " + line);
                }
                atoms.add(atomId);
            }
        }
        addConnection(atoms, false);
        // output the tensor data
        matchingDataConnections.writeOutputFiles(outputDir + "/connections");
    }

    public void buildPredictionTensor() throws IOException, SolrServerException {
        for (Dataset atom : atomFileDatasetMap.values()) {
            for (String match : computeMatchingAtoms(atom)) {
                if (!matchingDataPredictions.getAtoms().contains(createAtomId(atom))
                                || !matchingDataPredictions.getAtoms().contains(match)) {
                    throw new IOException(
                                    "No atom found in input directory for connection specified in connection file:  \n"
                                                    + createAtomId(atom) + "\n" + match);
                }
                matchingDataPredictions.addAtomConnection(createAtomId(atom), match, false);
            }
        }
        // output the tensor data
        matchingDataPredictions.writeOutputFiles(outputDir + "/predictions");
    }

    private List<String> computeMatchingAtoms(Dataset atom) throws IOException, SolrServerException {
        TestAtomQueryFactory atomQuery = new TestAtomQueryFactory(atom);
        SolrDocumentList docs = queryExecutor.executeAtomQuery(atomQuery.createQuery(), 20, null,
                        new BasicAtomQueryFactory(atom).createQuery());
        SolrDocumentList matchedDocs = hintBuilder.calculateMatchingResults(docs);
        List<String> matchedAtoms = new LinkedList<>();
        for (SolrDocument doc : matchedDocs) {
            String matchedAtomId = doc.getFieldValue("id").toString();
            matchedAtoms.add(matchedAtomId);
        }
        return matchedAtoms;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    public void setConnectionsFile(String connectionsFile) {
        this.connectionsFile = connectionsFile;
    }

    private void addConnection(List<String> atoms, boolean ignoreAtomsNotFound) throws IOException {
        for (int i = 1; i < atoms.size(); i++) {
            String atom1 = atoms.get(0);
            String atom2 = atoms.get(i);
            if (!matchingDataConnections.getAtoms().contains(atom1)
                            || !matchingDataConnections.getAtoms().contains(atom2)) {
                if (!ignoreAtomsNotFound) {
                    throw new IOException(
                                    "No atom found in input directory for connection specified in connection file:  \n"
                                                    + atom1 + "\n" + atom2);
                }
            }
            matchingDataConnections.addAtomConnection(atom1, atom2, false);
        }
    }
}
