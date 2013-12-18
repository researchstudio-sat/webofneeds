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

package won.bot.generator;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * Interface for producing needs. The needs produced must be suitable as the 'model'
 * argument to the createNeed protocol method (i.e., don't have a URI, and only contain user-definable data).
 */
public interface NeedFactory
{
  /**
   * Returns a new Model that represents the created need.
   * @return
   */
  public Model create();

}
