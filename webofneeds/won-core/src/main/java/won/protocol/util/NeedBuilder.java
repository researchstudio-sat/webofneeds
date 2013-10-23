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

package won.protocol.util;

import com.hp.hpl.jena.rdf.model.Model;
import org.apache.commons.lang3.Range;
import won.protocol.model.BasicNeedType;
import won.protocol.model.NeedState;

import java.net.URI;
import java.util.Date;

/**
 * User: fkleedorfer
 * Date: 18.09.13
 */
public interface NeedBuilder<T>
{
  public T build();

  public <O> void copyValuesToBuilder(NeedBuilder<O> otherNeedBuilder);
  public void copyValuesFromProduct(T product);

  public NeedBuilder<T> setUri(URI uri);
  public NeedBuilder<T> setUri(String uri);

  public NeedBuilder<T> setBasicNeedType(URI type);
  public NeedBuilder<T> setBasicNeedType(BasicNeedType type);
  public NeedBuilder<T> setBasicNeedType(String basicNeedTypeUri);

  public NeedBuilder<T> setTitle(String title);
  public NeedBuilder<T> addTag(String tag);
  public NeedBuilder<T> setTags(String[] tags);
  public NeedBuilder<T> setDescription(String description);

  public NeedBuilder<T> setPriceLimit(String price);
  public NeedBuilder<T> setPriceLimit(Double from, Double to);
  public NeedBuilder<T> setPriceLimit(Range<Double> price);
  public NeedBuilder<T> setCurrency(String currency);

  public NeedBuilder<T> addInterval(String interval);
  public NeedBuilder<T> addInterval(Date from, Date to);
  public NeedBuilder<T> addInterval(Long from, Long to);
  public NeedBuilder<T> addInterval(NeedBuilderBase<T>.Interval interval);

  public NeedBuilder<T> addAvailableBefore(Date date);
  public NeedBuilder<T> addAvailableAfter(Date date);

  public NeedBuilder<T> setAvailableAtLocation(Float latitude, Float longitude);
  public NeedBuilder<T> setAvailableAtLocation(String latitude, String longitude);
  public NeedBuilder<T> setAvailableAtLocation(String region);

  public NeedBuilder<T> setCreationDate(Date date);

  public NeedBuilder<T> setNeedProtocolEndpoint(URI endpoint);
  public NeedBuilder<T> setNeedProtocolEndpoint(String endpoint);
  public NeedBuilder<T> setOwnerProtocolEndpoint(URI endpoint);
  public NeedBuilder<T> setMatcherProtocolEndpoint(URI endpoint);
  public NeedBuilder<T> setOwnerProtocolEndpoint(String endpoint);
  public NeedBuilder<T> setMatcherProtocolEndpoint(String endpoint);

  public NeedBuilder<T> setContentDescription(Model content);

  public NeedBuilder<T> setState(URI state);
  public NeedBuilder<T> setState(NeedState state);
  public NeedBuilder<T> setState(String stateURI);

  /**
   * Set RDF content in Turtle format.
   * @param content
   * @return
   */
  public NeedBuilder<T> setContentDescription(String content);


}
