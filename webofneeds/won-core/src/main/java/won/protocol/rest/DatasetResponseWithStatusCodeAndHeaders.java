/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package won.protocol.rest;

import org.apache.jena.query.Dataset;
import org.springframework.http.HttpHeaders;

/**
 * Simple structure to hold a dataset and the response headers that were sent
 * along with the dataset.
 */
public class DatasetResponseWithStatusCodeAndHeaders {
  private Dataset dataset;
  private int statusCode;
  private HttpHeaders responseHeaders;

  public DatasetResponseWithStatusCodeAndHeaders(final Dataset dataset, final int statusCode,
      final HttpHeaders responseHeaders) {
    this.dataset = dataset;
    this.statusCode = statusCode;
    this.responseHeaders = responseHeaders;
  }

  public Dataset getDataset() {
    return dataset;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public HttpHeaders getResponseHeaders() {
    return responseHeaders;
  }
}
