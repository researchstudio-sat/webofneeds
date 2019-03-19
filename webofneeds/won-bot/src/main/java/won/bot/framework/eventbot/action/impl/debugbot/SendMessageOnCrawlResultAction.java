package won.bot.framework.eventbot.action.impl.debugbot;

import org.apache.jena.rdf.model.Model;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandEvent;
import won.bot.framework.eventbot.event.impl.crawlconnection.CrawlConnectionCommandFailureEvent;
import won.bot.framework.eventbot.event.impl.crawlconnection.CrawlConnectionCommandSuccessEvent;
import won.protocol.model.Connection;

public abstract class SendMessageOnCrawlResultAction extends ProcessCrawlResultAction {

    private Connection con;

    public SendMessageOnCrawlResultAction(EventListenerContext eventListenerContext, Connection con) {
        super(eventListenerContext);
        this.con = con;
    }

    @Override
    protected final void onCrawlFailure(CrawlConnectionCommandFailureEvent failureEvent) {
        Model messageModel = makeFailureMessage(failureEvent);
        if (messageModel == null)
            return;
        getEventListenerContext().getEventBus().publish(new ConnectionMessageCommandEvent(con, messageModel));
    }

    protected Model makeFailureMessage(CrawlConnectionCommandFailureEvent failureEvent) {
        return null;
    }

    @Override
    protected final void onCrawlSuccess(CrawlConnectionCommandSuccessEvent successEvent) {
        Model messageModel = makeSuccessMessage(successEvent);
        if (messageModel == null)
            return;
        getEventListenerContext().getEventBus().publish(new ConnectionMessageCommandEvent(con, messageModel));
    }

    protected Model makeSuccessMessage(CrawlConnectionCommandSuccessEvent successEvent) {
        return null;
    }

}
