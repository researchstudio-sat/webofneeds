package won.utils.conversationutils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import won.protocol.highlevel.HighlevelFunctionFactory;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonConversationQueryBuilder;
import won.protocol.util.WonConversationUtils;
import won.protocol.util.WonMessageQueryBuilder;

public class WonConversationUtilsTest {

	private static final String inputFolder = "/won/utils/conversationutils/input/";
	private static final String expectedOutputFolder = "/won/utils/conversationutils/expected/";

	@BeforeClass
	public static void setLogLevel() {
		Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		root.setLevel(Level.INFO);
	}

	// This is the case where there are no agreements, that is no predicates from
	// the agreement protocol. The output should be nothing...
	@Test
	public void testAllMessageUris() throws IOException {
		Dataset input = loadDataset(inputFolder + "allmessageuris.trig");
		List<URI> expectedOutput = loadUriList(expectedOutputFolder + "allmessageuris.txt");
		List<URI> actual = WonConversationUtils.getAllMessageURIs(input);
		if (!expectedOutput.equals(actual)) {
			System.out.println("actual: \n" + actual.stream().map(x -> x.toString()).collect(Collectors.joining("\n")));
		}
		Assert.assertEquals(expectedOutput, actual);
	}
	
	@Test
	public void testLatestMessageOfNeed() throws IOException {
		Dataset input = loadDataset(inputFolder + "allmessageuris.trig");
		URI actual = WonConversationUtils.getLatestMessageOfNeed(input,URI.create("https://localhost:8443/won/resource/need/7820503869697675000"));
		List<URI> expectedOutput = loadUriList(expectedOutputFolder + "latestmessagageofneed.txt");
		if (actual != null && ! expectedOutput.get(0).equals(actual)) {
			System.out.println("actual: \n" + actual.toString());
		}
		Assert.assertEquals(expectedOutput.get(0), actual);
	}
	
	@Test
	public void testLatestRetractsMessageOfNeed() throws IOException {
		Dataset input = loadDataset(inputFolder + "conversation-with-retracts.trig");
		URI actual = WonConversationUtils.getLatestRetractsMessageOfNeed(input,URI.create("https://localhost:8443/won/resource/need/ekdwt2a60tesl77r13mu"));
		List<URI> expectedOutput = loadUriList(expectedOutputFolder + "latestretractsmessagageofneed.txt");
		if (actual != null && ! expectedOutput.get(0).equals(actual)) {
			System.out.println("actual: \n" + actual.toString());
		}
		Assert.assertEquals(expectedOutput.get(0), actual);
	}
	
	@Test
	public void testLatestProposesMessageOfNeed() throws IOException {
		Dataset input = loadDataset(inputFolder + "conversation-with-agreements.trig");
		URI actual = WonConversationUtils.getLatestProposesMessageOfNeed(input,URI.create("https://localhost:8443/won/resource/need/7820503869697675000"));
		List<URI> expectedOutput = loadUriList(expectedOutputFolder + "latestproposesmessagageofneed.txt");
		if (actual != null && ! expectedOutput.get(0).equals(actual)) {
			System.out.println("actual: \n" + actual.toString());
		}
		Assert.assertEquals(expectedOutput.get(0), actual);
	}
	
	@Test
	public void testLatestProposesToCancelMessageOfNeed() throws IOException {
		Dataset input = loadDataset(inputFolder + "conversation-with-agreements.trig");
		URI actual = WonConversationUtils.getLatestProposesToCancelMessageOfNeed(input,URI.create("https://localhost:8443/won/resource/need/7820503869697675000"));
		List<URI> expectedOutput = loadUriList(expectedOutputFolder + "latestproposestocancelmessagageofneed.txt");
		if (actual != null && ! expectedOutput.get(0).equals(actual)) {
			System.out.println("actual: \n" + actual.toString());
		}
		Assert.assertEquals(expectedOutput.get(0), actual);
	}

	@Test
	public void testLatestAcceptsMessageOfNeed() throws IOException {
		Dataset input = loadDataset(inputFolder + "conversation-with-agreements.trig");
		URI actual = WonConversationUtils.getLatestAcceptsMessageOfNeed(input,URI.create("https://localhost:8443/won/resource/need/7820503869697675000"));
		List<URI> expectedOutput = loadUriList(expectedOutputFolder + "latestacceptsmessagageofneed.txt");
		if (actual != null && ! expectedOutput.get(0).equals(actual)) {
			System.out.println("actual: \n" + actual.toString());
		}
		Assert.assertEquals(expectedOutput.get(0), actual);
	}
	

	public void test(Dataset input, Dataset expectedOutput) {

		// check that the computed dataset is the expected one
		Dataset actual = HighlevelFunctionFactory.getAgreementFunction().apply(input);
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
			is = WonConversationUtilsTest.class.getResourceAsStream(path);
			dataset = DatasetFactory.create();
			RDFDataMgr.read(dataset, is, RDFFormat.TRIG.getLang());
		} finally {
			if (is != null) {
				is.close();
			}
		}

		return dataset;
	}
	
	private static List<URI> loadUriList(String path) throws IOException {
		InputStream is = WonConversationUtilsTest.class.getResourceAsStream(path);
		try (BufferedReader buffer = new BufferedReader(new InputStreamReader(is))) {
            return buffer.lines().map(string -> URI.create(string)).collect(Collectors.toList());
        }
	}

	private static Dataset loadDatasetFromFileSystem(String path) throws IOException {

		InputStream is = null;
		Dataset dataset = null;
		try {
			is = new FileInputStream(new File(path));
			dataset = DatasetFactory.create();
			RDFDataMgr.read(dataset, is, RDFFormat.TRIG.getLang());
		} finally {
			if (is != null) {
				is.close();
			}
		}

		return dataset;
	}
	
	public static void main (String... args) throws Exception {
		//Dataset input = loadDataset("/won/utils/agreement/input/one-agreement-one-cancellation.trig");
		Dataset input = loadDataset("/won/utils/conversationutils/input/allmessageuris.trig");
		//initialBinding.add("senderNeed", new ResourceImpl("https://localhost:8443/won/resource/need/7820503869697675000"));
		//initialBinding.add("senderConnection", new ResourceImpl("https://localhost:8443/won/resource/connection/4t9deo4t6bqx83jxk5ex"));
		
		URI uri = WonConversationUtils.getLatestMessageOfNeed(input, URI.create("https://localhost:8443/won/resource/need/7820503869697675000"));
		//URI uri = WonConversationUtils.getLatestMessageOfNeed(input, URI.create("https://192.168.124.49:8443/won/resource/need/z35td13ftmn5k1hzella"));
		System.out.println("uri: " + uri);
		/*
		List<QuerySolution> actual = WonConversationQueryBuilder.getBuilder((x -> x))
				//.senderNeed(URI.create("https://localhost:8443/won/resource/need/7820503869697675000"))
				.newestFirst()
				.noResponses()
				.build()
				.apply(input);
		*/
//		System.out.println("actual: \n" + actual.stream().map(x -> x.toString()).collect(Collectors.joining("\n")));
		/*
		URI uri = WonConversationUtils.getLatestProposesMessageOfNeed(input, URI.create("https://localhost:8443/won/resource/need/7820503869697675000"));
		System.out.println("uri: " + uri);
		uri = WonConversationUtils.getLatestAcceptsMessageOfNeed(input, URI.create("https://localhost:8443/won/resource/need/7820503869697675000"));
		System.out.println("uri: " + uri);*/
	}
}
