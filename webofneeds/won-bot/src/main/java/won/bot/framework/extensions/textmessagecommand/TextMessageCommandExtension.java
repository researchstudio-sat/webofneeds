package won.bot.framework.extensions.textmessagecommand;

import won.bot.framework.eventbot.filter.impl.NotFilter;

import java.util.Objects;

@FunctionalInterface
public interface TextMessageCommandExtension {
    /**
     * The Behaviour defining this extension. For an example, see
     * TextMessageCommandBehaviour
     */
    TextMessageCommandBehaviour getTextMessageCommandBehaviour();

    /**
     * Initializes and returns a NotFilter that can be used to exclude Events that
     * match any of the commands defined in the TextMessageCommandBehaviour
     * 
     * @return NotFilter that excludes all textMessageCommand messages
     * @throws IllegalStateException if TextMessageCommandBehaviour is null, and
     * therefore a Filter cant be Created
     */
    default NotFilter getNoTextMessageCommandFilter() throws IllegalStateException {
        if (Objects.nonNull(getTextMessageCommandBehaviour())) {
            return new NotFilter(
                            new TextMessageCommandFilter(getTextMessageCommandBehaviour().getEventListenerContext(),
                                            getTextMessageCommandBehaviour()));
        } else {
            throw new IllegalStateException("Can't create Filter, TextMessageCommandBehaviour is null");
        }
    };
}
