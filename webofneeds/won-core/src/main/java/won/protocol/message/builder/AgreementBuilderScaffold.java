package won.protocol.message.builder;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import won.protocol.message.WonMessage;
import won.protocol.vocabulary.WONAGR;

public class AgreementBuilderScaffold<THIS extends AgreementBuilderScaffold<THIS, PARENT>, PARENT extends BuilderScaffold<PARENT, ?>>
                extends BuilderScaffold<THIS, PARENT> {
    public AgreementBuilderScaffold(PARENT parent) {
        super(parent);
    }

    /**
     * Propose the specified messages by their URIs.
     * 
     * @param other the message URI to propose
     * @return the parent builder
     */
    public PARENT proposes(URI... other) {
        builder.addToMessageResource(WONAGR.proposes, other);
        return parent.get();
    }

    /**
     * Accept the specified messages by their URIs.
     * 
     * @param other the message URIs to accept
     * @return the parent builder
     */
    public PARENT accepts(URI... other) {
        builder.addToMessageResource(WONAGR.accepts, other);
        return parent.get();
    }

    /**
     * Claim the specified messages by their URIs.
     * 
     * @param other the message URIs to propose
     * @return the parent builder
     */
    public PARENT claims(URI... other) {
        builder.addToMessageResource(WONAGR.claims, other);
        return parent.get();
    }

    /**
     * Propose to cancel the agreement(s) identified by the specified URIs.
     * 
     * @param other the message URIs to propose to cancel
     * @return the parent builder
     */
    public PARENT proposesToCancel(URI... other) {
        builder.addToMessageResource(WONAGR.proposesToCancel, other);
        return parent.get();
    }

    /**
     * Reject the specified messages by their URIs.
     * 
     * @param other the message URIs to reject
     * @return the parent builder
     */
    public PARENT rejects(URI... other) {
        builder.addToMessageResource(WONAGR.rejects, other);
        return parent.get();
    }

    /**
     * Propose the specified messages.
     * 
     * @param other the messages to propose
     * @return the parent builder
     */
    public PARENT proposes(WonMessage... other) {
        builder.addToMessageResource(WONAGR.proposes, mapMessageURIs(other));
        return parent.get();
    }

    /**
     * Accept the specified messages.
     * 
     * @param other the messages to accept
     * @return the parent builder
     */
    public PARENT accepts(WonMessage... other) {
        builder.addToMessageResource(WONAGR.accepts, mapMessageURIs(other));
        return parent.get();
    }

    /**
     * Claim the specified messages.
     * 
     * @param other the messages to claim
     * @return the parent builder
     */
    public PARENT claims(WonMessage... other) {
        builder.addToMessageResource(WONAGR.claims, mapMessageURIs(other));
        return parent.get();
    }

    /**
     * Propose to cancel the specified messages.
     * 
     * @param other the messages to propose to cancel
     * @return the parent builder
     */
    public PARENT proposesToCancel(WonMessage... other) {
        builder.addToMessageResource(WONAGR.proposesToCancel, mapMessageURIs(other));
        return parent.get();
    }

    /**
     * Reject the specified messages.
     * 
     * @param other the messages to reject
     * @return the parent builder
     */
    public PARENT rejects(WonMessage... other) {
        builder.addToMessageResource(WONAGR.rejects, mapMessageURIs(other));
        return parent.get();
    }

    List<URI> mapMessageURIs(WonMessage... other) {
        return Arrays.asList(other).stream().map(m -> m.getMessageURIRequired()).collect(Collectors.toList());
    }
}
