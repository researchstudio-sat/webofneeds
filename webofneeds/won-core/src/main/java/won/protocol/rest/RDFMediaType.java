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

package won.protocol.rest;

import javax.ws.rs.core.MediaType;

/**
 * User: fkleedorfer
 * Date: 28.11.12
 */
public class RDFMediaType
{
  public static final MediaType TEXT_TURTLE = new MediaType("text","turtle");
  public static final MediaType APPLICATION_RDF_XML = new MediaType("application","rdf+xml");
  public static final MediaType APPLICATION_X_TURTLE = new MediaType("application","x-turtle");
  public static final MediaType TEXT_RDF_N3 = new MediaType("text","rdf+n3");
  public static final MediaType APPLICATION_JSON = new MediaType("application","json");

}
