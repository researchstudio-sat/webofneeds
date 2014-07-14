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

package won.bot;

import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.sparql.path.Path;
import com.hp.hpl.jena.sparql.path.PathParser;
import won.protocol.vocabulary.WON;

import java.util.ArrayList;
import java.util.List;

/**
 * User: syim
 * Date: 11.07.14
 */
public abstract class PropertyPathConfigurator
{
  public static List<Path> configurePropertyPaths(){
    List<Path> propertyPaths = new ArrayList<>();
    String pathString = "<"+ WON.HAS_CONNECTIONS+">";
    Path path = PathParser.parse(pathString, PrefixMapping.Standard);
    propertyPaths.add(path);
    pathString = "<"+WON.HAS_CONNECTIONS+">"+"/"+"rdfs:member";
    path = PathParser.parse(pathString, PrefixMapping.Standard);
    propertyPaths.add(path);
    pathString = "<"+WON.HAS_CONNECTIONS+">"+"/"+"rdfs:member"+"/<"+WON
      .HAS_REMOTE_CONNECTION+">";
    path = PathParser.parse(pathString, PrefixMapping.Standard);
    propertyPaths.add(path);
    pathString = "<"+WON.HAS_CONNECTIONS+">"+"/"+"rdfs:member"+"/<"+WON
      .HAS_REMOTE_CONNECTION+">/<"+WON
      .BELONGS_TO_NEED+">";
    path = PathParser.parse(pathString, PrefixMapping.Standard);
    propertyPaths.add(path);
    return propertyPaths;
  }
}
