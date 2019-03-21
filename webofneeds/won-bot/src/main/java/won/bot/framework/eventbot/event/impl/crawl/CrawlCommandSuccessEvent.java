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

import org.apache.jena.query.Dataset;
import won.bot.framework.eventbot.event.impl.cmd.BaseCommandSuccessEvent;

/**
 * Indicates a successful crawl and contains the crawled dataset.
 */
public class CrawlCommandSuccessEvent extends BaseCommandSuccessEvent<CrawlCommandEvent> {
  private Dataset crawledData;

  public CrawlCommandSuccessEvent(CrawlCommandEvent originalCommandEvent, Dataset crawledData, String message) {
    super(message, originalCommandEvent);
    this.crawledData = crawledData;
  }

  public CrawlCommandSuccessEvent(CrawlCommandEvent originalCommandEvent, Dataset crawledData) {
    super(originalCommandEvent);
    this.crawledData = crawledData;
  }

  public Dataset getCrawledData() {
    return crawledData;
  }
}
