/*
 * Copyright 2012 Research Studios Austria Forschungsges.m.b.H. Licensed under
 * the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package won.node.springsecurity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.util.StopWatch;
import won.cryptography.webid.WebIDVerificationAgent;
import won.node.springsecurity.userdetails.AnonymousUserDetails;
import won.node.springsecurity.userdetails.ClientCertificateUserDetails;
import won.node.springsecurity.userdetails.WebIdUserDetails;

import java.lang.invoke.MethodHandles;
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
public class UserDetailsService implements AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Autowired
    private WebIDVerificationAgent webIDVerificationAgent;

    @Override
    public UserDetails loadUserDetails(final PreAuthenticatedAuthenticationToken token)
                    throws UsernameNotFoundException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            String principal = (String) token.getPrincipal();
            Object credentials = token.getCredentials();
            logger.debug("Adding userDetails for '" + principal + "'");
            if (credentials instanceof Certificate) {
                return handleClientCertificate((Certificate) credentials, principal);
            } else {
                throw new IllegalArgumentException("Cannot load user details for pricipal/credentials");
            }
        } finally {
            stopWatch.stop();
            logger.debug("webID check took " + stopWatch.getLastTaskTimeMillis() + " millis");
        }
    }

    private UserDetails handleAnonymousRequest() {
        return new AnonymousUserDetails(List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
    }

    private UserDetails handleClientCertificate(Certificate certificate, String principal) {
        List<GrantedAuthority> authorities = new ArrayList<>(3);
        authorities.add(new SimpleGrantedAuthority("ROLE_CLIENT_CERTIFICATE"));
        logger.debug("checking if principal '" + principal + "' is a webId");
        URI webID = toUriIfPossible(principal);
        if (webID != null) {
            // principal is an URI, try to verify:
            try {
                if (webIDVerificationAgent.verify(certificate.getPublicKey(), webID)) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_WEBID"));
                    logger.debug("webId '" + principal + "' successfully verified - ROLE_WEBID granted");
                    return new WebIdUserDetails(webID, authorities);
                } else {
                    logger.debug("could not verify webId '" + principal + "'. ROLE_WEBID not granted");
                }
            } catch (Exception e) {
                logger.debug("could not verify webId '" + principal
                                + "' because of an error during verification. ROLE_WEBID "
                                + "not granted. Cause is logged",
                                e);
            }
        }
        // principal is not an URI or verification failed. Still, the client presented a
        // certificate
        return new ClientCertificateUserDetails(principal, authorities);
    }

    private URI toUriIfPossible(String principal) {
        try {
            return new URI(principal);
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
