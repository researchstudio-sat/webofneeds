package won.bot.framework.eventbot.behaviour;

import java.util.Optional;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.impl.wonmessage.execCommand.ExecuteDeactivateAtomCommandAction;
import won.bot.framework.eventbot.event.impl.command.deactivate.DeactivateAtomCommandEvent;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;

/**
 * Created by fsuda on 17.05.2017.
 */
public class DeactivateAtomBehaviour extends BotBehaviour {
    public DeactivateAtomBehaviour(EventListenerContext context) {
        super(context);
    }

    public DeactivateAtomBehaviour(EventListenerContext context, String name) {
        super(context, name);
    }

    @Override
    protected void onActivate(Optional<Object> message) {
        this.subscribeWithAutoCleanup(DeactivateAtomCommandEvent.class,
                        new ActionOnEventListener(context, new ExecuteDeactivateAtomCommandAction(context)));
    }
}
