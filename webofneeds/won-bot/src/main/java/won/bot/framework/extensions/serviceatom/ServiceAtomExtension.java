package won.bot.framework.extensions.serviceatom;

@FunctionalInterface
public interface ServiceAtomExtension {
    /**
     * The Behaviour defining this extension. For an example, see
     * ServiceAtomBehaviour
     */
    ServiceAtomBehaviour getServiceAtomBehaviour();
}
