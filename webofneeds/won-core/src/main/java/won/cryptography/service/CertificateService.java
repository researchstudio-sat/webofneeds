package won.cryptography.service;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyPair;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * User: fsalcher Date: 13.06.2014
 */
public class CertificateService {

  private static final String PROVIDER_BC = org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;

  private final Logger logger = LoggerFactory.getLogger(getClass());

  public CertificateService() {
  }

  /**
   * @param serialNumber unique number within the certificate issuer
   * @param key          key pair for which the certificate is to be created
   * @param commonName   subject common name. For servers, this should be the host
   *                     name. E.g. if a server supports https at
   *                     https://www.example.com/secure, the common name should be
   *                     specified as www.example.com. For clients, it can be
   *                     anything.
   * @param webId        webID that represents the identity of the subject or null
   *                     if the subject has no webID
   * @return
   * @throws IOException
   */
  public X509Certificate createSelfSignedCertificate(BigInteger serialNumber, KeyPair key, String commonName,
      String webId) {

    Map<ASN1ObjectIdentifier, String> subjectData = new HashMap<ASN1ObjectIdentifier, String>();

    subjectData.put(BCStyle.CN, commonName);
    // ToDo: which attributes to use? make them configurable?
    // subjectData.put(BCStyle.C, "your county");
    // subjectData.put(BCStyle.O, "your organization name");
    // subjectData.put(BCStyle.OU, "your organization unit name");
    // subjectData.put(BCStyle.E, "your e-mail");
    subjectData.put(BCStyle.OU, "Web of Needs");

    return createSelfSignedCertificate(serialNumber, key, subjectData, webId);
  }

  /**
   * @param serialNumber unique number within the certificate issuer.
   * @param key          key pair for which the certificate is to be created.
   * @param subjectData  subject data to be put in distinguished name field of the
   *                     certificate. Such as CN - common name, O - organization,
   *                     OU - organization unit, L - locality, C - county, etc.
   * @param webId        webID that represents the identity of the subject or null
   *                     if the subject has no webID.
   * @return
   * @throws IOException
   */
  public X509Certificate createSelfSignedCertificate(BigInteger serialNumber, KeyPair key,
      Map<ASN1ObjectIdentifier, String> subjectData, String webId) {

    X509Certificate cert = null;
    try {
      X509v3CertificateBuilder certBuilder = createBuilderWithBasicInfo(serialNumber, key, subjectData);
      if (webId != null) {
        addToCertBuilderWebIdInfo(certBuilder, webId);
      }
      ContentSigner certSigner = createContentSigner(key);
      cert = new JcaX509CertificateConverter().setProvider(PROVIDER_BC).getCertificate(certBuilder.build(certSigner));
      cert.checkValidity(new Date());
      cert.verify(cert.getPublicKey());
    } catch (Exception e) {
      throw new IllegalArgumentException(
          "Could not create certificate for key pair with algorithm " + key.getPublic().getAlgorithm(), e);
    }
    return cert;
  }

  private void addToCertBuilderWebIdInfo(final X509v3CertificateBuilder certBuilder, final String webID)
      throws CertificateException {
    if (webID != null) {
      org.bouncycastle.asn1.x509.GeneralName[] genNames = new org.bouncycastle.asn1.x509.GeneralName[1];
      genNames[0] = new org.bouncycastle.asn1.x509.GeneralName(
          org.bouncycastle.asn1.x509.GeneralName.uniformResourceIdentifier, webID);
      try {
        certBuilder.addExtension(org.bouncycastle.asn1.x509.Extension.subjectAlternativeName, false,
            new org.bouncycastle.asn1.x509.GeneralNames(genNames));
      } catch (CertIOException e) {
        throw new CertificateException("Could not add webID to the certificate" + webID, e);
      }
    }
  }

  private ContentSigner createContentSigner(final KeyPair key) throws Exception {
    String signatureAlgorithm;
    if (key.getPublic().getAlgorithm().contains("ECDSA")) {
      signatureAlgorithm = "SHA256WithECDSA";
    } else if (key.getPublic().getAlgorithm().contains("RSA")) {
      signatureAlgorithm = "SHA256WithRSA";
    } else if (key.getPublic().getAlgorithm().contains("DSA")) {
      signatureAlgorithm = "SHA256WithDSA";
    } else {
      throw new IllegalArgumentException(key.getPublic().getAlgorithm() + " is not supported");
    }
    JcaContentSignerBuilder csBuilder = new JcaContentSignerBuilder(signatureAlgorithm);
    ContentSigner sigGen = csBuilder.setProvider(PROVIDER_BC).build(key.getPrivate());
    return sigGen;
  }

  private X509v3CertificateBuilder createBuilderWithBasicInfo(BigInteger serialNumber, KeyPair key,
      Map<ASN1ObjectIdentifier, String> subjectData) {

    DateTime today = new DateTime();
    Date notBefore = today.minusDays(1).withTimeAtStartOfDay().toDate();
    Date notAfter = today.plusYears(2).withTimeAtStartOfDay().toDate();

    X500NameBuilder nameBuilder = new X500NameBuilder(BCStyle.INSTANCE);
    for (ASN1ObjectIdentifier objectIdentifier : subjectData.keySet()) {
      nameBuilder.addRDN(objectIdentifier, subjectData.get(objectIdentifier));
    }
    X500Name subject = nameBuilder.build();

    SubjectPublicKeyInfo subjPubKeyInfo = new SubjectPublicKeyInfo(
        ASN1Sequence.getInstance(key.getPublic().getEncoded()));

    X509v3CertificateBuilder certGen = new X509v3CertificateBuilder(subject, serialNumber, notBefore, notAfter, subject,
        subjPubKeyInfo);

    return certGen;

  }

  public static List<URI> getWebIdFromSubjectAlternativeNames(final X509Certificate cert)
      throws CertificateParsingException {
    List<URI> webIDs = new ArrayList<URI>();
    Collection<List<?>> alternativeNames = cert.getSubjectAlternativeNames();
    if (alternativeNames != null) {
      for (List<?> alternativeName : alternativeNames) {
        Integer id = (Integer) alternativeName.get(0);
        // according to https://tools.ietf.org/html/rfc3280#page-33
        // index 6 is used to provide URI in subject alternative names, represented as
        // IA5String.
        // This is how subject's webID is represented in the certificate.
        if (id == 6) {
          try {
            URI webID = new URI((String) alternativeName.get(1));
            webIDs.add(webID);
          } catch (URISyntaxException e) {
            throw new CertificateParsingException("Could not retrieve webID from SAN", e);
          }
        }
      }
    }
    return webIDs;
  }

}
