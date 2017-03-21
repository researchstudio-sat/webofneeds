package won.owner.pojo;

import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.model.Facet;
import won.protocol.model.FacetType;
import won.protocol.model.NeedState;
import won.protocol.vocabulary.GEO;
import won.protocol.vocabulary.MAONT;
import won.protocol.vocabulary.WON;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;


/**
 * User: Gabriel
 * Date: 19.12.12
 * Time: 11:44
 */
public class NeedPojo
{
  final Logger logger = LoggerFactory.getLogger(getClass());

  private String needURI;

  private String title;
  private NeedState state = NeedState.ACTIVE;
  private String[] needFacetURIs;
  private String[] facetURIs = FacetType.getNames();
  private String ownFacetURI;
  private String remoteFacetURI;
  private String[] facetTypes = new String[]{"http://purl.org/webofneeds/model#OwnerFacet"};
  private boolean anonymize;
  private String wonNode;

  private String textDescription;
  private String tags;
  private String creationDate;

  // using objects to be able to check for null
  private Double upperPriceLimit;
  private Double lowerPriceLimit;
  private String currency;

  private Double latitude;
  private Double longitude;

  private String startTime;
  private String endTime;
  private Long recurIn;
  private Integer recurTimes;
  private boolean recurInfiniteTimes;

  private String contentDescription;

  private long needId = -1;

  private List<ImagePojo> images;

  public NeedPojo()
  {

  }

  public NeedPojo(List<Facet> facets)  {
    needFacetURIs = new String[facets.size()];
    int i = 0;

    for(Facet facet : facets) {
        needFacetURIs[i++] = facet.getFacetType().getURI().toString();
    }
  }

  public NeedPojo(URI needUri, final Model model)
  {
    this.needURI = needUri.toString();
    Resource need = model.getResource(needUri.toString());
    Statement creationDateStat = need.getProperty(DCTerms.created);
    if (creationDateStat!=null){
      creationDate = creationDateStat.getString();
    }

    StmtIterator facetIter = need.listProperties(WON.HAS_FACET);
    List<String> facets = new ArrayList<String>(10);
    while(facetIter.hasNext()) {
      Statement stmt = facetIter.nextStatement();
      facets.add(stmt.getObject().toString());
    }
    this.needFacetURIs = facets.toArray(new String[facets.size()]);

    Statement needContentStatement = need.getProperty(WON.HAS_CONTENT);
    if (needContentStatement != null) {
      Resource needContent = needContentStatement.getResource();
      Statement titleStat = needContent.getProperty(DC.title);
      if (titleStat != null) title = titleStat.getString();

      Statement textDescriptionStat = needContent.getProperty(WON.HAS_TEXT_DESCRIPTION);
      if (textDescriptionStat != null) textDescription = textDescriptionStat.getString();

      Statement contentDescriptionStat = needContent.getProperty(WON.HAS_CONTENT_DESCRIPTION);
      if (contentDescriptionStat != null) contentDescription = " [ RDF CONTENT ] ";

      Statement attachedMedia = needContent.getProperty(WON.HAS_ATTACHED_MEDIA);
      List<ImagePojo> images = new ArrayList<ImagePojo>();
      if (attachedMedia != null){
        Seq imageSeq = attachedMedia.getSeq();
        NodeIterator it = imageSeq.iterator();
        while (it.hasNext()) {
          //check if the node is a MA:IMAGGE
          RDFNode node = it.next();
          if (! node.isResource()) continue;
          Resource nodeRes = node.asResource();
          if (!nodeRes.hasProperty(RDF.type, MAONT.IMAGE)) continue;
          //it's an image. get the URI (ma:locator)
          ImagePojo imagePojo = new ImagePojo();
          Statement locatorStmt = nodeRes.getProperty(MAONT.LOCATOR);
          if (locatorStmt == null) continue;
          imagePojo.setUri(locatorStmt.getObject().toString());
        }
      }
      this.images = images;

      StmtIterator tagProps = needContent.listProperties(WON.HAS_TAG);
      StringBuilder tags = new StringBuilder();
      while (tagProps.hasNext()) {
        tags.append(tagProps.next().getObject().toString());
        if (tagProps.hasNext())
          tags.append(", ");
      }
      this.tags = tags.toString();
    }

    Statement needModality = need.getProperty(WON.HAS_NEED_MODALITY);
    if (needModality != null) {

      Statement location = needModality.getResource().getProperty(WON.HAS_LOCATION);
      if (location != null) {
        latitude = location.getProperty(GEO.LATITUDE).getDouble();
        longitude = location.getProperty(GEO.LONGITUDE).getDouble();
      }

      Statement timeConstraints = needModality.getResource().getProperty(WON.HAS_TIME_SPECIFICATION);
      if (timeConstraints != null) {

        Statement startTimeStat = timeConstraints.getResource().getProperty(WON.HAS_START_TIME);
        if (startTimeStat != null) startTime = startTimeStat.getString();

        Statement endTimeStat = timeConstraints.getResource().getProperty(WON.HAS_END_TIME);
        if (endTimeStat != null) endTime = endTimeStat.getString();

        Statement timeConstraintStat = timeConstraints.getResource().getProperty(WON.HAS_RECURS_IN);
        if (timeConstraintStat != null) recurIn = timeConstraintStat.getLong();

        Statement recurTimesStat = timeConstraints.getResource().getProperty(WON.HAS_RECURS_TIMES);
        if (recurTimesStat != null) recurTimes = recurTimesStat.getInt();

        recurInfiniteTimes = timeConstraints.getResource().getProperty(WON.HAS_RECUR_INFINITE_TIMES).getBoolean();
      }

       Statement priceSpecification = needModality.getResource().getProperty(WON.HAS_PRICE_SPECIFICATION);
       if (priceSpecification != null) {

        Statement currencyStat = priceSpecification.getResource().getProperty(WON.HAS_CURRENCY);
        if (currencyStat != null) currency = currencyStat.getString();

        Statement lowerStat = priceSpecification.getResource().getProperty(WON.HAS_LOWER_PRICE_LIMIT);
        if (lowerStat != null) lowerPriceLimit = lowerStat.getDouble();

        Statement upperStat = priceSpecification.getResource().getProperty(WON.HAS_UPPER_PRICE_LIMIT);
        if (upperStat != null) upperPriceLimit = upperStat.getDouble();
      }

    }

  }

  public String[] getNeedFacetURIs() {
      return needFacetURIs;
  }

  public void setNeedFacetURIs(String[] needFacetURIs) {
      this.needFacetURIs = needFacetURIs;
  }

  public String getOwnFacetURI() {
        return ownFacetURI;
  }

  public void setOwnFacetURI(String ownFacetURI) {
        this.ownFacetURI = ownFacetURI;
  }

  public String getRemoteFacetURI() {
        return remoteFacetURI;
  }

  public void setRemoteFacetURI(String remoteFacetURI) {
        this.remoteFacetURI = remoteFacetURI;
  }

  public String[] getFacetTypes() {
     return facetTypes;
  }

  public void setFacetTypes(String[] facetTypes) {
     this.facetTypes = facetTypes;
  }

  public String[] getFacetURIs() {
      return facetURIs;
  }

  public void setFacetURIs(String[] facetURIs) {
        this.facetURIs = facetURIs;
  }

  public String getCreationDate()
  {
    return creationDate;
  }

  public void setCreationDate(final String creationDate)
  {
    this.creationDate = creationDate;
  }

  public String getTitle()
  {
    return title;
  }

  public void setTitle(final String title)
  {
    this.title = title;
  }

  public NeedState getState()
  {
    return state;
  }

  public void setState(final NeedState state)
  {
    this.state = state;
  }

  public boolean isAnonymize()
  {
    return anonymize;
  }

  public void setAnonymize(final boolean anonymize)
  {
    this.anonymize = anonymize;
  }

  public String getWonNode()
  {
    return wonNode;
  }

  public void setWonNode(final String wonNode)
  {
    this.wonNode = wonNode;
  }

  public String getTextDescription()
  {
    return textDescription;
  }

  public void setTextDescription(final String textDescription)
  {
    this.textDescription = textDescription;
  }

  public String getTags()
  {
    return tags;
  }

  public void setTags(final String tags)
  {
    this.tags = tags;
  }

  public String getNeedURI()
  {
    return needURI;
  }

  public void setNeedURI(final String needURI)
  {
    this.needURI = needURI;
  }

  public Double getUpperPriceLimit()
  {
    return upperPriceLimit;
  }

  public void setUpperPriceLimit(Double upperPriceLimit)
  {
    this.upperPriceLimit = upperPriceLimit;
  }

  public Double getLowerPriceLimit()
  {
    return lowerPriceLimit;
  }

  public void setLowerPriceLimit(Double lowerPriceLimit)
  {
    this.lowerPriceLimit = lowerPriceLimit;
  }

  public String getCurrency()
  {
    return currency;
  }

  public void setCurrency(String currency)
  {
    this.currency = currency;
  }

  public Double getLatitude()
  {
    return latitude;
  }

  public void setLatitude(Double latitude)
  {
    this.latitude = latitude;
  }

  public Double getLongitude()
  {
    return longitude;
  }

  public void setLongitude(Double longitude)
  {
    this.longitude = longitude;
  }

  public String getStartTime()
  {
    return startTime;
  }

  public void setStartTime(String startTime)
  {
    this.startTime = startTime;
  }

  public String getEndTime()
  {
    return endTime;
  }

  public void setEndTime(String endTime)
  {
    this.endTime = endTime;
  }

  public Long getRecurIn()
  {
    return recurIn;
  }

  public void setRecurIn(Long recurIn)
  {
    this.recurIn = recurIn;
  }

  public Integer getRecurTimes()
  {
    return recurTimes;
  }

  public void setRecurTimes(Integer recurTimes)
  {
    this.recurTimes = recurTimes;
  }

  public boolean getRecurInfiniteTimes()
  {
    return recurInfiniteTimes;
  }

  public void setRecurInfiniteTimes(boolean recurInfiniteTimes)
  {
    this.recurInfiniteTimes = recurInfiniteTimes;
  }

  public long getNeedId()
  {
    return needId;
  }

  public void setNeedId(long needId)
  {
    this.needId = needId;
  }

  public String getContentDescription()
  {
    return contentDescription;
  }

  public void setContentDescription(final String contentDescription)
  {
    this.contentDescription = contentDescription;
  }

  public List<ImagePojo> getImages() {
    return images;
  }

  public void setImages(List<ImagePojo> images) {
    this.images = images;
  }

}