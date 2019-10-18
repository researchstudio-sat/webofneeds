package won.bot.framework.extensions.textmessagecommand;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.impl.wonmessage.SendMessageAction;
import won.bot.framework.eventbot.behaviour.BotBehaviour;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.impl.wonmessage.MessageFromOtherAtomEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.OpenFromOtherAtomEvent;
import won.bot.framework.eventbot.filter.EventFilter;
import won.bot.framework.extensions.textmessagecommand.command.PatternMatcherTextMessageCommand;
import won.bot.framework.extensions.textmessagecommand.command.TextMessageCommand;
import won.protocol.model.Connection;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Behaviour that checks for Matching command Strings, within a
 * OpenFromOtherAtomEvent or MessageFromOtherAtomEvent Can be filtered to limit
 * the Behaviour on certain event publications Adds a default "Usage" command
 * that sends a message with all possible commands within this behaviour
 */
public class TextMessageCommandBehaviour extends BotBehaviour {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final List<TextMessageCommand> commands;
    private final String usageCommandMessage;
    private final EventFilter eventFilter;

    /**
     * @param context eventListenerContext
     * @param commands set of possible Commands
     */
    public TextMessageCommandBehaviour(EventListenerContext context, TextMessageCommand... commands) {
        this(context, (EventFilter) null, commands);
    }

    /**
     * @param context eventListenerContext
     * @param eventFilter filters event subscriptions so that the behaviour is only
     * active for certain event publications
     * @param commands set of possible Commands
     */
    public TextMessageCommandBehaviour(EventListenerContext context, EventFilter eventFilter,
                    TextMessageCommand... commands) {
        super(context);
        final EventBus bus = context.getEventBus();
        PatternMatcherTextMessageCommand usageCommand = new PatternMatcherTextMessageCommand("usage",
                        "display this message",
                        Pattern.compile("^usage|\\?|help|debug$", Pattern.CASE_INSENSITIVE),
                        (Connection connection, Matcher matcher) -> bus.publish(new UsageCommandEvent(connection)));
        this.commands = new ArrayList<>();
        this.commands.add(usageCommand);
        this.commands.addAll(Arrays.asList(commands));
        this.eventFilter = eventFilter;
        this.usageCommandMessage = getUsageCommandMessage();
    }

    /**
     * @param context eventListenerContext
     * @param name given name so we can identify the behaviour in logging (useful if
     * more than one behaviour of this type is active)
     * @param commands set of possible Commands
     */
    public TextMessageCommandBehaviour(EventListenerContext context, String name, TextMessageCommand... commands) {
        this(context, name, null, commands);
    }

    /**
     * @param context eventListenerContext
     * @param name given name so we can identify the behaviour in logging (useful if
     * more than one behaviour of this type is active)
     * @param eventFilter filters event subscriptions so that the behaviour is only
     * active for certain event publications
     * @param commands set of possible Commands
     */
    public TextMessageCommandBehaviour(EventListenerContext context, String name, EventFilter eventFilter,
                    TextMessageCommand... commands) {
        super(context, name);
        final EventBus bus = context.getEventBus();
        PatternMatcherTextMessageCommand usageCommand = new PatternMatcherTextMessageCommand("usage",
                        "display this message",
                        Pattern.compile("^usage|\\?|help|debug$", Pattern.CASE_INSENSITIVE),
                        (Connection connection, Matcher matcher) -> bus.publish(new UsageCommandEvent(connection)));
        this.commands = new ArrayList<>();
        this.commands.add(usageCommand);
        this.commands.addAll(Arrays.asList(commands));
        this.usageCommandMessage = getUsageCommandMessage();
        this.eventFilter = eventFilter;
    }

    @Override
    protected void onActivate(Optional<Object> optional) {
        logger.debug("activating TextMessageCommandBehaviour");
        EventListenerContext ctx = this.context;
        EventBus bus = ctx.getEventBus();
        TextMessageCommandExecutor textMessageUsageCommandChecker = new TextMessageCommandExecutor(context, commands);
        this.subscribeWithAutoCleanup(OpenFromOtherAtomEvent.class, eventFilter, textMessageUsageCommandChecker);
        this.subscribeWithAutoCleanup(MessageFromOtherAtomEvent.class, eventFilter, textMessageUsageCommandChecker);
        // react to usage command event
        this.subscribeWithAutoCleanup(UsageCommandEvent.class, eventFilter,
                        new SendMessageAction(ctx, this.usageCommandMessage));
    }

    private String getUsageCommandMessage() {
        StringBuilder usageCommandsString = new StringBuilder("# Usage:\n");
        for (TextMessageCommand command : commands) {
            usageCommandsString
                            .append("* ")
                            .append(command.getUsageCommandMessage())
                            .append("\n");
        }
        return usageCommandsString.toString();
    }

    public boolean isMatchingBotCommand(String message) {
        if (message != null) {
            for (TextMessageCommand command : commands) {
                if (command.matchesCommand(message)) {
                    return true;
                }
            }
        }
        return false;
    }
}
