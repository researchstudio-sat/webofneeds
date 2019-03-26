package won.bot.framework.eventbot.action.impl.telegram;

import java.util.Collection;
import java.util.Map;
import java.util.function.BiConsumer;

import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.AbsSender;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.bots.commands.BotCommand;
import org.telegram.telegrambots.bots.commands.CommandRegistry;
import org.telegram.telegrambots.bots.commands.ICommandRegistry;

import won.bot.framework.eventbot.action.impl.telegram.Commands.CritiqueBotCommand;
import won.bot.framework.eventbot.action.impl.telegram.Commands.DemandBotCommand;
import won.bot.framework.eventbot.action.impl.telegram.Commands.HelpBotCommand;
import won.bot.framework.eventbot.action.impl.telegram.Commands.OfferBotCommand;
import won.bot.framework.eventbot.action.impl.telegram.Commands.TogetherBotCommand;
import won.bot.framework.eventbot.action.impl.telegram.util.TelegramMessageGenerator;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.impl.telegram.TelegramMessageReceivedEvent;

public class WonTelegramBotHandler extends TelegramLongPollingBot implements ICommandRegistry {
  private String token;
  private String botName;
  private EventBus bus;

  private CommandRegistry commandRegistry;

  private TelegramMessageGenerator telegramMessageGenerator;

  public WonTelegramBotHandler(EventBus bus, TelegramMessageGenerator telegramMessageGenerator, String botName,
      String token) {
    this.bus = bus;
    this.token = token;
    this.botName = botName;
    this.telegramMessageGenerator = telegramMessageGenerator;

    this.commandRegistry = new CommandRegistry(true, botName);

    BotCommand offerBotCommand = new OfferBotCommand("offer", "create a offer need", bus);
    BotCommand demandBotCommand = new DemandBotCommand("demand", "create a demand need", bus);
    BotCommand critiqueBotCommand = new CritiqueBotCommand("critique", "create a critique need", bus);
    BotCommand togetherBotCommand = new TogetherBotCommand("together", "create a together need", bus);
    BotCommand helpBotCommand = new HelpBotCommand("help", "list help", bus);
    commandRegistry.registerAll(helpBotCommand, offerBotCommand, demandBotCommand, critiqueBotCommand,
        togetherBotCommand);
  }

  @Override
  public void onUpdateReceived(Update update) {
    bus.publish(new TelegramMessageReceivedEvent(update));
  }

  @Override
  public String getBotToken() {
    return token;
  }

  @Override
  public String getBotUsername() {
    return botName;
  }

  public CommandRegistry getCommandRegistry() {
    return commandRegistry;
  }

  public TelegramMessageGenerator getTelegramMessageGenerator() {
    return telegramMessageGenerator;
  }

  public void setTelegramMessageGenerator(TelegramMessageGenerator telegramMessageGenerator) {
    this.telegramMessageGenerator = telegramMessageGenerator;
  }

  @Override
  public void registerDefaultAction(BiConsumer<AbsSender, Message> biConsumer) {
    BotCommand helpBotCommand = new HelpBotCommand("help", "list help", bus);
  }

  @Override
  public boolean register(BotCommand botCommand) {
    return commandRegistry.register(botCommand);
  }

  @Override
  public Map<BotCommand, Boolean> registerAll(BotCommand... botCommands) {
    return commandRegistry.registerAll(botCommands);
  }

  @Override
  public boolean deregister(BotCommand botCommand) {
    return commandRegistry.deregister(botCommand);
  }

  @Override
  public Map<BotCommand, Boolean> deregisterAll(BotCommand... botCommands) {
    return commandRegistry.deregisterAll(botCommands);
  }

  @Override
  public Collection<BotCommand> getRegisteredCommands() {
    return commandRegistry.getRegisteredCommands();
  }

  @Override
  public BotCommand getRegisteredCommand(String s) {
    return commandRegistry.getRegisteredCommand(s);
  }
}
