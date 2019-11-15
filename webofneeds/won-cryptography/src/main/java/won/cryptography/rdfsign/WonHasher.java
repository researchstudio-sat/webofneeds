package won.cryptography.rdfsign;

import java.lang.invoke.MethodHandles;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Arrays;

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
    private SignatureAlgorithmInterface algorithm;

    public WonHasher() {
        // default algorithm: Fisteus2010
        this.algorithm = new SignatureAlgorithmFisteus2010();
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
        this.algorithm.canonicalize(inputWithOneNamedGraph);
        this.algorithm.postCanonicalize(inputWithOneNamedGraph);
        this.algorithm.hash(inputWithOneNamedGraph, ENV_HASH_ALGORITHM);
        this.algorithm.postHash(inputWithOneNamedGraph);
        return inputWithOneNamedGraph.getSignature();
    }

    public String calculateHashIdForDataset(Dataset dataset) throws Exception {
        GraphCollection graphCollection = ModelConverter.fromDataset(dataset);
        this.algorithm.canonicalize(graphCollection);
        this.algorithm.postCanonicalize(graphCollection);
        this.algorithm.hash(graphCollection, ENV_HASH_ALGORITHM);
        this.algorithm.postHash(graphCollection);
        return hashToString(graphCollection.getSignature().getHash());
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
}
