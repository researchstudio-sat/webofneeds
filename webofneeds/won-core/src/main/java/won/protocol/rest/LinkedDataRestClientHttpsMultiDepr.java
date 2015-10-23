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
 * Date: 07.10.15
 */
public class LinkedDataRestClientHttpsMultiDepr extends LinkedDataRestClient
{
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private RestTemplate defaultRestTemplate;
  private HttpEntity entity;

  private Integer readTimeout;
  private Integer connectionTimeout;

  private WonKeyMaterialDepr wonKeyMaterial;
  private WonTrustMaterialDepr wonTrustMaterial;



  public LinkedDataRestClientHttpsMultiDepr() {
      this(10000,10000); //DEF. TIMEOUT IS 10sec
  }


  public LinkedDataRestClientHttpsMultiDepr(int connectionTimeout, int readTimeout) {
    this.readTimeout = readTimeout;
    this.connectionTimeout = connectionTimeout;
  }

  @PostConstruct
  public void initialize() {
    HttpMessageConverter datasetConverter = new RdfDatasetConverter();
    try {
      defaultRestTemplate = CryptographyUtils.createSslRestTemplate(wonKeyMaterial, wonTrustMaterial, readTimeout,
                                                             connectionTimeout);
    } catch (Exception e) {
      logger.error("Failed to create ssl tofu rest template", e);
      throw new RuntimeException(e);
    }
    defaultRestTemplate.getMessageConverters().add(datasetConverter);
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


  @Override
  public Dataset readResourceData(URI resourceURI, final URI requesterWebID) {

    HttpMessageConverter datasetConverter = new RdfDatasetConverter();
    RestTemplate restTemplate;
    try {
      restTemplate = CryptographyUtils.createSslRestTemplate(wonKeyMaterial, requesterWebID.toString(),
                                                              wonTrustMaterial,
                                                             readTimeout,
                                                             connectionTimeout);
    } catch (Exception e) {
      logger.error("Failed to create ssl tofu rest template", e);
      throw new RuntimeException(e);
    }
    restTemplate.getMessageConverters().add(datasetConverter);

    return super.readResourceData(resourceURI, restTemplate, entity);
  }

  @Override
  public Dataset readResourceData(final URI resourceURI) {
    return super.readResourceData(resourceURI, defaultRestTemplate, entity);
  }



}
