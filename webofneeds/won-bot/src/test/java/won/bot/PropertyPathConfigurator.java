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

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.path.PathParser;

import won.protocol.vocabulary.WON;

/**
 * User: syim Date: 11.07.14
 */
public class PropertyPathConfigurator {
  public static List<Path> configurePropertyPaths() {
    List<Path> propertyPaths = new ArrayList<>();
    addPropertyPath(propertyPaths, "<" + WON.HAS_CONNECTIONS + ">");
    addPropertyPath(propertyPaths, "<" + WON.HAS_CONNECTIONS + ">" + "/" + "rdfs:member");
    addPropertyPath(propertyPaths,
        "<" + WON.HAS_CONNECTIONS + ">" + "/" + "rdfs:member" + "/<" + WON.HAS_REMOTE_CONNECTION + ">");
    addPropertyPath(propertyPaths, "<" + WON.HAS_CONNECTIONS + ">" + "/" + "rdfs:member" + "/<"
        + WON.HAS_REMOTE_CONNECTION + ">/<" + WON.BELONGS_TO_NEED + ">");
    return propertyPaths;
  }

  public static void addPropertyPath(final List<Path> propertyPaths, String pathString) {
    Path path = PathParser.parse(pathString, PrefixMapping.Standard);
    propertyPaths.add(path);
  }
}
