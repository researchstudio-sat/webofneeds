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

package won.node.springsecurity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * Assumes that the provided username is a linked data URI that contains WebID
 * information. The URI is accessed and the RDF is downloaded and added to the
 * UserDetails for future reference.
 */
public class ClientCertificateNoWebIdUserDetailsService
    implements AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Override
  public UserDetails loadUserDetails(final PreAuthenticatedAuthenticationToken token) throws UsernameNotFoundException {
    String principal = (String) token.getPrincipal();
    Certificate certificate = (Certificate) token.getCredentials();

    logger.debug("Adding userDetails for '" + principal + "'");
    URI commonName = null;
    try {
      commonName = new URI(principal);
    } catch (URISyntaxException e) {
      throw new BadCredentialsException(
          "Principal of X.509 Certificate must be a WebId URI. Actual value: '" + principal + "'");
    }

    // at this point, we know that a client certificate was presented. Grant this
    // role:
    List<GrantedAuthority> authorities = new ArrayList<>(3);
    authorities.add(new SimpleGrantedAuthority("ROLE_CLIENT_CERTIFICATE_PRESENTED"));
    return new ClientCertificateNoWebIdUserDetails(commonName, authorities);
  }
}
