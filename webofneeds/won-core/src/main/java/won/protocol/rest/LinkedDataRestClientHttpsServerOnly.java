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
import org.apache.http.conn.ssl.TrustStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import won.cryptography.service.CryptographyUtils;

import javax.annotation.PostConstruct;
import java.net.URI;

/**
 * User: ypanchenko
 * Date: 07.10.15
 */
public class LinkedDataRestClientHttpsServerOnly extends LinkedDataRestClient
{

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private RestTemplate restTemplate;
  private HttpEntity entity;
  private HttpMessageConverter datasetConverter;

  private Integer readTimeout;
  private Integer connectionTimeout;

  private TrustStrategy trustStrategy;



  public LinkedDataRestClientHttpsServerOnly(TrustStrategy trustStrategy) {
    this.readTimeout = 10000;
    this.connectionTimeout = 10000; //DEF. TIMEOUT IS 10sec
    this.trustStrategy = trustStrategy;
  }

  @PostConstruct
  public void initialize() {
    datasetConverter = new RdfDatasetConverter();
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(datasetConverter.getSupportedMediaTypes());
    entity = new HttpEntity(headers);

    try {
      restTemplate = createRestTemplateForReadingLinkedData();
    } catch (Exception e) {
      logger.error("Failed to create ssl rest template", e);
      throw new RuntimeException(e);
    }
  }

  private RestTemplate createRestTemplateForReadingLinkedData() throws Exception {
    RestTemplate template = CryptographyUtils.createSslRestTemplate(
      this.trustStrategy,
      readTimeout, connectionTimeout);
    template.getMessageConverters().add(datasetConverter);
    return template;
  }

  @Override
  public Dataset readResourceData(URI resourceURI, final URI requesterWebID) {
    return super.readResourceData(resourceURI, restTemplate, entity);
  }


  @Override
  public Dataset readResourceData(final URI resourceURI) {
    return super.readResourceData(resourceURI, restTemplate, entity);
  }

  public void setReadTimeout(final Integer readTimeout) {
    this.readTimeout = readTimeout;
  }

  public void setConnectionTimeout(final Integer connectionTimeout) {
    this.connectionTimeout = connectionTimeout;
  }
}
