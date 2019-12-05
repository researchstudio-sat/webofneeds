package won.cryptography.rdfsign;

import java.lang.invoke.MethodHandles;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Arrays;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.jena.query.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_koblenz.aggrimm.icp.crypto.sign.algorithm.SignatureAlgorithmInterface;
import de.uni_koblenz.aggrimm.icp.crypto.sign.algorithm.algorithm.SignatureAlgorithmFisteus2010;
import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.GraphCollection;
import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.SignatureData;
import io.ipfs.multihash.Multihash;
import io.ipfs.multihash.Multihash.Type;
import won.cryptography.rdfsign.exception.WonMessageHashingException;

/**
 * Created by ypanchenko on 12.06.2014.
 */
public class WonHasher {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    public static final String ENV_HASH_ALGORITHM = "sha-256";
    public static final Type MULTIHASH_TYPE = Type.sha3_256;
    private static GenericObjectPool<SignatureAlgorithmInterface> signatureAlgorithmPool = new GenericObjectPool<SignatureAlgorithmInterface>(
                    new SignatureAlgorithmFactory());
    static {
        signatureAlgorithmPool.setMaxTotal(20);
    }

    public WonHasher() {
    }

    /**
     * Calculates the hash of the one named graph contained in the specified
     * GraphCollection
     * 
     * @param inputWithOneNamedGraph
     * @return the SignatureData object that holds the hash
     * @throws Exception
     */
    public SignatureData hashNamedGraphForSigning(
                    final GraphCollection inputWithOneNamedGraph)
                    throws Exception {
        SignatureAlgorithmInterface algorithm = new SignatureAlgorithmFisteus2010();
        // signatureAlgorithmPool.borrowObject();
        try {
            algorithm.canonicalize(inputWithOneNamedGraph);
            algorithm.postCanonicalize(inputWithOneNamedGraph);
            algorithm.hash(inputWithOneNamedGraph, ENV_HASH_ALGORITHM);
            algorithm.postHash(inputWithOneNamedGraph);
            inputWithOneNamedGraph.getSignature().getDigestGen().reset();
            return inputWithOneNamedGraph.getSignature();
        } finally {
            if (algorithm != null) {
                // signatureAlgorithmPool.returnObject(algorithm);
            }
        }
    }

    public String calculateHashIdForDataset(Dataset dataset) throws Exception {
        SignatureAlgorithmInterface algorithm = new SignatureAlgorithmFisteus2010();
        // signatureAlgorithmPool.borrowObject();
        try {
            GraphCollection graphCollection = ModelConverter.fromDataset(dataset);
            algorithm.canonicalize(graphCollection);
            algorithm.postCanonicalize(graphCollection);
            algorithm.hash(graphCollection, ENV_HASH_ALGORITHM);
            algorithm.postHash(graphCollection);
            graphCollection.getSignature().getDigestGen().reset();
            return hashToString(graphCollection.getSignature().getHash());
        } finally {
            if (algorithm != null) {
                // signatureAlgorithmPool.returnObject(algorithm);
            }
        }
    }

    /**
     * Convert a BigInteger sha2_256 hash value to a Base58 string.
     * 
     * @param hash
     * @return
     * @throws Exception
     */
    static String hashToString(BigInteger hash) {
        // Prepare Digest
        return hashToString(hash.toByteArray());
    }

    static String hashToString(byte[] data) {
        // Prepare Digest
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(ENV_HASH_ALGORITHM, WonSigner.SIGNING_ALGORITHM_PROVIDER);
        } catch (Exception e) {
            throw new WonMessageHashingException("Error computing hash", e);
        }
        byte[] hashed = md.digest(data);
        Multihash multiHash = new Multihash(MULTIHASH_TYPE, hashed);
        return multiHash.toBase58();
    }

    /**
     * Check if the specified string hash is the value you get when you hash the
     * specified biginteger.
     * 
     * @param expected
     * @return
     * @throws Exception
     */
    static boolean verify(String expected, BigInteger valueToHash) {
        return verify(expected, valueToHash.toByteArray());
    }

    static boolean verify(String expected, byte[] valueToHash) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(ENV_HASH_ALGORITHM, WonSigner.SIGNING_ALGORITHM_PROVIDER);
        } catch (Exception e) {
            throw new WonMessageHashingException("Error verifying hash", e);
        }
        byte[] hashed = md.digest(valueToHash);
        Multihash multihash = Multihash.fromBase58(expected);
        return Arrays.equals(hashed, multihash.getHash());
    }

    public static class SignatureAlgorithmFactory extends BasePooledObjectFactory<SignatureAlgorithmInterface> {
        public SignatureAlgorithmFactory() {
        }

        @Override
        public SignatureAlgorithmInterface create() throws Exception {
            return new SignatureAlgorithmFisteus2010();
        }

        @Override
        public PooledObject<SignatureAlgorithmInterface> wrap(SignatureAlgorithmInterface obj) {
            return new DefaultPooledObject<SignatureAlgorithmInterface>(obj);
        }
    }
}
