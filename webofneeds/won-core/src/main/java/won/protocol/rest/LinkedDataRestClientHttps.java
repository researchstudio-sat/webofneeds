/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package won.protocol.rest;

import java.net.URI;

import javax.annotation.PostConstruct;

import org.apache.http.ssl.TrustStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import won.cryptography.keymanagement.KeyPairAliasDerivationStrategy;
import won.cryptography.keymanagement.NeedUriAsAliasStrategy;
import won.cryptography.service.CryptographyUtils;
import won.cryptography.service.TrustStoreService;
import won.cryptography.service.keystore.KeyStoreService;
import won.cryptography.ssl.PredefinedAliasPrivateKeyStrategy;

/**
 * User: ypanchenko
 * Date: 07.10.15
 */
public class LinkedDataRestClientHttps extends LinkedDataRestClient {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private HttpMessageConverter datasetConverter;
    String acceptHeaderValue = null;

    private Integer readTimeout;
    private Integer connectionTimeout;

    private KeyStoreService keyStoreService;
    private TrustStoreService trustStoreService;
    private TrustStrategy trustStrategy;
    private KeyPairAliasDerivationStrategy keyPairAliasDerivationStrategy = new NeedUriAsAliasStrategy();


    public LinkedDataRestClientHttps(KeyStoreService keyStoreService, TrustStoreService trustStoreService, TrustStrategy trustStrategy, KeyPairAliasDerivationStrategy keyPairAliasDerivationStrategy) {
        this.readTimeout = 5000;
        this.connectionTimeout = 5000; //DEF. TIMEOUT IS 5 sec
        this.keyStoreService = keyStoreService;
        this.trustStoreService = trustStoreService;
        this.trustStrategy = trustStrategy;
        this.keyPairAliasDerivationStrategy = keyPairAliasDerivationStrategy;
    }

    @PostConstruct
    public void initialize() {
        datasetConverter = new RdfDatasetConverter();
        HttpHeaders headers = new HttpHeaders();
        this.acceptHeaderValue = MediaType.toString(datasetConverter.getSupportedMediaTypes());
    }

    private RestTemplate createRestTemplateForReadingLinkedData(String webID) {
        RestTemplate template = null;
        try {
            template = CryptographyUtils.createSslRestTemplate(
                    this.keyStoreService.getUnderlyingKeyStore(),
                    this.keyStoreService.getPassword(),
                    new PredefinedAliasPrivateKeyStrategy(keyPairAliasDerivationStrategy.getAliasForNeedUri(webID)),
                    this.trustStoreService.getUnderlyingKeyStore(),
                    this.trustStrategy,
                    readTimeout, connectionTimeout, true);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create rest template for webID '" + webID + "'", e);
        }
        template.getMessageConverters().add(datasetConverter);
        return template;
    }

    @Override
    public DatasetResponseWithStatusCodeAndHeaders readResourceDataWithHeaders(final URI resourceURI) {
        return readResourceDataWithHeaders(resourceURI, (URI) null);
    }

    @Override
    public DatasetResponseWithStatusCodeAndHeaders readResourceDataWithHeaders(URI resourceURI, final URI requesterWebID) {

        HttpMessageConverter datasetConverter = new RdfDatasetConverter();
        RestTemplate restTemplate;
        try {
            restTemplate = getRestTemplateForReadingLinkedData(requesterWebID == null ? null : requesterWebID.toString());
        } catch (Exception e) {
            logger.error("Failed to create ssl tofu rest template", e);
            throw new RuntimeException(e);
        }
        restTemplate.getMessageConverters().add(datasetConverter);
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add(HttpHeaders.ACCEPT, this.acceptHeaderValue);
        return super.readResourceData(resourceURI, restTemplate, requestHeaders);
    }

    @Override
    public DatasetResponseWithStatusCodeAndHeaders readResourceDataWithHeaders(
            final URI resourceURI, final URI requesterWebID, final HttpHeaders requestHeaders) {
        requestHeaders.add(HttpHeaders.ACCEPT, this.acceptHeaderValue);
        return super.readResourceData(resourceURI, getRestTemplateForReadingLinkedData(requesterWebID == null ? null : requesterWebID.toString()),
                requestHeaders);
    }


    private RestTemplate getRestTemplateForReadingLinkedData(String webID) {
        return createRestTemplateForReadingLinkedData(webID);
    }

    @Override
    public DatasetResponseWithStatusCodeAndHeaders readResourceDataWithHeaders(final URI resourceURI, HttpHeaders requestHeaders) {
    	return readResourceDataWithHeaders(resourceURI, null, requestHeaders);
    }

    public void setReadTimeout(final Integer readTimeout) {
        this.readTimeout = readTimeout;
    }

    public void setConnectionTimeout(final Integer connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }
}
