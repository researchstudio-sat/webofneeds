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

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.authentication.preauth.x509.SubjectDnX509PrincipalExtractor;
import org.springframework.security.web.authentication.preauth.x509.X509PrincipalExtractor;

import won.protocol.vocabulary.WONCRYPT;

/**
 * Created by fkleedorfer on 28.11.2016.
 */
public class ReverseProxyCompatibleX509AuthenticationFilter extends AbstractPreAuthenticatedProcessingFilter {
    private final boolean behindProxy;
    private X509PrincipalExtractor principalExtractor = new SubjectDnX509PrincipalExtractor();

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
     * Depending on the value of behindProxy, the certificate is extracted from the request context or from the
     * 'X-Client-Certificate' header.
     * 
     * @param request
     * @return
     */
    private X509Certificate extractClientCertificate(HttpServletRequest request) {
        X509Certificate[] certificateChainObj = null;

        if (behindProxy) {

            CertificateFactory certificateFactory = null;
            try {
                certificateFactory = CertificateFactory.getInstance("X.509");
            } catch (CertificateException e) {
                throw new InternalAuthenticationServiceException("could not extract certificate from request", e);
            }
            String certificateHeader = request.getHeader(WONCRYPT.CLIENT_CERTIFICATE_HEADER);

            if (certificateHeader == null) {
                throw new AuthenticationCredentialsNotFoundException(
                        "No HTTP header 'X-Client-Certificate' set that contains client authentication certificate! If property "
                                + "'client.authentication.behind.proxy' is set to true, this header must be "
                                + "set by the reverse proxy!");
            }
            // the load balancer (e.g. nginx) forwards the certificate into a header by replacing new lines with
            // whitespaces
            // (2 or more for nginx, 1 for apache 2.4 - for the latter case we have to add the lookbehind pattern). Also
            // replace tabs, which sometimes nginx may send instead of whitespace
            String certificateContent = certificateHeader
                    .replaceAll("(?<!-----BEGIN|-----END)\\s+", System.lineSeparator())
                    .replaceAll("\\t+", System.lineSeparator());
            if (logger.isDebugEnabled()) {
                logger.debug("found this certificate in the " + WONCRYPT.CLIENT_CERTIFICATE_HEADER + " header: "
                        + certificateHeader);
                logger.debug("found this certificate in the " + WONCRYPT.CLIENT_CERTIFICATE_HEADER
                        + " header (after whitespace replacement): " + certificateContent);
            }
            X509Certificate[] userCertificate = new X509Certificate[1];
            try {
                userCertificate[0] = (X509Certificate) certificateFactory
                        .generateCertificate(new ByteArrayInputStream(certificateContent.getBytes("ISO-8859-11")));
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
                throw new AuthenticationCredentialsNotFoundException(
                        "Client certificate attribute is null! Check if you are behind a proxy server that takes care about the "
                                + "client authentication already. If so, set the property 'client.authentication.behind.proxy' to true and "
                                + "make sure the proxy sets the HTTP header 'X-Client-Certificate' appropriately to the sent client certificate");
            }
        }

        return certificateChainObj[0];
    }

    public void setPrincipalExtractor(X509PrincipalExtractor principalExtractor) {
        this.principalExtractor = principalExtractor;
    }
}
