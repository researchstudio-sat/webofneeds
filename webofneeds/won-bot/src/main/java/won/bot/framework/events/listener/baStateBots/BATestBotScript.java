package won.bot.framework.events.listener.baStateBots;

import java.util.Iterator;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Danijel
 * Date: 5.3.14.
 * Time: 12.35
 * To change this template use File | Settings | File Templates.
 */
public abstract class BATestBotScript {
    private List<BATestScriptAction> actions;
    private Iterator<BATestScriptAction> iterator;

    public BATestBotScript() {
        this.actions = setupActions();
        this.iterator = actions.iterator();
    }

    protected abstract List<BATestScriptAction> setupActions();

    public BATestScriptAction getNextAction() {
        return iterator.next();
    }

    public boolean hasNext(){
        return iterator.hasNext();
    }

}
