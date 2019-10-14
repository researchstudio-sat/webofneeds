/*
 * Copyright 2012 Research Studios Austria Forschungsges.m.b.H. Licensed under
 * the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package won.bot.framework.eventbot.action.impl.crawl;

import org.apache.jena.query.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.crawl.CrawlCommandEvent;
import won.bot.framework.eventbot.event.impl.crawl.CrawlCommandFailureEvent;
import won.bot.framework.eventbot.event.impl.crawl.CrawlCommandSuccessEvent;
import won.bot.framework.eventbot.listener.EventListener;

import java.lang.invoke.MethodHandles;

/**
 * Expects a CrawlCommandEvent to contain the required crawl information. Crawls
 * the data using the LinkedDataSource and publishes a CrawlSuccessEvent when
 * done.
 */
public class CrawlAction extends BaseEventBotAction {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public CrawlAction(EventListenerContext eventListenerContext) {
        super(eventListenerContext);
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        if (!(event instanceof CrawlCommandEvent))
            return;
        CrawlCommandEvent crawlCommandEvent = (CrawlCommandEvent) event;
        EventListenerContext ctx = getEventListenerContext();
        logger.debug("starting crawl for {}", crawlCommandEvent.getStartURI());
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Dataset crawledData = null;
        try {
            crawledData = ctx.getLinkedDataSource().getDataForResourceWithPropertyPath(crawlCommandEvent.getStartURI(),
                            crawlCommandEvent.getAtomURI(), crawlCommandEvent.getPropertyPaths(),
                            crawlCommandEvent.getGetMaxRequest(), crawlCommandEvent.getMaxDepth());
        } catch (Exception e) {
            logger.debug("caught exception during crawl for {}", crawlCommandEvent.getStartURI(), e);
            ctx.getEventBus()
                            .publish(new CrawlCommandFailureEvent(crawlCommandEvent,
                                            "Could not crawl " + crawlCommandEvent.getStartURI() + " with WebID "
                                                            + crawlCommandEvent.getAtomURI() + ": caught " + e));
            return;
        }
        stopWatch.stop();
        logger.debug("finished crawl for {} in {} millis", crawlCommandEvent.getStartURI(),
                        stopWatch.getTotalTimeMillis());
        ctx.getEventBus().publish(new CrawlCommandSuccessEvent(crawlCommandEvent, crawledData, "Finished crawling "
                        + crawlCommandEvent.getStartURI() + " in " + stopWatch.getTotalTimeMillis()));
    }
}
