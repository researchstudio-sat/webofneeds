package won.cryptography.service;

import org.apache.http.conn.ssl.PrivateKeyStrategy;
import org.apache.http.conn.ssl.TrustStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.cryptography.keymanagement.KeyPairAliasDerivationStrategy;
import won.cryptography.ssl.*;
import won.protocol.rest.LinkedDataRestBridge;
import won.protocol.rest.LinkedDataRestClient;
import won.protocol.rest.LinkedDataRestClientHttps;

import javax.annotation.PostConstruct;

/**
 * User: ypanchenko
 * Date: 07.10.2015
 */
public class DefaultSecurityWonTransmissionService implements WonTransmissionService
{

  private final Logger logger  = LoggerFactory.getLogger(getClass());

  private LinkedDataRestClient linkedDataClient;
  private LinkedDataRestBridge linkedDataRestBridge;

  private String registrationQuery;
  private RegistrationClient registrationClient;
  private RegistrationServer registrationServer;

  private MessagingContext clientMessagingContext;

  private CryptographyService clientCryptographyService;

  // The same trustStoreService is used both when acting as client and as server.
  private TrustStoreService trustStoreService;

  // For the client we have a key store, that is not updated in case of node and matcher, and is
  // updated in case of owner (we add needs' keys)
  private KeyStoreService clientKeyStoreService;

  //strategy for deriving a key pair alias from a need uri
  private KeyPairAliasDerivationStrategy keyPairAliasDerivationStrategy;

  // TODO For the server we have a keystore that won't be changed or updated, so it can be used directly. In the
  // current implementation, the server's key store is different from the store that server uses when it itself
  // serves as client (see client key store).
  // It would be more 'pretty' if we wrap the server's key store into storeService as well, but because it can be in
  // another format than our clientKeyStore supports, we don't do it at the moment. Instead, the server keystore
  // is provided directly - in server.xml and in activemq.xml
  // private KeyStoreService serverKeyStoreService;


  public void setTrustStoreService(final TrustStoreService trustStoreService) {
    this.trustStoreService = trustStoreService;
  }

  public void setClientKeyStoreService(final KeyStoreService clientKeyStoreService) {
    this.clientKeyStoreService = clientKeyStoreService;
  }

  //TODO can we make this part of the node information service and instead of configuring here, the registration
  //client will register using call to e.g. register(won-node, registrationQuery) directly
  public void setRegistrationQuery(final String registrationQuery) {
    this.registrationQuery = registrationQuery;
  }

  public void setKeyPairAliasDerivationStrategy(KeyPairAliasDerivationStrategy keyPairAliasDerivationStrategy) {
    this.keyPairAliasDerivationStrategy = keyPairAliasDerivationStrategy;
  }

  /**
   * Initializes necessary for WON transmission properties according to the default security settings that can be
   * described shortly as:
   * 1. ssl/tls enabled on http (server authentication) and messaging/activemq (server and client authentication)
   * 2. during linked data requests client trusts all the servers
   * 3. during linked data requests client can provide his certificate to prove his WebID identity if server requires
   * 4. during registration parties exchange certificates after which they trust each other (those certificates)
   * 5. during messaging parties only trust each other if they already know each other certificates (according to 4)
   */
  @PostConstruct
  public void initialize() {

    //----------CRYPTOGRAPHY SERVICE--------
    // Many components (e.g. Signature checking and Key adding processor) rely on the cryptography service pointing
    // to the correct key store (where also the default application certificate is expected to be found) and applying
    // the correct (supported in WON) key pair and certificate generation. It has to be initialized as first, because
    // it will create the default certificate for the key store if not present.
    clientCryptographyService = new CryptographyService(clientKeyStoreService, new KeyPairService(), new CertificateService());
    //----------CRYPTOGRAPHY SERVICE--------




    //---------------LINKED DATA---------------
    // LINKED DATA client-side (for accessing linked data on Node with GET):
    TrustStrategy linkedDataClientStrategy = new TrustAnyCertificateStrategy();
    //this.linkedDataClientStrategy = new TrustSelfSignedStrategy();
    //this.linkedDataClientStrategy = new TrustNooneStrategy();
    this.linkedDataClient = new LinkedDataRestClientHttps(clientKeyStoreService,
                                                          trustStoreService, linkedDataClientStrategy, keyPairAliasDerivationStrategy);


    // temporary client to access response of linked data resources of node -
    // here uses the same key/trust setting as linkedDataClient
    this.linkedDataRestBridge = new LinkedDataRestBridge(clientKeyStoreService,
                                                         trustStoreService, linkedDataClientStrategy, keyPairAliasDerivationStrategy);

    // LINKED DATA server-side:
    // Server-side is configured via Filters that do access control and WebID verification in spring/node-context.xml
    // at Node (won-node-webapp). Because of such WebID-based access restrictions to some of the linked data resources,
    // the linked data client specified above should be HTTPS client able to prove its WebID identity.
    //---------------LINKED DATA---------------




    //---------------REGISTRATION---------------
    // REGISTRATION client-side:
    TOFUStrategy registrationClientStrategy = new TOFUStrategy();
    registrationClientStrategy.setTrustStoreService(trustStoreService);
    registrationClientStrategy.setAliasGenerator(new AliasFromFingerprintGenerator());
    PrivateKeyStrategy clientDefaultAliasKeyStrategy = new
      PredefinedAliasPrivateKeyStrategy(clientKeyStoreService.getDefaultAlias());
    this.registrationClient = new RegistrationRestClientHttps(clientKeyStoreService, clientDefaultAliasKeyStrategy,
                                                              trustStoreService,
                                                              registrationClientStrategy,
                                                              registrationQuery);

    // REGISTRATION server-side:
    TOFUStrategy registrationServerStrategy = new TOFUStrategy();
    registrationServerStrategy.setTrustStoreService(trustStoreService);
    registrationServerStrategy.setAliasGenerator(new AliasFromFingerprintGenerator());
    this.registrationServer = new RegistrationServerCertificateBased(registrationServerStrategy);
    //---------------REGISTRATION---------------




    //---------------MESSAGING---------------
    // MESSAGING client-side:
    //TODO ?TrustFromStoreServiceStrategy
    this.clientMessagingContext = new MessagingContext(this.clientKeyStoreService, clientDefaultAliasKeyStrategy,
                                                       this.trustStoreService);

    // MESSAGING server-side:
    // the server's (broker) activemq config is currently configured in <broker> in xml where the keystore to use and
    // defined trustmanager to use are located. The web server's (in our case tomcat) config is defined in server.xml
    // and defines the key store to use as well as trust configuration - in our case it trusts all self-signed
    // certificates. The same server's private key (same keystore) is used for broker and for web-server -
    // it is different from the keystore that we use when the server serves as client (i.e. serves as
    // producer/consumer/http client).
    //---------------MESSAGING---------------

  }

  @Override
  public MessagingContext getClientMessagingContext() {
    return clientMessagingContext;
  }

  @Override
  public RegistrationClient getRegistrationClient() {
    return registrationClient;
  }

  @Override
  public RegistrationServer getRegistrationServer() {
    return registrationServer;
  }

  @Override
  public LinkedDataRestClient getLinkedDataClient() {
    return linkedDataClient;
  }

  @Override
  public LinkedDataRestBridge getLinkedDataRestBridge() {
    return linkedDataRestBridge;
  }

  @Override
  public CryptographyService getClientCryptographyService() {
    return clientCryptographyService;
  }

}
