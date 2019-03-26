package won.bot.framework.eventbot.action.impl.debugbot;

import java.text.DecimalFormat;
import java.time.Duration;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.springframework.util.StopWatch;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.event.impl.crawlconnection.CrawlConnectionCommandFailureEvent;
import won.bot.framework.eventbot.event.impl.crawlconnection.CrawlConnectionCommandSuccessEvent;
import won.protocol.model.Connection;
import won.protocol.util.WonRdfUtils;

public class SendMessageReportingCrawlResultAction extends SendMessageOnCrawlResultAction {

  private StopWatch crawlStopWatch;

  public SendMessageReportingCrawlResultAction(EventListenerContext eventListenerContext, Connection con,
      StopWatch crawlStopWatch) {
    super(eventListenerContext, con);
    this.crawlStopWatch = crawlStopWatch;
  }

  @Override
  protected Model makeFailureMessage(CrawlConnectionCommandFailureEvent failureEvent) {
    String message = failureEvent.getMessage();
    if (message == null || message.trim().length() == 0) {
      message = "[no message available]";
    }
    return WonRdfUtils.MessageUtils.textMessage("Could not crawl connection data. Problem: " + message);
  }

  @Override
  protected Model makeSuccessMessage(CrawlConnectionCommandSuccessEvent successEvent) {
    crawlStopWatch.stop();
    Duration crawlDuration = Duration.ofMillis(crawlStopWatch.getLastTaskTimeMillis());
    Dataset conversation = successEvent.getCrawledData();
    return WonRdfUtils.MessageUtils.textMessage("Finished crawl in " + getDurationString(crawlDuration)
        + " seconds. The dataset has " + conversation.asDatasetGraph().size() + " rdf graphs.");
  }

  private String getDurationString(Duration queryDuration) {
    return new DecimalFormat("###.##").format(queryDuration.toMillis() / 1000d);
  }
}
