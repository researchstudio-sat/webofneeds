package won.protocol.util;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.vocabulary.DC;
import won.protocol.vocabulary.WON;

import java.util.Collection;

/**
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

  public DefaultNeedModelWrapper(final Model needModel) {
    super(needModel);
  }

  private void createContentNodeIfNonExist(ContentType type) {
    if (getContentNodes(type).size() == 0) {
      createContentNode(type, null);
    }
  }

  public DefaultNeedModelWrapper setTitle(ContentType type, String title) {
    createContentNodeIfNonExist(type);
    setPropertyStringValue(type, DC.title, title);
    return this;
  }

  public String getTitle(ContentType type) {
    return getPropertyStringValue(type, DC.title);
  }

  public DefaultNeedModelWrapper setDescription(ContentType type, String description) {
    createContentNodeIfNonExist(type);
    setPropertyStringValue(type, WON.HAS_TEXT_DESCRIPTION, description);
    return this;
  }

  public String getDescription(ContentType type) {
    return getPropertyStringValue(type, WON.HAS_TEXT_DESCRIPTION);
  }

  public DefaultNeedModelWrapper addTag(ContentType type, String tag) {
    createContentNodeIfNonExist(type);
    addPropertyStringValue(type, WON.HAS_TAG, tag);
    return this;
  }

  public Collection<String> getTags(ContentType type) {
    return getPropertyStringValues(type, WON.HAS_TAG);
  }
}
