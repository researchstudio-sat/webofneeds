package won.protocol.message;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDF;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.SFSIG;
import won.protocol.vocabulary.WONMSG;

import java.io.IOException;

/**
 * User: ypanchenko
 * Date: 28.04.2015
 */
public class WonMessageValidatorTest
{

  private static final String NEED_LOCAL_URI = "http://localhost:8080/won/resource/need/o1ybhchandwvg6c8pv81";
  private static final String NODE_LOCAL_URI = "http://localhost:8080/won/resource";

  // create-need message

  private static final String RESOURCE_FILE_CREATE_MSG_VALID =
    "/validation/valid/create_msg.trig";

  private static final String CREATE_CONTENT_NAME =
    "http://localhost:8080/won/resource/event/td3u9uqz1pismn4qtn92#content-9ti7";
  private static final String CREATE_ENV1_NAME =
    "http://localhost:8080/won/resource/event/td3u9uqz1pismn4qtn92#envelope-lxzj";
  private static final String CREATE_ENV1_SIG_NAME =
    "http://localhost:8080/won/resource/event/td3u9uqz1pismn4qtn92#envelope-lxzj-sig";
  private static final String CREATE_ENV2_NAME =
    "http://localhost:8080/won/resource/event/td3u9uqz1pismn4qtn92#envelope-6dv8";
  private static final String CREATE_ENV1_ENV2_MSG_URI = "http://localhost:8080/won/resource/event/td3u9uqz1pismn4qtn92";


  private Dataset createMessageDataset;



  // response message local

  private static final String RESOURCE_FILE_RESPONSE_MSG_VALID =
    "/validation/valid/response_msg_local.trig";

  private static final String RESPONSE_LOCAL_ENV1_NAME =
    "http://localhost:8080/won/resource/event/kpft39z0ladmp3cqm4ju#envelope-c37o";
  private static final String RESPONSE_LOCAL_ENV1_SIG_NAME =
    "http://localhost:8080/won/resource/event/kpft39z0ladmp3cqm4ju#envelope-c37o-sig";
  private static final String RESPONSE_LOCAL_ENV2_NAME =
    "http://localhost:8080/won/resource/event/kpft39z0ladmp3cqm4ju#envelope-g66h";
  private static final String RESPONSE_LOCAL_ENV1_ENV2_MSG_URI =
    "http://localhost:8080/won/resource/event/kpft39z0ladmp3cqm4ju";
    ;

  private Dataset responseMessageDataset;


  // text message (i.e. connection message) sent from external node

  private static final String RESOURCE_FILE_TEXT_MSG_VALID =
    "/validation/valid/text_msg_remote.trig";

  private Dataset textMessageDataset;

  @Before
  public void init() throws IOException {
    createMessageDataset = Utils.createTestDataset(RESOURCE_FILE_CREATE_MSG_VALID);
    responseMessageDataset = Utils.createTestDataset(RESOURCE_FILE_RESPONSE_MSG_VALID);
    textMessageDataset = Utils.createTestDataset(RESOURCE_FILE_TEXT_MSG_VALID);
  }

  @Test
  public void testValidCreateMessage() throws IOException {
    WonMessageValidator validator = new WonMessageValidator();
    StringBuilder message = new StringBuilder();
    boolean valid = validator.validate(createMessageDataset, message);
    Assert.assertTrue("validation is expected not to fail at " + message, valid);
  }

  @Test
  public void testValidResponseLocalMessage() throws IOException {
    WonMessageValidator validator = new WonMessageValidator();
    StringBuilder message = new StringBuilder();
    boolean valid = validator.validate(responseMessageDataset, message);
    Assert.assertTrue("validation is expected not to fail at " + message, valid);
  }

  @Test
  public void testValidTextRemoteMessage() throws IOException {
    WonMessageValidator validator = new WonMessageValidator();
    StringBuilder message = new StringBuilder();
    boolean valid = validator.validate(textMessageDataset, message);
    Assert.assertTrue("validation is expected not to fail at " + message, valid);
  }

  @Test
  public void testInvalidDefaultGraph() throws IOException {

    // create invalid dataset by adding a triple into the default graph
    Dataset invalidDataset = WonRdfUtils.MessageUtils.copyByDatasetSerialization(new WonMessage(createMessageDataset)
    ).getCompleteDataset();
    Model defaultModel = invalidDataset.getDefaultModel();
    Statement stmt = defaultModel.createStatement(ResourceFactory.createResource(), ResourceFactory.createProperty
      ("test:property:uri"), ResourceFactory.createPlainLiteral("test literal"));
    defaultModel.add(stmt);

    // validate this invalid dataset
    WonMessageValidator validator = new WonMessageValidator();
    StringBuilder message = new StringBuilder();
    boolean valid = validator.validate(invalidDataset, message);
    Assert.assertTrue("validation is expected to fail", !valid);
    Assert.assertTrue(message.toString().contains("invalid_default_graph"));

  }

  @Test
  public void testMissingAndInvalidMessageDirection() throws IOException {

    // create invalid dataset by removing a triple with message direction
    Dataset invalidDataset = WonRdfUtils.MessageUtils.copyByDatasetSerialization(new WonMessage(createMessageDataset)
    ).getCompleteDataset();
    Model env1Model = invalidDataset.getNamedModel(CREATE_ENV1_NAME);
    Model env2Model = invalidDataset.getNamedModel(CREATE_ENV2_NAME);

    Statement stmtOld = env2Model.createStatement(ResourceFactory.createResource(CREATE_ENV1_ENV2_MSG_URI), RDF.type,
                                                  WONMSG.TYPE_FROM_OWNER);
    env1Model.remove(stmtOld);
    env2Model.remove(stmtOld);

    // validate this invalid dataset
    WonMessageValidator validator = new WonMessageValidator();
    StringBuilder message = new StringBuilder();
    boolean valid = validator.validate(invalidDataset, message);
    Assert.assertTrue("validation is expected to fail", !valid);
    Assert.assertTrue(message.toString().contains("missing_direction"));

    // create invalid dataset by adding a triple with invalid message direction
    Statement stmtNew = env2Model.createStatement(ResourceFactory.createResource(CREATE_ENV1_ENV2_MSG_URI), RDF.type,
                                                  ResourceFactory.createProperty("test:property:uri"));
    env2Model.add(stmtNew);

    // validate this invalid dataset
    valid = validator.validate(invalidDataset, message);
    Assert.assertTrue("validation is expected to fail", !valid);
    Assert.assertTrue(message.toString().contains("invalid_direction"));

  }

  @Test
  public void testMissingAndInvalidMessageType() throws IOException {

    // create invalid dataset by removing a triple with message type
    Dataset invalidDataset = WonRdfUtils.MessageUtils.copyByDatasetSerialization(new WonMessage(createMessageDataset)
    ).getCompleteDataset();
    Model env1Model = invalidDataset.getNamedModel(CREATE_ENV1_NAME);

    Statement stmtOld = env1Model.createStatement(ResourceFactory.createResource(CREATE_ENV1_ENV2_MSG_URI), WONMSG.HAS_MESSAGE_TYPE_PROPERTY,
                                                  WONMSG.TYPE_CREATE);
    env1Model.remove(stmtOld);

    // validate this invalid dataset
    WonMessageValidator validator = new WonMessageValidator();
    StringBuilder message = new StringBuilder();
    boolean valid = validator.validate(invalidDataset, message);
    Assert.assertTrue("validation is expected to fail", !valid);
    Assert.assertTrue(message.toString().contains("missing_type"));

    // create invalid dataset by adding a triple with invalid message type
    Statement stmtNew = env1Model.createStatement(ResourceFactory.createResource(CREATE_ENV1_ENV2_MSG_URI), WONMSG.HAS_MESSAGE_TYPE_PROPERTY,
                                                  ResourceFactory.createProperty("test:property:uri"));
    env1Model.add(stmtNew);

    // validate this invalid dataset
    valid = validator.validate(invalidDataset, message);
    Assert.assertTrue("validation is expected to fail", !valid);
    Assert.assertTrue(message.toString().contains("invalid_type"));

  }

  @Test
  public void testMissingTimestamp() throws IOException {

    // create invalid dataset by removing a triple with received timestamp
    Dataset invalidDataset = WonRdfUtils.MessageUtils.copyByDatasetSerialization(new WonMessage(createMessageDataset)
    ).getCompleteDataset();
    Model env1Model = invalidDataset.getNamedModel(CREATE_ENV1_NAME);
    Model env2Model = invalidDataset.getNamedModel(CREATE_ENV2_NAME);

    Statement stmt1Old = env1Model.createStatement(ResourceFactory.createResource(CREATE_ENV1_ENV2_MSG_URI),
                                                  WONMSG.HAS_SENT_TIMESTAMP,
                                                  ResourceFactory.createTypedLiteral("1433774711093", XSDDatatype.XSDlong));
    Statement stmt2Old = env2Model.createStatement(ResourceFactory.createResource(CREATE_ENV1_ENV2_MSG_URI),
                                                  WONMSG.HAS_RECEIVED_TIMESTAMP,
                                                  ResourceFactory.createTypedLiteral("1433774714580", XSDDatatype.XSDlong));
    env1Model.remove(stmt1Old);
    env2Model.remove(stmt2Old);

    // validate this invalid dataset
    WonMessageValidator validator = new WonMessageValidator();
    StringBuilder message = new StringBuilder();
    boolean valid = validator.validate(invalidDataset, message);
    Assert.assertTrue("validation is expected to fail", !valid);
    Assert.assertTrue(message.toString().contains("missing_timestamp"));

  }

  @Test
  public void testInvalidEnvelopeChain() throws IOException {

    // test 1
    // create invalid dataset by removing a triple that references envelope 1 from envelope 2
    Dataset invalidDataset = WonRdfUtils.MessageUtils.copyByDatasetSerialization(new WonMessage(createMessageDataset)
    ).getCompleteDataset();
    Model env2Model = invalidDataset.getNamedModel(CREATE_ENV2_NAME);
    Model env1Model = invalidDataset.getNamedModel(CREATE_ENV1_NAME);

    Statement stmtOld = env2Model.createStatement(ResourceFactory.createResource(CREATE_ENV2_NAME),
                                                  WONMSG.CONTAINS_ENVELOPE,
                                                  ResourceFactory.createResource(CREATE_ENV1_NAME));
    env2Model.remove(stmtOld);

    // validate this invalid dataset
    WonMessageValidator validator = new WonMessageValidator();
    StringBuilder message = new StringBuilder();
    boolean valid = validator.validate(invalidDataset, message);
    Assert.assertTrue("validation is expected to fail", !valid);
    Assert.assertTrue(message.toString().contains("invalid_envelope_chain"));
    // reset for further testing
    env2Model.add(stmtOld);



    // test 2
    // create invalid dataset by adding a triple that references envelope 2 from envelope 1,
    // thus creating a cycle in the envelope chain
    Statement stmtNew = env1Model.createStatement(ResourceFactory.createResource(CREATE_ENV1_NAME),
                                                  WONMSG.CONTAINS_ENVELOPE,
                                                  ResourceFactory.createResource(CREATE_ENV2_NAME));
    env1Model.add(stmtNew);

    // validate this invalid dataset
    valid = validator.validate(invalidDataset, message);
    Assert.assertTrue("validation is expected to fail", !valid);
    Assert.assertTrue(message.toString().contains("invalid_envelope_chain"));
    // reset for further testing
    env1Model.remove(stmtNew);


    // test 3
    // create invalid dataset by adding a triple that references an envelope that is not present in the dataset
    stmtNew = env1Model.createStatement(ResourceFactory.createResource(CREATE_ENV1_NAME),
                                                  WONMSG.CONTAINS_ENVELOPE,
                                                  ResourceFactory.createResource("test:resource:uri"));
    env1Model.add(stmtNew);

    // validate this invalid dataset
    valid = validator.validate(invalidDataset, message);
    Assert.assertTrue("validation is expected to fail", !valid);
    Assert.assertTrue(message.toString().contains("invalid_envelope_chain"));
    // reset for further testing
    env1Model.remove(stmtNew);
  }

  @Test
  public void testInvalidContentChain() throws IOException {

    Dataset invalidDataset = WonRdfUtils.MessageUtils.copyByDatasetSerialization(new WonMessage(createMessageDataset)
    ).getCompleteDataset();
    Model env1Model = invalidDataset.getNamedModel(CREATE_ENV1_NAME);
    Model env2Model = invalidDataset.getNamedModel(CREATE_ENV2_NAME);

    // test 4
    // create invalid dataset by adding a triple that references a content from the second envelope (additionally to
    // the first envelope)
    Statement stmtNew = env2Model.createStatement(ResourceFactory.createResource(CREATE_ENV1_ENV2_MSG_URI),
                                        WONMSG.HAS_CONTENT_PROPERTY,
                                        ResourceFactory.createResource(CREATE_CONTENT_NAME));
    env2Model.add(stmtNew);

    // validate this invalid dataset
    WonMessageValidator validator = new WonMessageValidator();
    StringBuilder message = new StringBuilder();
    boolean valid = validator.validate(invalidDataset, message);
    Assert.assertTrue("validation is expected to fail", !valid);
    Assert.assertTrue(message.toString().contains("invalid_content_chain"));
    // reset for further testing
    env2Model.remove(stmtNew);



    // TODO test 5
    // create invalid dataset by removing a reference to the content from the envelope
    Statement stmtOld = env1Model.createStatement(ResourceFactory.createResource(CREATE_ENV1_ENV2_MSG_URI),
                                        WONMSG.HAS_CONTENT_PROPERTY,
                                        ResourceFactory.createResource(CREATE_CONTENT_NAME));
    env1Model.remove(stmtOld);

    // validate this invalid dataset
    valid = validator.validate(invalidDataset, message);
    Assert.assertTrue("validation is expected to fail", !valid);
    Assert.assertTrue(message.toString().contains("invalid_content_chain"));
    // reset for further testing
    env1Model.add(stmtOld);
  }

  @Test
  public void testMetaAndSignerConsistency() throws IOException {


    // Test fromSystem envelopes signer consistency
    Dataset invalidDataset = WonRdfUtils.MessageUtils.copyByDatasetSerialization(new WonMessage(responseMessageDataset)
    ).getCompleteDataset();
    //Model env1Model = invalidDataset.getNamedModel(RESPONSE_LOCAL_ENV1_NAME);
    Model env1sigModel = invalidDataset.getNamedModel(RESPONSE_LOCAL_ENV1_SIG_NAME);
    //Model env2Model = invalidDataset.getNamedModel(RESPONSE_LOCAL_ENV2_NAME);

    // create invalid dataset by replacing a signer
    Statement stmtOld = env1sigModel.createStatement(ResourceFactory.createResource(RESPONSE_LOCAL_ENV1_SIG_NAME),
                                                  SFSIG.HAS_VERIFICATION_CERT,
                                                  ResourceFactory.createResource(NODE_LOCAL_URI));
    env1sigModel.remove(stmtOld);

    Statement stmtNew = env1sigModel.createStatement(ResourceFactory.createResource(RESPONSE_LOCAL_ENV1_SIG_NAME),
                                                     SFSIG.HAS_VERIFICATION_CERT,
                                                     ResourceFactory.createResource(NEED_LOCAL_URI));
    env1sigModel.add(stmtNew);


    WonMessageValidator validator = new WonMessageValidator();
    StringBuilder message = new StringBuilder();
    // validate this invalid dataset
    boolean valid = validator.validate(invalidDataset, message);
    Assert.assertTrue("validation is expected to fail", !valid);
    Assert.assertTrue(message.toString().contains("invalid_from_system_signer"));

    //reset for further testing:
    env1sigModel.remove(stmtNew);
    env1sigModel.add(stmtOld);




    // Test fromOwner leaf envelope signer consistency
    invalidDataset = WonRdfUtils.MessageUtils.copyByDatasetSerialization(new WonMessage(createMessageDataset)
    ).getCompleteDataset();
    env1sigModel = invalidDataset.getNamedModel(CREATE_ENV1_SIG_NAME);

    // create invalid dataset by replacing a signer
    stmtOld = env1sigModel.createStatement(ResourceFactory.createResource(CREATE_ENV1_SIG_NAME),
                                                     SFSIG.HAS_VERIFICATION_CERT,
                                                     ResourceFactory.createResource(NEED_LOCAL_URI));
    env1sigModel.remove(stmtOld);

    stmtNew = env1sigModel.createStatement(ResourceFactory.createResource(CREATE_ENV1_SIG_NAME),
                                                     SFSIG.HAS_VERIFICATION_CERT,
                                                     ResourceFactory.createResource(NODE_LOCAL_URI));
    env1sigModel.add(stmtNew);


    validator = new WonMessageValidator();
    message = new StringBuilder();
    // validate this invalid dataset
    valid = validator.validate(invalidDataset, message);
    Assert.assertTrue("validation is expected to fail", !valid);
    Assert.assertTrue(message.toString().contains("invalid_from_owner_signer"));

    //reset for further testing:
    env1sigModel.remove(stmtNew);
    env1sigModel.add(stmtOld);

  }


}
