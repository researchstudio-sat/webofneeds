package won.cryptography.service;

import org.apache.activemq.broker.ConnectionContext;
import org.apache.activemq.command.Message;
import org.apache.activemq.security.MessageAuthorizationPolicy;
import org.apache.commons.codec.digest.DigestUtils;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

/**
 * User: ypanchenko
 * Date: 17.09.2015
 */
//TODO make extractable froms won transmission config if server-side configurations are there as well
public class MessageOwnerConsumptionPolicy implements MessageAuthorizationPolicy
{
  private static final String OWNER_OUT_DESTINATION_NAME_START = "OwnerProtocol.Out.";

  @Override
  public boolean isAllowedToConsume(final ConnectionContext context, final Message message) {
    if (isForSpecificOwner(message)) {
      return isOwnerAllowedToConsume(context, message);
    }
    return true;
  }

  /**
   * Owner id is defined as a sha-1 fingerprint of owner certificate, based on the results of
   * comparing the owner id in queue name and the provided certificate fingerprint, the access
   * to read from that queue can be granted or denied.
   * @param context
   * @param message
   * @return
   */
  private boolean isOwnerAllowedToConsume(final ConnectionContext context, final Message message) {
    if (context.getConnectionState().getInfo().getTransportContext() instanceof X509Certificate[]) {
      X509Certificate ownerCert = ((X509Certificate[]) context.getConnectionState().getInfo().getTransportContext())[0];
      String ownerSha1Fingerprint = null;
      try {
        ownerSha1Fingerprint = DigestUtils.shaHex(ownerCert.getEncoded());
      } catch (CertificateEncodingException e) {
        new IllegalArgumentException("Could not calculate sha-1 of owner certificate", e);
      }
      String forOwnerId = message.getDestination().getPhysicalName().substring(OWNER_OUT_DESTINATION_NAME_START
                                                                                 .length());
      if (ownerSha1Fingerprint.equals(forOwnerId)) {
        return true;
      }
    }
    return false;
  }

  private boolean isForSpecificOwner(final Message message) {
    if (message.getDestination().getPhysicalName().indexOf(OWNER_OUT_DESTINATION_NAME_START) == 0) {
      return true;
    }
    return false;
  }
}
