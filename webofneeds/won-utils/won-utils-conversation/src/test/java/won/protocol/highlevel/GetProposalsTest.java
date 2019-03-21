package won.protocol.highlevel;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import won.protocol.agreement.AgreementProtocolState;
import won.protocol.util.RdfUtils;

public class GetProposalsTest {

	// for agreement protocol::
	private static final String inputFolder = "/won/protocol/highlevel/proposals/input/";
	private static final String expectedOutputFolder = "/won/protocol/highlevel/proposals/expected/";

	// Boolean that sets whether we include the base level agreement protocol tests
	private static final Boolean agreementTests = true;

	// for getAgreements with acknowledgement, modification, and agreement
	// protocol...
	private static final String getAGinputFolder = "/won/utils/getagreements/input/";
	private static final String getAGexpectedOutputFolder = "/won/utils/getagreements/expected/";

	@BeforeClass
	public static void setLogLevel() {
		Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		root.setLevel(Level.INFO);
	}

	
	// This is the case where there are no agreements, that is no predicates from
	// the agreement protocol. The output should be nothing...
	@Test
	public void noAgreementsTest() throws IOException {
		Dataset input = loadDataset(inputFolder + "no-agreements.trig");
		Dataset expectedOutput = loadDataset(expectedOutputFolder + "no-agreements.trig");
		test(input, expectedOutput);
	}

	// This is the case where there is one agreement. That is one proposal and one
	// accept making one agreement. The output should be an agreement.
	@Test
	public void oneAgreementTest() throws IOException {
		Dataset input = loadDataset(inputFolder + "one-agreement.trig");
		Dataset expectedOutput = loadDataset(expectedOutputFolder + "one-agreement.trig");
		test(input, expectedOutput);
	}
	
	
	@Test
	public void oneAgreementProposalToCancelRetractedTest() throws IOException {
		Dataset input = loadDataset(inputFolder + "one-agreement-proposaltocancel-retracted.trig");
		Dataset expectedOutput = loadDataset(expectedOutputFolder + "one-agreement-proposaltocancel-retracted.trig");
		test(input, expectedOutput);
	}
	
	
	@Test
	public void oneAgreementProposalToCancelRetractedBeforeAcceptTest() throws IOException {
		Dataset input = loadDataset(inputFolder + "one-agreement-proposaltocancel-retracted-b4-accept.trig");
		Dataset expectedOutput = loadDataset(expectedOutputFolder + "one-agreement-proposaltocancel-retracted-b4-accept.trig");
		test(input, expectedOutput);
	}
	
	@Test
	public void oneAgreementProposalToCancelRejectedBeforeAcceptTest() throws IOException {
		Dataset input = loadDataset(inputFolder + "one-agreement-proposaltocancel-rejected-b4-accept.trig");
		Dataset expectedOutput = loadDataset(expectedOutputFolder + "one-agreement-proposaltocancel-rejected-b4-accept.trig");
		test(input, expectedOutput);
	}
				

	public void test(Dataset input, Dataset expectedOutput) {
		input = RdfUtils.cloneDataset(input);
		expectedOutput = RdfUtils.cloneDataset(expectedOutput);

		// check that the computed dataset is the expected one
		Dataset actual = AgreementProtocolState.of(input).getAgreements();
		// TODO: remove before checking in
		RdfUtils.Pair<Dataset> diff = RdfUtils.diff(expectedOutput, actual);
		if (!(diff.getFirst().isEmpty() && diff.getSecond().isEmpty())) {
			System.out.println("diff - only in expected:");
			RDFDataMgr.write(System.out, diff.getFirst(), Lang.TRIG);
			System.out.println("diff - only in actual:");
			RDFDataMgr.write(System.out, diff.getSecond(), Lang.TRIG);
		}
		Assert.assertTrue(RdfUtils.isIsomorphicWith(expectedOutput, actual));
	}

	private static RdfUtils.Pair<Dataset> loadDatasetPair(String filename) throws IOException {
		Dataset input = loadDataset(inputFolder + filename);
		Dataset expectedOutput = loadDataset(expectedOutputFolder + filename);
		return new RdfUtils.Pair<Dataset>(input, expectedOutput);
	}

	private static Dataset loadDataset(String path) throws IOException {

		InputStream is = null;
		Dataset dataset = null;
		try {
			is = GetProposalsTest.class.getResourceAsStream(path);
			dataset = DatasetFactory.createGeneral();
			RDFDataMgr.read(dataset, is, RDFFormat.TRIG.getLang());
		} finally {
			if (is != null) {
				is.close();
			}
		}

		return dataset;
	}

	private static Dataset loadDatasetFromFileSystem(String path) throws IOException {

		InputStream is = null;
		Dataset dataset = null;
		try {
			is = new FileInputStream(new File(path));
			dataset = DatasetFactory.createGeneral();
			RDFDataMgr.read(dataset, is, RDFFormat.TRIG.getLang());
		} finally {
			if (is != null) {
				is.close();
			}
		}

		return dataset;
	}

	
}
