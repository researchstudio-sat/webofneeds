package won.bot.core.impl;

import com.hp.hpl.jena.rdf.model.Model;
import won.bot.core.Bot;
import won.bot.core.base.ScheduledActionBot;
import won.protocol.model.ChatMessage;
import won.protocol.model.Connection;
import won.protocol.model.Match;

import java.net.URI;
import java.util.Date;
import java.util.Random;

/**
 * Bot that processes events after a configurable delay.
 */
public class ConfigurableDelayDelegatingBot extends ScheduledActionBot {
  private Bot delegate;
  private Random random = new Random(0);
  private Integer onCloseMinDelay = null;
  private Integer onCloseMaxDelay = null;
  private Integer onConnectMinDelay = null;
  private Integer onConnectMaxDelay = null;
  private Integer onHintMinDelay = null;
  private Integer onHintMaxDelay = null;
  private Integer onMessageMinDelay = null;
  private Integer onMessageMaxDelay = null;
  private Integer onOpenMinDelay = null;
  private Integer onOpenMaxDelay = null;
  private Integer onNewNeedMinDelay = null;
  private Integer onNewNeedMaxDelay = null;
  private Integer actMinDelay = null;
  private Integer actMaxDelay = null;


  @Override
  public void onCloseFromOtherNeed(final Connection con, final Model content) throws Exception {
    getTaskScheduler().schedule(new Runnable() {
      @Override
      public void run() {
        try {
          delegate.onCloseFromOtherNeed(con, content);
        } catch (Exception e) {
          logger.warn("caught exception", e);
        }
      }
    }, calculateDelay(onCloseMinDelay, onCloseMaxDelay));
  }

  @Override
  public void onConnectFromOtherNeed(final Connection con, final Model content) throws Exception {
    getTaskScheduler().schedule(new Runnable() {
      @Override
      public void run() {
        try {
          delegate.onConnectFromOtherNeed(con, content);
        } catch (Exception e) {
          logger.warn("caught exception", e);
        }
      }
    }, calculateDelay(onConnectMinDelay, onConnectMaxDelay));
  }

  @Override
  public void onHintFromMatcher(final Match match, final Model content) throws Exception {
    getTaskScheduler().schedule(new Runnable() {
      @Override
      public void run() {
        try {
          delegate.onHintFromMatcher(match, content);
        } catch (Exception e) {
          logger.warn("caught exception", e);
        }
      }
    }, calculateDelay(onHintMinDelay, onHintMaxDelay));
  }

  @Override
  public void onMessageFromOtherNeed(final Connection con, final ChatMessage message, final Model content) throws Exception {
    getTaskScheduler().schedule(new Runnable() {
      @Override
      public void run() {
        try {
          delegate.onMessageFromOtherNeed(con, message, content);
        } catch (Exception e) {
          logger.warn("caught exception", e);
        }
      }
    }, calculateDelay(onMessageMinDelay, onMessageMaxDelay));
  }

  @Override
  public void onNewNeedCreated(final URI needUri, final URI wonNodeUri, final Model needModel) {
    getTaskScheduler().schedule(new Runnable() {
      @Override
      public void run() {
        try {
          delegate.onNewNeedCreated(needUri, wonNodeUri, needModel);
        } catch (Exception e) {
          logger.warn("caught exception", e);
        }
      }
    }, calculateDelay(onNewNeedMinDelay, onNewNeedMaxDelay));
  }

  @Override
  public void onOpenFromOtherNeed(final Connection con, final Model content) throws Exception {
    getTaskScheduler().schedule(new Runnable() {
      @Override
      public void run() {
        try {
          delegate.onOpenFromOtherNeed(con, content);
        } catch (Exception e) {
          logger.warn("caught exception", e);
        }
      }
    }, calculateDelay(onOpenMinDelay, onOpenMaxDelay));
  }

  public void setDelegate(Bot delegate) {
    this.delegate = delegate;
  }

  /**
   * Calculates the a date in the future, which is set as the soonest execution date for the callbacks.
   * If both values are specified, a value between them is chosen at random, otherwise the specified delay is used.
   * @param minDelay in seconds.
   * @param maxDelay in seconds.
   * @return a Date in the future.
   */
  private Date calculateDelay(Integer minDelay, Integer maxDelay){
    if (minDelay != null && maxDelay != null) {
      int diff = Math.abs(maxDelay - minDelay);
      int min = Math.min(maxDelay, minDelay);
      return new Date(new Date().getTime() + random.nextInt(diff));
    }
    Integer delay = minDelay != null ? minDelay : maxDelay;
    if (delay == null) {
      return new Date();
    }
    return new Date(new Date().getTime() + delay);
  }
}
