package won.bot.framework.extensions.serviceatom;

import won.bot.framework.eventbot.filter.EventFilter;
import won.bot.framework.eventbot.filter.impl.NotFilter;
import won.bot.framework.extensions.serviceatom.filter.ServiceAtomCreatedAtomRelationFilter;
import won.bot.framework.extensions.serviceatom.filter.ServiceAtomFilter;

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
            return new NotFilter(new ServiceAtomCreatedAtomRelationFilter(
                            getServiceAtomBehaviour().getEventListenerContext()));
        } else {
            throw new IllegalStateException("Can't create Filter, ServiceAtomBehaviour is null");
        }
    };

    /**
     * Initializes and returns a Filter that can be used to exclude Events that do
     * not include the ServiceAtom itself
     *
     * @return EventFilter that excludes all non serviceAtom related events
     * @throws IllegalStateException if getServiceAtomBehaviour is null, and
     * therefore a Filter cant be Created
     */
    default EventFilter getServiceAtomFilter() throws IllegalStateException {
        if (Objects.nonNull(getServiceAtomBehaviour())) {
            return new ServiceAtomFilter(getServiceAtomBehaviour().getEventListenerContext());
        } else {
            throw new IllegalStateException("Can't create Filter, ServiceAtomBehaviour is null");
        }
    }
}
