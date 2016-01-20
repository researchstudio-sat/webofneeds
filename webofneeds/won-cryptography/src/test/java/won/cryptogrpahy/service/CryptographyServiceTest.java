package won.cryptogrpahy.service;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import won.cryptography.exception.KeyNotSupportedException;
import won.cryptography.key.KeyInformationExtractorBouncyCastle;
import won.cryptography.service.KeyPairService;
import won.protocol.vocabulary.WON;

import java.io.ByteArrayOutputStream;
import java.security.KeyPair;

/**
 * User: fsalcher
 * Date: 17.07.2014
 */
public class CryptographyServiceTest {


    private ApplicationContext context;

    @Before
    public void init() {
        context = new ClassPathXmlApplicationContext(
                new String[]{"spring/component/cryptographyServices.xml"});
    }

    // ToDo: no tests yet, just used for debugging (FS)

    @Test
    public void generateKeyPair() {

        KeyPairService keyPairService = context.getBean("keyPairService", KeyPairService.class);
        KeyPair keypair = keyPairService.generateNewKeyPairInBrainpoolp384r1();
        System.out.println(keypair);
    }

    @Test
    public void getParametersOfKeyPair() {

        KeyPairService keyPairService = context.getBean("keyPairService", KeyPairService.class);
        KeyPair keypair = keyPairService.generateNewKeyPairInBrainpoolp384r1();

        KeyInformationExtractorBouncyCastle extractor = new KeyInformationExtractorBouncyCastle();

        try {
            System.out.println("algorithm: " + extractor.getAlgorithm(keypair.getPublic()));
            System.out.println("curveID: " + extractor.getCurveID(keypair.getPublic()));
            System.out.println("qx: " + extractor.getQX(keypair.getPublic()));
            System.out.println("qy: " + extractor.getQY(keypair.getPublic()));
        } catch (KeyNotSupportedException ex) {
            ex.printStackTrace();
        }

    }

    @Test
    public void getRDFFromKeyParameters() {

        KeyPairService keyPairService = context.getBean("keyPairService", KeyPairService.class);
        KeyPair keypair = keyPairService.generateNewKeyPairInBrainpoolp384r1();


        try {

            Model needModel = ModelFactory.createDefaultModel();
            Resource needResource = needModel.createResource("no:uri", WON.NEED);

            keyPairService.appendPublicKeyRDF(needResource, keypair);

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            needModel.write(os, "Turtle");
            System.out.println(new String(os.toByteArray()));

        } catch (KeyNotSupportedException ex) {
            ex.printStackTrace();
        }

    }
}
