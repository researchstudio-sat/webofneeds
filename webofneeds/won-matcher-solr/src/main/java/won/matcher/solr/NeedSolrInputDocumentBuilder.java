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

package won.matcher.solr;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileUtils;
import org.apache.solr.common.SolrInputDocument;
import won.protocol.solr.SolrFields;
import won.protocol.util.NeedBuilderBase;
import won.protocol.util.NeedModelBuilder;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collection;

/**
 * User: fkleedorfer
 * Date: 19.09.13
 */
public class NeedSolrInputDocumentBuilder extends NeedBuilderBase<SolrInputDocument>
{

  @Override
  public SolrInputDocument build()
  {
    String location = (getAvailableAtLocationLatitude() != null && getAvailableAtLocationLongitude() != null)?
        getAvailableAtLocationLatitude()+","+getAvailableAtLocationLongitude():null;
    SolrInputDocument doc = new SolrInputDocument();
    addFieldIfPresent(doc, SolrFields.URL, getURI());
    addFieldIfPresent(doc, SolrFields.BASIC_NEED_TYPE, getBasicNeedTypeURI());
    addFieldIfPresent(doc, SolrFields.DESCRIPTION, getDescription());
    addFieldIfPresent(doc, SolrFields.LOCATION, location);
    addFieldIfPresent(doc, SolrFields.LOWER_PRICE_LIMIT, getLowerPriceLimit());
    addFieldIfPresent(doc, SolrFields.UPPER_PRICE_LIMIT, getUpperPriceLimit());
    addContentDescriptionIfPresent(doc, SolrFields.NTRIPLE, getContentDescription());
    if (getTags() != null) {
      for(String tag: getTags()){
        doc.addField(SolrFields.TAG,tag);
      }
    }
    for(String tag: getHashtags(getTitle())){
      doc.addField(SolrFields.TAG,tag);
    }
    for(String tag: getHashtags(getDescription())){
      doc.addField(SolrFields.TAG,tag);
    }

    addFieldIfPresent(doc, SolrFields.TITLE,getTitle());
    return doc;
  }

  @Override
  public void copyValuesFromProduct(final SolrInputDocument document)
  {
    setUri(getStringValueIfPresent(document, SolrFields.URL));
    String location = getStringValueIfPresent(document,SolrFields.LOCATION);
    if (location != null){
      String[] latLong = location.split(",");
      if (latLong.length == 2) {
        setAvailableAtLocation(latLong[0], latLong[1]);
      }
    }
    setLowerPriceLimit(getStringValueIfPresent(document, SolrFields.LOWER_PRICE_LIMIT));
    setUpperPriceLimit(getStringValueIfPresent(document, SolrFields.UPPER_PRICE_LIMIT));
    setBasicNeedType(getStringValueIfPresent(document, SolrFields.BASIC_NEED_TYPE));
    setDescription(getStringValueIfPresent(document, SolrFields.DESCRIPTION));
    Collection<Object> tags = document.getFieldValues(SolrFields.TAG);
    if (tags != null && tags.size() > 0) {
      for (Object tag: tags){
        this.addTag((String) tag);
      }
    }
    setTitle(getStringValueIfPresent(document, SolrFields.TITLE));
    Long timeStart = (Long) document.getFieldValue(SolrFields.TIME_START);
    Long timeEnd = (Long) document.getFieldValue(SolrFields.TIME_END);
    if (timeEnd != null || timeStart != null){
      addInterval(timeStart, timeEnd);
    }
    String ntriples = getStringValueIfPresent(document, SolrFields.NTRIPLE);
    if (ntriples != null) {
      Model model = ModelFactory.createDefaultModel();
      model.read(new StringReader(ntriples), FileUtils.langNTriple, getStringValueIfPresent(document,SolrFields.URL));
      setContentDescription(model);
    }
  }

  private void addContentDescriptionIfPresent(final SolrInputDocument doc, final String field, final Model contentDescription)
  {
    if (contentDescription != null){
      StringWriter writer = new StringWriter();
      NeedModelBuilder needModelBuilder = new NeedModelBuilder();
      copyValuesToBuilder(needModelBuilder);
      Model model = needModelBuilder.build();
      model.write(writer, FileUtils.langNTriple, getURI().toString());
      doc.addField(field, writer.toString());
    }
  }

  private void addFieldIfPresent(SolrInputDocument doc, String field, Object value){
    if (value == null) return;
    String stringValue = value.toString();
    if (stringValue == null) return;
    doc.addField(field, stringValue);
  }

  private String getStringValueIfPresent(SolrInputDocument doc, String field) {
    Object value = doc.getFieldValue(field);
    if (value != null) return value.toString();
    return null;
  }
}
