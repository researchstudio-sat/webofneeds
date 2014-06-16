package won.cryptography.rdfsign;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.util.FileUtils;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.*;
import java.util.Map;

/**
 * A simple algorithms that canonicalizes RDF graph (model?) by
 * replacing prefixes with full URIs, ordering statements alphabetically
 * (ignoring blank nodes) and serializing the result into turtle
 * representation.
 *
 * This is a temporary algorithm to be used only before more
 * established algorithms are implemented/integrated into WonSigner.
 *
 * Created by ypanchenko on 12.06.2014.
 */
public class TurtleHeuristicsAlgorithm implements SigningAlgorithm {

    //TODO chng exceptions to WON exceptions
    @Override
    //public byte[] sign(Model model, PrivateKey privateKey) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
    public String sign(Model model, PrivateKey privateKey) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, NoSuchProviderException {

        // TODO eliptic curve will be used
        Signature sig = Signature.getInstance("SHA256WithECDSA", "BC");
        sig.initSign(privateKey);
        byte[] data = serializeForSigning(model);
        byte[] hashedData = hashForSigning(data);
        sig.update(hashedData);
        byte[] signatureBytes = sig.sign();
        String sigString = new BASE64Encoder().encode(signatureBytes);
        System.out.println("Singature:" + sigString);

        return sigString;
    }

    //TODO chng exceptions to WON exceptions
    @Override
    public boolean verify(Model model, PublicKey publicKey, String signature) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, IOException, NoSuchProviderException {

        // serialize and hash without-signature model
        // dataHash
        byte[] data = serializeForSigning(model);
        byte[] hashedData = hashForSigning(data);

        // decrypt signature using the public key
        // and compare dataHash to SigHash
        Signature sig = Signature.getInstance("SHA256WithECDSA", "BC");
        sig.initVerify(publicKey);
        sig.update(hashedData);

        byte[] sigBytes = new BASE64Decoder().decodeBuffer(signature);//signature.getBytes();
        return sig.verify(sigBytes);

    }

    private byte[] hashForSigning(byte[] data) throws NoSuchAlgorithmException {

        // TODO check with hash to use
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(data);
        return md.digest();

    }

    private byte[] serializeForSigning(Model model) {

        Map<String, String> prefixMap = model.getNsPrefixMap();
        for (String prefix : prefixMap.keySet()) {
            //System.out.println("KEY: " + prefix);
            //System.out.println("VALUE: " + prefixMap.get(prefix));
            model.removeNsPrefix(prefix);
        }
        // TODO add with simple canonization of the graph,
        // e.g. ordering of the nodes, etc...
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        model.write(bout, FileUtils.langTurtle);
        return bout.toByteArray();

    }

}
