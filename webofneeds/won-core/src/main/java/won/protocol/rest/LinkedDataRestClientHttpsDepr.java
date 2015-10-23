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

import com.hp.hpl.jena.query.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import won.cryptography.service.CryptographyUtils;
import won.cryptography.depr.WonKeyMaterialDepr;
import won.cryptography.depr.WonTrustMaterialDepr;

import javax.annotation.PostConstruct;
import java.net.URI;


/**
 * User: ypanchenko
 * Date: 28.09.2015
 */
public class LinkedDataRestClientHttpsDepr extends LinkedDataRestClient
{

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private RestTemplate restTemplate;
  private HttpEntity entity;

  private Integer readTimeout;
  private Integer connectionTimeout;

  private WonKeyMaterialDepr wonKeyMaterial;
  private WonTrustMaterialDepr wonTrustMaterial;


  public LinkedDataRestClientHttpsDepr() {
    this(10000,10000); //DEF. TIMEOUT IS 10sec
  }

  public LinkedDataRestClientHttpsDepr(int readTimeout, int connectionTimeout) {
    this.readTimeout = readTimeout;
    this.connectionTimeout = connectionTimeout;
  }

  @PostConstruct
  public void initialize() {
    HttpMessageConverter datasetConverter = new RdfDatasetConverter();
    try {
      //TODO password from properties
      restTemplate = CryptographyUtils.createSslRestTemplate(wonKeyMaterial, wonTrustMaterial, readTimeout,
                                                             connectionTimeout);
    } catch (Exception e) {
      logger.error("Failed to create ssl tofu rest template", e);
      throw new RuntimeException(e);
    }
    restTemplate.getMessageConverters().add(datasetConverter);
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(datasetConverter.getSupportedMediaTypes());
    entity = new HttpEntity(headers);
  }

  public void setWonKeyMaterial(final WonKeyMaterialDepr wonKeyMaterial) {
    this.wonKeyMaterial = wonKeyMaterial;
  }

  public void setWonTrustMaterial(final WonTrustMaterialDepr wonTrustMaterial) {
    this.wonTrustMaterial = wonTrustMaterial;
  }



//  /**
//   * Request RDF resource and return as dataset
//   *
//   * @param uri request resource for uri
//   * @return dataset object that represents resource
//   * @throws RestClientException
//   */
//  public Dataset requestDataset(String uri) throws RestClientException {
//
//    ResponseEntity<Dataset> response = null;
//    log.debug("Request URI: {}", uri);
//    response = restTemplate.exchange(uri, HttpMethod.GET, entity, Dataset.class);
//
//    if (response.getStatusCode() != HttpStatus.OK) {
//      log.warn("HTTP GET request returned status code: {}", response.getStatusCode());
//      throw new HttpClientErrorException(response.getStatusCode());
//    }
//
//    return response.getBody();
//  }





//  public LinkedDataRestClientHttps(int connectTimeout, int readTimeout) {
//    HttpMessageConverter datasetConverter = new RdfDatasetConverter();
//
//
//    HttpClient httpClient = null;
//    try {
//      SSLContext sslContext = new SSLContextBuilder()
//        .loadTrustMaterial(null, new TrustSelfSignedStrategy())
//        .build();
//      SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContext);
//      httpClient = HttpClients.custom()//.useSystemProperties()
//        .setSSLSocketFactory
//          (sslConnectionSocketFactory)
//        .build();
//
//    } catch (Exception e) {
//      throw new RuntimeException("Failed to initialize SSLContect for accessing linked data");
//    }
//
//    HttpComponentsClientHttpRequestFactory customRequestFactory = new HttpComponentsClientHttpRequestFactory();
//    customRequestFactory.setHttpClient(httpClient);
//
//    restTemplate = new RestTemplate(customRequestFactory);
//    ClientHttpRequestFactory requestFactory = restTemplate.getRequestFactory();
//
//    if (requestFactory instanceof SimpleClientHttpRequestFactory) {
//      if(connectTimeout>0){((SimpleClientHttpRequestFactory) requestFactory).setConnectTimeout(connectTimeout);}
//      if(readTimeout>0){((SimpleClientHttpRequestFactory) requestFactory).setReadTimeout(readTimeout);}
//    } else if (requestFactory instanceof HttpComponentsClientHttpRequestFactory) {
//      if(connectTimeout>0){((HttpComponentsClientHttpRequestFactory) requestFactory).setConnectTimeout(connectTimeout);}
//      if(readTimeout>0){((HttpComponentsClientHttpRequestFactory) requestFactory).setReadTimeout(readTimeout);}
//    }
//
//    restTemplate.getMessageConverters().add(datasetConverter);
//
//    HttpHeaders headers = new HttpHeaders();
//    headers.setAccept(datasetConverter.getSupportedMediaTypes());
//
//    entity = new HttpEntity(headers);
//  }

//  /**
//   * Retrieves RDF for the specified resource URI.
//   * Expects that the resource URI will lead to a 303 response, redirecting to the URI where RDF can be downloaded.
//   * Paging is not supported.
//   *
//   * @param resourceURI
//   * @return
//   */
//  public Dataset readResourceDataDefaultTrust(URI resourceURI){
//    assert resourceURI != null : "resource URI must not be null";
//    logger.debug("fetching linked data resource: {}", resourceURI);
//
//    //If a RestClientException is thrown here complaining that it can't read a Model with MIME media type text/html,
//    //it was probably the wrong resourceURI
//    Dataset result;
//    try {
//        ResponseEntity<Dataset> response = restTemplate.exchange(resourceURI, HttpMethod.GET, entity, Dataset.class);
//        //RestTemplate will automatically follow redirects on HttpGet calls
//
//        if(response.getStatusCode()!=HttpStatus.OK){
//            throw new HttpClientErrorException(response.getStatusCode());
//        }
//        result = response.getBody();
//    } catch (RestClientException e) {
//      if(e instanceof HttpClientErrorException){
//          throw e;
//      }
//      throw new IllegalArgumentException(
//        MessageFormat.format(
//        "caught a clientHandler exception, " +
//        "which may indicate that the URI that was accessed isn''t a" +
//        " linked data URI, please check {0}", resourceURI), e);
//    }
//    if (logger.isDebugEnabled()) {
//      logger.debug("fetched model with {} statements in default model for resource {}",result.getDefaultModel().size(),
//        resourceURI);
//    }
//    return result;
//  }


  /**
   * Retrieves RDF for the specified resource URI.
   * Expects that the resource URI will lead to a 303 response, redirecting to the URI where RDF can be downloaded.
   * Paging is not supported.
   *
   * @param resourceURI
   * @return
   */
  @Override
  public Dataset readResourceData(URI resourceURI) {
    return super.readResourceData(resourceURI, restTemplate, entity);
  }


  @Override
  public Dataset readResourceData(final URI resourceURI, final URI requesterWebID) {
    logger.warn("Requester specific Data retrieval not supported - WebID is ignored");
    return readResourceData(resourceURI);
  }

}
