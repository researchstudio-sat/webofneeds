package won.cryptography.rdfsign;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import won.cryptography.exception.KeyNotSupportedException;
import won.cryptography.key.KeyInformationExtractor;
import won.cryptography.key.KeyInformationExtractorBouncyCastle;
import won.protocol.message.WonMessage;
import won.protocol.util.RdfUtils;
import won.protocol.util.linkeddata.uriresolver.WonRelativeUriHelper;
import won.protocol.vocabulary.CERT;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONMSG;

import javax.transaction.NotSupportedException;
import java.math.BigInteger;
import java.net.URI;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.*;

/**
 * A helper class to read from/write to RDF EC public key won representation, as
 * well read as key uri referenced in the signature. User: ypanchenko Date:
 * 27.03.2015
 */
public class WonKeysReaderWriter {
    private static final Provider securityProvider = new BouncyCastleProvider();

    public static Map<String, PublicKey> readKeyFromMessage(WonMessage wonMessage)
                    throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        Map<String, PublicKey> keys = new HashMap<>();
        Optional<Model> keyGraph = wonMessage.getKeyGraph();
        if (keyGraph.isEmpty()) {
            return keys;
        }
        readFromModel(keyGraph.get(), keys);
        return keys;
    }

    public static Set<PublicKey> readKeyFromAtom(URI atomUri, Dataset dataset, String keyUri)
                    throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        URI keyGraphUri = WonRelativeUriHelper.createKeyGraphURIForAtomURI(atomUri);
        PublicKey key = readFromModel(dataset.getNamedModel(keyGraphUri.toString()), keyUri);
        Set<PublicKey> keys = new HashSet<>();
        if (key != null) {
            keys.add(key);
        } else {
            String aclGraphUri = WonRelativeUriHelper.createAclGraphURIForAtomURI(atomUri).toString();
            String sysinfoGraphUri = WonRelativeUriHelper.createSysInfoGraphURIForAtomURI(atomUri).toString();
            // allow legacy atoms (keys in content graph)
            Iterator<String> names = dataset.listNames();
            while (names.hasNext()) {
                String graphUri = names.next();
                if (graphUri.endsWith(WonMessage.SIGNATURE_URI_GRAPHURI_SUFFIX)
                                || graphUri.equals(aclGraphUri)
                                || graphUri.equals(sysinfoGraphUri)) {
                    continue;
                }
                key = readFromModel(dataset.getNamedModel(graphUri), keyUri);
                if (key != null) {
                    keys.add(key);
                    return keys;
                }
            }
        }
        return keys;
    }

    public static Map<String, PublicKey> readFromModel(Model model)
                    throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        Map<String, PublicKey> keys = new HashMap<>();
        readFromModel(model, keys);
        return keys;
    }

    public static PublicKey readFromModel(Model model, String keyUri)
                    throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        Map<String, PublicKey> keys = new HashMap<>();
        Resource keyRes = model.createResource(keyUri);
        readFromModel(model, keys, keyRes);
        return keys.get(keyUri);
    }

    private static void readFromModel(final Model model, final Map<String, PublicKey> keys)
                    throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException {
        readFromModel(model, keys, null);
    }

    private static void readFromModel(final Model model, final Map<String, PublicKey> keys, Resource keyAgent)
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
                    KeyFactory keyFactory = KeyFactory.getInstance(algName, securityProvider);
                    PublicKey key = keyFactory.generatePublic(pubKeySpec);
                    keys.put(keyAgent.getURI(), key);
                }
            }
        }
    }

    public static void deleteFromModel(final Model model, URI key) {
        List<Resource> toRemove = new ArrayList<>();
        StmtIterator keyStmts = model.listStatements(model.getResource(key.toString()),
                        CERT.KEY,
                        RdfUtils.EMPTY_RDF_NODE);
        if (keyStmts.hasNext()) {
            Statement statement = keyStmts.next();
            toRemove.add(statement.getObject().asResource());
        }
        toRemove.forEach(r -> RdfUtils.removeResource(model, r));
    }

    public static Set<String> readKeyReferences(Dataset dataset) {
        List<String> keyRefs = RdfUtils.visitFlattenedToList(dataset, model -> {
            StmtIterator it = model.listStatements((Resource) null, WONMSG.signer, (RDFNode) null);
            List<String> ret = new ArrayList<>();
            while (it.hasNext()) {
                ret.add(it.next().getObject().toString());
            }
            return ret;
        });
        return new HashSet<>(keyRefs);
    }

    private static void writeToModel(Model model, Resource keySubject, WonEccPublicKey pubKey) {
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

    public static Model writeToModel(Resource keySubject, WonEccPublicKey pubKey) {
        Objects.requireNonNull(keySubject);
        Objects.requireNonNull(pubKey);
        Model model = ModelFactory.createDefaultModel();
        writeToModel(model, keySubject, pubKey);
        return model;
    }

    public static void writeToModel(Model model, Resource keySubject, PublicKey publicKey)
                    throws NotSupportedException, KeyNotSupportedException {
        Objects.requireNonNull(keySubject);
        Objects.requireNonNull(publicKey);
        Objects.requireNonNull(model);
        KeyInformationExtractor info = new KeyInformationExtractorBouncyCastle();
        writeToModel(model, keySubject, new WonEccPublicKey(info.getCurveID(publicKey),
                        info.getAlgorithm(publicKey), info.getQX(publicKey), info.getQY(publicKey)));
    }
}
