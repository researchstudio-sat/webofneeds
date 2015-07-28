package won.protocol.message;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;
import org.apache.jena.riot.Lang;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import won.protocol.util.RdfUtils;
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
  private static final String NODE_REMOTE_URI = "http://remotehost:8080/won/resource";

  // create-need message

  private static final String RESOURCE_FILE_CREATE_MSG_VALID =
    "/validation/valid/create_msg.trig";

  private static final String CREATE_CONTENT_NAME =
    "http://localhost:8080/won/resource/event/td3u9uqz1pismn4qtn92#content-9ti7";
  private static final String CREATE_CONTENT_NAME_SIG =
    "http://localhost:8080/won/resource/event/td3u9uqz1pismn4qtn92#content-9ti7-sig";
  private static final String CREATE_ENV1_NAME =
    "http://localhost:8080/won/resource/event/td3u9uqz1pismn4qtn92#envelope-lxzj";
  private static final String CREATE_ENV1_SIG_NAME =
    "http://localhost:8080/won/resource/event/td3u9uqz1pismn4qtn92#envelope-lxzj-sig";
  private static final String CREATE_ENV2_NAME =
    "http://localhost:8080/won/resource/event/td3u9uqz1pismn4qtn92#envelope-6dv8";
  private static final String CREATE_ENV2_SIG_NAME =
    "http://localhost:8080/won/resource/event/td3u9uqz1pismn4qtn92#envelope-6dv8-sig";
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

  private static final String TEXT_ENV1_NAME =
    "http://localhost:8080/won/resource/event/z7rgjxnyvjpo3l9m0d79#envelope-f65t";
  private static final String TEXT_ENV1_SIG_NAME =
    "http://localhost:8080/won/resource/event/z7rgjxnyvjpo3l9m0d79#envelope-f65t-sig";
  private static final String TEXT_ENV2_NAME =
    "http://localhost:8080/won/resource/event/z7rgjxnyvjpo3l9m0d79#envelope-qdpq";
  private static final String TEXT_ENV2_SIG_NAME =
    "http://localhost:8080/won/resource/event/z7rgjxnyvjpo3l9m0d79#envelope-qdpq-sig";
  private static final String TEXT_ENV3_NAME =
    "http://localhost:8080/won/resource/event/m8cjzr6892213okiek04#envelope-5t8c";
  private static final String TEXT_ENV3_SIG_NAME =
    "http://localhost:8080/won/resource/event/m8cjzr6892213okiek04#envelope-5t8c-sig";
  private static final String TEXT_ENV4_NAME =
    "http://localhost:8080/won/resource/event/m8cjzr6892213okiek04#envelope-ojt8";
  private static final String TEXT_ENV4_SIG_NAME =
    "http://localhost:8080/won/resource/event/m8cjzr6892213okiek04#envelope-ojt8-sig";

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
  public void testMetaAndSignerConsistencyFromSystem() throws IOException {


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

  }

  @Test
  public void testMetaAndSignerConsistencyFromOwner() throws IOException {


    // Test fromOwner leaf envelope signer consistency
    Dataset invalidDataset = WonRdfUtils.MessageUtils.copyByDatasetSerialization(new WonMessage(createMessageDataset)
    ).getCompleteDataset();
    Model env1sigModel = invalidDataset.getNamedModel(CREATE_ENV1_SIG_NAME);

    // create invalid dataset by replacing a signer in leaf envelope
    Statement stmtOld = env1sigModel.createStatement(ResourceFactory.createResource(CREATE_ENV1_SIG_NAME),
                                           SFSIG.HAS_VERIFICATION_CERT,
                                           ResourceFactory.createResource(NEED_LOCAL_URI));
    env1sigModel.remove(stmtOld);

    Statement stmtNew = env1sigModel.createStatement(ResourceFactory.createResource(CREATE_ENV1_SIG_NAME),
                                           SFSIG.HAS_VERIFICATION_CERT,
                                           ResourceFactory.createResource(NODE_LOCAL_URI));
    env1sigModel.add(stmtNew);


    WonMessageValidator validator = new WonMessageValidator();
    StringBuilder message = new StringBuilder();
    // validate this invalid dataset
    boolean valid = validator.validate(invalidDataset, message);
    Assert.assertTrue("validation is expected to fail", !valid);
    Assert.assertTrue(message.toString().contains("invalid_from_owner_signer"));

    //reset for further testing:
    env1sigModel.remove(stmtNew);
    env1sigModel.add(stmtOld);

    // Test fromOwner non-leaf envelopes signer consistency
    invalidDataset = WonRdfUtils.MessageUtils.copyByDatasetSerialization(new WonMessage(textMessageDataset)
    ).getCompleteDataset();
    Model env2sigModel = invalidDataset.getNamedModel(TEXT_ENV2_SIG_NAME);

    // create invalid dataset by replacing a signer in non-leaf envelope
    stmtOld = env2sigModel.createStatement(ResourceFactory.createResource(TEXT_ENV2_SIG_NAME),
                                                     SFSIG.HAS_VERIFICATION_CERT,
                                                     ResourceFactory.createResource(NODE_LOCAL_URI));
    env2sigModel.remove(stmtOld);

    stmtNew = env2sigModel.createStatement(ResourceFactory.createResource(TEXT_ENV2_SIG_NAME),
                                                     SFSIG.HAS_VERIFICATION_CERT,
                                                     ResourceFactory.createResource(NEED_LOCAL_URI));
    env2sigModel.add(stmtNew);


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


  @Test
  public void testMetaAndSignerConsistencyFromExternal() throws IOException {


    // Test fromExternal close to leaf envelope signer consistency
    Dataset invalidDataset = WonRdfUtils.MessageUtils.copyByDatasetSerialization(new WonMessage(textMessageDataset)
    ).getCompleteDataset();
    Model env3sigModel = invalidDataset.getNamedModel(TEXT_ENV3_SIG_NAME);

    // create invalid dataset by replacing a signer - sender node - in close to leaf envelope
    Statement stmtOld = env3sigModel.createStatement(ResourceFactory.createResource(TEXT_ENV3_SIG_NAME),
                                                     SFSIG.HAS_VERIFICATION_CERT,
                                                     ResourceFactory.createResource(NODE_LOCAL_URI));
    env3sigModel.remove(stmtOld);

    Statement stmtNew = env3sigModel.createStatement(ResourceFactory.createResource(TEXT_ENV3_SIG_NAME),
                                                     SFSIG.HAS_VERIFICATION_CERT,
                                                     ResourceFactory.createResource(NODE_REMOTE_URI));
    env3sigModel.add(stmtNew);


    WonMessageValidator validator = new WonMessageValidator();
    StringBuilder message = new StringBuilder();
    // validate this invalid dataset
    boolean valid = validator.validate(invalidDataset, message);
    Assert.assertTrue("validation is expected to fail", !valid);
    Assert.assertTrue(message.toString().contains("invalid_from_external_signer"));

    //reset for further testing:
    env3sigModel.remove(stmtNew);
    env3sigModel.add(stmtOld);

    // Test fromExternal non-leaf envelopes signer consistency
    Model env4sigModel = invalidDataset.getNamedModel(TEXT_ENV4_SIG_NAME);

    // create invalid dataset by replacing a signer in non-leaf envelope
    stmtOld = env4sigModel.createStatement(ResourceFactory.createResource(TEXT_ENV4_SIG_NAME),
                                           SFSIG.HAS_VERIFICATION_CERT,
                                           ResourceFactory.createResource(NODE_LOCAL_URI));
    env4sigModel.remove(stmtOld);

    stmtNew = env4sigModel.createStatement(ResourceFactory.createResource(TEXT_ENV4_SIG_NAME),
                                           SFSIG.HAS_VERIFICATION_CERT,
                                           ResourceFactory.createResource(NEED_LOCAL_URI));
    env4sigModel.add(stmtNew);


    validator = new WonMessageValidator();
    message = new StringBuilder();
    // validate this invalid dataset
    valid = validator.validate(invalidDataset, message);
    Assert.assertTrue("validation is expected to fail", !valid);
    Assert.assertTrue(message.toString().contains("invalid_from_external_signer"));

    //reset for further testing:
    env4sigModel.remove(stmtNew);
    env4sigModel.add(stmtOld);

  }


  @Test
  public void testSignatureRequiredProperties() throws IOException {


    // Test signature of the 1st envelope
    Dataset invalidDataset = WonRdfUtils.MessageUtils.copyByDatasetSerialization(new WonMessage(createMessageDataset)
    ).getCompleteDataset();
    Model env1sigModel = invalidDataset.getNamedModel(CREATE_ENV1_SIG_NAME);

    StmtIterator iter = env1sigModel.listStatements(ResourceFactory.createResource(CREATE_ENV1_SIG_NAME),
                                             SFSIG.HAS_GRAPH_SIGNING_METHOD,
                                             RdfUtils.EMPTY_RDF_NODE);
    Statement stmtOld = iter.removeNext();

    WonMessageValidator validator = new WonMessageValidator();
    StringBuilder message = new StringBuilder();
    // validate this invalid dataset
    boolean valid = validator.validate(invalidDataset, message);
    Assert.assertTrue("validation is expected to fail", !valid);
    Assert.assertTrue(message.toString().contains("signature_properties"));

    //reset for further testing:
    env1sigModel.add(stmtOld);


    iter = env1sigModel.listStatements(ResourceFactory.createResource(CREATE_ENV1_SIG_NAME),
                                                    WONMSG.HAS_SIGNED_GRAPH_PROPERTY,
                                                    RdfUtils.EMPTY_RDF_NODE);
    Statement stmtModified = iter.nextStatement();
    stmtModified.changeObject(ResourceFactory.createResource("test:object:uri"));
    env1sigModel.add(stmtModified);

    //String test = RdfUtils.writeDatasetToString(invalidDataset, Lang.TRIG);
    //System.out.println("OUT:\n" + test);

    validator = new WonMessageValidator();
    message = new StringBuilder();
    // validate this invalid dataset
    valid = validator.validate(invalidDataset, message);
    Assert.assertTrue("validation is expected to fail", !valid);
    Assert.assertTrue(message.toString().contains("signature_properties"));

    //reset for further testing:
    env1sigModel.add(stmtOld);



  }


  @Test
  public void testSignatureReferenceValues() throws IOException {


    // Test signature of the 1st envelope: replace value of the signature with some dummy value
    Dataset invalidDataset = WonRdfUtils.MessageUtils.copyByDatasetSerialization(new WonMessage(createMessageDataset)
    ).getCompleteDataset();
    Model env1sigModel = invalidDataset.getNamedModel(CREATE_ENV1_SIG_NAME);

    StmtIterator iter = env1sigModel.listStatements(ResourceFactory.createResource(CREATE_ENV1_SIG_NAME),
                                                    SFSIG.HAS_SIGNATURE_VALUE,
                                                    RdfUtils.EMPTY_RDF_NODE);
    Statement stmtOld = iter.removeNext();

    Statement stmtNew = env1sigModel.createStatement(ResourceFactory.createResource(CREATE_ENV1_SIG_NAME),
                                                     SFSIG.HAS_SIGNATURE_VALUE,
                                                     ResourceFactory.createPlainLiteral("eve's value"));
    env1sigModel.add(stmtNew);


    WonMessageValidator validator = new WonMessageValidator();
    StringBuilder message = new StringBuilder();
    // validate this invalid dataset
    boolean valid = validator.validate(invalidDataset, message);
    Assert.assertTrue("validation is expected to fail", !valid);
    Assert.assertTrue(message.toString().contains("signature_reference_values"));

    //reset for further testing:
    env1sigModel.add(stmtOld);

  }

  @Test
  public void testAllGraphsSigned() throws IOException {

    // this check actually is redundant with other checks,
    // therefore the error message can be from any of those...


    // create a dataset where there is a non-signature graph that is not signed
    Dataset invalidDataset = WonRdfUtils.MessageUtils.copyByDatasetSerialization(new WonMessage(createMessageDataset)
    ).getCompleteDataset();
    Model env2sigModel = invalidDataset.getNamedModel(CREATE_ENV2_SIG_NAME);
    StmtIterator iter = env2sigModel.listStatements(ResourceFactory.createResource(CREATE_ENV2_SIG_NAME),
                                                    WONMSG.HAS_SIGNED_GRAPH_PROPERTY,
                                                    RdfUtils.EMPTY_RDF_NODE);
    Statement stmtOld = iter.removeNext();
    Statement stmtNew = env2sigModel.createStatement(ResourceFactory.createResource(CREATE_ENV2_SIG_NAME),
                                                     WONMSG.HAS_SIGNED_GRAPH_PROPERTY,
                                                     ResourceFactory.createResource("test:resource:uri"));
    env2sigModel.add(stmtNew);

    WonMessageValidator validator = new WonMessageValidator();
    StringBuilder message = new StringBuilder();
    // validate this invalid dataset
    boolean valid = validator.validate(invalidDataset, message);
    Assert.assertTrue("validation is expected to fail", !valid);
    // actually
    Assert.assertTrue(message.toString().contains("signed_or_signature") || message.toString().contains("signer") ||
                        message.toString().contains("graph_uris"));

    //reset for further testing:
    env2sigModel.add(stmtOld);
    env2sigModel.remove(stmtNew);

  }

  @Test
  public void testSignatureChainParallelToEnvelopeChain() throws IOException {


    // create a dataset where the first envelope references the signature of the second envelope instead of content's
    // and the 2nd references the signature of the content
    Dataset invalidDataset = WonRdfUtils.MessageUtils.copyByDatasetSerialization(new WonMessage(createMessageDataset)
    ).getCompleteDataset();
    Model env1Model = invalidDataset.getNamedModel(CREATE_ENV1_NAME);
    StmtIterator iter = env1Model.listStatements(null,
                                                    WONMSG.HAS_SIGNATURE_GRAPH_PROPERTY,
                                                    RdfUtils.EMPTY_RDF_NODE);
    Statement stmtOld = iter.removeNext();
    iter = env1Model.listStatements(null,
                                                    WONMSG.HAS_SIGNED_GRAPH_PROPERTY,
                                                    RdfUtils.EMPTY_RDF_NODE);
    Statement stmtOld2 = iter.removeNext();





    Statement stmtNew = stmtOld.changeObject(ResourceFactory.createResource(CREATE_ENV1_SIG_NAME));
    env1Model.add(stmtNew);
    Statement stmtNew2 = stmtOld2.changeObject(ResourceFactory.createResource(CREATE_ENV1_NAME));
    env1Model.add(stmtNew2);

    Model env2Model = invalidDataset.getNamedModel(CREATE_ENV2_NAME);
    iter = env2Model.listStatements(null, WONMSG.HAS_SIGNATURE_GRAPH_PROPERTY,
                                    RdfUtils.EMPTY_RDF_NODE);
    Statement stmtOld3 = iter.removeNext();
    iter = env2Model.listStatements(null,
                                    WONMSG.HAS_SIGNED_GRAPH_PROPERTY,
                                    RdfUtils.EMPTY_RDF_NODE);
    Statement stmtOld4 = iter.removeNext();
    Statement stmtNew3 = stmtOld3.changeObject(ResourceFactory.createResource(CREATE_CONTENT_NAME_SIG));
    env2Model.add(stmtNew3);
    Statement stmtNew4 = stmtOld4.changeObject(ResourceFactory.createResource(CREATE_CONTENT_NAME));
    env2Model.add(stmtNew4);


//    String test = RdfUtils.writeDatasetToString(invalidDataset, Lang.TRIG);
//    System.out.println("OUT:\n" + test);

    WonMessageValidator validator = new WonMessageValidator();
    StringBuilder message = new StringBuilder();
    // validate this invalid dataset
    boolean valid = validator.validate(invalidDataset, message);
    Assert.assertTrue("validation is expected to fail", !valid);
    // actually
    Assert.assertTrue(message.toString().contains("signature_chain"));

    //reset for further testing:
    env1Model.add(stmtOld);
    env1Model.remove(stmtNew);
    env1Model.add(stmtOld2);
    env1Model.remove(stmtNew2);
    env2Model.add(stmtOld3);
    env2Model.remove(stmtNew3);
    env2Model.add(stmtOld4);
    env2Model.remove(stmtNew4);

  }

  @Test
  public void testGraphUris() throws IOException {


    // create a dataset with invalid content uris - i.e. replace valid content graph name with
    // the one that does not start with the corresponding event uri: replace the uri in the
    // respective envelope content and content signature reference, as well as in the content signature
    Dataset invalidDataset = WonRdfUtils.MessageUtils.copyByDatasetSerialization(new WonMessage(createMessageDataset)
    ).getCompleteDataset();
    Model contModel = invalidDataset.getNamedModel(CREATE_CONTENT_NAME);
    invalidDataset.removeNamedModel(CREATE_CONTENT_NAME);
    String dummyName = "test:graph:uri";
    invalidDataset.addNamedModel(dummyName, contModel);

    Model env1Model = invalidDataset.getNamedModel(CREATE_ENV1_NAME);

    StmtIterator iter = env1Model.listStatements(null,
                                                    WONMSG.HAS_CONTENT_PROPERTY,
                                                    RdfUtils.EMPTY_RDF_NODE);
    Statement stmtOld = iter.removeNext();
    Statement stmtNew = env1Model.createStatement(stmtOld.getSubject(),
                                                  stmtOld.getPredicate(),
                                                  ResourceFactory.createResource(dummyName));
    env1Model.add(stmtNew);

    iter = env1Model.listStatements(null,
                                    WONMSG.HAS_SIGNED_GRAPH_PROPERTY,
                                    ResourceFactory.createResource(CREATE_CONTENT_NAME));
    Statement stmtOld2 = iter.removeNext();
    Statement stmtNew2 = env1Model.createStatement(stmtOld2.getSubject(),
                                                  stmtOld2.getPredicate(),
                                                  ResourceFactory.createResource(dummyName));
    env1Model.add(stmtNew2);

    Model sigModel = invalidDataset.getNamedModel(CREATE_CONTENT_NAME_SIG);
    iter = sigModel.listStatements(null,
                                    WONMSG.HAS_SIGNED_GRAPH_PROPERTY,
                                    ResourceFactory.createResource(CREATE_CONTENT_NAME));
    Statement stmtOld3 = iter.removeNext();
    Statement stmtNew3 = sigModel.createStatement(stmtOld3.getSubject(),
                                                   stmtOld3.getPredicate(),
                                                   ResourceFactory.createResource(dummyName));
    sigModel.add(stmtNew3);


    //String test = RdfUtils.writeDatasetToString(invalidDataset, Lang.TRIG);
    //System.out.println("OUT:\n" + test);


    WonMessageValidator validator = new WonMessageValidator();
    StringBuilder message = new StringBuilder();
    // validate this invalid dataset
    boolean valid = validator.validate(invalidDataset, message);
    Assert.assertTrue("validation is expected to fail", !valid);
    // actually
    Assert.assertTrue(message.toString().contains("graph_uris"));

    //reset for further testing:
    //env2sigModel.add(stmtOld);
    //env2sigModel.remove(stmtNew);

  }

  @Test
  public void testEventConsistency() throws IOException {


    // create a dataset with invalid remoteEvent uri by replacing the original remote event uri
    // with the dummy uri
    Dataset invalidDataset = WonRdfUtils.MessageUtils.copyByDatasetSerialization(new WonMessage(textMessageDataset)
    ).getCompleteDataset();
    Model envModel = invalidDataset.getNamedModel(TEXT_ENV3_NAME);
    String dummyName = TEXT_ENV3_NAME;

    StmtIterator iter = envModel.listStatements(null,
                                                 WONMSG.HAS_CORRESPONDING_REMOTE_MESSAGE,
                                                 RdfUtils.EMPTY_RDF_NODE);
    Statement stmtOld = iter.removeNext();
    Statement stmtNew = envModel.createStatement(stmtOld.getSubject(),
                                                  stmtOld.getPredicate(),
                                                  ResourceFactory.createResource(dummyName));
    envModel.add(stmtNew);



    String test = RdfUtils.writeDatasetToString(invalidDataset, Lang.TRIG);
    System.out.println("OUT:\n" + test);


    WonMessageValidator validator = new WonMessageValidator();
    StringBuilder message = new StringBuilder();
    // validate this invalid dataset
    boolean valid = validator.validate(invalidDataset, message);
    Assert.assertTrue("validation is expected to fail", !valid);
    // actually
    Assert.assertTrue(message.toString().contains("number_of_events"));

    //reset for further testing:
    //env2sigModel.add(stmtOld);
    //env2sigModel.remove(stmtNew);

  }


}
