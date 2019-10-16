package won.bot.framework.extensions.textmessagecommand.command;

import won.protocol.model.Connection;

import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TextMessageCommand that checks if a message matches to a regex-pattern
 */
public final class PatternMatcherTextMessageCommand extends TextMessageCommand {
    private final Pattern pattern;
    private final BiConsumer<Connection, Matcher> action;

    /**
     * Creates a TextMessageCommand that checks if messages match the given
     * regex-pattern
     * 
     * @param commandSyntax syntax string for super.getUsageCommandMessage()
     * @param infoMessage info string for super.getUsageCommandMessage()
     * @param pattern regex pattern to check
     * @param action to execute if matchesCommmand is true
     */
    public PatternMatcherTextMessageCommand(String commandSyntax, String infoMessage, Pattern pattern,
                    BiConsumer<Connection, Matcher> action) {
        super(commandSyntax, infoMessage);
        this.pattern = pattern;
        this.action = action;
    }

    /**
     * Check if message matches the pattern
     * 
     * @param message message to check
     * @return true if the message matches the pattern
     */
    @Override
    public boolean matchesCommand(String message) {
        return message != null && pattern.matcher(message).matches();
    }

    public Matcher getMatcher(String message) {
        return pattern.matcher(message);
    }

    public void execute(Connection con, Matcher matcher) {
        if (this.action != null) {
            this.action.accept(con, matcher);
        } else {
            throw new UnsupportedOperationException(
                            "PatternMatcherTextMessageCommand is non executable, no BiConsumer action set");
        }
    }
}
