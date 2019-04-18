package won.protocol.message;

import org.apache.jena.rdf.model.Resource;

import won.protocol.vocabulary.WONMSG;

public enum ResponseState {
    SUCCESS(WONMSG.SuccessResponseState), FAILURE(WONMSG.FailureResponseState),
    DUPLICATE_ATOM_ID(WONMSG.DuplicateAtomIdResponseState),
    DUPLICATE_CONNECTION_ID(WONMSG.DuplicateConnectionIdResponseState),
    DUPLICATE_MESSAGE_ID(WONMSG.DuplicateMessageIdResponseState);
    private Resource resource;

    private ResponseState(Resource resource) {
        this.resource = resource;
    }

    public Resource getResource() {
        return resource;
    }

    public static ResponseState getResponseState(Resource resource) {
        if (WONMSG.SuccessResponseState.equals(resource))
            return SUCCESS;
        if (WONMSG.FailureResponseState.equals(resource))
            return FAILURE;
        if (WONMSG.DuplicateAtomIdResponseState.equals(resource))
            return DUPLICATE_ATOM_ID;
        if (WONMSG.DuplicateConnectionIdResponseState.equals(resource))
            return DUPLICATE_CONNECTION_ID;
        if (WONMSG.DuplicateMessageIdResponseState.equals(resource))
            return DUPLICATE_MESSAGE_ID;
        return null;
    }
}
