package won.owner.linkeddata;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.DC;
import won.owner.pojo.NeedPojo;
import won.protocol.util.DefaultPrefixUtils;
import won.protocol.util.Interval;
import won.protocol.util.NeedBuilderBase;
import won.protocol.util.RdfUtils;
import won.protocol.vocabulary.GEO;
import won.protocol.vocabulary.WON;

import java.net.URI;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

/**
 * User: LEIH-NB
 * Date: 07.07.14
 */
public class NeedPojoNeedModelBuilder extends NeedBuilderBase<Model>
{

  public NeedPojoNeedModelBuilder(NeedPojo needPojo){
    if (!needPojo.getTitle().isEmpty())
      this.setTitle(needPojo.getTitle());
    if (needPojo.getContentDescription() != null && !needPojo.getContentDescription().isEmpty())
      this.setContentDescription(needPojo.getContentDescription());
    if (needPojo.getState()!=null)
      this.setState(needPojo.getState());
    if (needPojo.getBasicNeedType()!=null)
      this.setBasicNeedType(needPojo.getBasicNeedType());
    if (needPojo.getCreationDate()!=null)
      this.setCreationDate(Date.valueOf(needPojo.getCreationDate()));

    List<URI> facetURIs = new ArrayList<>();
    for (String facet : needPojo.getFacetTypes()){

        facetURIs.add(URI.create(facet));
    }
    this.setFacetTypes(facetURIs);
    if (needPojo.getLatitude()!=null && needPojo.getLongitude()!=null)
      this.setAvailableAtLocation(needPojo.getLatitude().toString(),needPojo.getLongitude().toString());
    if ((needPojo.getStartTime()!=null && needPojo.getEndTime()!=null)&&(!needPojo.getEndTime().isEmpty()&&!needPojo
      .getStartTime().isEmpty()))
      this.addInterval(Long.valueOf(needPojo.getStartTime()), Long.valueOf(needPojo.getEndTime()));
    if (needPojo.getTags()!=null && !needPojo.getTags().isEmpty())
      this.addTag(needPojo.getTags());
    if(needPojo.getLowerPriceLimit()!=null && needPojo.getUpperPriceLimit()!=null)
     this.setPriceLimit(needPojo.getLowerPriceLimit(),needPojo.getUpperPriceLimit());
    if (needPojo.getNeedURI()!=null&&!needPojo.getNeedURI().isEmpty())
      this.setUri(needPojo.getNeedURI());
    if (needPojo.getStartTime()!=null&&!needPojo.getStartTime().isEmpty() && needPojo.getEndTime()!=null && !needPojo
      .getEndTime().isEmpty()){
      this.addInterval(Long.valueOf(needPojo.getStartTime()),Long.valueOf(needPojo.getEndTime()));
    }
    if (needPojo.getCurrency()!=null&&!needPojo.getCurrency().isEmpty()){
      this.setCurrency(needPojo.getCurrency());
    }
    this.setRecurInfiniteTimes(needPojo.getRecurInfiniteTimes());
    if (needPojo.getRecurIn()!=null)
     this.setRecurIn(needPojo.getRecurIn());
    if (needPojo.getRecurTimes()!=null)
     this.setRecurTimes(needPojo.getRecurTimes());
  }


  @Override
  public Model build() {
    Model needModel = ModelFactory.createDefaultModel();
    DefaultPrefixUtils.setDefaultPrefixes(needModel);

    Resource needResource = needModel.createResource(this.getNeedURIString(), WON.NEED);
    // need type
    needModel.add(needModel.createStatement(needResource, WON.HAS_BASIC_NEED_TYPE, WON.toResource(this.getBasicNeedTypeBNT())));

    // need content
    Resource needContent = needModel.createResource(WON.NEED_CONTENT);

    needContent.addProperty(DC.title, getTitle(), XSDDatatype.XSDstring);
   /* if (!needPojo.getTextDescription().isEmpty())
      needContent.addProperty(WON.HAS_TEXT_DESCRIPTION, needPojo.getTextDescription(), XSDDatatype.XSDstring);     */
      attachRdfToModelViaBlanknode(RdfUtils.toString(getContentDescription()), "TTL", needContent,
                                   WON.HAS_CONTENT_DESCRIPTION,
                                   needModel);

      List<String> tags = this.getTags();
      for (String tag : tags) {
        needModel.add(needModel.createStatement(needContent, WON.HAS_TAG, tag.trim()));
      }


    needModel.add(needModel.createStatement(needResource, WON.HAS_CONTENT, needContent));

    for(URI ft : getFacetTypes()) {
      needModel.add(needModel.createStatement(needResource, WON.HAS_FACET, needModel.createResource(ft.toString())));
    }


    // need modalities
    Resource needModality = needModel.createResource(WON.NEED_MODALITY);

    //price and currency
    if (getPriceLimit()!=null&& getPriceLimit().getMaximum() != null && getPriceLimit().getMinimum() != null) {
      Resource priceSpecification = needModel.createResource(WON.PRICE_SPECIFICATION);
      if ( getPriceLimit().getMinimum()  != null)
        priceSpecification.addProperty(WON.HAS_LOWER_PRICE_LIMIT, Double.toString(getPriceLimit().getMinimum()), XSDDatatype.XSDfloat);
      if (getPriceLimit().getMaximum() != null)
        priceSpecification.addProperty(WON.HAS_UPPER_PRICE_LIMIT, Double.toString(getPriceLimit().getMaximum()), XSDDatatype.XSDfloat);
      if (!getCurrency().isEmpty())
        priceSpecification.addProperty(WON.HAS_CURRENCY,getCurrency(), XSDDatatype.XSDstring);

      needModel.add(needModel.createStatement(needModality, WON.HAS_PRICE_SPECIFICATION, priceSpecification));
    }

    if (this.getAvailableAtLocationLatitude() != null && getAvailableAtLocationLongitude() != null) {
      Resource location = needModel.createResource(GEO.POINT)
                                   .addProperty(GEO.LATITUDE, Double.toString(this.getAvailableAtLocationLatitude()))
                                   .addProperty(GEO.LONGITUDE, Double.toString(this.getAvailableAtLocationLongitude()));

      needModel.add(needModel.createStatement(needModality, WON.AVAILABLE_AT_LOCATION, location));
    }

    // time constraint
    if (!this.getIntervals().isEmpty()) {
      Resource timeConstraint = needModel.createResource(WON.TIME_SPECIFICATION)
                                         .addProperty(WON.HAS_RECUR_INFINITE_TIMES, Boolean.toString(this.isRecurInfiniteTimes()),
                                                      XSDDatatype.XSDboolean);
      for (Interval interval : this.getIntervals()){
        timeConstraint.addProperty(WON.HAS_START_TIME,interval.getFrom().toString(), XSDDatatype.XSDdateTime);
        timeConstraint.addProperty(WON.HAS_END_TIME, interval.getTo().toString(), XSDDatatype.XSDdateTime);
        timeConstraint.addProperty(WON.HAS_RECURS_IN,Long.toString(this.getRecurIn()));
        timeConstraint.addProperty(WON.HAS_RECURS_TIMES,Integer.toString(this.getRecurTimes()));
      }
      needModel.add(needModel.createStatement(needModality, WON.HAS_TIME_SPECIFICATION, timeConstraint));

    }
    return needModel;
      //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public void copyValuesFromProduct(final Model product) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  private void attachRdfToModelViaBlanknode(final String rdfAsString, final String rdfLanguage, final Resource resourceToLinkTo, final Property propertyToLinkThrough, final com.hp.hpl.jena.rdf.model.Model modelToModify)
  {
    com.hp.hpl.jena.rdf.model.Model model = RdfUtils.readRdfSnippet(rdfAsString, rdfLanguage);
    RdfUtils.attachModelByBaseResource(resourceToLinkTo,propertyToLinkThrough, model);
  }

}
