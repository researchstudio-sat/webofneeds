package won.protocol.util;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.vocabulary.DC;
import won.protocol.exception.IncorrectPropertyCountException;
import won.protocol.model.NeedContentPropertyType;
import won.protocol.vocabulary.WON;

import java.util.Collection;

/**
 * Extends {@link NeedModelWrapper} to add matchat specific methods to access content fields like title, description, tags, etc.
 * In many methods {@link NeedContentPropertyType} is used as a parameter to specify which content node you want to access.
 *
 * Created by hfriedrich on 16.03.2017.
 */
public class DefaultNeedModelWrapper extends NeedModelWrapper
{
  public DefaultNeedModelWrapper(final String needUri) {
    super(needUri);
  }

  public DefaultNeedModelWrapper(final Dataset needDataset) {
    super(needDataset);
  }

  public DefaultNeedModelWrapper(final Model needModel, final Model sysInfoModel) {
    super(needModel, sysInfoModel);
  }

  private void createContentNodeIfNonExist(NeedContentPropertyType type) {
    if (getContentNodes(type).size() == 0) {
      createContentNode(type, null);
    }
  }

  public void setTitle(NeedContentPropertyType type, String title) {
    createContentNodeIfNonExist(type);
    setContentPropertyStringValue(type, DC.title, title);
  }

  public String getTitleFromIsOrAll() {

    String title = null;
    try {
      title = getContentPropertyStringValue(NeedContentPropertyType.IS, DC.title);
    } catch (IncorrectPropertyCountException e1) {
        title = getContentPropertyStringValue(NeedContentPropertyType.ALL, DC.title);
    }

    return title;
  }

  public String getTitle(NeedContentPropertyType type) {
    return getContentPropertyStringValue(type, DC.title);
  }

  public void setDescription(NeedContentPropertyType type, String description) {
    createContentNodeIfNonExist(type);
    setContentPropertyStringValue(type, WON.HAS_TEXT_DESCRIPTION, description);
  }

  public String getDescription(NeedContentPropertyType type) {
    return getContentPropertyStringValue(type, WON.HAS_TEXT_DESCRIPTION);
  }

  public void addTag(NeedContentPropertyType type, String tag) {
    createContentNodeIfNonExist(type);
    addContentPropertyStringValue(type, WON.HAS_TAG, tag);
  }

  public Collection<String> getTags(NeedContentPropertyType type) {
    return getContentPropertyStringValues(type, WON.HAS_TAG);
  }

  public Float getLocationLatitude(NeedContentPropertyType type) {

    Float latitude = null;
    try {
      String lat = getContentPropertyStringValue(type, "won:hasLocation/<s:geo>/<s:latitude>");
      if (lat != null) {
        latitude = new Float(lat);
      }
      return latitude;
    } catch (IncorrectPropertyCountException e) {
      // no need to handle this exception here
    }

    return latitude;
  }

  public Float getLocationLongitude(NeedContentPropertyType type) {

    Float longitude = null;
    try {
      String lon = getContentPropertyStringValue(type, "won:hasLocation/<s:geo>/<s:longitude>");
      if (lon != null) {
        longitude = new Float(lon);
      }
      return longitude;
    } catch (IncorrectPropertyCountException e) {
      // no need to handle this exception here
    }

    return longitude;
  }
}
