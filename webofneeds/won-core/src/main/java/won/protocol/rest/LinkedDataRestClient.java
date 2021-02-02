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
package won.protocol.rest;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StopWatch;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import won.protocol.util.LogMarkers;
import won.protocol.util.linkeddata.IncludedWonOntologies;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;

public abstract class LinkedDataRestClient {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final int FALLBACK_CACHE_MAX_AGE_SECONDS = 3600;
    // the includedeWonOntologies caches all ontologies we cannot access online and
    // therefore use in the packaged version. Obviously, this constitues a memory
    // leak if there are many instances of the LinkedDataRestClient but we assume
    // this is not the case

    /**
     * Retrieves RDF for the specified resource URI. Expects that the resource URI
     * will lead to a 303 response, redirecting to the URI where RDF can be
     * downloaded. Paging is not supported.
     *
     * @param resourceURI
     * @return
     */
    public Dataset readResourceData(URI resourceURI) {
        return readResourceDataWithHeaders(resourceURI).getDataset();
    }

    public abstract DatasetResponseWithStatusCodeAndHeaders readResourceDataWithHeaders(URI resourceURI,
                    HttpHeaders httpHeaders);

    /**
     * Retrieves RDF for the specified resource URI for the entity with provided
     * WebID. Expects that the resource URI will lead to a 303 response, redirecting
     * to the URI where RDF can be downloaded. Paging is not supported.
     *
     * @param resourceURI
     * @param requesterWebID
     * @return
     */
    public Dataset readResourceData(URI resourceURI, URI requesterWebID) {
        return readResourceDataWithHeaders(resourceURI, requesterWebID).getDataset();
    }

    public abstract DatasetResponseWithStatusCodeAndHeaders readResourceDataWithHeaders(URI resourceURI);

    public abstract DatasetResponseWithStatusCodeAndHeaders readResourceDataWithHeaders(URI resourceURI,
                    URI requesterWebID);

    public abstract DatasetResponseWithStatusCodeAndHeaders readResourceDataWithHeaders(URI resourceURI,
                    URI requesterWebID, HttpHeaders requestHeaders);

    protected DatasetResponseWithStatusCodeAndHeaders readResourceData(URI resourceURI, RestTemplate restTemplate,
                    HttpHeaders requestHeaders) {
        assert resourceURI != null : "resource URI must not be null";
        StopWatch sw = new StopWatch();
        sw.start();
        logger.debug("fetching linked data resource: {}", resourceURI);
        // If a RestClientException is thrown here complaining that it can't read a
        // Model with MIME media type text/html,
        // it was probably the wrong resourceURI
        Dataset result;
        int statusCode;
        HttpHeaders responseHeaders;
        try {
            HttpEntity entity = new HttpEntity(null, requestHeaders);
            ResponseEntity<Dataset> response = restTemplate.exchange(resourceURI, HttpMethod.GET, entity,
                            Dataset.class);
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
            logger.debug("Could not fetch {} from the Web, searching fallback in included resources", resourceURI);
            if (e instanceof HttpClientErrorException.NotFound) {
                Optional<Model> fallbackResult = IncludedWonOntologies.get(resourceURI);
                if (fallbackResult.isPresent()) {
                    logger.debug("Found fallback resource for {}, returning as result", resourceURI);
                    // we want the application to get a (possibly updated) version of this resource
                    // eventually, but we
                    // also want to avoid to keep making failing requests. We return the fallback
                    // result and use
                    // a caching period of one hour.
                    Dataset dataset = DatasetFactory.createGeneral();
                    dataset.setDefaultModel(fallbackResult.get());
                    HttpHeaders headers = new HttpHeaders();
                    headers.add(HttpHeaders.CACHE_CONTROL, "max-age=" + FALLBACK_CACHE_MAX_AGE_SECONDS);
                    return new DatasetResponseWithStatusCodeAndHeaders(dataset, 200, headers);
                }
            }
            if (e instanceof HttpClientErrorException.Forbidden) {
                List<String> wwwAuthenticateHeaders = ((HttpClientErrorException.Forbidden) e).getResponseHeaders()
                                .get(HttpHeaders.WWW_AUTHENTICATE);
                if (wwwAuthenticateHeaders != null && !wwwAuthenticateHeaders.isEmpty()) {
                    String headerValue = wwwAuthenticateHeaders.get(0);
                    throw new LinkedDataFetchingException.ForbiddenAuthMethodProvided(resourceURI,
                                    String.format("Access to %s was denied (forbidden), but WWW-Authenticate Header indicates how to authorize",
                                                    resourceURI),
                                    e, headerValue);
                } else {
                    throw new LinkedDataFetchingException.Forbidden(resourceURI,
                                    String.format("Access to %s was denied (forbidden)",
                                                    resourceURI),
                                    e);
                }
            }
            if (e instanceof HttpClientErrorException.Unauthorized) {
                List<String> wwwAuthenticateHeaders = ((HttpClientErrorException.Forbidden) e).getResponseHeaders()
                                .get(HttpHeaders.WWW_AUTHENTICATE);
                if (wwwAuthenticateHeaders != null && !wwwAuthenticateHeaders.isEmpty()) {
                    String headerValue = wwwAuthenticateHeaders.get(0);
                    throw new LinkedDataFetchingException.UnauthorizedAuthMethodProvided(resourceURI,
                                    String.format("Access to %s was denied (unauthorized, possibly due to an invalid token), but WWW-Authenticate Header indicates how to authorize",
                                                    resourceURI),
                                    e, headerValue);
                } else {
                    throw new LinkedDataFetchingException.Unauthorized(resourceURI,
                                    String.format("Access to %s was denied (unauthorized, possibly due to an invalid token)",
                                                    resourceURI),
                                    e);
                }
            }
            if (e instanceof HttpClientErrorException) {
                throw new LinkedDataFetchingException(MessageFormat.format(
                                "Caught a HttpClientErrorException exception, for {0}. Underlying error message is: {1}, response Body: {2}",
                                resourceURI, e.getMessage(), ((HttpClientErrorException) e).getResponseBodyAsString()),
                                e, resourceURI, ((HttpClientErrorException) e).getRawStatusCode());
            }
            if (e instanceof HttpServerErrorException) {
                throw new LinkedDataFetchingException(MessageFormat.format(
                                "Caught a HttpServerErrorException exception, for {0}. Underlying error message is: {1}, response Body: {2}",
                                resourceURI, e.getMessage(), ((HttpServerErrorException) e).getResponseBodyAsString()),
                                e, resourceURI, ((HttpServerErrorException) e).getRawStatusCode());
            }
            throw new LinkedDataFetchingException(resourceURI,
                            MessageFormat.format("Caught a clientHandler exception, "
                                            + "which may indicate that the URI that was accessed isn''t a"
                                            + " linked data URI, please check {0}. Underlying error message is: {1}",
                                            resourceURI, e.getMessage()),
                            e);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("fetched model with {} statements in default model for resource {}",
                            result.getDefaultModel().size(), resourceURI);
        }
        sw.stop();
        logger.debug(LogMarkers.TIMING, "fetching {} took {} millis", resourceURI, sw.getLastTaskTimeMillis());
        return new DatasetResponseWithStatusCodeAndHeaders(result, statusCode, responseHeaders);
    }
}
