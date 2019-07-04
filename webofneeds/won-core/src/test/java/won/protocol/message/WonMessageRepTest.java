package won.protocol.message;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import won.protocol.util.WonRepRdfUtils;
import won.protocol.vocabulary.CERT;
import won.protocol.vocabulary.REP;

import java.net.URI;

/**
 * Based on PrivateRide we define Bob as the passenger and Alice as the driver.
 * The service provider remains the central point which releases blind
 * signatures and manages the ratings. After the transaction of a payment is
 * successful, a rating-process is initiated. Bob and Alice generate a random
 * hash via SHA512 and exchange it - test_REP_rdf_message_exchangeRandomHash()
 * Bob receives the hash from Alice and signs it with his private key. Bob forms
 * a reputation token, which contains his certificate and the signed hash. The
 * certificate contains the public key which matches the private key with which
 * the hash was signed. Both Bob and Alice perform the following procedure: form
 * a reputation token, which contains its certificate and the signed hash. The
 * certificate contains the public key that matches the private key with which
 * the hash was signed. Send Reputation Token to Service Provider,
 * test_REP_rdf_message_blindSign() Service Provider creates a blind signature
 * of the token and sends it back, test_REP_rdf_message_blindSignFinish()
 * Replacing the Reputation Token with the blindly signed token,
 * test_REP_rdf_message_exchangeReputationToken() As soon as Bob receives the
 * reputation token together with the blindly signed reputation token, the
 * signature is verified using the public key. Bob then sends the reputation
 * token, blindly signed token and its original hash to the service provider -
 * test_REP_rdf_message_verifyReputationToken() - which checks whether the blind
 * signature is correct and whether the random hash can be verified with the
 * signed hash. The status is communicated with
 * test_REP_rdf_message_verificationState(). If the response from the service
 * provider contains 'true', Bob sends his rating to the service provider -
 * test_REP_rdf_message_ratePerson.()
 */
public class WonMessageRepTest {
    @BeforeClass
    public static void setLogLevel() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
    }

    @Test
    public void test_REP_rdf_message_exchangeRandomHash() {
        Model model = WonRepRdfUtils.createBaseModel();
        Resource baseRes = model.createResource();
        baseRes.addProperty(REP.RANDOM_HASH, "7a84d381e462d83ac918c90eb045c6077d764b2ca03e30906fc4c30ef43ad57d");
        final WonMessage msg = WonMessageBuilder.setMessagePropertiesForConnectionMessage(
                        URI.create("messageUri"),
                        URI.create("localConnection"),
                        URI.create("localAtom"),
                        URI.create("localWonNode"),
                        URI.create("targetConnection"),
                        URI.create("TargetAtom"),
                        URI.create("remoteWonNode"),
                        model).build();
        RDFDataMgr.write(System.out, msg.getCompleteDataset(), Lang.TRIG);
    }

    @Test
    public void test_REP_rdf_message_blindSign() {
        Model model = WonRepRdfUtils.createBaseModel();
        Resource baseRes = model.createResource();
        Resource certificate = model.createResource();
        certificate.addProperty(REP.USER_ID, "1");
        certificate.addProperty(REP.PUBLIC_KEY, "6lLkr3HbnfuOhGKzEuydFgWZIiWTtXdLKKsXIftYg7E=");
        Resource reputationToken = model.createResource();
        reputationToken.addProperty(REP.CERTIFICATE, certificate);
        // TODO built in property exists?
        reputationToken.addProperty(REP.SIGNED_RANDOM_HASH,
                        "eA0Aum8jgAkHoECTgn6T1ZqjOoE9rbxG9vJDzhnt9dIfp7W7rNBdWbQg/JWXjbGVUmXZTUHm9BhqmVMstma+iSUDsOkdKt+cnYQ8ctt7jcEAhENxJgsL1GmTA07hSunHpD+yTuPVNZyTuKHe47q0hJOvFiKcYN2boEA3iU3uwJA=");
        baseRes.addProperty(REP.REPUTATIONTOKEN, reputationToken);
        final WonMessage msg = WonMessageBuilder.setMessagePropertiesForConnectionMessage(
                        URI.create("messageUri"),
                        URI.create("localConnection"),
                        URI.create("localAtom"),
                        URI.create("localWonNode"),
                        URI.create("targetConnection"),
                        URI.create("TargetAtom"),
                        URI.create("remoteWonNode"),
                        model).build();
        RDFDataMgr.write(System.out, msg.getCompleteDataset(), Lang.TRIG);
    }

    @Test
    public void test_REP_rdf_message_blindSignFinish() {
        Model model = WonRepRdfUtils.createBaseModel();
        Resource baseRes = model.createResource();
        Resource certificate = model.createResource();
        certificate.addProperty(REP.USER_ID, "1");
        certificate.addProperty(REP.PUBLIC_KEY, "6lLkr3HbnfuOhGKzEuydFgWZIiWTtXdLKKsXIftYg7E=");
        Resource reputationToken = model.createResource();
        reputationToken.addProperty(REP.CERTIFICATE, certificate);
        // TODO built in property exists?
        reputationToken.addProperty(REP.SIGNED_RANDOM_HASH,
                        "eA0Aum8jgAkHoECTgn6T1ZqjOoE9rbxG9vJDzhnt9dIfp7W7rNBdWbQg/JWXjbGVUmXZTUHm9BhqmVMstma+iSUDsOkdKt+cnYQ8ctt7jcEAhENxJgsL1GmTA07hSunHpD+yTuPVNZyTuKHe47q0hJOvFiKcYN2boEA3iU3uwJA=");
        baseRes.addProperty(REP.REPUTATIONTOKEN, reputationToken);
        baseRes.addProperty(REP.BLIND_SIGNED_REPUTATIONTOKEN,
                        "jKrRkeUktfOYVxj1UWIKBDN+5aICIBNAkNBtG3TelP+LOK9sMHNy2YApjtv/nw4GwEq6tTsYZMMmptah+8nc9m3siM6HuxspbI/gT6ZcveUQSKvBbDQk00GVrCgpyA8rYmE4QFPEjLYGnXF6/QpUP9nt/dR1kX54YBWBTTgYDBU=");
        final WonMessage msg = WonMessageBuilder.setMessagePropertiesForConnectionMessage(
                        URI.create("messageUri"),
                        URI.create("localConnection"),
                        URI.create("localAtom"),
                        URI.create("localWonNode"),
                        URI.create("targetConnection"),
                        URI.create("TargetAtom"),
                        URI.create("remoteWonNode"),
                        model).build();
        RDFDataMgr.write(System.out, msg.getCompleteDataset(), Lang.TRIG);
    }

    @Test
    public void test_REP_rdf_message_exchangeReputationToken() {
        Model model = WonRepRdfUtils.createBaseModel();
        Resource baseRes = model.createResource();
        Resource certificate = model.createResource();
        certificate.addProperty(REP.USER_ID, "1");
        certificate.addProperty(REP.PUBLIC_KEY, "6lLkr3HbnfuOhGKzEuydFgWZIiWTtXdLKKsXIftYg7E=");
        Resource reputationToken = model.createResource();
        reputationToken.addProperty(REP.CERTIFICATE, certificate);
        // TODO built in property exists?
        reputationToken.addProperty(REP.SIGNED_RANDOM_HASH,
                        "eA0Aum8jgAkHoECTgn6T1ZqjOoE9rbxG9vJDzhnt9dIfp7W7rNBdWbQg/JWXjbGVUmXZTUHm9BhqmVMstma+iSUDsOkdKt+cnYQ8ctt7jcEAhENxJgsL1GmTA07hSunHpD+yTuPVNZyTuKHe47q0hJOvFiKcYN2boEA3iU3uwJA=");
        baseRes.addProperty(REP.REPUTATIONTOKEN, reputationToken);
        baseRes.addProperty(REP.BLIND_SIGNED_REPUTATIONTOKEN,
                        "jKrRkeUktfOYVxj1UWIKBDN+5aICIBNAkNBtG3TelP+LOK9sMHNy2YApjtv/nw4GwEq6tTsYZMMmptah+8nc9m3siM6HuxspbI/gT6ZcveUQSKvBbDQk00GVrCgpyA8rYmE4QFPEjLYGnXF6/QpUP9nt/dR1kX54YBWBTTgYDBU=");
        final WonMessage msg = WonMessageBuilder.setMessagePropertiesForConnectionMessage(
                        URI.create("messageUri"),
                        URI.create("localConnection"),
                        URI.create("localAtom"),
                        URI.create("localWonNode"),
                        URI.create("targetConnection"),
                        URI.create("TargetAtom"),
                        URI.create("remoteWonNode"),
                        model).build();
        RDFDataMgr.write(System.out, msg.getCompleteDataset(), Lang.TRIG);
    }

    @Test
    public void test_REP_rdf_message_verifyReputationToken() {
        Model model = WonRepRdfUtils.createBaseModel();
        Resource baseRes = model.createResource();
        Resource certificate = model.createResource();
        certificate.addProperty(REP.USER_ID, "1");
        certificate.addProperty(REP.PUBLIC_KEY, "6lLkr3HbnfuOhGKzEuydFgWZIiWTtXdLKKsXIftYg7E=");
        Resource reputationToken = model.createResource();
        reputationToken.addProperty(REP.CERTIFICATE, certificate);
        // TODO built in property exists?
        reputationToken.addProperty(REP.SIGNED_RANDOM_HASH,
                        "eA0Aum8jgAkHoECTgn6T1ZqjOoE9rbxG9vJDzhnt9dIfp7W7rNBdWbQg/JWXjbGVUmXZTUHm9BhqmVMstma+iSUDsOkdKt+cnYQ8ctt7jcEAhENxJgsL1GmTA07hSunHpD+yTuPVNZyTuKHe47q0hJOvFiKcYN2boEA3iU3uwJA=");
        baseRes.addProperty(REP.REPUTATIONTOKEN, reputationToken);
        baseRes.addProperty(REP.BLIND_SIGNED_REPUTATIONTOKEN,
                        "jKrRkeUktfOYVxj1UWIKBDN+5aICIBNAkNBtG3TelP+LOK9sMHNy2YApjtv/nw4GwEq6tTsYZMMmptah+8nc9m3siM6HuxspbI/gT6ZcveUQSKvBbDQk00GVrCgpyA8rYmE4QFPEjLYGnXF6/QpUP9nt/dR1kX54YBWBTTgYDBU=");
        baseRes.addProperty(REP.RANDOM_HASH, "7a84d381e462d83ac918c90eb045c6077d764b2ca03e30906fc4c30ef43ad57d");
        final WonMessage msg = WonMessageBuilder.setMessagePropertiesForConnectionMessage(
                        URI.create("messageUri"),
                        URI.create("localConnection"),
                        URI.create("localAtom"),
                        URI.create("localWonNode"),
                        URI.create("targetConnection"),
                        URI.create("TargetAtom"),
                        URI.create("remoteWonNode"),
                        model).build();
        RDFDataMgr.write(System.out, msg.getCompleteDataset(), Lang.TRIG);
    }

    @Test
    public void test_REP_rdf_message_verificationState() {
        Model model = WonRepRdfUtils.createBaseModel();
        Resource baseRes = model.createResource();
        Resource certificate = model.createResource();
        certificate.addProperty(REP.USER_ID, "1");
        certificate.addProperty(REP.PUBLIC_KEY, "6lLkr3HbnfuOhGKzEuydFgWZIiWTtXdLKKsXIftYg7E=");
        Resource reputationToken = model.createResource();
        reputationToken.addProperty(REP.CERTIFICATE, certificate);
        // TODO built in property exists?
        reputationToken.addProperty(REP.SIGNED_RANDOM_HASH,
                        "eA0Aum8jgAkHoECTgn6T1ZqjOoE9rbxG9vJDzhnt9dIfp7W7rNBdWbQg/JWXjbGVUmXZTUHm9BhqmVMstma+iSUDsOkdKt+cnYQ8ctt7jcEAhENxJgsL1GmTA07hSunHpD+yTuPVNZyTuKHe47q0hJOvFiKcYN2boEA3iU3uwJA=");
        baseRes.addProperty(REP.REPUTATIONTOKEN, reputationToken);
        baseRes.addProperty(REP.BLIND_SIGNED_REPUTATIONTOKEN,
                        "jKrRkeUktfOYVxj1UWIKBDN+5aICIBNAkNBtG3TelP+LOK9sMHNy2YApjtv/nw4GwEq6tTsYZMMmptah+8nc9m3siM6HuxspbI/gT6ZcveUQSKvBbDQk00GVrCgpyA8rYmE4QFPEjLYGnXF6/QpUP9nt/dR1kX54YBWBTTgYDBU=");
        baseRes.addProperty(REP.VERIFICATION_STATE, "true");
        final WonMessage msg = WonMessageBuilder.setMessagePropertiesForConnectionMessage(
                        URI.create("messageUri"),
                        URI.create("localConnection"),
                        URI.create("localAtom"),
                        URI.create("localWonNode"),
                        URI.create("targetConnection"),
                        URI.create("TargetAtom"),
                        URI.create("remoteWonNode"),
                        model).build();
        RDFDataMgr.write(System.out, msg.getCompleteDataset(), Lang.TRIG);
    }

    @Test
    public void test_REP_rdf_message_ratePerson() {
        Model model = WonRepRdfUtils.createBaseModel();
        Resource baseRes = model.createResource();
        Resource certificate = model.createResource();
        certificate.addProperty(REP.USER_ID, "1");
        certificate.addProperty(REP.PUBLIC_KEY, "6lLkr3HbnfuOhGKzEuydFgWZIiWTtXdLKKsXIftYg7E=");
        Resource reputationToken = model.createResource();
        reputationToken.addProperty(REP.CERTIFICATE, certificate);
        // TODO built in property exists?
        reputationToken.addProperty(REP.SIGNED_RANDOM_HASH,
                        "eA0Aum8jgAkHoECTgn6T1ZqjOoE9rbxG9vJDzhnt9dIfp7W7rNBdWbQg/JWXjbGVUmXZTUHm9BhqmVMstma+iSUDsOkdKt+cnYQ8ctt7jcEAhENxJgsL1GmTA07hSunHpD+yTuPVNZyTuKHe47q0hJOvFiKcYN2boEA3iU3uwJA=");
        baseRes.addProperty(REP.REPUTATIONTOKEN, reputationToken);
        baseRes.addProperty(REP.BLIND_SIGNED_REPUTATIONTOKEN,
                        "jKrRkeUktfOYVxj1UWIKBDN+5aICIBNAkNBtG3TelP+LOK9sMHNy2YApjtv/nw4GwEq6tTsYZMMmptah+8nc9m3siM6HuxspbI/gT6ZcveUQSKvBbDQk00GVrCgpyA8rYmE4QFPEjLYGnXF6/QpUP9nt/dR1kX54YBWBTTgYDBU=");
        baseRes.addProperty(REP.RATING, "4.5");
        baseRes.addProperty(REP.RATING_COMMENT, "Smooth and fast Transaction");
        final WonMessage msg = WonMessageBuilder.setMessagePropertiesForConnectionMessage(
                        URI.create("messageUri"),
                        URI.create("localConnection"),
                        URI.create("localAtom"),
                        URI.create("localWonNode"),
                        URI.create("targetConnection"),
                        URI.create("TargetAtom"),
                        URI.create("remoteWonNode"),
                        model).build();
        RDFDataMgr.write(System.out, msg.getCompleteDataset(), Lang.TRIG);
    }
}
