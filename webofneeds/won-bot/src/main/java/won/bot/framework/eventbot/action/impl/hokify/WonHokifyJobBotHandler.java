package won.bot.framework.eventbot.action.impl.hokify;

import java.util.Collection;
import java.util.Map;
import java.util.function.BiConsumer;

import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.bots.AbsSender;
import org.telegram.telegrambots.bots.commands.BotCommand;
import org.telegram.telegrambots.bots.commands.CommandRegistry;
import org.telegram.telegrambots.bots.commands.ICommandRegistry;

import won.bot.framework.eventbot.action.impl.hokify.util.HokifyMessageGenerator;
import won.bot.framework.eventbot.action.impl.telegram.Commands.HelpBotCommand;
import won.bot.framework.eventbot.bus.EventBus;

/**
 * @author MS Handle messagest and requests to hokify in the future
 */
public class WonHokifyJobBotHandler implements ICommandRegistry {
    private String token;
    private String botName;
    private EventBus bus;
    private CommandRegistry commandRegistry;
    private HokifyMessageGenerator hokifyMessageGenerator;

    public WonHokifyJobBotHandler(EventBus bus, HokifyMessageGenerator hokifyMessageGenerator, String botName,
                    String token) {
        this.bus = bus;
        this.token = token;
        this.botName = botName;
        this.hokifyMessageGenerator = hokifyMessageGenerator;
        this.commandRegistry = new CommandRegistry(true, botName);
        // BotCommand offerBotCommand = new OfferBotCommand("offer", "create a offer
        // need", bus);
        // BotCommand demandBotCommand = new DemandBotCommand("demand", "create a demand
        // need", bus);
        // BotCommand critiqueBotCommand = new CritiqueBotCommand("critique", "create a
        // critique need", bus);
        // BotCommand togetherBotCommand = new TogetherBotCommand("together", "create a
        // together need", bus);
        BotCommand helpBotCommand = new HelpBotCommand("help", "list help", bus);
        // commandRegistry.registerAll(helpBotCommand, offerBotCommand,
        // demandBotCommand, critiqueBotCommand, togetherBotCommand);
        commandRegistry.registerAll(helpBotCommand);
    }

    public CommandRegistry getCommandRegistry() {
        return commandRegistry;
    }

    public HokifyMessageGenerator getHokifyMessageGenerator() {
        return this.hokifyMessageGenerator;
    }

    public void setHokifyMessageGenerator(HokifyMessageGenerator hokifyMessageGenerator) {
        this.hokifyMessageGenerator = hokifyMessageGenerator;
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

    public Message sendMessage(SendMessage connectMessage) {
        // TODO Auto-generated method stub
        System.out.println("---------------------------------------------- send message: " + connectMessage);
        return null;
    }
}
