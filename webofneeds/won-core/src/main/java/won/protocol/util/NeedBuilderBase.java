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
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileUtils;
import org.apache.commons.lang3.Range;
import won.protocol.model.BasicNeedType;
import won.protocol.model.NeedState;
import won.protocol.vocabulary.WON;

import java.io.StringReader;
import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builder for an RDF representation of a need. The build() method returns the internal model instance.
 */
public abstract class NeedBuilderBase<T> implements NeedBuilder<T>
{
  private URI uriURI;
  private String uriString;
  private URI basicNeedTypeURI;
  private BasicNeedType basicNeedTypeBNT;
  private String basicNeedTypeURIString;
  private URI stateURI;
  private NeedState stateNS;
  private String stateURIString;
  private String title;
  private List<String> tags = new ArrayList<String>();
  private String description;
  private Range<Double> priceLimit;
  private String priceLimitString;
  private String currency;
  private List<Interval> intervals = new ArrayList<Interval>();
  private Date creationDate;
  private URI needProtocolEndpointURI;
  private String needProtocolEndpointString;
  private URI ownerProtocolEndpointURI;
  private String ownerProtocolEndpointString;
  private URI matcherProtocolEndpointURI;
  private String matcherProtocolEndpointString;
  private Float availableAtLocationLatitude;
  private Float availableAtLocationLongitude;
  private String availableAtLocationLatitudeString;
  private String availableAtLocationLongitudeString;
  private String availableAtLocationRegion;
  private Model contentDescription;

  //pattern for finding hashtags in title and description
  private static final Pattern PATTERN_HASHTAG = Pattern.compile("#\\w+");

  private static final String PRICE_SEPARATOR = "-";
  private static final String DATE_SEPARATOR = "/";

  protected class Interval{
    final Date from;
    final Date to;

    public Interval(final Date from, final Date to)
    {
      if (from == null) {
        if (to == null) throw new IllegalArgumentException("At least one date must be specified!");
        this.from = new Date(0);
        this.to = to;
      } else if (to == null) {
        this.from = from;
        this.to = new Date(Long.MAX_VALUE);
      } else if (from.after(to)){
        this.from = from;
        this.to = to;
      } else {
        this.to = from;
        this.from = to;
      }
    }
  }

  @Override
  public <O> void copyValuesToBuilder(final NeedBuilder<O> otherNeedBuilder)
  {
    for (Interval interval: this.intervals){
      otherNeedBuilder.addInterval(interval.from, interval.to);
    }
    otherNeedBuilder.setDescription(getDescription())
      .setPriceLimit(getPriceLimit());
    for (String tag: this.tags){
      otherNeedBuilder.addTag(tag);
    }
    if (getAvailableAtLocationRegion() != null){
      otherNeedBuilder.setAvailableAtLocation(getAvailableAtLocationRegion());
    } else {
      otherNeedBuilder.setAvailableAtLocation(getAvailableAtLocationLatitude(), getAvailableAtLocationLongitude());
    }
    otherNeedBuilder.setBasicNeedType(getBasicNeedTypeURI())
      .setContentDescription(getContentDescription())
      .setCreationDate(getCreationDate())
      .setCurrency(getCurrency())
      .setMatcherProtocolEndpoint(getMatcherProtocolEndpointURI())
      .setNeedProtocolEndpoint(getNeedProtocolEndpointURI())
      .setOwnerProtocolEndpoint(getOwnerProtocolEndpointURI())
      .setState(getStateURI())
      .setTitle(getTitle())
      .setUri(getURI());
  }

  private URI getURIforURI(String stringUri, URI uri) {
    if (uri != null) return uri;
    if (stringUri == null) return null;
    return URI.create(stringUri);
  }

  private String getStringForURI(String stringUri, URI uri) {
    if (stringUri != null) return stringUri;
    if (uri == null) return null;
    return uri.toString();
  }

  /**
   * Finds all #hashtags in the specified string. If none are found or the specified string is null,
   * an empty list is returned.
   * @param content
   * @return
   */
  protected List<String> getHashtags(String content) {
    List<String> ret = new ArrayList<String>();
    if (content == null) return ret;
    Matcher m = PATTERN_HASHTAG.matcher(content);
    while(m.find()){
      ret.add(m.group(0));
    }
    return ret;
  }

  protected URI getURI()
  {
    if (uriURI != null) return uriURI;
    if (uriString != null) return URI.create(uriString);
    return null;
  }

  protected URI getOwnerProtocolEndpointURI(){
    return getURIforURI(this.ownerProtocolEndpointString, this.ownerProtocolEndpointURI);
  }

  protected String getOwnerProtocolEndpointString() {
    return getStringForURI(this.ownerProtocolEndpointString, this.ownerProtocolEndpointURI);
  }

  protected URI getMatcherProtocolEndpointURI(){
    return getURIforURI(this.matcherProtocolEndpointString, this.matcherProtocolEndpointURI);
  }

  protected String getMatcherProtocolEndpointString() {
    return getStringForURI(this.matcherProtocolEndpointString, this.matcherProtocolEndpointURI);
  }

  protected URI getNeedProtocolEndpointURI(){
    return getURIforURI(this.needProtocolEndpointString, this.needProtocolEndpointURI);
  }

  protected String getNeedProtocolEndpointString() {
    return getStringForURI(this.needProtocolEndpointString, this.needProtocolEndpointURI);
  }

  protected String getNeedURIString(){
    return getStringForURI(this.uriString, this.uriURI);
  }

  protected URI getBasicNeedTypeURI(){
    if (this.basicNeedTypeURI != null) return this.basicNeedTypeURI;
    if (this.basicNeedTypeBNT != null) return URI.create(WON.toResource(this.basicNeedTypeBNT).getURI());
    if (this.basicNeedTypeURIString != null) return  URI.create(this.basicNeedTypeURIString);
    return null;
  }

  protected URI getStateURI(){
    if (this.stateURI != null) return this.stateURI;
    if (this.stateNS != null) return URI.create(WON.toResource(this.stateNS).getURI());
    if (this.stateURIString != null) return URI.create(this.stateURIString);
    return null;
  }

  protected String getTitle()
  {
    return title;
  }

  protected List<String> getTags()
  {
    return tags;
  }

  protected String getDescription()
  {
    return description;
  }

  protected Range<Double> getPriceLimit()
  {
    if (priceLimit != null) return priceLimit;
    if (priceLimitString != null) return parseDoubleInterval(priceLimitString, PRICE_SEPARATOR);
    return null;
  }

  public String getPriceLimitString()
  {
    if(priceLimitString != null) return priceLimitString;
    if(priceLimit != null) return priceLimit.getMinimum() + PRICE_SEPARATOR + priceLimit.getMaximum();
    return null;
  }

  protected Range<Double> parseDoubleInterval(String interval, String separator) {
    String[] parts = interval.split(separator);
    if(parts.length != 2)
      throw new IllegalArgumentException("There should be exactly two parts. Found " + parts.length);

    return Range.between(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]));
  }

  protected String getCurrency()
  {
    return currency;
  }

  protected List<Interval> getIntervals()
  {
    return intervals;
  }

  protected Date getCreationDate()
  {
    return creationDate;
  }

  protected Float getAvailableAtLocationLatitude()
  {
    if (availableAtLocationLatitude != null) return availableAtLocationLatitude;
    if (availableAtLocationLatitudeString != null) return Float.parseFloat(availableAtLocationLatitudeString);
    return null;
  }

  protected Float getAvailableAtLocationLongitude()
  {
    if (availableAtLocationLongitude != null) return availableAtLocationLongitude;
    if (availableAtLocationLongitudeString != null) return Float.parseFloat(availableAtLocationLongitudeString);
    return null;
  }

  protected String getAvailableAtLocationRegion()
  {
    return availableAtLocationRegion;
  }

  protected Model getContentDescription()
  {
    return contentDescription;
  }

  @Override
  public NeedBuilder<T> setUri(final URI uri)
  {
    this.uriString = null;
    this.uriURI = uri;
    return this;
  }

  @Override
  public NeedBuilder<T> setUri(final String uri)
  {
    this.uriString = uri;
    this.uriURI = null;
    return this;
  }

  @Override
  public NeedBuilder<T> setBasicNeedType(final URI type)
  {
    this.basicNeedTypeURI = type;
    this.basicNeedTypeBNT = null;
    this.basicNeedTypeURIString = null;
    return this;
  }

  @Override
  public NeedBuilder<T> setBasicNeedType(final BasicNeedType type)
  {
    this.basicNeedTypeBNT = type;
    this.basicNeedTypeURI = null;
    this.basicNeedTypeURIString = null;
    return this;
  }

  @Override
  public NeedBuilder<T> setBasicNeedType(final String basicNeedTypeUri)
  {
    this.basicNeedTypeBNT = null;
    this.basicNeedTypeURI = null;
    this.basicNeedTypeURIString = basicNeedTypeUri;
    return this;
  }

  @Override
  public NeedBuilder<T> setTitle(final String title)
  {
    this.title = title;
    return this;
  }

  @Override
  public NeedBuilder<T> addTag(final String tag)
  {
    this.tags.add(tag);
    return this;
  }

  @Override
  public NeedBuilder<T> setTags(final String[] tags)
  {
    for (int i = 0; i < tags.length; i++) {
      this.tags.add(tags[i]);
    }
    return this;
  }

  @Override
  public NeedBuilder<T> setDescription(final String description)
  {
    this.description = description;
    return this;
  }

  @Override
  public NeedBuilder<T> setPriceLimit(final Double from, final Double to)
  {
    priceLimit = Range.between(from,to);
    return this;
  }

  @Override
  public NeedBuilder<T> setPriceLimit(final Range<Double> priceLimit)
  {
    this.priceLimit = priceLimit;
    return this;
  }

  @Override
  public NeedBuilder<T> setPriceLimit(final String price)
  {
    this.priceLimitString = price;
    return this;
  }

  @Override
  public NeedBuilder<T> setCurrency(final String currency)
  {
    this.currency = currency;
    return this;
  }

  @Override
  public NeedBuilder<T> addInterval(final String interval)
  {
    return addInterval(parseDateInterval(interval, DATE_SEPARATOR));
  }

  protected Interval parseDateInterval(String interval, String separator) {
    String[] parts = interval.split(separator);
    if(parts.length != 2)
      throw new IllegalArgumentException("There should be exactly two parts. Found " + parts.length);

    Date from, to;
    try {
      from = SimpleDateFormat.getInstance().parse(parts[0]);
      to = SimpleDateFormat.getInstance().parse(parts[0]);
    } catch (ParseException e) {
      throw new IllegalArgumentException("The dates could not be parsed", e);
    }

    return new Interval(from, to);
  }

  @Override
  public NeedBuilder<T> addInterval(final Date from, final Date to)
  {
    this.intervals.add(new Interval(from,to));
    return this;
  }

  @Override
  public NeedBuilder<T> addInterval(final Long from, final Long to)
  {
    if (from == null && to == null) return this;
    Date fromDate = from != null ? new Date(from): null;
    Date toDate = to != null ? new Date(to): null;
    this.intervals.add(new Interval(fromDate,toDate));
    return this;
  }

  @Override
  public NeedBuilder<T> addInterval(final Interval interval)
  {
    if (interval != null) this.intervals.add(interval);
    return this;
  }

  @Override
  public NeedBuilder<T> addAvailableBefore(final Date date)
  {
    return addInterval(null, date);
  }

  @Override
  public NeedBuilder<T> addAvailableAfter(final Date date)
  {
    return addInterval(date, null);
  }

  @Override
  public NeedBuilder<T> setAvailableAtLocation(final Float latitude, final Float longitude)
  {
    this.availableAtLocationLatitude = latitude;
    this.availableAtLocationLongitude = longitude;
    this.availableAtLocationLatitudeString = null;
    this.availableAtLocationLongitudeString = null;
    return this;
  }

  @Override
  public NeedBuilder<T> setAvailableAtLocation(final String region)
  {
    this.availableAtLocationRegion = region;
    return this;
  }

  @Override
  public NeedBuilder<T> setAvailableAtLocation(final String latitude, final String longitude)
  {
    this.availableAtLocationLatitude = null;
    this.availableAtLocationLongitude = null;
    this.availableAtLocationLatitudeString = latitude;
    this.availableAtLocationLongitudeString = longitude;
    return this;
  }

  @Override
  public NeedBuilder<T> setCreationDate(final Date date)
  {
    this.creationDate = date;
    return this;
  }

  @Override
  public NeedBuilder<T> setNeedProtocolEndpoint(final URI endpoint)
  {
    this.needProtocolEndpointString = null;
    this.needProtocolEndpointURI = endpoint;
    return this;
  }

  @Override
  public NeedBuilder<T> setNeedProtocolEndpoint(final String endpoint)
  {
    this.needProtocolEndpointString = endpoint;
    this.needProtocolEndpointURI = null;
    return this;
  }

  @Override
  public NeedBuilder<T> setOwnerProtocolEndpoint(final URI endpoint)
  {
    this.ownerProtocolEndpointString = null;
    this.ownerProtocolEndpointURI = endpoint;
    return this;
  }

  @Override
  public NeedBuilder<T> setOwnerProtocolEndpoint(final String endpoint)
  {
    this.ownerProtocolEndpointString = endpoint;
    this.ownerProtocolEndpointURI = null;
    return this;
  }

  @Override
  public NeedBuilder<T> setMatcherProtocolEndpoint(final URI endpoint)
  {
    this.matcherProtocolEndpointString = null;
    this.matcherProtocolEndpointURI = endpoint;
    return this;
  }

  @Override
  public NeedBuilder<T> setMatcherProtocolEndpoint(final String endpoint)
  {
    this.matcherProtocolEndpointString = endpoint;
    this.matcherProtocolEndpointURI = null;
    return this;
  }

  @Override
  public NeedBuilder<T> setContentDescription(final Model content)
  {
    this.contentDescription = content;
    return this;
  }

  @Override
  public NeedBuilder<T> setContentDescription(final String content)
  {
    Model model = ModelFactory.createDefaultModel();
    String baseURI= "no:setUri";
    model.setNsPrefix("", baseURI);
    StringReader reader = new StringReader(content);
    model.read(reader, baseURI, FileUtils.langTurtle);
    this.contentDescription = model;
    return this;
  }

  @Override
  public NeedBuilder<T> setState(final URI state)
  {
    this.stateURI = state;
    this.stateNS = null;
    this.stateURIString = null;
    return this;
  }

  @Override
  public NeedBuilder<T> setState(final NeedState state)
  {
    this.stateURI= null;
    this.stateNS = state;
    this.stateURIString = null;
    return this;
  }

  @Override
  public NeedBuilder<T> setState(final String stateURI)
  {
    this.stateURI= null;
    this.stateNS = null;
    this.stateURIString = stateURI;
    return this;
  }
}

