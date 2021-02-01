package won.auth.rest;

import org.apache.http.ssl.TrustStrategy;
import org.apache.jena.query.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StopWatch;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import won.auth.model.AuthToken;
import won.cryptography.keymanagement.KeyPairAliasDerivationStrategy;
import won.cryptography.service.TrustStoreService;
import won.cryptography.service.keystore.KeyStoreService;
import won.protocol.rest.DatasetResponseWithStatusCodeAndHeaders;
import won.protocol.rest.LinkedDataFetchingException;
import won.protocol.rest.LinkedDataRestClientHttps;
import won.protocol.util.LogMarkers;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AuthEnabledLinkedDataRestClient extends LinkedDataRestClientHttps {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private ConcurrentHashMap<TokenKey, AuthToken> tokens = new ConcurrentHashMap<>();

    public AuthEnabledLinkedDataRestClient(KeyStoreService keyStoreService,
                    TrustStoreService trustStoreService,
                    TrustStrategy trustStrategy,
                    KeyPairAliasDerivationStrategy keyPairAliasDerivationStrategy) {
        super(keyStoreService, trustStoreService, trustStrategy, keyPairAliasDerivationStrategy);
    }

    protected void purgeExpiredTokens() {
        Iterator<Map.Entry<TokenKey, AuthToken>> it = tokens.entrySet().iterator();
        Instant now = Instant.now();
        while (it.hasNext()) {
            Map.Entry<TokenKey, AuthToken> entry = it.next();
            if (entry.getValue().getTokenExp().asCalendar().toInstant().compareTo(now) < 0) {
                it.remove();
            }
        }
    }

    /**
     * Retrieves RDF for the specified resource URI for the entity, authenticating
     * with the provided token. Expects that the resource URI will lead to a 303
     * response, redirecting to the URI where RDF can be downloaded. Paging is not
     * supported.
     *
     * @param resourceURI
     * @param authToken
     * @return
     */
    public DatasetResponseWithStatusCodeAndHeaders readResourceDataWithHeaders(URI resourceURI,
                    String authToken) {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add(HttpHeaders.AUTHORIZATION, "Bearer " + authToken);
        return readResourceDataWithHeaders(resourceURI, null, requestHeaders);
    }

    public Dataset readResourceData(URI resourceURI, String authToken) {
        return readResourceDataWithHeaders(resourceURI, authToken).getDataset();
    }

    public Set<String> readAccessTokens(URI resourceURI, URI webId) {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add(HttpHeaders.ACCEPT, MimeTypeUtils.APPLICATION_JSON_VALUE);
        return readAccessTokens(resourceURI,
                        getRestTemplateForReadingLinkedData(webId == null ? null : webId.toString()), requestHeaders);
    }

    public Set<String> readAccessTokens(URI resourceURI, String authToken) {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add(HttpHeaders.ACCEPT, MimeTypeUtils.APPLICATION_JSON_VALUE);
        requestHeaders.add(HttpHeaders.AUTHORIZATION, "Bearer " + authToken);
        return readAccessTokens(resourceURI, getRestTemplateForReadingLinkedData(null), requestHeaders);
    }

    public Set<String> readAccessTokens(URI resourceURI, RestTemplate restTemplate,
                    HttpHeaders requestHeaders) {
        assert resourceURI != null : "resource URI must not be null";
        StopWatch sw = new StopWatch();
        sw.start();
        logger.debug("fetching linked data resource: {}", resourceURI);
        // If a RestClientException is thrown here complaining that it can't read a
        // Model with MIME media type text/html,
        // it was probably the wrong resourceURI
        Set<String> result;
        int statusCode;
        HttpHeaders responseHeaders;
        try {
            HttpEntity entity = new HttpEntity(null, requestHeaders);
            ResponseEntity<Set> response = restTemplate.exchange(resourceURI, HttpMethod.GET, entity, Set.class);
            // RestTemplate will automatically follow redirects on HttpGet calls
            statusCode = response.getStatusCode().value();
            responseHeaders = response.getHeaders();
            if (response.getStatusCode().is4xxClientError()) {
                throw new HttpClientErrorException(response.getStatusCode());
            }
            if (response.getStatusCode().is5xxServerError()) {
                throw new HttpServerErrorException(response.getStatusCode());
            }
            result = response.getBody();
        } catch (RestClientException e) {
            // first, let's see if we can answer the request from loaded ontologies:
            if (e instanceof HttpClientErrorException) {
                throw new LinkedDataFetchingException(MessageFormat.format(
                                "Caught a HttpClientErrorException exception trying to obtain token from {0}. Underlying error message is: {1}, response Body: {2}",
                                resourceURI, e.getMessage(), ((HttpClientErrorException) e).getResponseBodyAsString()),
                                e, resourceURI, ((HttpClientErrorException) e).getRawStatusCode());
            }
            if (e instanceof HttpServerErrorException) {
                throw new LinkedDataFetchingException(MessageFormat.format(
                                "Caught a HttpServerErrorException exception trying to obtain token from {0}. Underlying error message is: {1}, response Body: {2}",
                                resourceURI, e.getMessage(), ((HttpServerErrorException) e).getResponseBodyAsString()),
                                e, resourceURI, ((HttpServerErrorException) e).getRawStatusCode());
            }
            throw new LinkedDataFetchingException(resourceURI,
                            MessageFormat.format(
                                            "Caught a clientHandler exception trying to obtain token from {0}. Underlying error message is: {1}",
                                            resourceURI, e.getMessage()),
                            e);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("fetched auth token from {}", resourceURI);
        }
        sw.stop();
        logger.debug(LogMarkers.TIMING, "fetching {} took {} millis", resourceURI, sw.getLastTaskTimeMillis());
        return result;
    }

    private static class TokenKey {
        private URI atom;
        private String scope;

        public TokenKey(URI atom, String scope) {
            Objects.requireNonNull(atom);
            Objects.requireNonNull(scope);
            this.atom = atom;
            this.scope = scope;
        }

        public URI getAtom() {
            return atom;
        }

        public String getScope() {
            return scope;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            TokenKey tokenKey = (TokenKey) o;
            return Objects.equals(atom, tokenKey.atom) &&
                            Objects.equals(scope, tokenKey.scope);
        }

        @Override
        public int hashCode() {
            return Objects.hash(atom, scope);
        }
    }
}
