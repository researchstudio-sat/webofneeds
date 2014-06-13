package won.cryptography.service;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * User: fsalcher
 * Date: 13.06.2014
 */
public class CertificateService {

    private static final String PROVIDER_BC = org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public X509Certificate createSelfSignedCertificate(KeyPair key) {

        DateTime today = new DateTime();

        Date notBefore = today.minusDays(1).withTimeAtStartOfDay().toDate();
        Date notAfter = today.plusYears(2).withTimeAtStartOfDay().toDate();

        BigInteger serialNumber = BigInteger.valueOf(1);

        X500NameBuilder nameBuilder = new X500NameBuilder(BCStyle.INSTANCE);

        // ToDo: which attributes to use? make them configurable?
        nameBuilder.addRDN(BCStyle.C, "Austria");
        nameBuilder.addRDN(BCStyle.O, "RSA");
        nameBuilder.addRDN(BCStyle.E, "office.sat@researchstudio.at");
        nameBuilder.addRDN(BCStyle.CN, "SelfSignedStuff");
        nameBuilder.addRDN(BCStyle.SN, "SN1234");
        X500Name subject = nameBuilder.build();

        X509v3CertificateBuilder certGen = new JcaX509v3CertificateBuilder(
                subject,
                serialNumber,
                notBefore,
                notAfter,
                subject, key.getPublic());


//            AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find("SHA1WithRSA");

        JcaContentSignerBuilder csBuilder = new JcaContentSignerBuilder("SHA256WithECDSA");

        X509Certificate cert = null;

        try {

            ContentSigner sigGen = csBuilder
                    .setProvider(PROVIDER_BC).build(key.getPrivate());

            cert = new JcaX509CertificateConverter().setProvider(PROVIDER_BC)
                    .getCertificate(certGen.build(sigGen));
            cert.checkValidity(new Date());
            cert.verify(cert.getPublicKey());


        } catch (Exception e) {
            // ToDo: proper error handling
            e.printStackTrace();
        }

        return cert;

    }

}
