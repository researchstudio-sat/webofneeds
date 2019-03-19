package won.matcher.solr.evaluation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FilenameUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.jsonldjava.core.JsonLdError;

import won.matcher.solr.hints.HintBuilder;
import won.matcher.solr.index.NeedIndexer;
import won.matcher.solr.query.TestMatcherQueryExecutor;
import won.matcher.solr.query.factory.BasicNeedQueryFactory;
import won.matcher.solr.query.factory.TestNeedQueryFactory;
import won.matcher.utils.tensor.TensorMatchingData;
import won.protocol.exception.IncorrectPropertyCountException;
import won.protocol.util.DefaultNeedModelWrapper;

/**
 * Created by hfriedrich on 05.08.2016.
 *
 * This class can be used to do evaluation of the quality of matching of Solr querying. It reads needs mail files from
 * supply and demand directories on the hard drive. Subject will be mapped to title and content will be mapped to
 * description. These needs can be written to the Solr index and queried. The class uses a solr query executor that
 * defines the Solr query to test for matching. The class can build to tensors that can be used by the
 * "wonpreprocessing" project to evaluate the quality of the matching. The connection tensor has all ground truth
 * connections between all needs (read from the connections file). The prediction tensor has all computed matches
 * between all needs using the solr querying. These tensor slices can be compared by the "wonpreprocessing" project to
 * compute statistical evaluation measures like precision, recall, accuracy and f-score.
 */
@Component
public class SolrMatcherEvaluation {
    @Autowired
    TestMatcherQueryExecutor queryExecutor;

    @Autowired
    NeedIndexer needIndexer;

    @Autowired
    private MailDirNeedProducer seeksNeedProducer;

    @Autowired
    private MailDirNeedProducer isNeedProducer;

    @Autowired
    HintBuilder hintBuilder;

    private String outputDir;
    private String connectionsFile;

    private Map<String, Dataset> needFileDatasetMap;
    private TensorMatchingData matchingDataConnections;
    private TensorMatchingData matchingDataPredictions;

    public void setSeeksNeedProducer(final MailDirNeedProducer seeksNeedProducer) {
        this.seeksNeedProducer = seeksNeedProducer;
    }

    public void setIsNeedProducer(final MailDirNeedProducer isNeedProducer) {
        this.isNeedProducer = isNeedProducer;
    }

    public static String createNeedId(Dataset need) {

        String title = "";
        String description = "";

        try {
            DefaultNeedModelWrapper needModelWrapper = new DefaultNeedModelWrapper(need);
            title = needModelWrapper.getAllTitles().iterator().next();
            title = title.replaceAll("[^A-Za-z0-9 ]", "_");
            title = title.replaceAll("NOT", "_");
            title = title.replaceAll("AND", "_");
            title = title.replaceAll("OR", "_");
            description = needModelWrapper.getSomeDescription();
        } catch (IncorrectPropertyCountException e) {

            // do nothing
        }

        if (title.isEmpty()) {
            throw new IllegalArgumentException("need has no title!!");
        }

        return title + "_" + (title + description).hashCode();
    }

    public SolrMatcherEvaluation() {

        matchingDataConnections = new TensorMatchingData();
        matchingDataPredictions = new TensorMatchingData();
        needFileDatasetMap = new HashMap<>();
    }

    @PostConstruct
    public void init() throws IOException {

        initNeedDir(seeksNeedProducer);
        initNeedDir(isNeedProducer);
    }

    private void initNeedDir(MailDirNeedProducer needProducer) throws IOException {

        // read the need files and add needs to the tensor
        if (needProducer.getDirectory() == null || !needProducer.getDirectory().isDirectory()) {
            throw new IOException("Input folder not a directory: "
                    + ((needProducer.getDirectory() != null) ? needProducer.getDirectory().toString() : null));
        }

        while (!needProducer.isExhausted()) {
            String needFileName = needProducer.getCurrentFileName();

            Dataset ds = needProducer.create();
            String needId = createNeedId(ds);

            if (needProducer == seeksNeedProducer) {
                matchingDataConnections.addNeedAttribute("needtype", needId, "WANT");
                matchingDataPredictions.addNeedAttribute("needtype", needId, "WANT");
            } else if (needProducer == isNeedProducer) {
                matchingDataConnections.addNeedAttribute("needtype", needId, "OFFER");
                matchingDataPredictions.addNeedAttribute("needtype", needId, "OFFER");
            }

            needFileDatasetMap.put(FilenameUtils.removeExtension(needFileName), ds);
        }
    }

    public void indexNeeds() throws IOException, JsonLdError {

        for (Dataset need : needFileDatasetMap.values()) {
            needIndexer.indexNeedModel(need.getDefaultModel(), createNeedId(DatasetFactory.create(need)), true);
        }
    }

    public void buildConnectionTensor() throws IOException {

        // read the connection file and add connections to the tensor
        BufferedReader reader = new BufferedReader(new FileReader(connectionsFile));
        String line = "";
        List<String> needs = new LinkedList<String>();

        while ((line = reader.readLine()) != null) {
            if (line.length() == 0) {
                // add a connection between the first need and all following needs until empty line
                addConnection(needs, false);
                needs = new LinkedList<String>();
            } else {

                Dataset ds = needFileDatasetMap.get(line.trim());
                if (ds == null) {
                    throw new IOException("Dataset is null for need file entry: " + line.trim());
                }

                String needId = createNeedId(ds);
                if (needId == null) {
                    throw new IOException("Need from connection file not found in need directory: " + line);
                }
                needs.add(needId);
            }
        }
        addConnection(needs, false);

        // output the tensor data
        matchingDataConnections.writeOutputFiles(outputDir + "/connections");
    }

    public void buildPredictionTensor() throws IOException, SolrServerException {

        for (Dataset need : needFileDatasetMap.values()) {
            for (String match : computeMatchingNeeds(need)) {
                if (!matchingDataPredictions.getNeeds().contains(createNeedId(need))
                        || !matchingDataPredictions.getNeeds().contains(match)) {
                    throw new IOException(
                            "No need found in input directory for connection specified in connection file:  \n"
                                    + createNeedId(need) + "\n" + match);
                }
                matchingDataPredictions.addNeedConnection(createNeedId(need), match, false);
            }
        }

        // output the tensor data
        matchingDataPredictions.writeOutputFiles(outputDir + "/predictions");
    }

    private List<String> computeMatchingNeeds(Dataset need) throws IOException, SolrServerException {

        TestNeedQueryFactory needQuery = new TestNeedQueryFactory(need);

        SolrDocumentList docs = queryExecutor.executeNeedQuery(needQuery.createQuery(), 20, null,
                new BasicNeedQueryFactory(need).createQuery());

        SolrDocumentList matchedDocs = hintBuilder.calculateMatchingResults(docs);

        List<String> matchedNeeds = new LinkedList<>();
        for (SolrDocument doc : matchedDocs) {
            String matchedNeedId = doc.getFieldValue("id").toString();
            matchedNeeds.add(matchedNeedId);
        }

        return matchedNeeds;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    public void setConnectionsFile(String connectionsFile) {
        this.connectionsFile = connectionsFile;
    }

    private void addConnection(List<String> needs, boolean ignoreNeedsNotFound) throws IOException {
        for (int i = 1; i < needs.size(); i++) {
            String need1 = needs.get(0);
            String need2 = needs.get(i);
            if (!matchingDataConnections.getNeeds().contains(need1)
                    || !matchingDataConnections.getNeeds().contains(need2)) {
                if (!ignoreNeedsNotFound) {
                    throw new IOException(
                            "No need found in input directory for connection specified in connection file:  \n" + need1
                                    + "\n" + need2);
                }
            }
            matchingDataConnections.addNeedConnection(need1, need2, false);
        }
    }

}
