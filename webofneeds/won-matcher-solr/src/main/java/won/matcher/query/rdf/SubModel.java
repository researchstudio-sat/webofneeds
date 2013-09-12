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

package won.matcher.query.rdf;

/**
 * User: fkleedorfer
 * Date: 09.09.13
 */

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Statement;

/**
 * Wrapper around model to hold a subgraph extracted from a bigger graph along with the statement that connected
 * the subgraph with the bigger graph.
 */
public class SubModel
{
  private Model model;
  private Statement attachingStatement;

  public Model getModel()
  {
    return model;
  }

  public Statement getAttachingStatement()
  {
    return attachingStatement;
  }

  public SubModel(final Model model, final Statement attachingStatement)
  {
    this.model = model;
    this.attachingStatement = attachingStatement;
  }
}
