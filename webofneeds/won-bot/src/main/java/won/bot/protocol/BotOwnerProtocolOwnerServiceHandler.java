package won.bot.protocol;

import org.springframework.scheduling.TaskScheduler;
import won.bot.core.Bot;
import won.bot.registry.BotRegistry;
import won.owner.service.OwnerProtocolOwnerServiceHandler;
import won.protocol.model.ChatMessage;
import won.protocol.model.Connection;
import won.protocol.model.Match;

import java.net.URI;
import java.util.Date;

/**
 * OwnerProtocolOwnerServiceHandler that dispatches the calls to the bots.
 */
public class BotOwnerProtocolOwnerServiceHandler implements OwnerProtocolOwnerServiceHandler {
  BotRegistry botRegistry;

  TaskScheduler taskScheduler;

  @Override
  public void onClose(final Connection con) {
    taskScheduler.schedule(new Runnable(){
      public void run(){
        try {
          getBotForNeedUri(con.getNeedURI()).onCloseFromOtherNeed(con);
        } catch (Exception e) {
          e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
      }
    }, new Date());
  }

  @Override
  public void onHint(final Match match) {
    taskScheduler.schedule(new Runnable(){
      public void run(){
        try {
          getBotForNeedUri(match.getFromNeed()).onHintFromMatcher(match);
        } catch (Exception e) {
          e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
      }
    }, new Date());
  }

  @Override
  public void onConnect(final Connection con) {
    taskScheduler.schedule(new Runnable(){
      public void run(){
        try {
          getBotForNeedUri(con.getNeedURI()).onConnectFromOtherNeed(con);
        } catch (Exception e) {
          e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
      }
    }, new Date());
  }

  @Override
  public void onOpen(final Connection con) {
    taskScheduler.schedule(new Runnable(){
      public void run(){
        try {
          getBotForNeedUri(con.getNeedURI()).onOpenFromOtherNeed(con);
        } catch (Exception e) {
          e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
      }
    }, new Date());
  }

  @Override
  public void onTextMessage(final Connection con, final ChatMessage message) {
    taskScheduler.schedule(new Runnable(){
      public void run(){
        try {
          getBotForNeedUri(con.getNeedURI()).onMessageFromOtherNeed(con, message);
        } catch (Exception e) {
          e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
      }
    }, new Date());
  }

  public void setBotRegistry(BotRegistry botRegistry) {
    this.botRegistry = botRegistry;
  }

  public void setTaskScheduler(TaskScheduler taskScheduler) {
    this.taskScheduler = taskScheduler;
  }


  private Bot getBotForNeedUri(URI needUri) {
    Bot bot = botRegistry.getBot(needUri);
    if (bot == null) throw new IllegalStateException("No bot registered for uri " + needUri);
    return bot;
  }
}
