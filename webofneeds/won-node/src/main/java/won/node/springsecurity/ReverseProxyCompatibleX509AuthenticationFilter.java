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
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.authentication.preauth.x509.SubjectDnX509PrincipalExtractor;
import org.springframework.security.web.authentication.preauth.x509.X509PrincipalExtractor;
import won.protocol.vocabulary.WON;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandles;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;

/**
 * Created by fkleedorfer on 28.11.2016.
 */
public class ReverseProxyCompatibleX509AuthenticationFilter extends AbstractPreAuthenticatedProcessingFilter {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final boolean behindProxy;
    private X509PrincipalExtractor principalExtractor = new SubjectDnX509PrincipalExtractor();
    private Instant lastWarning;
    private Duration warningInterval = Duration.ofHours(1);

    public ReverseProxyCompatibleX509AuthenticationFilter(final boolean behindProxy) {
        this.behindProxy = behindProxy;
    }

    protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
        X509Certificate cert = extractClientCertificate(request);
        if (cert == null) {
            return null;
        }
        return principalExtractor.extractPrincipal(cert);
    }

    @Override
    protected Object getPreAuthenticatedCredentials(final HttpServletRequest request) {
        return extractClientCertificate(request);
    }

    /**
     * Depending on the value of behindProxy, the certificate is extracted from the
     * request context or from the 'X-Client-Certificate' header.
     * 
     * @param request
     * @return
     */
    private X509Certificate extractClientCertificate(HttpServletRequest request) {
        X509Certificate[] certificateChainObj = null;
        if (behindProxy) {
            String certificateHeader = request.getHeader(WON.CLIENT_CERTIFICATE_HEADER);
            if (certificateHeader == null) {
                warnAboutMissingClientCertificate();
                return null;
            }
            CertificateFactory certificateFactory = null;
            try {
                certificateFactory = CertificateFactory.getInstance("X.509");
            } catch (CertificateException e) {
                throw new InternalAuthenticationServiceException("could not extract certificate from request", e);
            }
            String certificateContent = URLDecoder.decode(certificateHeader, StandardCharsets.UTF_8);
            if (logger.isDebugEnabled()) {
                logger.debug("found this certificate in the " + WON.CLIENT_CERTIFICATE_HEADER + " header: "
                                + certificateHeader);
                logger.debug("found this certificate in the " + WON.CLIENT_CERTIFICATE_HEADER
                                + " header (after whitespace replacement): " + certificateContent);
            }
            X509Certificate[] userCertificate = new X509Certificate[1];
            try {
                userCertificate[0] = (X509Certificate) certificateFactory.generateCertificate(
                                new ByteArrayInputStream(certificateContent.getBytes("ISO-8859-11")));
            } catch (CertificateException e) {
                throw new AuthenticationCredentialsNotFoundException("could not extract certificate from request", e);
            } catch (UnsupportedEncodingException e) {
                throw new AuthenticationCredentialsNotFoundException(
                                "could not extract certificate from request with encoding " + "ISO-8859-11", e);
            }
            certificateChainObj = userCertificate;
        } else {
            certificateChainObj = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
            if (certificateChainObj == null) {
                warnAboutMissingClientCertificate();
                return null;
            }
        }
        return certificateChainObj[0];
    }

    private void warnAboutMissingClientCertificate() {
        if (lastWarning == null || lastWarning
                        .isBefore(Instant.from(warningInterval.subtractFrom(Instant.now())))) {
            logger.warn(
                            "Client certificate attribute is null. This is ok for many requests but it may indicate that you are behind a proxy server that terminates TLS and your system is misconfigured."
                                            + " If this is the case, you never receive requests that include client certificates. If so, set the property 'client.authentication.behind.proxy' to true and "
                                            + "make sure the proxy sets the HTTP header 'X-Client-Certificate' appropriately, passing the client certificate on to you. This warning is not generated more than once per hour.");
            lastWarning = Instant.now();
        }
    }

    public void setPrincipalExtractor(X509PrincipalExtractor principalExtractor) {
        this.principalExtractor = principalExtractor;
    }
}
