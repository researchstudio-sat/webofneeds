package common.service;

import com.hp.hpl.jena.query.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import won.cryptography.service.CryptographyUtils;
import won.cryptography.service.KeyStoreService;
import won.cryptography.service.TrustStoreService;
import won.protocol.rest.RdfDatasetConverter;

import javax.annotation.PostConstruct;

/**
 * Service to use HTTP to request resources
 *
 * User: hfriedrich
 * Date: 04.05.2015
 */
@Component
@DependsOn({"keyStoreService", "trustStoreService"})
public class HttpsRequestService
{
  private final Logger log = LoggerFactory.getLogger(getClass());
  private RestTemplate restTemplate;
  private HttpEntity entity;

  private Integer readTimeout;
  private Integer connectionTimeout;

  @Autowired
  private KeyStoreService keyStoreService;
  @Autowired
  private TrustStoreService trustStoreService;

  public HttpsRequestService() {
  }

  public HttpsRequestService(int readTimeout, int connectionTimeout) {
    this.readTimeout = readTimeout;
    this.connectionTimeout = connectionTimeout;
  }

  @PostConstruct
  public void initialize() {
    HttpMessageConverter datasetConverter = new RdfDatasetConverter();
    try {
      //TODO password from properties
      restTemplate = CryptographyUtils.createSslTofuRestTemplate(keyStoreService, "temp",
                                                                              trustStoreService, readTimeout, connectionTimeout);
    } catch (Exception e) {
      log.error("Failed to create ssl tofu rest template", e);
      throw new RuntimeException(e);
    }
    restTemplate.getMessageConverters().add(datasetConverter);
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(datasetConverter.getSupportedMediaTypes());
    entity = new HttpEntity(headers);
  }

  /**
   * Request RDF resource and return as dataset
   *
   * @param uri request resource for uri
   * @return dataset object that represents resource
   * @throws RestClientException
   */
  public Dataset requestDataset(String uri) throws RestClientException {

    ResponseEntity<Dataset> response = null;
    log.debug("Request URI: {}", uri);
    response = restTemplate.exchange(uri, HttpMethod.GET, entity, Dataset.class);

    if (response.getStatusCode() != HttpStatus.OK) {
      log.warn("HTTP GET request returned status code: {}", response.getStatusCode());
      throw new HttpClientErrorException(response.getStatusCode());
    }

    return response.getBody();
  }

//  public KeyStoreService getKeyStoreService() {
//    return keyStoreService;
//  }
//
//  public void setKeyStoreService(final KeyStoreService keyStoreService) {
//    this.keyStoreService = keyStoreService;
//  }
//
//  public TrustStoreService getTrustStoreService() {
//    return trustStoreService;
//  }
//
//  public void setTrustStoreService(final TrustStoreService trustStoreService) {
//    this.trustStoreService = trustStoreService;
//  }
}
