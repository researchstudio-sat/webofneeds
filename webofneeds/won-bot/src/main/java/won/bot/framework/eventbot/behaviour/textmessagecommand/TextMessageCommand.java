package won.bot.framework.eventbot.behaviour.textmessagecommand;

import won.protocol.model.Connection;

import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextMessageCommand {
    private final Pattern pattern;
    private final String commandSyntax;
    private final String infoMessage;
    private final BiConsumer<Connection, Matcher> action;

    public TextMessageCommand(String commandSyntax, String infoMessage, Pattern pattern,
                    BiConsumer<Connection, Matcher> action) {
        this.pattern = pattern;
        this.commandSyntax = commandSyntax;
        this.infoMessage = infoMessage;
        this.action = action;
    }

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
            throw new UnsupportedOperationException("TextMessageCommand is non executable, no BiConsumer action set");
        }
    }

    @Override
    public String toString() {
        return "`" + commandSyntax + "`: \t\t" + infoMessage;
    }
}