package won.cryptography.webid;

import com.hp.hpl.jena.query.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.cryptography.rdfsign.WonKeysReaderWriter;
import won.protocol.util.linkeddata.LinkedDataSource;

import java.io.IOException;
import java.net.URI;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * User: ypanchenko
 * Date: 28.07.2015
 */
public class WebIDVerificationAgent
{
  final Logger logger = LoggerFactory.getLogger(getClass());

  private LinkedDataSource linkedDataSource;

  /**
   *
   * @return list of those webIDs that were successfully verified by fetching the webID's url
   * and comparing public key data found there with the provided in constructor public key data
   */
  public List<String> verify(PublicKey publicKey, List<URI> webIDs) throws Exception {
    List<String> verified = new ArrayList<String>();

    for (URI webID : webIDs) {

      Dataset dataset = linkedDataSource.getDataForResource(webID);

      //TODO for RSA key
      //      if (publicKey instanceof RSAPublicKey) {
      //        RSAPublicKey rsaPublicKey = (RSAPublicKey) publicKey;
      //        BigInteger modulus = WonRdfUtils.SignatureUtils.getRsaPublicKeyModulus(webID, dataset);
      //        BigInteger exponent = WonRdfUtils.SignatureUtils.getRsaPublicKeyExponent(webID, dataset);
      //        if (exponent != null && rsaPublicKey.getPublicExponent().equals(exponent)) {
      //          if (modulus != null && rsaPublicKey.getModulus().equals(modulus)) {
      //            verified.add(webID.toString());
      //          }
      //        }
      //      }
      if (publicKey instanceof ECPublicKey) {
        ECPublicKey ecPublicKey = (ECPublicKey) publicKey;
        WonKeysReaderWriter ecKeyReader = new WonKeysReaderWriter();
        Set<PublicKey> keys = ecKeyReader.readFromDataset(dataset, webID.toString());
        for (PublicKey key: keys) {
          ECPublicKey ecPublicKeyFetched = (ECPublicKey) key;
          //TODO check if equals work
          if (ecPublicKey.getW().getAffineX().equals(ecPublicKeyFetched.getW().getAffineX())) {
            if (ecPublicKey.getW().getAffineY().equals(ecPublicKeyFetched.getW().getAffineY())) {
              verified.add(webID.toString());
            }
          }
        }
      } else {
        throw new IOException("Key type " + publicKey.getAlgorithm() + " not supported");
      }
    }
    return verified;
  }

  public void setLinkedDataSource(final LinkedDataSource linkedDataSource) {
    this.linkedDataSource = linkedDataSource;
  }

}
