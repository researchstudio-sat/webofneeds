/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package won.node.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import won.cryptography.service.CertificateService;
import won.cryptography.service.KeyPairService;
import won.cryptography.service.KeyStoreService;

import java.math.BigInteger;
import java.net.URI;
import java.security.KeyPair;
import java.security.cert.Certificate;

/**
 * Checks if the node certificat is already present in the specified keystore
 * and creates it if this is not the case.
 */
public class NodeCertificateOnStartupCreator implements InitializingBean {
  private final Logger logger  = LoggerFactory.getLogger(getClass());

  @Value("${uri.prefix.resource}")
  private String alias;

  @Autowired(required = true)
  private KeyStoreService keyStoreService;
  private CertificateService certificateService = new CertificateService();
  private KeyPairService keyPairService = new KeyPairService();

  @Override
  public void afterPropertiesSet() throws Exception {
    logger.debug("checking if the node certificate with alias {} is in the keystore", alias);
    Certificate cert = keyStoreService.getCertificate(alias);
    if (cert != null) {
      logger.info("node certificate with alias {} found in the keystore", alias);
      return;
    }
    //no certificate, create it:
    logger.info("node certificate not found under alias {}, creating new one", alias);
    KeyPair keyPair = keyPairService.generateNewKeyPairInSecp384r1();
    BigInteger serialNumber = BigInteger.valueOf(1);
    cert = certificateService.createSelfSignedCertificate(serialNumber, keyPair, URI.create(alias).getAuthority(), alias);
    keyStoreService.putKey(alias, keyPair.getPrivate(), new Certificate[]{cert});
    logger.info("node certificate created");
  }

  public void setAlias(String alias) {
    this.alias = alias;
  }

  public void setKeyStoreService(KeyStoreService keyStoreService) {
    this.keyStoreService = keyStoreService;
  }
}
