package won.bot.framework.eventbot.action.impl.debugbot;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.crawlconnection.CrawlConnectionCommandFailureEvent;
import won.bot.framework.eventbot.event.impl.crawlconnection.CrawlConnectionCommandSuccessEvent;
import won.bot.framework.eventbot.listener.EventListener;

public class ProcessCrawlResultAction extends BaseEventBotAction {

    protected ProcessCrawlResultAction(EventListenerContext eventListenerContext) {
        super(eventListenerContext);
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        if (event instanceof CrawlConnectionCommandSuccessEvent) {
            CrawlConnectionCommandSuccessEvent successEvent = (CrawlConnectionCommandSuccessEvent) event;
            onCrawlSuccess(successEvent);
        } else if (event instanceof CrawlConnectionCommandFailureEvent) {
            CrawlConnectionCommandFailureEvent failureEvent = (CrawlConnectionCommandFailureEvent) event;
            onCrawlFailure(failureEvent);
        }
    }

    /**
     * Called if the crawl fails.
     * 
     * @param failureEvent
     */
    protected void onCrawlFailure(CrawlConnectionCommandFailureEvent failureEvent) {

    }

    /**
     * Called if the crawl succeeds.
     * 
     * @param successEvent
     */
    protected void onCrawlSuccess(CrawlConnectionCommandSuccessEvent successEvent) {

    }

}
