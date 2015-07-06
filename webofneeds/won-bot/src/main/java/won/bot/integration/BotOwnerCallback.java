package won.bot.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import won.bot.framework.bot.Bot;
import won.bot.framework.manager.BotManager;
import won.owner.protocol.message.OwnerCallback;
import won.protocol.message.WonMessage;
import won.protocol.model.Connection;
import won.protocol.model.Match;

import java.net.URI;
import java.util.Date;

/**
 * OwnerProtocolOwnerServiceCallback that dispatches the calls to the bots.
 */
@Qualifier("default")
public class BotOwnerCallback implements OwnerCallback
{
  private final Logger logger = LoggerFactory.getLogger(getClass());
  BotManager botManager;

  TaskScheduler taskScheduler;



  @Override
  public void onCloseFromOtherNeed(final Connection con, final WonMessage wonMessage) {
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
  public void onHintFromMatcher(final Match match, final WonMessage wonMessage) {
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
  public void onConnectFromOtherNeed(final Connection con, final WonMessage wonMessage) {
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
  public void onOpenFromOtherNeed(final Connection con, final WonMessage wonMessage) {
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
  public void onMessageFromOtherNeed(final Connection con, final WonMessage wonMessage) {
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
  public void onSuccessResponse(final URI successfulMessageUri, final WonMessage wonMessage) {
    taskScheduler.schedule(new Runnable(){
      public void run(){
        try {
          logger.debug("onSuccessResponse for message {} ",successfulMessageUri);
          URI needUri = wonMessage.getReceiverNeedURI();
          getBotForNeedUri(needUri).onSuccessResponse(successfulMessageUri, wonMessage);
        } catch (Exception e) {
          logger.warn("error while handling onSuccessResponse()",e);
        }
      }
    }, new Date());
  }

  @Override
  public void onFailureResponse(final URI failedMessageUri, final WonMessage wonMessage) {
    taskScheduler.schedule(new Runnable(){
      public void run(){
        try {
          logger.debug("onFailureResponse for message {} ",failedMessageUri);
          URI needUri = wonMessage.getReceiverNeedURI();
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
