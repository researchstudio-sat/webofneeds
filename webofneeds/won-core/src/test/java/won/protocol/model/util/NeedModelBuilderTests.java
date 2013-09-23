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
import junit.framework.Assert;
import org.junit.Test;
import won.protocol.util.NeedModelBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.StringWriter;

/**
 * User: fkleedorfer
 * Date: 20.09.13
 */
public class NeedModelBuilderTests
{
  @Test
  public void testRoundTrip() throws FileNotFoundException
  {
    Model model = readTTL("won-core/src/test/resources/docs/test_content_cupboard_45_45_15.ttl","http://www.example.com/resource/need/12");
    NeedModelBuilder builder = new NeedModelBuilder();
    NeedModelBuilder otherBuilder = new NeedModelBuilder();
    builder.copyValuesFromProduct(model);
    builder.copyValuesToBuilder(otherBuilder);
    Model model2 = otherBuilder.build();
    Assert.assertTrue(model.isIsomorphicWith(model2));
  }

  private static void printModel(String label, Model model){
    System.out.println(label+":");
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
}
