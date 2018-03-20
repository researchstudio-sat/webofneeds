package won.protocol.highlevel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import won.protocol.highlevel.HighlevelFunctionFactory;
import won.protocol.highlevel.HighlevelProtocols;
import won.protocol.util.RdfUtils;

public class GetAgreementsTests {

	// for agreement protocol::
	private static final String inputFolder = "/won/protocol/highlevel/agreements/input/";
	private static final String expectedOutputFolder = "/won/protocol/highlevel/agreements/expected/";

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

	// This the case where there is one agreement that is cancelled. That is one
	// proposal and one accept making one agreement, and one proposeToCancel and one
	// accept making an
	// agreement to cancel of the previous agreement. The result should be nothing.
	@Test
	public void oneAgreementOneCancellationTest() throws IOException {
		Dataset input = loadDataset(inputFolder + "one-agreement-one-cancellation.trig");
		Dataset expectedOutput = loadDataset(expectedOutputFolder + "one-agreement-one-cancellation.trig");
		test(input, expectedOutput);
	}

	// This contains an agreement with one proposal and one acceptance. There is
	// also an attempted cancellation of the agreement, but instead of canceling the
	// agreement containing the
	// agr:accepts triple, the proposal for the agreement agr:proposes is targeted
	// instead. This should result in an intact agreement like in oneAgreementTest
	// () .
	@Test
	public void oneAgreementOneCancellationTestProposalError() throws IOException {
		Dataset input = loadDataset(inputFolder + "one-agreement-one-cancellation-proposal-error.trig");
		Dataset expectedOutput = loadDataset(
				expectedOutputFolder + "one-agreement-one-cancellation-proposal-error.trig");
		test(input, expectedOutput);
	}

	// This contains an agreement with two proposals, that is one event that
	// proposes two clauses with agr:proposes that is agr:accepted in an agreement
	// in another event.
	// This should result in an agreement with two clauses.
	@Test
	public void oneAgreementTwoProposalClauses() throws IOException {
		Dataset input = loadDataset(inputFolder + "one-agreement-two-proposal-clauses.trig");
		Dataset expectedOutput = loadDataset(expectedOutputFolder + "one-agreement-two-proposal-clauses.trig");
		test(input, expectedOutput);
	}

	// This contains an agreement that is missing a proposal...That is an envelope
	// with an agr:accepts predicate, but no envelope with an agr:proposes predicate
	// This should result in no agreement (an empty expected file)
	@Test
	public void oneAgreementMissingProposal() throws IOException {
		Dataset input = loadDataset(inputFolder + "one-agreement-missing-proposal.trig");
		Dataset expectedOutput = loadDataset(expectedOutputFolder + "one-agreement-missing-proposal.trig");
		test(input, expectedOutput);
	}

	// This contains an agreement that is missing a clause in the proposal. That is
	// an envelope with an agr:accepts predicate, and an envelope with an
	// agr:proposes predicate, but a
	// agr:proposes predicate that references an non-existent object or message
	// envelope. To do this I deleted the event envelope with msg:hasTextMessage
	// predicate and non-existent object
	// as the subject.
	@Test
	public void oneAgreementMissingClause() throws IOException {
		Dataset input = loadDataset(inputFolder + "one-agreement-missing-clause.trig");
		Dataset expectedOutput = loadDataset(expectedOutputFolder + "one-agreement-missing-clause.trig");
		test(input, expectedOutput);
	}

	// This tries to cancel an agreement that does not exist. That is, there is a
	// proposal (agr:proposes) for an agreement, but no acceptance of the agreement
	// (agr:accepts).
	// Thus, it does not matter that there is an agr:proposesToCancel and
	// corresponding agr:accepts since there was no agreement to begin with. This
	// should return an empty result in the
	// expected file...
	@Test
	public void noAgreementOneCancellationError() throws IOException {
		Dataset input = loadDataset(inputFolder + "no-agreement-one-cancellation-error.trig");
		Dataset expectedOutput = loadDataset(expectedOutputFolder + "no-agreement-one-cancellation-error.trig");
		test(input, expectedOutput);
	}

	// This tries to cancel an agreement that accepts two proposals. However, there
	// is an error in the agreement since one of the accepted agreements is not a
	// proposal, but a message.
	// The file name needs to be changed..this should produce an empty file
	// I discovered that it creates problems to use event:6671551888677331000
	@Test
	public void twoProposalOneAgreementOneCancellationmsgError() throws IOException {
		Dataset input = loadDataset(inputFolder + "2proposal-one-agreement-one-cancellation-msgerror.trig");
		Dataset expectedOutput = loadDataset(
				expectedOutputFolder + "2proposal-one-agreement-one-cancellation-msgerror.trig");
		test(input, expectedOutput);
	}

	// This tries to cancel an agreement that accepts two proposals.
	// This should produce an empty file
	@Test
	public void twoProposalOneAgreementOneCancellation() throws IOException {
		Dataset input = loadDataset(inputFolder + "2proposal-one-agreement-one-cancellation.trig");
		Dataset expectedOutput = loadDataset(expectedOutputFolder + "2proposal-one-agreement-one-cancellation.trig");
		test(input, expectedOutput);
	}

	// This contains an agreement that accepts two text messages instead of
	// proposals. There are proposals that utilize these text messages, yet these
	// proposals are not accepted.
	// There is a valid agr:proposesToCancel and agr:acceptance of this cancellation
	// of invalid agreement.
	// This should return and empty output file...
	@Test
	public void twoProposalOneAgreementOneCancellationError() throws IOException {
		Dataset input = loadDataset(inputFolder + "2proposal-one-agreement-errormsg-one-cancellation.trig");
		Dataset expectedOutput = loadDataset(
				expectedOutputFolder + "2proposal-one-agreement-errormsg-one-cancellation.trig");
		test(input, expectedOutput);
	}

	// This test allows for an agreement with an invalid and valid accept in the
	// same agreement envelope
	@Test
	public void twoProposalOneAgreementError() throws IOException {
		Dataset input = loadDataset(inputFolder + "2proposal-one-agreement-errormsg.trig");
		Dataset expectedOutput = loadDataset(expectedOutputFolder + "2proposal-one-agreement-errormsg.trig");
		test(input, expectedOutput);
	}

	// This is one agreement and two cancellation agreements of the same initial
	// agreement. The result should look like oneAgreementOneCancellationTest ()
	@Test
	public void oneAgreementTwoCancellationSameAgreement() throws IOException {
		Dataset input = loadDataset(inputFolder + "one-agreement-two-cancellation-same-agreement.trig");
		Dataset expectedOutput = loadDataset(
				expectedOutputFolder + "one-agreement-two-cancellation-same-agreement.trig");
		test(input, expectedOutput);
	}

	// This is two proposals and two accepts (two agreements), with one
	// cancellation. The result should look like oneAgreementTest()
	// This should be renamed ... because it includes the bot accepting its own
	// agreement ...
	// However, there are not enough messages within the file to build this test. so
	// it has been commented out
	/*
	 * @Test public void twoProposalTwoAgreementsOneCancellation () throws
	 * IOException { Dataset input = loadDataset( inputFolder +
	 * "2proposal-2agreements-onecancellation.trig"); Dataset expectedOutput =
	 * loadDataset( expectedOutputFolder +
	 * "2proposal-2agreements-onecancellation.trig"); test(input,expectedOutput); }
	 */

	// This is two agreements with one having a missing proposal, the result should
	// look like oneAgreementMissingProposal()
	@Test
	public void oneProposalTwoAccepted() throws IOException {
		Dataset input = loadDataset(inputFolder + "one-proposal-two-accepted.trig");
		Dataset expectedOutput = loadDataset(expectedOutputFolder + "one-proposal-two-accepted.trig");
		test(input, expectedOutput);
	}

	// The stuff starting from below has not been fed into the condenser

	// This is one agreement with two accepts
	@Test
	public void oneAgreementTwoAccepts() throws IOException {
		Dataset input = loadDataset(inputFolder + "oneproposal-twoaccepts.trig");
		Dataset expectedOutput = loadDataset(expectedOutputFolder + "oneproposal-twoaccepts.trig");
		test(input, expectedOutput);
	}

	// This is one agreement with two accepts
	@Test
	public void oneAgreementTwoSimultaneousAccepts() throws IOException {
		Dataset input = loadDataset(inputFolder + "oneproposal-two-simultaneous-accepts.trig");
		Dataset expectedOutput = loadDataset(expectedOutputFolder + "oneproposal-two-simultaneous-accepts.trig");
		test(input, expectedOutput);
	}

	// This is one agreement that is self accepted. The graphs containing the
	// agr:accept and agr:proposes are by the same agent/connection.
	@Test
	public void oneSelfAcceptedAgreement() throws IOException {
		Dataset input = loadDataset(inputFolder + "one-self-accepted-agreement.trig");
		Dataset expectedOutput = loadDataset(expectedOutputFolder + "one-self-accepted-agreement.trig");
		test(input, expectedOutput);
	}

	// This is a test where the accept comes before the proposal...
	@Test
	public void oneAcceptBeforeProposeAgreement() throws IOException {
		Dataset input = loadDataset(inputFolder + "one-accept-before-propose-agreement.trig");
		Dataset expectedOutput = loadDataset(expectedOutputFolder + "one-accept-before-propose-agreement.trig");
		test(input, expectedOutput);
	}

	// This is a test where the accept comes before the proposal...
	@Test
	public void oneSelfAcceptedAgreementSameGraph() throws IOException {
		Dataset input = loadDataset(inputFolder + "one-self-accepted-agreement-same-graph.trig");
		Dataset expectedOutput = loadDataset(expectedOutputFolder + "one-self-accepted-agreement-same-graph.trig");
		test(input, expectedOutput);
	}

	// This is a test where the accept comes before the proposal...
	@Test
	public void falseAgreementUsingSuccessResponse() throws IOException {
		Dataset input = loadDataset(inputFolder + "false-agreement-using-success-responses.trig");
		Dataset expectedOutput = loadDataset(expectedOutputFolder + "false-agreement-using-success-responses.trig");
		test(input, expectedOutput);
	}

	// This tries to propose a HintFeedBackMessage
	@Test
	public void agreementProposesHintFeedback() throws IOException {
		Dataset input = loadDataset(inputFolder + "agreement-proposes-in-hint-feedback-clause.trig");
		Dataset expectedOutput = loadDataset(expectedOutputFolder + "agreement-proposes-in-hint-feedback-clause.trig");
		test(input, expectedOutput);
	}

	// This tries to propose a ConnectMessage
	@Test
	public void agreementProposesConnectMessage() throws IOException {
		Dataset input = loadDataset(inputFolder + "agreement-proposes-connect-message.trig");
		Dataset expectedOutput = loadDataset(expectedOutputFolder + "agreement-proposes-connect-message.trig");
		test(input, expectedOutput);
	}

	// This tries to propose a ConnectMessage
	@Test
	public void agreementConnectMessageProposesHintFeedbackMessage() throws IOException {
		Dataset input = loadDataset(inputFolder + "agreementConnectMessageProposesHintFeedbackMessage.trig");
		Dataset expectedOutput = loadDataset(
				expectedOutputFolder + "agreementConnectMessageProposesHintFeedbackMessage.trig");
		test(input, expectedOutput);
	}

	// This tries to propose two Agreements with three envelopes. One of the
	// envelopes has an accept message of the 1st agreement
	@Test
	public void twoAgreementsSharingEnvelopeforAcceptsPurposes() throws IOException {
		Dataset input = loadDataset(inputFolder + "two-Agreements-Sharing-Envelopes-for-Accepts-Purposes.trig");
		Dataset expectedOutput = loadDataset(
				expectedOutputFolder + "two-Agreements-Sharing-Envelopes-for-Accepts-Purposes.trig");
		test(input, expectedOutput);
	}

	// This tries to propose two Agreements with three envelopes. One of the
	// envelopes has an accept message of the 1st agreement
	@Test
	public void cancelledTwoAgreementsSharingEnvelopesforAcceptsPurposes() throws IOException {
		Dataset input = loadDataset(
				inputFolder + "cancelled-Two-Agreements-Sharing-Envelopes-for-Accepts-Purposes.trig");
		Dataset expectedOutput = loadDataset(
				expectedOutputFolder + "cancelled-Two-Agreements-Sharing-Envelopes-for-Accepts-Purposes.trig");
		test(input, expectedOutput);
	}

	// This tries to propose a Proposal, with different agents making a proposal...
	@Test
	public void oneAgreementProposedProposaldAgent() throws IOException {
		Dataset input = loadDataset(inputFolder + "one-agreement-proposed-proposal-dagent.trig");
		Dataset expectedOutput = loadDataset(expectedOutputFolder + "one-agreement-proposed-proposal-dagent.trig");
		test(input, expectedOutput);
	}

	// This tries to propose a Proposal, with the same agent making a proposal...
	@Test
	public void oneAgreementProposedProposalsAgent() throws IOException {
		Dataset input = loadDataset(inputFolder + "one-agreement-proposed-proposal-sagent.trig");
		Dataset expectedOutput = loadDataset(expectedOutputFolder + "one-agreement-proposed-proposal-sagent.trig");
		test(input, expectedOutput);
	}

	// This includes a Proposal that Cancels itself...
	@Test
	public void selfCancelledTwoAgreementsSharingEnvelopesforAcceptsPurposes() throws IOException {
		Dataset input = loadDataset(
				inputFolder + "self-cancelled-Two-Agreements-Sharing-Envelopes-for-Accepts-Purposes.trig");
		Dataset expectedOutput = loadDataset(
				expectedOutputFolder + "self-cancelled-Two-Agreements-Sharing-Envelopes-for-Accepts-Purposes.trig");
		test(input, expectedOutput);
	}

	// This includes a Accept that is retracted
	@Test
	public void oneAgreementAcceptsRetracted() throws IOException {
		Dataset input = loadDataset(inputFolder + "one-agreement-accepts-retracted.trig");
		Dataset expectedOutput = loadDataset(expectedOutputFolder + "one-agreement-accepts-retracted.trig");
		test(input, expectedOutput);

	}

	// proposal is first accepted, then retracted
	@Test
	public void oneAgreementProposalRetractedTooLate() throws IOException {
		Dataset input = loadDataset(inputFolder + "one-agreement-proposes-retracted.trig");
		Dataset expectedOutput = loadDataset(expectedOutputFolder + "one-agreement-proposes-retracted.trig");
		test(input, expectedOutput);
	}
	
	// proposal not accepted as accept chain is interleaved with retracts chain
	@Test
	public void oneAgreementProposalRetractedInterleaved() throws IOException {
		Dataset input = loadDataset(inputFolder + "one-agreement-proposes-retracted-interleaved.trig");
		Dataset expectedOutput = loadDataset(expectedOutputFolder + "one-agreement-proposes-retracted-interleaved.trig");
		test(input, expectedOutput);
	}
	
	// proposal not accepted as accept chain is interleaved with retracts chain
	@Test
	public void oneAgreementAcceptContainsRetract() throws IOException {
		Dataset input = loadDataset(inputFolder + "one-agreement-proposes-accept-contains-retract.trig");
		Dataset expectedOutput = loadDataset(expectedOutputFolder + "one-agreement-proposes-accept-contains-retract.trig");
		test(input, expectedOutput);
	}


	// This includes a Proposal that is retracted
	@Test
	public void oneProposalTwoAcceptsFirstRetractedFirstCancelled() throws IOException {
		Dataset input = loadDataset(inputFolder + "oneProposalTwoAcceptsFirstRetractedFirstCancelled.trig");
		Dataset expectedOutput = loadDataset(
				expectedOutputFolder + "oneProposalTwoAcceptsFirstRetractedFirstCancelled.trig");
		test(input, expectedOutput);
	}

	// This tests a retraction of a proposal before an accept
	@Test
	public void oneAgreementProposalRetractedb4Accept() throws IOException {
		Dataset input = loadDataset(inputFolder + "one-agreement-proposes-retracted-b4-accept.trig");
		Dataset expectedOutput = loadDataset(
				expectedOutputFolder + "one-agreement-proposes-retracted-b4-accept.trig");
		test(input, expectedOutput);
	}

	// This retracts a proposaltocancel making a still cancelled agreement
	@Test
	public void retractProposalTocancelAfterAgreement() throws IOException {
		Dataset input = loadDataset(inputFolder + "one-agreement-proposaltocancel-retracted.trig");
		Dataset expectedOutput = loadDataset(
				expectedOutputFolder + "one-agreement-proposaltocancel-retracted.trig");
		test(input, expectedOutput);
	}

	// This retracts a proposaltocancel before the accept message
	@Test
	public void retractProposalTocancelBeforeAccept() throws IOException {
		Dataset input = loadDataset(inputFolder + "one-agreement-proposaltocancel-retracted-b4-accept.trig");
		Dataset expectedOutput = loadDataset(
				expectedOutputFolder + "one-agreement-proposaltocancel-retracted-b4-accept.trig");
		test(input, expectedOutput);
	}

	@Test
	public void oneProposalRejectedBeforeAccept() throws IOException {
		Dataset input = loadDataset(inputFolder + "one-proposal-rejected-before-accept.trig");
		Dataset expectedOutput = loadDataset(
				expectedOutputFolder + "one-proposal-rejected-before-accept.trig");
		test(input, expectedOutput);
	}
	
	private static boolean passesTest(Dataset input, Dataset expectedOutput) {
		Dataset actual = HighlevelFunctionFactory.getAgreementFunction().apply(input);
		return RdfUtils.isIsomorphicWith(expectedOutput, actual);
	}

	public void test(Dataset input, Dataset expectedOutput) {
		input = RdfUtils.cloneDataset(input);
		expectedOutput = RdfUtils.cloneDataset(expectedOutput);

		// check that the computed dataset is the expected one
		Dataset actual = HighlevelProtocols.getAgreements(input);
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
			is = GetAgreementsTests.class.getResourceAsStream(path);
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

	public static void main(String... args) throws Exception {
		Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		root.setLevel(Level.INFO);
		// condense test cases
		// read datasets (input and expected output)
		// modify input, removing graphs and triples until the test breaks
		String outputPath = "src/test/resources/won/utils/agreement/condensed/";
		String inputPath = "src/test/resources/won/utils/agreement/input/";
		Stream<Path> resources = Files.list(Paths.get(inputPath));
		resources.forEach(resource -> {
			try {
				System.out.println("trying to condense: " + resource.toAbsolutePath().toString());
				condenseTestCaseByQuery(resource, outputPath);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

	}

	private static List<String> getClasspathEntriesByPath(String path) throws IOException {
		InputStream is = GetAgreementsTests.class.getClassLoader().getResourceAsStream(path);

		StringBuilder sb = new StringBuilder();
		while (is.available() > 0) {
			byte[] buffer = new byte[1024];
			sb.append(new String(buffer, Charset.defaultCharset()));
		}

		return Arrays.asList(sb.toString().split("\n")) // Convert StringBuilder to individual lines
				.stream() // Stream the list
				.filter(line -> line.trim().length() > 0) // Filter out empty lines
				.collect(Collectors.toList()); // Collect remaining lines into a List again
	}

	private static String readCondensationQuery() {
		// InputStream is =
		// AgreementFunction.class.getResourceAsStream("/won/utils/agreement/condensation-query.sq");
		// Not used...not checking if the next line works...
		InputStream is = HighlevelFunctionFactory.getAgreementFunction().getClass()
				.getResourceAsStream("/won/utils/agreement/condensation-query.sq");
		StringWriter writer = new StringWriter();
		try {
			IOUtils.copy(is, writer, Charsets.UTF_8);
		} catch (IOException e) {
			;
			throw new IllegalStateException("Could not read queryString file", e);
		}
		return writer.toString();
	}

	private static void condenseTestCaseByQuery(Path resource, String outputPath) throws Exception {
		String condensationQuery = readCondensationQuery();
		UpdateRequest update = UpdateFactory.create(condensationQuery);
		Dataset condensedDataset = loadDatasetFromFileSystem(resource.toFile().getAbsolutePath());
		UpdateProcessor updateProcessor = UpdateExecutionFactory.create(update, condensedDataset);
		updateProcessor.execute();
		Iterator<String> graphNames = condensedDataset.listNames();
		while (graphNames.hasNext()) {
			Model graph = condensedDataset.getNamedModel(graphNames.next());
			if (graph.isEmpty()) {
				graphNames.remove();
			}
		}
		RDFDataMgr.write(new FileOutputStream(outputPath + resource.getFileName()), condensedDataset, Lang.TRIG);
		System.out.println("wrote condensed input file to: " + outputPath + resource.getFileName());
	}

	private static void condenseTestCaseIteratively(String filename, String outputPath) throws Exception {
		RdfUtils.Pair<Dataset> inputAndExpectedOutput = loadDatasetPair(filename);
		try {
			if (!passesTest(inputAndExpectedOutput.getFirst(), inputAndExpectedOutput.getSecond())) {
				System.out.println("test does not pass, cannot condense: " + filename);
				return;
			}
		} catch (Exception e) {
			System.out.println("test throws an Exception, cannot condense: " + filename);
			return;
		}
		Dataset condensedDataset = inputAndExpectedOutput.getFirst();
		Dataset expectedOutput = inputAndExpectedOutput.getSecond();
		Iterator<String> graphNamesIt = condensedDataset.listNames();
		int deletedStatements = 0;
		while (graphNamesIt.hasNext()) {
			String graphName = graphNamesIt.next();
			System.out.println("trying to remove graph: " + graphName);
			Dataset backupDataset = RdfUtils.cloneDataset(condensedDataset);
			condensedDataset.removeNamedModel(graphName);
			if (!passesTest(condensedDataset, expectedOutput)) {
				System.out.println("cannot remove graph: " + graphName + ", trying individual triples");
				condensedDataset = backupDataset;
				// now try to remove triples
				Model condensedModel = condensedDataset.getNamedModel(graphName);
				Model attepmtedStatements = ModelFactory.createDefaultModel();
				boolean done = false;
				while (!done) {
					Model backupModel = RdfUtils.cloneModel(condensedModel);
					StmtIterator it = condensedModel.listStatements();
					done = true;
					while (it.hasNext()) {
						Statement stmt = it.next();
						if (attepmtedStatements.contains(stmt)) {
							System.out.println("attempted this before");
							continue;
						}
						System.out.println("trying statement: " + stmt);
						attepmtedStatements.add(stmt);
						it.remove();
						deletedStatements++;
						done = false;
						break;
					}

					condensedDataset.removeNamedModel(graphName);
					if (!condensedModel.isEmpty()) {
						condensedDataset.addNamedModel(graphName, condensedModel);
					}
					if (!passesTest(condensedDataset, expectedOutput)) {
						System.out.println("could not delete statement");
						condensedModel = backupModel;
						condensedDataset.replaceNamedModel(graphName, condensedModel);
						deletedStatements--;
					} else {
						System.out.println("deleted a statement");
					}
				}
				if (!passesTest(condensedDataset, expectedOutput)) {
					System.out.println("test does not pass after removing statements!");
					condensedDataset = backupDataset;
				} else {
					System.out.println("removed " + deletedStatements + " statements");
				}
			} else {
				System.out.println("removed graph: " + graphName);
			}

			System.out.println("dataset has ");
		}
		RDFDataMgr.write(new FileOutputStream(Paths.get(outputPath + filename).toFile()), condensedDataset, Lang.TRIG);
		System.out.println("wrote condensed input file to: " + outputPath + filename);
	}
}
