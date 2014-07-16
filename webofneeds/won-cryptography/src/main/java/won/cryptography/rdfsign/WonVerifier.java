package won.cryptography.rdfsign;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.*;
import de.uni_koblenz.aggrimm.icp.crypto.sign.algorithm.SignatureAlgorithmInterface;
import de.uni_koblenz.aggrimm.icp.crypto.sign.algorithm.SignatureAlgorithmList;
import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.GraphCollection;
import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.SignatureData;
import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.Triple;
import de.uni_koblenz.aggrimm.icp.crypto.sign.ontology.Ontology;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import sun.misc.BASE64Decoder;

import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * User: ypanchenko
 * Date: 15.07.2014
 */
public class WonVerifier
{

  private BouncyCastleProvider provider;
  //TODO which hashing algorithm to use?
  private static String envHashAlgorithm = "sha-256";
  //private SignatureAlgorithmInterface algorithm;
  private Dataset dataset;
  private List<String> verifyGraphURIs = new ArrayList<String>();
  private List<String> currentGraphURIs = new ArrayList<String>();
  private List<Model> currentGraphSignatures = new ArrayList<Model>();
  private boolean signSig = true;
  //private  byte[] signature;
  private List<String> verifiedURIs = new ArrayList<String>();
  private boolean allVerified = true;

  //copy
  public WonVerifier(Dataset dataset
                     //, SignatureAlgorithmInterface algorithm
  ) {
    this.dataset = dataset;
    //this.algorithm = algorithm;
    this.signSig = true;
    initSignedGraphURIs();
    prepareForVerifying();
    //TODO check maybe it's not needed here
    provider = new BouncyCastleProvider();
    Security.addProvider(provider);
  }

  /**
   * Exclude from verification graphs that do not have signature in the default graph,
   * And associate signature as Model with its corresponding graph.
   */
  private void prepareForVerifying() {
    for (String graphURI : verifyGraphURIs) {
      Resource resource = dataset.getDefaultModel().getResource(graphURI);
      RDFNode tempNode = null;
      StmtIterator graphSubjStmtIterator =
        dataset.getDefaultModel().listStatements(resource, null, tempNode);
      if (graphSubjStmtIterator.hasNext()) { // there is a signature for this graphURI
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
        //String sigGraphURI = graphURI + "-SIG";
        //dataset.getDefaultModel().remove(model);
        //dataset.addNamedModel(sigGraphURI, model);
        currentGraphURIs.add(graphURI);
        currentGraphSignatures.add(model);
      }
    }
  }

  //copy
  private void add(final Model toModel, final StmtIterator stmtIterator) {
    while (stmtIterator.hasNext()) {
      toModel.add(stmtIterator.next());
    }
  }

  //copy
  private void initSignedGraphURIs() {
    Iterator<String> namesIterator = dataset.listNames();
    while (namesIterator.hasNext()) {
      this.verifyGraphURIs.add(namesIterator.next());
    }
  }

  //TODO chng exceptions to won exceptions?
  public boolean verify(PublicKey publicKey) throws Exception {

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

    return verify(currentGraphURIs, currentGraphSignatures, publicKey);
  }

  private boolean verify(final List<String> graphURIs,
                         final List<Model> graphSignatures,
                         final PublicKey publicKey) throws Exception {

    List<String> updatedVerifiedGraphURIs = new ArrayList<String>();
    List<Model> updatedGraphSignatures = new ArrayList<Model>();
    for (int i = 0; i < this.currentGraphURIs.size(); i++) {
      Model model = dataset.getNamedModel(currentGraphURIs.get(i));
      Model sigModel = currentGraphSignatures.get(i);
      String tempName = currentGraphURIs.get(i) + "-sig";
      dataset.addNamedModel(tempName, sigModel);
      GraphCollection inputGraph = ModelConverter.modelToGraphCollection(currentGraphURIs.get(i), dataset);
      GraphCollection sigGraph = ModelConverter.modelToGraphCollection(tempName, dataset);
      dataset.removeNamedModel(tempName);
      Ontology o = new Ontology();
      LinkedList<Triple> sigTriples = new LinkedList<Triple>();
      sigTriples.addAll(sigGraph.getGraphs().get(0).getTriples());
      SignatureData sigData = o.getSignatureDataFromTriples(sigTriples);
      inputGraph.setSignature(sigData);
      SignatureAlgorithmInterface canonicAlgorithm = getCanonicalizationAlgorithm(sigData);
      SignatureAlgorithmInterface hashingAlgorithm = getHashingAlgorithm(sigData);
      //TODO extract the public key from the signature triple/reference in signature triples?
      //Canonicalize
      if (canonicAlgorithm != null) {
        canonicAlgorithm.canonicalize(inputGraph);
        canonicAlgorithm.postCanonicalize(inputGraph);
      } else {
        throw new Exception(
          "No algorithm found for graph canoncialization method '" + sigData.getCanonicalizationMethod() + "'");
      }
      //Hash
      if (hashingAlgorithm != null) {
        hashingAlgorithm.hash(inputGraph, sigData.getDigestGen().getAlgorithm().toLowerCase());
        hashingAlgorithm.postHash(inputGraph);
      } else {
        throw new Exception("No algorithm found for graph digest method '" + sigData.getGraphDigestMethod() + "'");
      }
      //Verify
      //boolean verified = hashingAlgorithm.verify(inputGraph, publicKey);
      boolean verified = verify(hashingAlgorithm, inputGraph, publicKey);
      if (verified) {
        verifiedURIs.add(currentGraphURIs.get(i));
      }
      allVerified = allVerified && verified;

      // if the model itself represents a signature, this also would need to be verified
      // but with another public key...
      if (isSignature(model, dataset)) {
        Property typeProp = model.createProperty(Ontology.getW3CSyntaxURI(), "type");
        Resource sigRes = model.createResource(Ontology.getSigIri() + "signature");
        Resource resource = model.listSubjectsWithProperty(typeProp, sigRes).next();
        updatedVerifiedGraphURIs.add(resource.getURI());
        updatedGraphSignatures.add(model);
      }
    }

    if (updatedGraphSignatures.isEmpty()) {
      return allVerified;
    } else {
      //TODO what interface for providing another public key? Or only verify if
      //public key is present/referenced in the RDF (extract it)?
      //then  what guarantees the authenticity of the public key itself?
      //(sending the data and the signature separately from your public key greatly
      // reduces the likelihood of an attack)
      //return verify(updatedVerifiedGraphURIs, updatedGraphSignatures, publicKey);
      return allVerified;
    }
  }

  private boolean verify(final SignatureAlgorithmInterface hashingAlgorithm,
                         final GraphCollection graph,
                         final PublicKey publicKey) throws Exception {

    SignatureData sigData = graph.getSignature();
    Signature sig = Signature.getInstance("SHA256WithECDSA", "BC");

    String sigString = sigData.getSignature();
    if (sigString == null) {
      throw new Exception("Signature value not found");
    }
    if (sigString.length() == 0) {
      throw new Exception("Signature value is empty");
    }
    byte[] sigBytes = new BASE64Decoder().decodeBuffer(sigString);
    sig.initVerify(publicKey);
    sig.update(sigData.getHash().toByteArray());

    return sig.verify(sigBytes);
  }

  private SignatureAlgorithmInterface getHashingAlgorithm(final SignatureData sigData) {

    SignatureAlgorithmInterface algorithm = null;
    for (SignatureAlgorithmInterface a : SignatureAlgorithmList.getList()) {
      //Get hashing algorithm
      if ((Ontology.getDigestPrefix() + a.getName()).equals(sigData.getGraphDigestMethod())) {
        algorithm = a;
      }
    }
    return algorithm;
  }

  private SignatureAlgorithmInterface getCanonicalizationAlgorithm(final SignatureData sigData) {

    SignatureAlgorithmInterface algorithm = null;
    for (SignatureAlgorithmInterface a : SignatureAlgorithmList.getList()) {
      //Get canonicalization algorithm
      if ((Ontology.getCanonicalizationPrefix() + a.getName()).equals(sigData.getCanonicalizationMethod())) {
        algorithm = a;
      }
    }
    return algorithm;
  }


  //TODO improve and remove reduntant code in calling method
  private boolean isSignature(Model model, Dataset dataset) {
    Property typeProp = model.createProperty(Ontology.getW3CSyntaxURI(), "type");
    Resource sigRes = model.createResource(Ontology.getSigIri() + "signature");
    ResIterator ri = model.listSubjectsWithProperty(typeProp, sigRes);
    if (ri.hasNext() && dataset.getNamedModel(ri.next().getURI()) != null) {
      return true;
    }
    return false;
  }


  public List<String> getVerifiedURIs() {
    return verifiedURIs;
  }
}
