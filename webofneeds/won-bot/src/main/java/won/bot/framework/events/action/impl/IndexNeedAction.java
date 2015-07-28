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

package won.bot.framework.events.action.impl;

import com.hp.hpl.jena.query.Dataset;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.common.SolrInputDocument;
import won.bot.framework.events.EventListenerContext;
import won.bot.framework.events.action.BaseEventBotAction;
import won.bot.framework.events.event.Event;
import won.bot.framework.events.event.impl.NeedAddedToSolrEvent;
import won.bot.framework.events.event.impl.NeedCreatedEventForMatcher;
import won.matcher.solr.NeedSolrInputDocumentBuilder;
import won.protocol.message.WonMessage;
import won.protocol.util.NeedModelBuilder;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;

import java.net.MalformedURLException;

//import org.apache.solr.common.SolrInputDocument;

/**
 * Action that sends the need data from a NeedSpecificEvent to solr based matcher.
 * By default, will not to commit if subsequent calls are made within a certain minimal time
 * span (1s), this is intended for bulk operations. In that case, the server is expected to
 * perform a commit from time to time.
 *
 */
public class IndexNeedAction extends BaseEventBotAction
{
  private String uriListName;
  private SolrServer server;
  private transient long lastInvocation = 0;
  private boolean suppressCommitsInBulkOperation = true;

  //if the last run was less than this value ago (in millis), don't commit.
  //thus, we can avoid committing all the time during bulk creation.
  private long SUPPRESS_COMMIT_TIMESPAN=1000;

  public IndexNeedAction(final EventListenerContext eventListenerContext){
    this(eventListenerContext, false);
  }

    public IndexNeedAction(final EventListenerContext eventListenerContext, boolean suppressCommitsInBulkOperation)
  {
    this(eventListenerContext, null);
    this.suppressCommitsInBulkOperation = suppressCommitsInBulkOperation;
    init();
  }

  private void init(){
    try {
      this.server = new CommonsHttpSolrServer(getEventListenerContext().getSolrServerURI().toString());
    } catch (MalformedURLException e) {
      logger.warn("could not create solr server",e);
    }
  }


  public IndexNeedAction(EventListenerContext eventListenerContext, String uriListName) {
      super(eventListenerContext);
      this.uriListName = uriListName;
  }

  @Override
  protected synchronized void doRun(Event event) throws Exception
  {
    logger.debug("adding need {} to solr server", ((NeedCreatedEventForMatcher) event).getNeedURI());
    NeedCreatedEventForMatcher needEvent = (NeedCreatedEventForMatcher) event;
    Dataset wonMessageDataset = needEvent.getNeedData();
    WonMessage wonMessage = new WonMessage(wonMessageDataset);
    //this dataset contains the complete need data
    NeedSolrInputDocumentBuilder builder = new NeedSolrInputDocumentBuilder();
    NeedModelBuilder needModelBuilder = new NeedModelBuilder();
    needModelBuilder.copyValuesFromProduct(WonRdfUtils.NeedUtils.getNeedModelFromNeedDataset(wonMessage.getMessageContent()));
    needModelBuilder.copyValuesToBuilder(builder);
    if (logger.isDebugEnabled()){
      logger.debug("got this model from won node: {}", RdfUtils.toString(wonMessageDataset));
      logger.debug("writing this solrInputDocument to siren: {}", builder.build());
    }

    //we want the NTRIPLE field to contain the complete RDF data:
    //builder.setContentDescription(needModelBuilder.build());
    SolrInputDocument doc = builder.build();

    server.add(doc);
    long now = System.currentTimeMillis();

    if (!suppressCommitsInBulkOperation || Math.abs(now - lastInvocation) > SUPPRESS_COMMIT_TIMESPAN){
      server.commit();
    }
    this.lastInvocation = now;

    getEventListenerContext().getEventBus().publish(new NeedAddedToSolrEvent(((NeedCreatedEventForMatcher) event)
      .getNeedURI()));
    logger.debug("need {} added to solr", ((NeedCreatedEventForMatcher) event).getNeedURI());
  }


}
