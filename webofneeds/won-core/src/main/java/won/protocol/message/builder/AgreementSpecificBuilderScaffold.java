package won.protocol.message.builder;

interface AgreementSpecificBuilderScaffold<THIS extends AgreementSpecificBuilderScaffold<?>> {
    /**
     * Allows for making agreements and claims according to the Agreement Protocol.
     * 
     * @return the agreement builder
     */
    public abstract AgreementBuilderScaffold<?, ?> agreement();
}