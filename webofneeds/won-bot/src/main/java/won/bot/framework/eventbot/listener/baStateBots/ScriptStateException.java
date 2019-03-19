package won.bot.framework.eventbot.listener.baStateBots;

/**
 * Created with IntelliJ IDEA. User: Danijel Date: 5.3.14. Time: 17.40 To change this template use File | Settings |
 * File Templates.
 */
public class ScriptStateException extends Exception {
    BATestScriptAction action = null;

    public ScriptStateException(BATestScriptAction action, String message) {
        super(message);
        this.action = action;
    }

    public BATestScriptAction getAction() {
        return action;
    }

}
