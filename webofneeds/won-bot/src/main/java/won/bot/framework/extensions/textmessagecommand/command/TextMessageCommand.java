package won.bot.framework.extensions.textmessagecommand.command;

/**
 * Abstract superclass to define certain TextMessageCommand matcher
 */
public abstract class TextMessageCommand {
    private final String commandSyntax;
    private final String infoMessage;

    /**
     * @param commandSyntax syntax string for getUsageCommandMessage
     * @param infoMessage info string for getUsageCommandMessage
     */
    TextMessageCommand(String commandSyntax, String infoMessage) {
        this.commandSyntax = commandSyntax;
        this.infoMessage = infoMessage;
    }

    /**
     * String to visualize the syntax and information of the TextMessageCommand
     * 
     * @return `[commandSyntax]`: [infoMessage]
     */
    public String getUsageCommandMessage() {
        return "`" + commandSyntax + "`: \t\t" + infoMessage;
    }

    public abstract boolean matchesCommand(String message);
}