package won.protocol.message.builder;

interface ModificationSpecificBuilderScaffold<THIS extends ModificationSpecificBuilderScaffold<?>> {
    /**
     * Allows for modifying the conversation history according to the modifcation
     * Protocol.
     * 
     * @return the modification builder
     */
    public abstract ModificationBuilderScaffold<?, ?> modification();
}