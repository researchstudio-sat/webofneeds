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

package won.cryptography.webid.springsecurity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import won.cryptography.webid.WebIDVerificationAgent;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.Certificate;

/**
 * Assumes that the provided username is a linked data URI that contains WebID information.
 * The URI is accessed and the RDF is downloaded and added to the UserDetails for future reference.
 */
public class WebIdUserDetailsService implements AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken>
{
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired
  private WebIDVerificationAgent webIDVerificationAgent;

  @Override
  public UserDetails loadUserDetails(final PreAuthenticatedAuthenticationToken token) throws UsernameNotFoundException {
    String principal = (String) token.getPrincipal();
    Certificate certificate = (Certificate) token.getCredentials();
    logger.debug("Adding userDetails for '" + principal +"'");
    URI webID = null;
    try {
      webID = new URI(principal);
    } catch (URISyntaxException e){
      throw new BadCredentialsException("Principal of X.509 Certificate must be a WebId URI. Actual value: '" +
                                          principal+"'");
    }
    logger.debug("verifying webId '" + principal +"'");
    if (webIDVerificationAgent.verify(certificate.getPublicKey(), webID)){
      token.getAuthorities().add(new SimpleGrantedAuthority("ROLE_WEBID"));
      logger.debug("webId '" + principal +"' successfully verified - ROLE_WEBID granted");
    } else {
      logger.debug("could not verify webId '" + principal +"'. ROLE_WEBID not granted");
    }
    return new WebIdUserDetails(webID);
  }
}

