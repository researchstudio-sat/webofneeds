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
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

/**
 * User: fkleedorfer
 * Date: 28.11.12
 */
public class LinkedDataRestClientHttp extends LinkedDataRestClient
{
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private RestTemplate restTemplate;
  private HttpEntity entity;


  public LinkedDataRestClientHttp() {
      this(10000,10000); //DEF. TIMEOUT IS 10sec
  }

  public LinkedDataRestClientHttp(int connectTimeout, int readTimeout) {
      HttpMessageConverter datasetConverter = new RdfDatasetConverter();

      restTemplate = new RestTemplate();
      ClientHttpRequestFactory requestFactory = restTemplate.getRequestFactory();

      if (requestFactory instanceof SimpleClientHttpRequestFactory) {
          if(connectTimeout>0){((SimpleClientHttpRequestFactory) requestFactory).setConnectTimeout(connectTimeout);}
          if(readTimeout>0){((SimpleClientHttpRequestFactory) requestFactory).setReadTimeout(readTimeout);}
      } else if (requestFactory instanceof HttpComponentsClientHttpRequestFactory) {
          if(connectTimeout>0){((HttpComponentsClientHttpRequestFactory) requestFactory).setConnectTimeout(connectTimeout);}
          if(readTimeout>0){((HttpComponentsClientHttpRequestFactory) requestFactory ).setReadTimeout(readTimeout);}
      }

      restTemplate.getMessageConverters().add(datasetConverter);

      HttpHeaders headers = new HttpHeaders();
      headers.setAccept(datasetConverter.getSupportedMediaTypes());

      entity = new HttpEntity(headers);
  }


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
    logger.warn("Requester specific Data retrieval not supported - requesterWebID is ignored");
    return readResourceData(resourceURI);
  }


}
