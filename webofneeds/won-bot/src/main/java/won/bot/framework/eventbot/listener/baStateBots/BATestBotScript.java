package won.bot.framework.eventbot.listener.baStateBots;

import java.util.Iterator;
import java.util.List;

/**
 * Created with IntelliJ IDEA. User: Danijel Date: 5.3.14. Time: 12.35 To change this template use File | Settings |
 * File Templates.
 */
public abstract class BATestBotScript {
    private List<BATestScriptAction> actions;
    private Iterator<BATestScriptAction> iterator;
    private String name;

    public BATestBotScript(String name) {
        this.actions = setupActions();
        this.iterator = actions.iterator();
        this.name = name;
    }

    protected BATestBotScript() {
        this("");
        this.name = (getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()));
    }

    protected abstract List<BATestScriptAction> setupActions();

    public BATestScriptAction getNextAction() {
        return iterator.next();
    }

    public boolean hasNext() {
        return iterator.hasNext();
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }
}
