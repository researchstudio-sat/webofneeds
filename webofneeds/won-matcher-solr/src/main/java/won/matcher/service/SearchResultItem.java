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

package won.matcher.service;

import com.hp.hpl.jena.rdf.model.Model;

import java.net.URI;

/**
 * User: fkleedorfer
 * Date: 12.09.13
 */
public class SearchResultItem
{
  /**
   * The match score (betwee 0 and 1)
   */
  private float score;
  /**
   * An RDF model containing title, tags, image links and the first N characters of the content
   */
  private Model model;
  /**
   * The URI of the need
   */
  private URI uri;
  /**
   * An optional RDF model containing an explanation for the search result
   */
  private Model explanation;

  public SearchResultItem(final float score, final Model model, final URI uri, final Model explanation)
  {
    this.score = score;
    this.model = model;
    this.uri = uri;
    this.explanation = explanation;
  }

  public float getScore()
  {
    return score;
  }

  public Model getModel()
  {
    return model;
  }

  public URI getUri()
  {
    return uri;
  }

  public Model getExplanation()
  {
    return explanation;
  }
}
