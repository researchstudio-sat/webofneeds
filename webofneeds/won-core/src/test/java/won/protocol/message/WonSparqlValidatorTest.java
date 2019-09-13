package won.protocol.message;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import won.protocol.validation.WonSparqlValidator;

import java.io.IOException;

/**
 * User: ypanchenko Date: 02.06.2015
 */
public class WonSparqlValidatorTest {
    private static final String RESOURCE_FILE_CREATE_MSG_VALID = "/validation/valid/create_msg.trig";
    private static final String RESOURCE_FILE_CREATE_MSG_INVALID = "/validation/invalid/create_msg_invalid.trig";
    private static final String RESOURCE_FILE_ASK_CONSTRAINT = "/validation/query/query_ask_missing_type.rq";
    private static final String RESOURCE_FILE_SELECT_CONSTRAINT = "/validation/query/query_select_invalid_envelope_chain.rq";
    private Dataset createMessageDataset;
    private Dataset createMessageDatasetInvalid;
    private Query askConstraint;
    private Query selectConstraint;

    @Before
    public void init() throws IOException {
        createMessageDataset = Utils.createTestDataset(RESOURCE_FILE_CREATE_MSG_VALID);
        createMessageDatasetInvalid = Utils.createTestDataset(RESOURCE_FILE_CREATE_MSG_INVALID);
        askConstraint = Utils.createTestQuery(RESOURCE_FILE_ASK_CONSTRAINT);
        selectConstraint = Utils.createTestQuery(RESOURCE_FILE_SELECT_CONSTRAINT);
    }

    @Test
    public void testAskConstraintOnValidDataset() throws IOException {
        WonSparqlValidator validator = new WonSparqlValidator(askConstraint);
        boolean valid = validator.validate(createMessageDataset).isValid();
        Assert.assertTrue(valid);
    }

    @Test
    public void testAskConstraintOnInvalidDataset() throws IOException {
        WonSparqlValidator validator = new WonSparqlValidator(askConstraint);
        boolean valid = validator.validate(createMessageDatasetInvalid).isValid();
        Assert.assertFalse(valid);
    }

    @Test
    public void testSelectConstraintOnValidDataset() throws IOException {
        WonSparqlValidator validator = new WonSparqlValidator(selectConstraint);
        boolean valid = validator.validate(createMessageDataset).isValid();
        Assert.assertTrue(valid);
    }

    @Test
    public void testSelectConstraintOnInvalidDataset() throws IOException {
        WonSparqlValidator validator = new WonSparqlValidator(selectConstraint);
        boolean valid = validator.validate(createMessageDatasetInvalid).isValid();
        Assert.assertFalse(valid);
    }
}
