package won.cryptography.rdfsign;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.*;
import de.uni_koblenz.aggrimm.icp.crypto.sign.algorithm.SignatureAlgorithmInterface;
import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.GraphCollection;
import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.SignatureData;
import de.uni_koblenz.aggrimm.icp.crypto.sign.ontology.Ontology;
import sun.misc.BASE64Encoder;

import java.security.PrivateKey;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by ypanchenko on 12.06.2014.
 */
public class WonSigner
{

  public static final String SIGNING_ALGORITHM_NAME = "NONEwithECDSA";
  public static final String SIGNING_ALGORITHM_PROVIDER = "BC";
  //TODO which hashing algorithm to use?
  public static final String ENV_HASH_ALGORITHM = "sha-256";


  private SignatureAlgorithmInterface algorithm;
  private Dataset dataset;
  private List<String> signedGraphURIs = new ArrayList<String>();
  private boolean signSig = true;
  //private  byte[] signature;

  /**
   * All the graphs or graphs' signatures are to be signed:
   * all the graphs that don't have signatures will be signed,
   * all the graphs that already have signatures will not be re-signed
   * but their signatures will be put into a separate named graph(s)
   * and signed.
   */
  public WonSigner(Dataset dataset, SignatureAlgorithmInterface algorithm) {
    this.dataset = dataset;
    this.algorithm = algorithm;
    this.signSig = true;
    initSignedGraphURIs();
    prepareForSigning();
    //TODO check maybe it's not needed here
    //provider = new BouncyCastleProvider();
    //Security.addProvider(provider);
  }

//  /**
//   * Only the specified (by name) graphs or their signatures are to be signed:
//   * the graphs whose named are specified and that don't have signatures will
//   * be signed, those among the specified the graphs that already have
//   * signatures will not be re-signed, but if signSignatures is set to true, then
//   * their signatures will be put into a separate named graph(s) and signed.
//   */
//  public WonSigner(Dataset dataset, SignatureAlgorithmInterface algorithm, List<String> signedGraphURIs) {
//    //TODO add the option to specify wheather to sign signature?
//    //, boolean signSignatures)
//
//    this.dataset = dataset;
//    this.algorithm = algorithm;
//    this.signedGraphURIs.addAll(signedGraphURIs);
//    //this.signSig = signSignatures;
//  }


  private void initSignedGraphURIs() {
    Iterator<String> namesIterator = dataset.listNames();
    while (namesIterator.hasNext()) {
      this.signedGraphURIs.add(namesIterator.next());
    }
  }

  //TODO chng exceptions to won exceptions?
  public void sign(PrivateKey privateKey) throws Exception {

    //TODO how to preserve shared between graphs blank nodes?
    //i.e. Jena writes in TRIG blank nodes as anonimous nodes without labels
    //TODO how to reflect shared blank node in signature? I.e the way it works
    //now the attacker could replace _:a nodes shared between
    //the graphs to _:b in one of the graphs and the signature would
    //still be valid... :(
    //TODO check exactty what happens in canonization step if there
    //is a way around. Or, in worst case, the approach have to be
    //changed: the algorithm should be directly reimplemented to
    //work on Dataset or GraphCollection that has a Signature
    //associated with each NamedGraph...
    for (String name : this.signedGraphURIs) {
      Model model = dataset.getNamedModel(name);
      // create GraphCollection with one NamedGraph that corresponds to this Model
      GraphCollection inputGraph = ModelConverter.modelToGraphCollection(name, dataset);
      //sign the NamedGraph inside that GraphCollection
      signNamedGraph(inputGraph, privateKey);
      // assemble resulting signature into the original Dataset
      WonAssembler.assemble(inputGraph, dataset);
    }

  }

  /**
   * Exclude from signedGraphURIs the graphs that already have signature but include
   * those signatures as separate graphs, also adding them to the Dataset.
   */
  private void prepareForSigning() {

    List<String> updatedSignedGraphURIs = new ArrayList<String>();
    for (String graphURI : signedGraphURIs) {
      Resource resource = dataset.getDefaultModel().getResource(graphURI);
      RDFNode tempNode = null;
      StmtIterator graphSubjStmtIterator =
        dataset.getDefaultModel().listStatements(resource, null, tempNode);
      if (graphSubjStmtIterator.hasNext()) {
        Model model = ModelFactory.createDefaultModel();
        add(model, graphSubjStmtIterator);
        String sigIri = Ontology.getSigIri();
        Property propTemp = dataset.getDefaultModel().createProperty(sigIri, "hasGraphSigningMethod");
        NodeIterator objOfGraphSubjIter = dataset.getDefaultModel().listObjectsOfProperty(resource, propTemp);
        // we except only one node here
        RDFNode objOfGraphSubj = objOfGraphSubjIter.next();
        StmtIterator sigMethodSubjStmtIterator =
          dataset.getDefaultModel().listStatements(objOfGraphSubj.asResource(), null, tempNode);
        add(model, sigMethodSubjStmtIterator);
        //graphUriToSigModel.put(graphURI, model);
        String sigGraphURI = graphURI + "-SIG";
        dataset.getDefaultModel().remove(model);
        dataset.addNamedModel(sigGraphURI, model);
        updatedSignedGraphURIs.add(sigGraphURI);
      } else {
        updatedSignedGraphURIs.add(graphURI);
      }
    }
    this.signedGraphURIs = updatedSignedGraphURIs;

  }

  private void add(final Model toModel, final StmtIterator stmtIterator) {
    while (stmtIterator.hasNext()) {
      toModel.add(stmtIterator.next());
    }
  }

  private void signNamedGraph(final GraphCollection inputWithOneNamedGraph, final PrivateKey privateKey)
    throws Exception {
    this.algorithm.canonicalize(inputWithOneNamedGraph);
    this.algorithm.postCanonicalize(inputWithOneNamedGraph);
    this.algorithm.hash(inputWithOneNamedGraph, ENV_HASH_ALGORITHM);
    this.algorithm.postHash(inputWithOneNamedGraph);
    //this.algorithm.sign(inputWithOneNamedGraph, privateKey, "\"cert\"");
    sign(inputWithOneNamedGraph, privateKey, "\"cert\"");
  }

  private void sign(GraphCollection gc, PrivateKey privateKey, String verficiationCertificate) throws Exception {
    //Signature Data existing?
    if (!gc.hasSignature()) {
      throw new Exception("GraphCollection has no signature data. Call 'canonicalize' and 'hash' methods first.");
    }

    //Get Signature Data
    SignatureData sigData = gc.getSignature();
    // Sign
    Signature sig = Signature.getInstance(SIGNING_ALGORITHM_NAME, SIGNING_ALGORITHM_PROVIDER);
    sig.initSign(privateKey);
    sig.update(sigData.getHash().toByteArray());

    byte[] signatureBytes = sig.sign();
    String signature = new BASE64Encoder().encode(signatureBytes);


    //Update Signature Data
    //TODO is there a better way to escape new lines in the signature?
    // is there anything else that need to be escaped?
    //what if there will be 3 double quotes in a row in signature? or it cannot be?
    sigData.setSignature("\"\"\"" + signature + "\"\"\"");
    sigData.setSignatureMethod(privateKey.getAlgorithm().toLowerCase());
    sigData.setVerificationCertificate(verficiationCertificate);
  }

}
