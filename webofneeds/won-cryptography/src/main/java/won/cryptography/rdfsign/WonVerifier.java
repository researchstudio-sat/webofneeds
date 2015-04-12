package won.cryptography.rdfsign;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.*;
import de.uni_koblenz.aggrimm.icp.crypto.sign.algorithm.SignatureAlgorithmInterface;
import de.uni_koblenz.aggrimm.icp.crypto.sign.algorithm.algorithm.SignatureAlgorithmFisteus2010;
import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.GraphCollection;
import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.SignatureData;
import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.Triple;
import de.uni_koblenz.aggrimm.icp.crypto.sign.ontology.Ontology;
import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.message.SignatureReference;
import won.protocol.util.RdfUtils;
import won.protocol.vocabulary.SFSIG;
import won.protocol.vocabulary.WONMSG;

import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.util.LinkedList;
import java.util.Map;

/**
 * User: ypanchenko
 * Date: 15.07.2014
 */
public class WonVerifier
{

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private Dataset dataset;

  private SignatureVerificationResult result = new SignatureVerificationResult();

  public WonVerifier(Dataset dataset) {
    Provider provider = new BouncyCastleProvider();
    Security.addProvider(provider);
    this.dataset = dataset;
    prepareForVerifying();
  }

  /**
   * find corresponding signature graphs for all non-signature graphs
   */
  private void prepareForVerifying() {

    for (String graphURI : RdfUtils.getModelNames(dataset)) {
      Model model = dataset.getNamedModel(graphURI);
      if (isSignatureGraph(graphURI, model)) {
        addSignatureToResult(graphURI, model);
      } else {
        result.addSignedGraphName(graphURI);
        if (hasSignatureReference(graphURI, model)) {
          addSignatureReferenceToResult(graphURI, model);
        }
      }
    }
  }

  public SignatureVerificationResult getVerificationResult() {
    return result;
  }

  //TODO exceptions
  public boolean verify(Map<String,PublicKey> publicKeys) throws Exception {

    // check that the default graph is empty
    if (dataset.getDefaultModel().listStatements().hasNext()) {
      result.verificationFailed("unsigned data found in default graph");
      return result.isVerificationPassed();
    }

    // verify each signature's graph
    for (String sigGraphName : result.getSignatureGraphNames()) {
      // extract signature graph, signature data and corresponding signed graph
      String signedGraphName = result.getSignedGraphName(sigGraphName);
      GraphCollection inputGraph = ModelConverter.modelToGraphCollection(signedGraphName, dataset);
      GraphCollection sigGraph = ModelConverter.modelToGraphCollection(sigGraphName, dataset);
      Ontology o = new Ontology();
      LinkedList<Triple> sigTriples = new LinkedList<Triple>();
      sigTriples.addAll(sigGraph.getGraphs().get(0).getTriples());
      SignatureData sigData = o.getSignatureDataFromTriples(sigTriples);
      inputGraph.setSignature(sigData);
      // sigData.getVerificationCertificate() is not supported at the moment by signingframework and returns null
      // String certName = sigData.getVerificationCertificate();
      // therefore, the url of the public key/certificate to use is extracted manually here:
      PublicKey publicKey = null;
      Model sigModel = dataset.getNamedModel(sigGraphName);
      Resource resource = sigModel.getResource(sigGraphName);
      NodeIterator ni = sigModel.listObjectsOfProperty(resource, SFSIG.HAS_VERIFICATION_CERT);
      if (ni.hasNext()) {
        String cert = ni.next().asResource().getURI();
        publicKey = publicKeys.get(cert);
      }
      if (publicKey == null) {
        result.setVerificationFailed(sigGraphName, "No public key found for " + sigGraphName);
        return result.isVerificationPassed();
      }

      // normalize, hash and post-hash signed graph data

      // Get algorithms to use from signature data
      SignatureAlgorithmInterface canonicAlgorithm = getCanonicalizationAlgorithm(sigData);
      SignatureAlgorithmInterface hashingAlgorithm = getHashingAlgorithm(sigData);
      // Canonicalize
      if (canonicAlgorithm != null) {
        canonicAlgorithm.canonicalize(inputGraph);
        canonicAlgorithm.postCanonicalize(inputGraph);
      } else {
        throw new Exception(
          "No algorithm found for graph canoncialization method '" + sigData.getCanonicalizationMethod() + "'");
      }
      // Hash
      if (hashingAlgorithm != null) {
        hashingAlgorithm.hash(inputGraph, sigData.getDigestGen().getAlgorithm().toLowerCase());
        hashingAlgorithm.postHash(inputGraph);
      } else {
        throw new Exception("No algorithm found for graph digest method '" + sigData.getGraphDigestMethod() + "'");
      }
      // Verify
      boolean verified = verify(inputGraph, publicKey);
      if (verified) {
        result.setVerificationPassed(sigGraphName);
      } else {
        result.setVerificationFailed(sigGraphName, "Failed to verify " + sigGraphName);
        // interrupt verification process if one of the graph's verification fails
        return result.isVerificationPassed();
      }
    }

    return result.isVerificationPassed();
  }


  private boolean verify(final GraphCollection graph, PublicKey publicKey) throws Exception {

    SignatureData sigData = graph.getSignature();
    Signature sig = Signature.getInstance(WonSigner.SIGNING_ALGORITHM_NAME,
                                          WonSigner.SIGNING_ALGORITHM_PROVIDER);

    String sigString = sigData.getSignature();
    if (sigString == null) {
      throw new Exception("Signature value not found");
    }
    if (sigString.length() == 0) {
      throw new Exception("Signature value is empty");
    }

    byte[] sigBytes = Base64.decodeBase64(sigString);
    sig.initVerify(publicKey);
    sig.update(sigData.getHash().toByteArray());
    return sig.verify(sigBytes);
  }

  //cannot use SignatureAlgorithmList and their algorithms here because they are not thread-safe
//  private SignatureAlgorithmInterface getHashingAlgorithm(final SignatureData sigData) {
//
//    SignatureAlgorithmInterface algorithm = null;
//    for (SignatureAlgorithmInterface a : SignatureAlgorithmList.getList()) {
//      //Get hashing algorithm
//      if ((Ontology.getDigestPrefix() + a.getName()).equals(sigData.getGraphDigestMethod())) {
//        return a;
//      }
//    }
//    return algorithm;
//  }
//  private SignatureAlgorithmInterface getCanonicalizationAlgorithm(final SignatureData sigData) {
//
//    SignatureAlgorithmInterface algorithm = null;
//    for (SignatureAlgorithmInterface a : SignatureAlgorithmList.getList()) {
//      //Get canonicalization algorithm
//      if ((Ontology.getCanonicalizationPrefix() + a.getName()).equals(sigData.getCanonicalizationMethod())) {
//        return a;
//      }
//    }
//    return algorithm;
//  }


  private SignatureAlgorithmInterface getHashingAlgorithm(final SignatureData sigData) {

    SignatureAlgorithmInterface algorithm = null;
    if (SFSIG.DIGEST_METHOD_Fisteus2010.getURI().endsWith(sigData.getGraphDigestMethod())) {
      // this is not efficient, but sharing the algorithm was not possible due to its thread-not-safety
      algorithm = new SignatureAlgorithmFisteus2010();
    }
    // TODO for other algorithms
    return algorithm;
  }

  private SignatureAlgorithmInterface getCanonicalizationAlgorithm(final SignatureData sigData) {

    SignatureAlgorithmInterface algorithm = null;
    if (SFSIG.CANONICALIZATION_METHOD_Fisteus2010.getURI().endsWith(sigData.getCanonicalizationMethod())) {
      // this is not efficient, but sharing the algorithm was not possible due to its thread-not-safety
      algorithm = new SignatureAlgorithmFisteus2010();
    }
    // TODO for other algorithms
    return algorithm;
  }

  public static boolean isSignatureGraph(String graphUri, Model model) {
    // TODO check the presence of all the required triples
    Resource resource = model.getResource(graphUri);
    Property typeProp = model.createProperty(Ontology.getW3CSyntaxURI(), "type");
    Resource sigRes = model.createResource(Ontology.getSigIri() + "Signature");
    StmtIterator si = model.listStatements(resource, typeProp, sigRes);
    if (si.hasNext()) {
      return true;
    }
    return false;
  }

  public static boolean isSignature(Model model) {
    // TODO check the presence of all the required triples
    Property typeProp = model.createProperty(Ontology.getW3CSyntaxURI(), "type");
    Resource sigRes = model.createResource(Ontology.getSigIri() + "Signature");
    StmtIterator si = model.listStatements(null, typeProp, sigRes);
    if (si.hasNext()) {
      return true;
    }
    return false;
  }

  private void addSignatureToResult(final String graphUri, final Model model) {
    String signedGraphUri = null;
    String signatureValue = null;
    Resource resource = model.getResource(graphUri);
    NodeIterator ni = model.listObjectsOfProperty(resource, WONMSG.HAS_SIGNED_GRAPH_PROPERTY);
    if (ni.hasNext()) {
      signedGraphUri = ni.next().asResource().getURI();
    }
    Property hasSigValueProp = model.createProperty(Ontology.getSigIri(), "hasSignatureValue");
    NodeIterator ni2 = model.listObjectsOfProperty(resource, hasSigValueProp);
    if (ni2.hasNext()) {
      signatureValue = ni2.next().asLiteral().toString();
    }
    if (signedGraphUri != null && signatureValue != null) {
      result.addSignatureData(graphUri, signedGraphUri, signatureValue);
    }
  }


  private void addSignatureReferenceToResult(final String graphURI, final Model model) {


    RDFNode tempNode = null;
    StmtIterator si = model.listStatements(null, WONMSG.REFERENCES_SIGNATURE_PROPERTY, tempNode);
    while (si.hasNext()) {
      SignatureReference sigRef = new SignatureReference();
      // should be a blank node
      Resource refObj = si.next().getObject().asResource();
      sigRef.setReferencerGraphUri(graphURI);
      NodeIterator signedGraphIter = model.listObjectsOfProperty(refObj, WONMSG.HAS_SIGNED_GRAPH_PROPERTY);
      if (signedGraphIter.hasNext()) {
        sigRef.setSignedGraphUri(signedGraphIter.next().asResource().getURI());
      }
      NodeIterator sigValueIter = model.listObjectsOfProperty(refObj, WONMSG.HAS_SIGNATURE_VALUE_PROPERTY);
      if (sigValueIter.hasNext()) {
        sigRef.setSignatureValue(sigValueIter.next().asLiteral().getString());
      }
      NodeIterator sigGraphIter = model.listObjectsOfProperty(refObj,
                                                             WONMSG.HAS_SIGNATURE_GRAPH_PROPERTY);
      if (sigGraphIter.hasNext()) {
        sigRef.setSignatureGraphUri(sigGraphIter.next().asResource().getURI());
      }
      result.addSignatureReference(sigRef);
    }

  }

  private boolean hasSignatureReference(final String graphURI, final Model model) {
    RDFNode tempNode = null;
    StmtIterator si =
      dataset.getNamedModel(graphURI).listStatements(null, WONMSG.REFERENCES_SIGNATURE_PROPERTY, tempNode);
    if (si.hasNext()) {
      return true;
    }
    return false;
  }

}
