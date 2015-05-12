package commons.service;

import com.hp.hpl.jena.query.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import won.protocol.rest.RdfDatasetConverter;

/**
 * Service to use HTTP to request resources
 *
 * User: hfriedrich
 * Date: 04.05.2015
 */
public class HttpRequestService
{
  private final Logger log = LoggerFactory.getLogger(getClass());
  private RestTemplate restTemplate;
  private HttpEntity entity;

  public HttpRequestService() {

    HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
    init(factory);
  }

  public HttpRequestService(int readTimeout, int connectionTimeout) {

    HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
    factory.setReadTimeout(readTimeout);
    factory.setConnectTimeout(connectionTimeout);
    init(factory);
  }

  private void init(ClientHttpRequestFactory factory) {

    HttpMessageConverter datasetConverter = new RdfDatasetConverter();
    restTemplate = new RestTemplate(factory);
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
}
