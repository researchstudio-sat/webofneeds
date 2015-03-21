package won.bot.integration;

import com.hp.hpl.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import won.bot.framework.bot.Bot;
import won.bot.framework.manager.BotManager;
import won.owner.service.BotProtocolAdapter;
import won.protocol.message.WonMessage;
import won.protocol.model.Connection;
import won.protocol.model.Match;

import java.net.URI;
import java.util.Date;

/**
 * OwnerProtocolOwnerServiceCallback that dispatches the calls to the bots.
 */
public class BotOwnerProtocolAdapterCallback implements BotProtocolAdapter
{
  private final Logger logger = LoggerFactory.getLogger(getClass());
  BotManager botManager;

  TaskScheduler taskScheduler;



  @Override
  public void onClose(final Connection con, final Model content, final WonMessage wonMessage) {
    taskScheduler.schedule(new Runnable(){
      public void run(){
        try {
            logger.debug("onClose received for connection {} ",con);
          getBotForNeedUri(con.getNeedURI()).onCloseFromOtherNeed(con, wonMessage);
        } catch (Exception e) {
          logger.warn("error while handling onClose()",e);
        }
      }
    }, new Date());
  }


  @Override
  public void onHint(final Match match, final Model content, final WonMessage wonMessage) {
    taskScheduler.schedule(new Runnable(){
      public void run(){
        try {
          getBotForNeedUri(match.getFromNeed()).onHintFromMatcher(match, wonMessage);
        } catch (Exception e) {
          logger.warn("error while handling onHint()",e);
        }
      }
    }, new Date());
  }

  @Override
  public void onConnect(final Connection con, final Model content, final WonMessage wonMessage) {
    taskScheduler.schedule(new Runnable(){
      public void run(){
        try {
          logger.debug("onConnect called for connection {} ",con.getConnectionURI());
          getBotForNeedUri(con.getNeedURI()).onConnectFromOtherNeed(con, wonMessage);
        } catch (Exception e) {
          logger.warn("error while handling onConnect()",e);
        }
      }
    }, new Date());
  }

  @Override
  public void onOpen(final Connection con, final Model content, final WonMessage wonMessage) {
    taskScheduler.schedule(new Runnable(){
      public void run(){
        try {
          getBotForNeedUri(con.getNeedURI()).onOpenFromOtherNeed(con, wonMessage);
        } catch (Exception e) {
          logger.warn("error while handling onOpen()",e);
        }
      }
    }, new Date());
  }

  @Override
  public void onTextMessage(final Connection con,
                            final Model content, final WonMessage wonMessage) {
    taskScheduler.schedule(new Runnable(){
      public void run(){
        try {
          logger.debug("onTextMessage for Connection {} ",con.getConnectionURI());
          getBotForNeedUri(con.getNeedURI()).onMessageFromOtherNeed(con, wonMessage);
        } catch (Exception e) {
          logger.warn("error while handling onTextMessage()",e);
        }
      }
    }, new Date());
  }

  @Override
  public void onSuccessMessage(final URI successfulMessageUri, final WonMessage wonMessage) {
    taskScheduler.schedule(new Runnable(){
      public void run(){
        try {
          logger.debug("onSuccessResponse for message {} ",successfulMessageUri);
          URI needUri = wonMessage.getSenderNeedURI();
          getBotForNeedUri(needUri).onSuccessResponse(successfulMessageUri, wonMessage);
        } catch (Exception e) {
          logger.warn("error while handling onSuccessResponse()",e);
        }
      }
    }, new Date());
  }

  @Override
  public void onFailureMessage(final URI failedMessageUri, final WonMessage wonMessage) {
    taskScheduler.schedule(new Runnable(){
      public void run(){
        try {
          logger.debug("onFailureResponse for message {} ",failedMessageUri);
          URI needUri = wonMessage.getSenderNeedURI();
          getBotForNeedUri(needUri).onFailureResponse(failedMessageUri, wonMessage);
        } catch (Exception e) {
          logger.warn("error while handling onFailureResponse()",e);
        }
      }
    }, new Date());
  }


  public void setBotManager(BotManager botManager) {
    this.botManager = botManager;
  }

  public void setTaskScheduler(TaskScheduler taskScheduler) {
    this.taskScheduler = taskScheduler;
  }


  private Bot getBotForNeedUri(URI needUri) {
    Bot bot = botManager.getBotForNeedURI(needUri);
    if (bot == null) throw new IllegalStateException("No bot registered for uri " + needUri);
    if (!bot.getLifecyclePhase().isActive()) {
      throw new IllegalStateException("bot responsible for need " + needUri + " is not active (lifecycle phase is: " +bot.getLifecyclePhase()+")");
    }
    return bot;
  }
}
