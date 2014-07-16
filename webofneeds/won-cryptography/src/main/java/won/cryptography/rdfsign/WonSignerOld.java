package won.cryptography.rdfsign;

import com.hp.hpl.jena.rdf.model.*;

import java.security.*;

/**
 * Created by ypanchenko on 12.06.2014.
 */
public class WonSignerOld
{

    private Model model;
    private SigningAlgorithm algorithm;
    //private  byte[] signature;

    public WonSignerOld(Model model) {
        // default TurtleHeuristicsAlgorithm algorithm should be
        // replaced by a better algorithm in future
        this(model,new TurtleHeuristicsAlgorithm());
    }

    public WonSignerOld(Model model, Selector signedSubgraphSelector) {
        //TODO
    }

    public WonSignerOld(Model model, SigningAlgorithm algorithm) {
        this.model = model;
        this.algorithm = algorithm;
    }

    public WonSignerOld(Model model, Selector signedSubgraphSelector, SigningAlgorithm algorithm) {
    //TODO
    }

    //TODO implement
    //TODO chng exceptions to won exceptions?
    //TODO where to put public key
    public Model addSignature(PrivateKey privateKey, PublicKey publicKey) throws Exception {

        String sigString = this.algorithm.sign(model, privateKey);

        Model signedModel = ModelFactory.createDefaultModel();
        signedModel.add(model);

        // TODO: is there better way to retrieve the URI of the need under consideration?
        Property typePredicate = model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "type");
        Resource needAsTypeObject = model.createResource("http://purl.org/webofneeds/model#Need");
        Resource needUriAsTypeSubject = model.listResourcesWithProperty(typePredicate, needAsTypeObject).nextResource();
        Property sigPredicate = model.createProperty("http://purl.org/signature#", "signature");
        // TODO more than one signature  statement will have to be added
        Statement sigStatement = model.createStatement(needUriAsTypeSubject, sigPredicate, sigString);
        signedModel.add(sigStatement);

        return signedModel;
    }

}
