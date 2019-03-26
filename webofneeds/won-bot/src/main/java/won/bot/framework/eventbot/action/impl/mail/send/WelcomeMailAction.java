package won.bot.framework.eventbot.action.impl.mail.send;

import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;

import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.mail.WelcomeMailEvent;
import won.bot.framework.eventbot.listener.EventListener;

/**
 * Created by hfriedrich on 16.11.2016.
 */
public class WelcomeMailAction extends BaseEventBotAction {
  private MessageChannel sendChannel;
  private WonMimeMessageGenerator mailGenerator;

  public WelcomeMailAction(WonMimeMessageGenerator mailGenerator, MessageChannel sendChannel) {

    super(mailGenerator.getEventListenerContext());
    this.sendChannel = sendChannel;
    this.mailGenerator = mailGenerator;
  }

  @Override
  protected void doRun(final Event event, EventListener executingListener) throws Exception {
    if (event instanceof WelcomeMailEvent) {
      WonMimeMessage welcomeMessage = mailGenerator.createWelcomeMail(((WelcomeMailEvent) event).getMessage());
      sendChannel.send(new GenericMessage<>(welcomeMessage));
    }
  }
}
