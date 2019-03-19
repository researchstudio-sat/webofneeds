package won.bot.framework.eventbot.listener.baStateBots;

/**
 * User: Danijel Date: 17.4.14.
 */
public class NopAction extends BATestScriptAction {
    public NopAction() {
        super(false, "", null);
    }

    @Override
    public boolean isNopAction() {
        return true;
    }

    public String toString() {
        return getClass().getSimpleName() + "@" + hashCode();
    }
}
