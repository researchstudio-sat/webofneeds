package won.cryptography.rdfsign;

import com.hp.hpl.jena.rdf.model.*;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import java.io.IOException;
import java.security.*;
import java.util.List;

/**
 * Created by ypanchenko on 12.06.2014.
 */
public class WonVerifier {

    private Model model;
    private SigningAlgorithm algorithm;

    public WonVerifier(Model model) {
        // in future the algorithm that was used for
        // signing should be resolved from the model
        // itself (should contain the signature info including
        // algorithm represented by an URI)
        this(model, new TurtleHeuristicsAlgorithm());
    }

    private WonVerifier(Model model, SigningAlgorithm algorithm) {
        this.model = model;
        this.algorithm = algorithm;
    }

    public WonVerifier(Model model, Selector signedSubgraphSelector) {
        //TODO
    }

    private WonVerifier(Model model, Selector signedSubgraphSelector, SigningAlgorithm algorithm) {
        //TODO
    }

    //TODO implement
    //TODO chng exceptions to won exceptions?
    public boolean verify(PublicKey publicKey) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, IOException, NoSuchProviderException {

        //TODO or get public key from eg model or some other known location?
        //TODO if many signatures first extract which parts should be verified with which signature?

        // get all parts of the model but signature related parts
        // into a without-signature model
        Model signedModel = ModelFactory.createDefaultModel();
        signedModel.add(this.model);
        Statement sigStatement = removeSignature(signedModel);
        Model unsignedModel = signedModel;

        // get signature itself
        Literal sig = sigStatement.getLiteral();
        System.out.println("SIG FOUND:\n" + sig.getString());

        return this.algorithm.verify(unsignedModel, publicKey, sig.getString());

    }

    public Statement removeSignature(Model signedModel) {

        // TODO see the same code in verifier to be improved
        Property typePredicate = signedModel.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "type");
        Resource needAsTypeObject = signedModel.createResource("http://purl.org/webofneeds/model#Need");
        Resource needUriAsTypeSubject = signedModel.listResourcesWithProperty(typePredicate, needAsTypeObject).nextResource();
        Property sigPredicate = signedModel.createProperty("http://purl.org/signature#", "signature");

        RDFNode node = null;
        StmtIterator sigStatements = signedModel.listStatements(needUriAsTypeSubject, sigPredicate, node);
        // TODO more than one signature statements will have to be removed
        Statement sigStatement = sigStatements.nextStatement();
        signedModel.remove(sigStatement);

        return sigStatement;
    }
}
