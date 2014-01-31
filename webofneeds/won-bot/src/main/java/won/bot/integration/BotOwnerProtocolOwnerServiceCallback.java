package won.bot.integration;

import com.hp.hpl.jena.rdf.model.Model;
import org.springframework.scheduling.TaskScheduler;
import won.bot.framework.core.Bot;
import won.bot.framework.manager.BotManager;
import won.owner.service.OwnerProtocolOwnerServiceCallback;
import won.protocol.model.ChatMessage;
import won.protocol.model.Connection;
import won.protocol.model.Match;

import java.net.URI;
import java.util.Date;

/**
 * OwnerProtocolOwnerServiceCallback that dispatches the calls to the bots.
 */
public class BotOwnerProtocolOwnerServiceCallback implements OwnerProtocolOwnerServiceCallback
{
  BotManager botManager;

  TaskScheduler taskScheduler;

  @Override
  public void onClose(final Connection con, final Model content) {
    taskScheduler.schedule(new Runnable(){
      public void run(){
        try {
          getBotForNeedUri(con.getNeedURI()).onCloseFromOtherNeed(con, content);
        } catch (Exception e) {
          e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
      }
    }, new Date());
  }

  @Override
  public void onHint(final Match match, final Model content) {
    taskScheduler.schedule(new Runnable(){
      public void run(){
        try {
          getBotForNeedUri(match.getFromNeed()).onHintFromMatcher(match, content);
        } catch (Exception e) {
          e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
      }
    }, new Date());
  }

  @Override
  public void onConnect(final Connection con, final Model content) {
    taskScheduler.schedule(new Runnable(){
      public void run(){
        try {
          getBotForNeedUri(con.getNeedURI()).onConnectFromOtherNeed(con, content);
        } catch (Exception e) {
          e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
      }
    }, new Date());
  }

  @Override
  public void onOpen(final Connection con, final Model content) {
    taskScheduler.schedule(new Runnable(){
      public void run(){
        try {
          getBotForNeedUri(con.getNeedURI()).onOpenFromOtherNeed(con, content);
        } catch (Exception e) {
          e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
      }
    }, new Date());
  }

  @Override
  public void onTextMessage(final Connection con, final ChatMessage message, final Model content) {
    taskScheduler.schedule(new Runnable(){
      public void run(){
        try {
          getBotForNeedUri(con.getNeedURI()).onMessageFromOtherNeed(con, message, content);
        } catch (Exception e) {
          e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
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
    Bot bot = botManager.getBot(needUri);
    if (bot == null) throw new IllegalStateException("No bot registered for uri " + needUri);
    return bot;
  }
}
