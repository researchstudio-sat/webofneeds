package won.bot.framework.extensions.textmessagecommand.command;

import won.protocol.model.Connection;

import java.util.function.BiConsumer;

/**
 * TextMessageCommand that checks if a message startsWith a certain String
 */
public final class StartsWithTextMessageCommand extends TextMessageCommand {
    private final String startsWithString;
    private final boolean trim;
    private final BiConsumer<Connection, String> action;

    /**
     * Creates a TextMessageCommand that checks if messages startWith the given
     * startsWithString (with trimming whitespace)
     * 
     * @param commandSyntax syntax string for super.getUsageCommandMessage()
     * @param infoMessage info string for super.getUsageCommandMessage()
     * @param startsWithString commandString to check
     * @param action to execute if matchesCommmand is true
     */
    public StartsWithTextMessageCommand(String commandSyntax, String infoMessage, String startsWithString,
                    BiConsumer<Connection, String> action) {
        this(commandSyntax, infoMessage, startsWithString, true, action);
    }

    /**
     * Creates a TextMessageCommand that checks if messages startWith the given
     * startsWithString (trim whitespace depending on trim parameter)
     * 
     * @param commandSyntax syntax string for super.getUsageCommandMessage()
     * @param infoMessage info string for super.getUsageCommandMessage()
     * @param startsWithString commandString to check
     * @param trim if whitespace should be trimmed or not
     * @param action to execute if matchesCommmand is true
     */
    public StartsWithTextMessageCommand(String commandSyntax, String infoMessage, String startsWithString, boolean trim,
                    BiConsumer<Connection, String> action) {
        super(commandSyntax, infoMessage);
        this.startsWithString = startsWithString;
        this.trim = trim;
        this.action = action;
    }

    /**
     * Check if message startsWith the startsWithString
     * 
     * @param message message to check
     * @return true if the message startsWith the startsWithString (trim depending
     * on the state of the trim flag)
     */
    @Override
    public boolean matchesCommand(String message) {
        return message != null
                        && (trim ? message.trim().startsWith(startsWithString) : message.startsWith(startsWithString));
    }

    /**
     * Executes the Consumer method stored in action
     * 
     * @param con connection that the message was sent in
     * @param message the whole message that matched the startsWithString so it is
     * possible to extract parts of the message string within the Consumer if need
     * be
     */
    public void execute(Connection con, String message) {
        if (this.action != null) {
            this.action.accept(con, message);
        } else {
            throw new UnsupportedOperationException(
                            "StartsWithTextMessageCommand is non executable, no BiConsumer action set");
        }
    }
}
