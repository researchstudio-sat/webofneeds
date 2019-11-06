package won.cryptography.rdfsign;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.transaction.NotSupportedException;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;

import won.cryptography.exception.KeyNotSupportedException;
import won.cryptography.key.KeyInformationExtractor;
import won.cryptography.key.KeyInformationExtractorBouncyCastle;
import won.protocol.util.RdfUtils;
import won.protocol.vocabulary.CERT;
import won.protocol.vocabulary.SFSIG;
import won.protocol.vocabulary.WON;

/**
 * A helper class to read from/write to RDF EC public key won representation, as
 * well read as key uri referenced in the signature. User: ypanchenko Date:
 * 27.03.2015
 */
public class WonKeysReaderWriter {
    public WonKeysReaderWriter() {
    }

    public Map<String, PublicKey> readFromDataset(Dataset dataset)
                    throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        Map<String, PublicKey> keys = new HashMap<>();
        readFromModel(dataset.getDefaultModel(), keys);
        for (String name : RdfUtils.getModelNames(dataset)) {
            readFromModel(dataset.getNamedModel(name), keys);
        }
        return keys;
    }

    public Set<PublicKey> readFromDataset(Dataset dataset, String keyUri)
                    throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        PublicKey key = readFromModel(dataset.getDefaultModel(), keyUri);
        Set<PublicKey> keys = new HashSet<>();
        if (key != null) {
            keys.add(key);
        }
        for (String name : RdfUtils.getModelNames(dataset)) {
            key = readFromModel(dataset.getNamedModel(name), keyUri);
            if (key != null) {
                keys.add(key);
            }
        }
        return keys;
    }

    public Map<String, PublicKey> readFromModel(Model model)
                    throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        Map<String, PublicKey> keys = new HashMap<>();
        readFromModel(model, keys);
        return keys;
    }

    private PublicKey readFromModel(Model model, String keyUri)
                    throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        Map<String, PublicKey> keys = new HashMap<>();
        Resource keyRes = model.createResource(keyUri);
        readFromModel(model, keys, keyRes);
        return keys.get(keyUri);
    }

    private void readFromModel(final Model model, final Map<String, PublicKey> keys)
                    throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException {
        readFromModel(model, keys, null);
    }

    private void readFromModel(final Model model, final Map<String, PublicKey> keys, Resource keyAgent)
                    throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException {
        StmtIterator keyStmts = model.listStatements(keyAgent, CERT.KEY, RdfUtils.EMPTY_RDF_NODE);
        // TODO replace if with while if we allow multiple keys
        if (keyStmts.hasNext()) {
            Statement statement = keyStmts.next();
            keyAgent = statement.getSubject();
            RDFNode keyObj = statement.getObject();
            // pub key statements
            NodeIterator ni = model.listObjectsOfProperty(keyObj.asResource(), CERT.PUBLIC_KEY);
            if (ni.hasNext()) {
                RDFNode eccKeyObj = ni.next();
                // ECC pub key statements
                StmtIterator eccPubKeyStmts = model.listStatements(eccKeyObj.asResource(), RDF.type, WON.ECCPublicKey);
                if (eccPubKeyStmts.hasNext()) {
                    // extract properties of ECC public key:
                    ni = model.listObjectsOfProperty(eccKeyObj.asResource(), WON.ecc_algorithm);
                    String algName;
                    String curveId;
                    String qx;
                    String qy;
                    if (ni.hasNext()) {
                        algName = ni.next().asLiteral().toString();
                    } else {
                        return;
                    }
                    ni = model.listObjectsOfProperty(eccKeyObj.asResource(), WON.ecc_curveId);
                    if (ni.hasNext()) {
                        curveId = ni.next().asLiteral().toString();
                    } else {
                        return;
                    }
                    ni = model.listObjectsOfProperty(eccKeyObj.asResource(), WON.ecc_qx);
                    if (ni.hasNext()) {
                        qx = ni.next().asLiteral().toString();
                    } else {
                        return;
                    }
                    ni = model.listObjectsOfProperty(eccKeyObj.asResource(), WON.ecc_qy);
                    if (ni.hasNext()) {
                        qy = ni.next().asLiteral().toString();
                    } else {
                        return;
                    }
                    ECNamedCurveParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec(curveId);
                    org.bouncycastle.math.ec.ECPoint ecPoint = ecSpec.getCurve().createPoint(new BigInteger(qx, 16),
                                    new BigInteger(qy, 16));
                    ECPublicKeySpec pubKeySpec = new ECPublicKeySpec(ecPoint, ecSpec);
                    KeyFactory keyFactory = KeyFactory.getInstance(algName, "BC");
                    PublicKey key = keyFactory.generatePublic(pubKeySpec);
                    keys.put(keyAgent.getURI(), key);
                }
            }
        }
    }

    public Set<String> readKeyReferences(Dataset dataset) {
        List<String> keyRefs = RdfUtils.visitFlattenedToList(dataset, model -> {
            StmtIterator it = model.listStatements((Resource) null, SFSIG.HAS_VERIFICATION_CERT, (RDFNode) null);
            List<String> ret = new ArrayList<>();
            while (it.hasNext()) {
                ret.add(it.next().getObject().toString());
            }
            return ret;
        });
        return new HashSet<>(keyRefs);
    }

    private void writeToModel(Model model, Resource keySubject, WonEccPublicKey pubKey) {
        // EC public key triples
        Resource bn = model.createResource();
        Statement stmt = model.createStatement(bn, RDF.type, WON.ECCPublicKey);
        model.add(stmt);
        stmt = model.createStatement(bn, WON.ecc_algorithm, pubKey.getAlgorithm());
        model.add(stmt);
        stmt = model.createStatement(bn, WON.ecc_curveId, pubKey.getCurveId());
        model.add(stmt);
        stmt = model.createStatement(bn, WON.ecc_qx, model.createLiteral(pubKey.getQx()));
        model.add(stmt);
        stmt = model.createStatement(bn, WON.ecc_qy, model.createLiteral(pubKey.getQy()));
        model.add(stmt);
        // public key triple
        Resource bn2 = model.createResource();
        stmt = model.createStatement(bn2, CERT.PUBLIC_KEY, bn);
        model.add(stmt);
        // key triple
        stmt = model.createStatement(keySubject, CERT.KEY, bn2);
        model.add(stmt);
    }

    public Model writeToModel(Resource keySubject, WonEccPublicKey pubKey) {
        Objects.requireNonNull(keySubject);
        Objects.requireNonNull(pubKey);
        Model model = ModelFactory.createDefaultModel();
        writeToModel(model, keySubject, pubKey);
        return model;
    }

    public void writeToModel(Model model, Resource keySubject, PublicKey publicKey)
                    throws NotSupportedException, KeyNotSupportedException {
        Objects.requireNonNull(keySubject);
        Objects.requireNonNull(publicKey);
        Objects.requireNonNull(model);
        KeyInformationExtractor info = new KeyInformationExtractorBouncyCastle();
        writeToModel(model, keySubject, new WonEccPublicKey(info.getCurveID(publicKey),
                        info.getAlgorithm(publicKey), info.getQX(publicKey), info.getQY(publicKey)));
    }
}
