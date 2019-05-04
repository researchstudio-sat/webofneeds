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

import java.net.URI;
import java.text.MessageFormat;

import org.apache.jena.query.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

public abstract class LinkedDataRestClient {
    private final Logger logger = LoggerFactory.getLogger(getClass());

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
        logger.debug("fetching linked data resource: {}", resourceURI);
        // If a RestClientException is thrown here complaining that it can't read a
        // Model with MIME media type text/html,
        // it was probably the wrong resourceURI
        Dataset result;
        int statusCode = -1;
        HttpHeaders responseHeaders = null;
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
            if (e instanceof HttpClientErrorException) {
                throw new LinkedDataFetchingException(resourceURI, MessageFormat.format(
                                "caught a HttpClientErrorException exception, for {0}. Underlying error message is: {1}, response Body: {2}",
                                resourceURI, e.getMessage(), ((HttpClientErrorException) e).getResponseBodyAsString()),
                                e);
            }
            if (e instanceof HttpServerErrorException) {
                throw new LinkedDataFetchingException(resourceURI, MessageFormat.format(
                                "caught a HttpServerErrorException exception, for {0}. Underlying error message is: {1}, response Body: {2}",
                                resourceURI, e.getMessage(), ((HttpServerErrorException) e).getResponseBodyAsString()),
                                e);
            }
            throw new LinkedDataFetchingException(resourceURI,
                            MessageFormat.format("caught a clientHandler exception, "
                                            + "which may indicate that the URI that was accessed isn''t a"
                                            + " linked data URI, please check {0}. Underlying error message is: {1}",
                                            resourceURI, e.getMessage()),
                            e);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("fetched model with {} statements in default model for resource {}",
                            result.getDefaultModel().size(), resourceURI);
        }
        return new DatasetResponseWithStatusCodeAndHeaders(result, statusCode, responseHeaders);
    }
}
