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

package won.protocol.model.util;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import won.protocol.model.BasicNeedType;
import won.protocol.model.NeedState;
import won.protocol.util.NeedBuilderBase;
import won.protocol.util.NeedModelBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.StringWriter;
import java.net.URI;

/**
 * User: fkleedorfer
 * Date: 20.09.13
 */
public class NeedModelBuilderTests
{
  @Test
  public void testRoundTrip1() throws FileNotFoundException
  {
    Model model = readTTL("won-core/src/test/resources/docs/test_content_cupboard_45_45_15.ttl", "http://www.example.com/resource/need/12");
    testRoundTrip(model);
  }

  @Test
  public void testRoundTrip2() throws FileNotFoundException
  {
    Model model = readTTL("won-core/src/test/resources/docs/test_intervals.ttl", "http://www.example.com/resource/need/12");
    testRoundTrip(model);
  }

  /**
   * Tests if conversion between NeedState enum and the corresponding URI works.
   * @throws Exception
   */
  @Test
  void testNeedStateURIandEnum() throws Exception {
    TestNeedBuilder builder = new TestNeedBuilder();
    builder.setState(NeedState.ACTIVE);
    Assert.assertEquals(NeedState.ACTIVE.getURI(), builder.testGetTheNeedStateURI());
    builder = new TestNeedBuilder();
    builder.setState(NeedState.ACTIVE.getURI());
    Assert.assertEquals(NeedState.ACTIVE, builder.testGetTheNeedStateNS());
  }

  /**
   * Tests if conversion between NeedState enum and the corresponding URI works.
   * @throws Exception
   */
  @Test
  void testBasicNeedTypeURIandEnum() throws Exception {
    TestNeedBuilder builder = new TestNeedBuilder();
    builder.setBasicNeedType(BasicNeedType.CRITIQUE);
    Assert.assertEquals(BasicNeedType.CRITIQUE.getURI(), builder.testGetBasicNeedTypeURI());
    builder = new TestNeedBuilder();
    builder.setBasicNeedType(BasicNeedType.DEMAND.getURI());
    Assert.assertEquals(BasicNeedType.DEMAND, builder.testGetBasicNeedTypeBNT());
  }

  public void testRoundTrip(Model model) throws FileNotFoundException
  {
    NeedModelBuilder builder = new NeedModelBuilder();
    NeedModelBuilder otherBuilder = new NeedModelBuilder();
    builder.copyValuesFromProduct(model);
    builder.copyValuesToBuilder(otherBuilder);
    Model model2 = otherBuilder.build();
    Assert.assertTrue(model.isIsomorphicWith(model2));
  }

  private static void printModel(String label, Model model)
  {
    System.out.println(label + ":");
    model.write(System.out, FileUtils.langTurtle);
  }

  private static String getNTriples(String filename, String baseURI) throws FileNotFoundException
  {
    File file = new File(filename);
    if (!file.exists()) {
      System.err.println("file not found: " + file.getAbsolutePath());
    }
    Model model = readTTL(filename, baseURI);
    return toNTriples(model, baseURI);
  }

  private static Model readTTL(String filename, String baseURI) throws FileNotFoundException
  {
    System.out.println("loading ntriples data for " + baseURI + " from " + filename);
    Model ret = ModelFactory.createDefaultModel();
    ret.read(new FileReader(filename), baseURI, FileUtils.langTurtle);
    return ret;
  }

  private static String toNTriples(Model model, String baseURI)
  {
    StringWriter writer = new StringWriter();
    model.write(writer, FileUtils.langNTriple, baseURI);
    return writer.toString();
  }

  /**
   * Need builder used for testing protected methods of the base builder.
   */
  private class TestNeedBuilder extends NeedBuilderBase<String>
  {
    @Override
    public String build() {
      return null;
    }

    @Override
    public void copyValuesFromProduct(String product) {
      //do nothing
    }

    public NeedState testGetTheNeedStateNS(){
      return getStateNS();
    }

    public URI testGetTheNeedStateURI(){
      return getStateURI();
    }

    public BasicNeedType testGetBasicNeedTypeBNT(){
      return getBasicNeedTypeBNT();
    }

    public URI testGetBasicNeedTypeURI(){
      return getBasicNeedTypeURI();
    }
  }
}
