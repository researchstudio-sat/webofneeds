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

package won.bot.framework.eventbot.event.impl.crawl;

import java.net.URI;
import java.util.List;

import org.apache.jena.sparql.path.Path;

import won.bot.framework.eventbot.event.BaseNeedSpecificEvent;
import won.bot.framework.eventbot.event.impl.cmd.CommandEvent;

/**
 * Initiates the crawling of linked data. The WebID of the specified need is
 * used.
 */
public class CrawlCommandEvent extends BaseNeedSpecificEvent implements CommandEvent {
  private List<Path> propertyPaths;
  private URI startURI;
  private int getMaxRequest = 1000;
  private int maxDepth = 5;

  public CrawlCommandEvent(URI needURI, URI startURI, List<Path> propertyPaths, int getMaxRequest, int maxDepth) {
    super(needURI);
    this.propertyPaths = propertyPaths;
    this.startURI = startURI;
    this.getMaxRequest = getMaxRequest;
    this.maxDepth = maxDepth;
  }

  public URI getStartURI() {
    return startURI;
  }

  public List<Path> getPropertyPaths() {
    return propertyPaths;
  }

  public int getGetMaxRequest() {
    return getMaxRequest;
  }

  public int getMaxDepth() {
    return maxDepth;
  }
}
