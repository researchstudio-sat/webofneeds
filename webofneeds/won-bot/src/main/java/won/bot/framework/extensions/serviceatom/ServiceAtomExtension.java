package won.bot.framework.extensions.serviceatom;

import won.bot.framework.eventbot.filter.impl.NotFilter;

import java.util.Objects;

@FunctionalInterface
public interface ServiceAtomExtension {
    /**
     * The Behaviour defining this extension. For an example, see
     * ServiceAtomBehaviour
     */
    ServiceAtomBehaviour getServiceAtomBehaviour();

    /**
     * Initializes and returns a NotFilter that can be used to exclude Events that
     * are called between the ServiceAtom and another owned Atom
     *
     * @return NotFilter that excludes all ServiceAtomRelated messages
     * @throws IllegalStateException if getServiceAtomBehaviour is null, and
     * therefore a Filter cant be Created
     */
    default NotFilter getNoInternalServiceAtomEventFilter() throws IllegalStateException {
        if (Objects.nonNull(getServiceAtomBehaviour())) {
            return new NotFilter(new ServiceAtomRelatedFilter(getServiceAtomBehaviour().getEventListenerContext()));
        } else {
            throw new IllegalStateException("Can't create Filter, ServiceAtomBehaviour is null");
        }
    };
}
