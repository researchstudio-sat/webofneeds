package won.cryptography.depr;

/**
 * User: ypanchenko
 * Date: 08.10.2015
 */
public class RegistrationServerDepr
{

//  final Logger logger = LoggerFactory.getLogger(getClass());
//
//
//  //TODO how to make the implementation dependant on registration strategy in won-config - because here I manually do
//  // TOFU and if this or config changes - can be inconsistencies between the specified in config and what really
//  // happens
//
//  //private WonTlsSecurityConfig securityConfig;
//  @Autowired
//  //private OwnerManagementServiceImpl ownerManagementService;
//  private ApplicationManagementService ownerManagementService;
//
////  public void setSecurityConfig(final WonTlsSecurityConfig securityConfig) {
////    this.securityConfig = securityConfig;
////  }
//
//  private TrustStoreService trustStoreService;
//
//  //TODO add/change to strategy... e.g. so that I can pass TOFU or other strategy...
//  public RegistrationServerDepr(final TrustStoreService trustStoreService) {
//    this.trustStoreService = trustStoreService;
//  }
//
//  public ResponseEntity<String> register(final String registeredType, final Object certificateChainObj) {
//    logger.info("REGISTER " + registeredType);
//    String supportedTypesMsg = "Request parameter error; supported 'register' parameter values: 'owner', 'node'";
//
//    if (registeredType == null || registeredType.isEmpty()) {
//      logger.warn(supportedTypesMsg);
//      return new ResponseEntity<String>(supportedTypesMsg, HttpStatus.BAD_REQUEST);
//    }
//
//
//    if (registeredType.equals("owner")) {
//      return registerOwner(certificateChainObj);
//    }
//
//    if (registeredType.equals("node")) {
//      return registerNode(certificateChainObj);
//    }
//
//    return new ResponseEntity<String>(supportedTypesMsg, HttpStatus.BAD_REQUEST);
//  }
//
//
//
//  private ResponseEntity<String> registerOwner(Object certificateChainObj) {
//
//    // Registration for owner without certificate is not supported:
//    if (certificateChainObj == null) {
//      logger.info("Cannot register - owner did not provide certificate!");
//      return new ResponseEntity<String>("Certificate not provided - cannot register owner" , HttpStatus.NOT_FOUND);
//    }
//
//    // Prepare certificate and calculated from it owner-id:
//    X509Certificate ownerCert = ((X509Certificate[]) certificateChainObj)[0];
//    String ownerSha1Fingerprint = null;
//    try {
//      ownerSha1Fingerprint = DigestUtils.shaHex(ownerCert.getEncoded());
//    } catch (CertificateEncodingException e) {
//      return new ResponseEntity<String>(e.toString(), HttpStatus.BAD_REQUEST);
//    }
//
//    // Check the trust store, if already exists, return the existing id:
//    if (trustStoreService.isCertKnown(ownerCert)) {
//      logger.info("Cannot register - owner with this certificate is already known!");
//      //TODO when in development mode - e.g. if redeployed - owner db can me empty, while the certificate is still in
//      // the trust store, should handle it somehow
//      return new ResponseEntity<String>(ownerSha1Fingerprint, HttpStatus.ALREADY_REPORTED);
//    }
//
//    // TODO the case when its the same physical owner with another certificate we probably cannot detect - we will
//    // just register it with new id. But we could at least check that the owner with the same web-id is not
//    // registered with different certificate already - in that case we should reject registration. For that we
//    // should require that owner certificate contains web-id in alternative names - and this it its turn would
//    // only make sense if owner itself is required to be published as a need.
//
//
//    // If the alias is known but certificate differs, we have a fingerprint collision. We are not accounting
//    // for this case at the moment, we just report an error:
//    Certificate retrieved = trustStoreService.getCertificate(ownerSha1Fingerprint);
//    if (retrieved != null) {
//      String msg = "Owner's fingerprint collision, cannot register - use another certificate!";
//      logger.warn(msg);
//      return new ResponseEntity<String>(msg, HttpStatus.BAD_REQUEST);
//    }
//
//    // Register with fingerprint as owner id
//    String ownerId = null;
//    try {
//      ownerId = ownerManagementService.registerOwnerApplication(ownerSha1Fingerprint);
//    } catch (Exception e) {
//      return new ResponseEntity<String>(e.toString(), HttpStatus.BAD_REQUEST);
//    }
//
//    // Add to the trust store
//    trustStoreService.addCertificate(ownerId, ownerCert);
//    // Return owner id
//    logger.info("Registered owner with id " + ownerId);
//    return new ResponseEntity<String>(ownerId, HttpStatus.OK);
//
//  }
//
//
//  private ResponseEntity<String> registerNode(Object certificateChainObj) {
//
//    // Registration for owner without certificate is not supported:
//    if (certificateChainObj == null) {
//      return new ResponseEntity<String>("Certificate not provided - cannot register owner" , HttpStatus.NOT_FOUND);
//    }
//
//    // Prepare certificate
//    X509Certificate nodeCert = ((X509Certificate[]) certificateChainObj)[0];
//
//    // Check the trust store, if already exists, return
//    if (trustStoreService.isCertKnown(nodeCert)) {
//      logger.info("Cannot register - this certificate is already trusted!");
//      return new ResponseEntity<String>(HttpStatus.ALREADY_REPORTED);
//    }
//
//    String alias;
//    try {
//      X500Name dnName = new X500Name(nodeCert.getSubjectDN().getName());
//      alias = dnName.getCommonName();
//      // TODO for web-id do in special call a method smth like...
//      //alias = nodeCert.getWebIdFromSubjectAlternativeNames()...
//    } catch (IOException e) {
//      return new ResponseEntity<String>("No CN or Web-ID name provided in certificate", HttpStatus.BAD_REQUEST);
//    }
//
//    // TODO the case when its the same physical node with another certificate we probably cannot detect
//    // But we could at least check that the node with the same web-id is not
//    // registered with different certificate already - in that case we should return an error.
//    // we should also in this case do the web-id verification check
//    // for now - as we take alias from common name - we check against that, i.e:
//    // If the alias is known but certificate differs, we report an error:
//    Certificate retrieved = trustStoreService.getCertificate(alias);
//    if (retrieved != null) {
//      String msg = "Node is already known under different certificate, cannot trust!";
//      logger.warn(msg);
//      return new ResponseEntity<String>(msg, HttpStatus.BAD_REQUEST);
//    }
//
//    // Add as trusted
//    trustStoreService.addCertificate(alias, nodeCert);
//    // Return owner id
//    logger.info("Added as trusted: " + alias);
//    return new ResponseEntity<String>(alias, HttpStatus.OK);
//
//  }
}
