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

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.RDF;
import won.protocol.vocabulary.GEO;
import won.protocol.vocabulary.WON;

import java.net.URI;
import java.util.Date;

/**
 * User: fkleedorfer
 * Date: 18.09.13
 */
public class NeedModelBuilder extends NeedBuilderBase<Model>
{


  @Override
  public Model build()
  {
    return buildNeedModel();
  }

  @Override
  public void copyValuesFromProduct(final Model needModel)
  {
    Resource needResource = identifyNeedResource(needModel);
    this.setUri(needResource.getURI());
    Resource basicNeedTypeResource = needResource.getPropertyResourceValue(WON.HAS_BASIC_NEED_TYPE);
    if (basicNeedTypeResource != null) this.setBasicNeedType(basicNeedTypeResource.getURI().toString());
    Statement creationDateStmt = needResource.getProperty(WON.NEED_CREATION_DATE);
    if (creationDateStmt != null){
      this.setCreationDate(DateTimeUtils.toDate(creationDateStmt.getObject(), needModel));
    }
    Resource ownerProtocolEndpoint= needResource.getPropertyResourceValue(WON.OWNER_PROTOCOL_ENDPOINT);
    if (ownerProtocolEndpoint != null){
      this.setOwnerProtocolEndpoint(ownerProtocolEndpoint.getURI());
    }
    Resource matcherProtocolEndpoint = needResource.getPropertyResourceValue(WON.MATCHER_PROTOCOL_ENDPOINT);
    if (matcherProtocolEndpoint != null){
      this.setMatcherProtocolEndpoint(matcherProtocolEndpoint.getURI());
    }
    Resource needProtocolEndpoint = needResource.getPropertyResourceValue(WON.NEED_PROTOCOL_ENDPOINT);
    if (needProtocolEndpoint != null){
      this.setNeedProtocolEndpoint(needProtocolEndpoint.getURI());
    }
    Resource needState = needResource.getPropertyResourceValue(WON.IS_IN_STATE);
    if (needState != null){
      this.setState(needState.getURI());
    }
    Resource needContent = needResource.getPropertyResourceValue(WON.HAS_CONTENT);
    copyValuesFromNeedContent(needContent);
    Resource needModality = needResource.getPropertyResourceValue(WON.HAS_NEED_MODALITY);
    copyValuesFromNeedModality(needModality);


  }

  private void copyValuesFromNeedModality(final Resource needModality)
  {
    StmtIterator it = needModality.listProperties(WON.HAS_TIME_SPECIFICATION);
    while (it.hasNext()){
      Statement stmt = it.next();
      RDFNode timeSpec = stmt.getObject();
      if (timeSpec.isResource()){
        this.addInterval(toInterval((Resource)timeSpec));
      }
    }
    Resource locationResource = needModality.getPropertyResourceValue(WON.AVAILABLE_AT_LOCATION);
    if (locationResource != null) {
      Float latitude = extractFloatFromObject(locationResource.getProperty(GEO.LATITUDE));
      Float longitude = extractFloatFromObject(locationResource.getProperty(GEO.LONGITUDE));
      if (latitude != null && longitude != null) {
        setAvailableAtLocation(latitude, longitude);
      }
      setAvailableAtLocation(extractStringFromObject(locationResource.getProperty(WON.HAS_ISO_CODE)));
    }
    Resource priceSpec = needModality.getPropertyResourceValue(WON.HAS_PRICE_SPECIFICATION);
    if (priceSpec != null) {
      setUpperPriceLimit(extractFloatFromObject(priceSpec.getProperty(WON.HAS_UPPER_PRICE_LIMIT)));
      setLowerPriceLimit(extractFloatFromObject(priceSpec.getProperty(WON.HAS_LOWER_PRICE_LIMIT)));
      setCurrency(extractStringFromObject(priceSpec.getProperty(WON.HAS_CURRENCY)));
    }
  }



  private Interval toInterval(final Resource timeSpec)
  {
    Statement fromStmt = timeSpec.getProperty(WON.HAS_START_TIME);
    Date fromDate = extractDateFromObject(fromStmt);
    Statement toStmt = timeSpec.getProperty(WON.HAS_END_TIME);
    Date toDate = extractDateFromObject(toStmt);
    if (toDate != null && fromDate != null) return new Interval(fromDate, toDate);
    return null;
  }

  private Date extractDateFromObject(final Statement fromStmt)
  {
    if (fromStmt != null) {
      RDFNode obj = fromStmt.getObject();
      if (obj.isLiteral()){
        return DateTimeUtils.toDate(obj.asLiteral(), fromStmt.getModel());
      }
    }
    return null;
  }

  private Float extractFloatFromObject(final Statement stmt)
  {
    if (stmt == null) return null;
    RDFNode obj = stmt.getObject();
    if (!obj.isLiteral()) return null;
    return stmt.getFloat();
  }

  private String extractStringFromObject(final Statement stmt)
  {
    if (stmt == null) return null;
    RDFNode obj = stmt.getObject();
    if (!obj.isLiteral()) return null;
    return stmt.getString();
  }

  private void copyValuesFromNeedContent(final Resource needContent)
  {
    Resource contentDescription = needContent.getPropertyResourceValue(WON.HAS_CONTENT_DESCRIPTION);
    if (contentDescription != null) {
      Graph g = contentDescription.getModel().getGraph();
      GraphExtract extract = new GraphExtract(TripleBoundary.stopNowhere);
      Graph contentDescriptionGraph = extract.extract(contentDescription.asNode(), g);
      //now replace the contentDescription node with the default URI of the extracted graph
      contentDescriptionGraph.getPrefixMapping().setNsPrefix("","no:uri"); //it really doesn't matter what we set here
      String prefixURI = contentDescriptionGraph.getPrefixMapping().getNsPrefixURI("");
      Node prefixNode = Node.createURI(prefixURI);
      ExtendedIterator<Triple> it = contentDescriptionGraph.find(contentDescription.asNode(), Node.ANY, Node.ANY);
      while (it.hasNext()) {
        Triple triple = it.next();
        it.remove();
        contentDescriptionGraph.add(new Triple(prefixNode,triple.getPredicate(), triple.getObject()));
      }
      Model contentDescriptionModel = ModelFactory.createModelForGraph(contentDescriptionGraph);
      contentDescriptionModel.getRDFNode(contentDescription.asNode());
      this.setContentDescription(contentDescriptionModel);
    }
    Statement textDescriptionStmt = needContent.getProperty(WON.HAS_TEXT_DESCRIPTION);
    if (textDescriptionStmt != null){
      RDFNode textDescriptionNode = textDescriptionStmt.getObject();
      if (textDescriptionNode.isLiteral()) {
        this.setDescription(textDescriptionNode.asLiteral().getString());
      }
    }
    Statement titleStmt = needContent.getProperty(DC.title);
    if (titleStmt != null){
      RDFNode titleNode = titleStmt.getObject();
      if (titleNode.isLiteral()) {
        this.setTitle(titleNode.asLiteral().getString());
      }
    }
    StmtIterator it = needContent.listProperties(WON.HAS_TAG);
    while(it.hasNext()) {
      Statement stmt = it.next();
      RDFNode obj = stmt.getObject();
      if (obj.isLiteral()) {
        this.addTag(obj.asLiteral().getString());
      }
    }

  }

  private Resource identifyNeedResource(final Model needModel)
  {
    Resource needResource = null;
    //try fetching the base URI resource. If that is a Need, we'll assume we found the need resource
    String baseUri = needModel.getNsPrefixURI("");
    if (baseUri != null){
      //fetch the resource, check if it has the rdf:type won:Need
      needResource = needModel.getResource(baseUri);
      if (! needResource.hasProperty(RDF.type, WON.NEED)){ needResource = null; }
    }
    if (needResource != null) return needResource;
    //found no need resource yet. Try to find it by type. We expect to find exactly one, otherwise we report an error
    ResIterator it = needModel.listSubjectsWithProperty(RDF.type, WON.NEED);
    if (it.hasNext()) needResource = it.next();
    if (it.hasNext()) throw new IllegalArgumentException("expecting only one resource of type won:Need in specified model");
    if (needResource == null) throw new IllegalArgumentException("expected to find a resource of type won:Need in specified model");
    return needResource;
  }

  private void addResourceIfPresent(Resource subject, Property predicate, URI object){
    if (object == null) return;
    subject.addProperty(predicate, subject.getModel().createResource(object.toString()));
  }

  private Model buildNeedModel()
  {
    Model needModel = ModelFactory.createDefaultModel();
    if (getURI() != null) {
      needModel.setNsPrefix("",getURI().toString());
    }
    Resource needResource = needModel.createResource(getNeedURIString(), WON.NEED);
    // need type
    addResourceIfPresent(needResource, WON.HAS_BASIC_NEED_TYPE, getBasicNeedTypeURI());
    addResourceIfPresent(needResource, WON.NEED_PROTOCOL_ENDPOINT, getNeedProtocolEndpointURI());
    addResourceIfPresent(needResource, WON.OWNER_PROTOCOL_ENDPOINT, getOwnerProtocolEndpointURI());
    addResourceIfPresent(needResource, WON.MATCHER_PROTOCOL_ENDPOINT, getMatcherProtocolEndpointURI());
    addResourceIfPresent(needResource, WON.IS_IN_STATE, getStateURI());
    addLiteralValueIfPresent(needModel,needResource, WON.NEED_CREATION_DATE, DateTimeUtils.toLiteral(getCreationDate(), needModel));
    // need content
    addNeedContent(needModel, needResource);
    // need modalities
    addNeedModality(needModel, needResource);
    return needModel;
  }

  private void addLiteralValueIfPresent(final Model needModel, final Resource subject, final Property property, final Literal literal)
  {
    if (literal != null) {
      subject.addProperty(property,literal);
    }
  }

  private void addNeedModality(final Model needModel, final Resource needResource)
  {
    Resource needModality = needModel.createResource(WON.NEED_MODALITY);

    // TODO: store need modalities in separate objects to enable easier checking and multiple instances
    //price and currency
    if (getUpperPriceLimit() != null || getLowerPriceLimit() != null) {
      Resource priceSpecification = needModel.createResource(WON.PRICE_SPECIFICATION);
      addLiteralValueIfPresent(needModel, priceSpecification, WON.HAS_LOWER_PRICE_LIMIT, getLowerPriceLimit(), XSDDatatype.XSDfloat);
      addLiteralValueIfPresent(needModel, priceSpecification, WON.HAS_UPPER_PRICE_LIMIT, getUpperPriceLimit(), XSDDatatype.XSDfloat);
      addLiteralValueIfPresent(needModel, priceSpecification, WON.HAS_CURRENCY, getCurrency(), XSDDatatype.XSDstring);
      needModel.add(needModel.createStatement(needModality, WON.HAS_PRICE_SPECIFICATION, priceSpecification));
    }

    if (getAvailableAtLocationLatitude() != null && getAvailableAtLocationLongitude() != null) {
      Resource location = needModel.createResource(GEO.POINT)
          .addProperty(GEO.LATITUDE, getAvailableAtLocationLatitude().toString(), XSDDatatype.XSDfloat)
          .addProperty(GEO.LONGITUDE, getAvailableAtLocationLongitude().toString(), XSDDatatype.XSDfloat);
      needModel.add(needModel.createStatement(needModality, WON.AVAILABLE_AT_LOCATION, location));
    }

    // time constraint - in a simplified manner: no recurrence!
    if (!getIntervals().isEmpty()) {
      for (Interval interval : getIntervals()) {
        Resource timeConstraint = needModel.createResource(WON.TIME_SPECIFICATION);
        addLiteralValueIfPresent(needModel, timeConstraint, WON.HAS_START_TIME, interval.from, XSDDatatype.XSDdateTime);
        addLiteralValueIfPresent(needModel, timeConstraint, WON.HAS_END_TIME, interval.to, XSDDatatype.XSDdateTime);
        needModel.add(needModel.createStatement(needModality, WON.HAS_TIME_SPECIFICATION, timeConstraint));
      }
    }

    needModel.add(needModel.createStatement(needResource, WON.HAS_NEED_MODALITY, needModality));
  }

  private void addNeedContent(final Model needModel, final Resource needResource)
  {
    Resource needContent = needModel.createResource(WON.NEED_CONTENT);
    needModel.add(needModel.createStatement(needResource, WON.HAS_CONTENT, needContent));
    addLiteralValueIfPresent(needModel, needContent, DC.title, getTitle(), XSDDatatype.XSDstring);
    addLiteralValueIfPresent(needModel, needContent, WON.HAS_TEXT_DESCRIPTION, getDescription(), XSDDatatype.XSDstring);
    if (!getTags().isEmpty()) {
      for (String tag : getTags()) {
        addLiteralValueIfPresent(needModel, needContent, WON.HAS_TAG, tag.trim(), XSDDatatype.XSDstring);
      }
    }
    if (getContentDescription() != null) {
      Model contentDescriptionModel = getContentDescription();
      Resource linkingBlankNode = needModel.createResource();
      RdfUtils.replaceBaseURI(contentDescriptionModel, linkingBlankNode);
      needContent.addProperty(WON.HAS_CONTENT_DESCRIPTION, linkingBlankNode);
      needModel.add(contentDescriptionModel);
    }
  }

  private void addLiteralValueIfPresent(Model model, Resource subject, Property predicate, Object object, XSDDatatype type)
  {
    if (object == null) return;
    String stringValue = object.toString();
    if (stringValue == null || stringValue.length() == 0) return;
    if (type == null) {
      subject.addProperty(predicate, stringValue);
    } else {
      subject.addProperty(predicate, stringValue, type);
    }
  }

}
