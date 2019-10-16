package won.bot.framework.extensions.textmessagecommand.command;

import won.protocol.model.Connection;

import java.util.function.Consumer;

/**
 * TextMessageCommand that checks if a message was equal to a certain String
 */
public final class EqualsTextMessageCommand extends TextMessageCommand {
    private final String equalsString;
    private final boolean ignoreCase;
    private final Consumer<Connection> action;

    /**
     * Creates a TextMessageCommand that checks for exact match between messages and
     * the given equalsString (without ignoring the case)
     * 
     * @param commandSyntax syntax string for super.getUsageCommandMessage()
     * @param infoMessage info string for super.getUsageCommandMessage()
     * @param equalsString commandString to check
     * @param action to execute if matchesCommmand is true
     */
    public EqualsTextMessageCommand(String commandSyntax, String infoMessage, String equalsString,
                    Consumer<Connection> action) {
        this(commandSyntax, infoMessage, equalsString, false, action);
    }

    /**
     * Creates a TextMessageCommand that checks for exact match between messages and
     * the given equalsString (ignore case depends on ignoreCase parameter)
     * 
     * @param commandSyntax syntax string for super.getUsageCommandMessage()
     * @param infoMessage info string for super.getUsageCommandMessage()
     * @param equalsString commandString to check
     * @param ignoreCase if case should be ignored
     * @param action to execute if matchesCommmand is true
     */
    public EqualsTextMessageCommand(String commandSyntax, String infoMessage, String equalsString, boolean ignoreCase,
                    Consumer<Connection> action) {
        super(commandSyntax, infoMessage);
        this.equalsString = equalsString;
        this.ignoreCase = ignoreCase;
        this.action = action;
    }

    /**
     * Check if message matches the command
     * 
     * @param message message to check
     * @return true if the message equals the equalsString (depending on the
     * ignoreCase flag)
     */
    @Override
    public boolean matchesCommand(String message) {
        return message != null && (ignoreCase ? message.equalsIgnoreCase(equalsString) : message.equals(equalsString));
    }

    /**
     * Executes the Consumer method stored in action
     * 
     * @param con connection that the message was sent in
     */
    public void execute(Connection con) {
        if (this.action != null) {
            this.action.accept(con);
        } else {
            throw new UnsupportedOperationException(
                            "EqualsTextMessageCommand is non executable, no BiConsumer action set");
        }
    }
}
