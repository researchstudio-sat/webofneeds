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

package won.protocol.message;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.lib.DatasetLib;
import com.hp.hpl.jena.vocabulary.RDF;
import org.junit.Assert;
import org.junit.Test;
import won.protocol.model.BasicNeedType;
import won.protocol.util.NeedModelBuilder;
import won.protocol.util.RdfUtils;

import java.net.URI;

/**
 * User: ypanchenko
 * Date: 05.08.2014
 */
public class WonMessageBuilderTest
{

  
  private static final String RESOURCE_DIR = "/need-lifecycle_with_message_02adj/";
  private static final String RESOURCE_FILE =
    RESOURCE_DIR + "01_create_need/01_OA_to_WN1-without-sig.trig";
    
  @Test
  public void testBuildCreateMessage() throws Exception {
    //create core model of need
    String needURIString = "http://www.example.com/resource/need/randomNeedID_1";
    Model contentDescriptionModel = ModelFactory.createDefaultModel();
    Resource res = RdfUtils.findOrCreateBaseResource(contentDescriptionModel);
    res.addProperty(RDF.type, contentDescriptionModel.createResource("http://purl.org/tio/ns#Taxi"));

    Model coreModel= new NeedModelBuilder()
      .setBasicNeedType(BasicNeedType.SUPPLY)
      .setUri(needURIString)
      .setContentDescription(contentDescriptionModel).build();

    Model contentDescriptionModel2 = ModelFactory.createDefaultModel();
    res = RdfUtils.findOrCreateBaseResource(contentDescriptionModel2);
    Property prop = contentDescriptionModel2.createProperty("http://purl.org/goodrelations/v1#condition");
    res.addProperty(prop,"Has been vacuumed three days ago!");

    Model transientModel= new NeedModelBuilder()
      .setUri("http://www.example.com/resource/need/randomNeedID_1")
      .setContentDescription(contentDescriptionModel2).build();
    
    WonMessage msg =  new WonMessageBuilder()
      .setMessageURI(URI.create("http://www.example.com/resource/need/randomNeedID_1/event/0"))
      .setReceiverNodeURI(URI.create("http://www.example.com/won"))
      .setSenderNeedURI(URI.create("http://www.example.com/resource/need/randomNeedID_1"))
      .setWonMessageType(WonMessageType.CREATE_NEED)
      .addContent(URI.create("http://www.example.com/resource/need/randomNeedID_1/core#data"), coreModel, null)
      .addContent(URI.create("http://www.example.com/resource/need/randomNeedID_1/transient#data"), transientModel,
        null)
      .build();

    Dataset expectedDataset = TestUtils.createTestDataset(RESOURCE_FILE);
    Dataset actualDataset = WonMessageEncoder.encodeAsDataset(msg);
    Assert.assertTrue(DatasetLib.isomorphic(expectedDataset, actualDataset));
  }

}
