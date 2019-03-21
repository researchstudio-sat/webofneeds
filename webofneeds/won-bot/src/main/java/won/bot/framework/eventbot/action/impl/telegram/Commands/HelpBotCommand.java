package won.bot.framework.eventbot.action.impl.telegram.Commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.api.objects.Chat;
import org.telegram.telegrambots.api.objects.User;
import org.telegram.telegrambots.bots.AbsSender;
import org.telegram.telegrambots.bots.commands.BotCommand;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.impl.telegram.SendHelpEvent;

/**
 * Created by fsuda on 15.12.2016.
 */
public class HelpBotCommand extends BotCommand {
  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private EventBus bus;

  public HelpBotCommand(String commandIdentifier, String description, EventBus bus) {
    super(commandIdentifier, description);
    this.bus = bus;
  }

  @Override public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
    bus.publish(new SendHelpEvent(absSender, user, chat, strings));
  }
}
