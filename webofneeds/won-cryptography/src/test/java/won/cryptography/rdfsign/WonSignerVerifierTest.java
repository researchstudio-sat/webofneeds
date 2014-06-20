package won.cryptography.rdfsign;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import won.cryptography.service.CryptographyService;

import java.io.*;
import java.net.URI;
import java.security.*;

/**
 * Created by ypanchenko on 16.06.2014.
 */
public class WonSignerVerifierTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    private static final String RESOURCE_FILE = "/test_12_content_cupboard_45_45_15.ttl";
    private static final String RESOURCE_URI = "http://www.example.com/resource/need/12";


    @Test
    public void modelAddRemoveSignatureTest() throws Exception {

        Model model1 = ModelFactory.createDefaultModel();
        InputStream is1 = WonSignerVerifierTest.class.getResourceAsStream(RESOURCE_FILE);
        model1.read(new InputStreamReader(is1), RESOURCE_URI, FileUtils.langTurtle);

//        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
//        kpg.initialize(1024);
//        KeyPair keyPair = kpg.genKeyPair();

        CryptographyService crypService = new CryptographyService();
        KeyPair keyPair = crypService.createNewNeedKeyPair(URI.create(RESOURCE_URI));

        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        //File outFile1 = testFolder.newFile();
        //System.out.println(outFile1);
        //model1.write(new FileWriter(outFile1), FileUtils.langTurtle);

        WonSigner signer = new WonSigner(model1);
        Model signedModel = signer.addSignature(privateKey, publicKey);
        //File outFile = testFolder.newFile();
        //System.out.println(outFile);
        //signedModel.write(new FileWriter(outFile), FileUtils.langTurtle);

        WonVerifier verifier = new WonVerifier(signedModel);
        verifier.removeSignature(signedModel);
        Model model2 = signedModel;

        //File outFile2 = testFolder.newFile();
        //System.out.println(outFile2);
        //model2.write(new FileWriter(outFile2), FileUtils.langTurtle);

        Assert.assertTrue(model1.isIsomorphicWith(model2));

    }

    //TODO ask Florian about the simple use-case test/tests in webofneeds project
    //that I can run and use to build upon when adding signing functionality
    @Test
    public void signVerifyTest() throws Exception {

        Model model1 = ModelFactory.createDefaultModel();
        InputStream is1 = WonSignerVerifierTest.class.getResourceAsStream(RESOURCE_FILE);
        model1.read(new InputStreamReader(is1), RESOURCE_URI, FileUtils.langTurtle);

        //KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        //kpg.initialize(1024);
        //KeyPair keyPair = kpg.genKeyPair();

        // TODO ask Fabian about exception
        CryptographyService crypService = new CryptographyService();
        KeyPair keyPair = crypService.createNewNeedKeyPair(URI.create(RESOURCE_URI));

        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        WonSigner signer = new WonSigner(model1);
        Model signedModel = signer.addSignature(privateKey, publicKey);

        File outFile = testFolder.newFile();
        System.out.println(outFile);
        signedModel.write(new FileWriter(outFile), FileUtils.langTurtle);

        WonVerifier verifier = new WonVerifier(signedModel);

        Assert.assertTrue(verifier.verify(publicKey));

    }
}
