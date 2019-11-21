package won.protocol.message.builder;

import java.net.URI;
import java.util.Optional;

import won.protocol.exception.MissingMessagePropertyException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.WonMessageType;
import won.protocol.vocabulary.WONMSG;

public class ResponseBuilder extends TerminalBuilderBase<ResponseBuilder> {
    WonMessage toRespondto = null;
    WonMessageDirection directionOfMessageToRespondTo = null;

    public ResponseBuilder(WonMessageBuilder builder) {
        super(builder);
    }

    public ResponseBuilder respondingToMessageFromOwner(WonMessage toRespondTo) {
        this.toRespondto = toRespondTo;
        this.directionOfMessageToRespondTo = WonMessageDirection.FROM_OWNER;
        return this;
    }

    public ResponseBuilder respondingToMessageFromExternal(WonMessage toRespondTo) {
        this.toRespondto = toRespondTo;
        this.directionOfMessageToRespondTo = WonMessageDirection.FROM_EXTERNAL;
        return this;
    }

    public ResponseBuilder respondingToMessageFromSystem(WonMessage toRespondTo) {
        this.toRespondto = toRespondTo;
        this.directionOfMessageToRespondTo = WonMessageDirection.FROM_SYSTEM;
        return this;
    }

    public ResponseBuilder respondingToMessage(WonMessage toRespondTo, WonMessageDirection direction) {
        this.toRespondto = toRespondTo;
        this.directionOfMessageToRespondTo = direction;
        return this;
    }

    public ResponseBuilder fromConnection(URI connectionURI) {
        builder.connection(connectionURI);
        return this;
    }

    public ResponseBuilder fromAtom(URI atomURI) {
        builder.atom(atomURI);
        return this;
    }

    public ResponseBuilder success() {
        builder.type(WonMessageType.SUCCESS_RESPONSE);
        return this;
    }

    public ResponseBuilder failure() {
        builder.type(WonMessageType.FAILURE_RESPONSE);
        return this;
    }

    public ContentBuilder<ResponseBuilder> content() {
        return new ContentBuilder<ResponseBuilder>(this);
    }

    public WonMessage build() {
        Optional<URI> senderSocketURI = Optional.ofNullable(toRespondto.getSenderSocketURI());
        Optional<URI> recipientSocketURI = Optional.ofNullable(toRespondto.getRecipientSocketURI());
        if (builder.wonMessageType == null) {
            throw new MissingMessagePropertyException(WONMSG.messageType);
        }
        if (toRespondto.getMessageTypeRequired().isAtomSpecificMessage()) {
            builder.atom(toRespondto.getAtomURIRequired());
            builder.connectionURI = null;
        } else if (toRespondto.getMessageTypeRequired().isConnectionSpecificMessage()) {
            if (builder.connectionURI == null && !(builder.wonMessageType == WonMessageType.FAILURE_RESPONSE
                            && toRespondto.getMessageTypeRequired().isConnect())) {
                // allow a failure response without connection uri in case of a connect - the
                // connection creation will have failed
                throw new MissingMessagePropertyException(WONMSG.connection);
            }
            if (builder.wonMessageType == WonMessageType.SUCCESS_RESPONSE) {
                // if we are responding to a message that already has one response from the
                // sending node
                // add a reference to that message, thereby confirming it
                if (toRespondto.isMessageWithResponse()) {
                    toRespondto.getResponse().ifPresent(msg -> builder.previousMessage(msg.getMessageURIRequired()));
                }
            }
            if (directionOfMessageToRespondTo.isFromExternal()) {
                // if the message is an external message, the original receiver becomes
                // the sender of the response.
                builder.senderSocket(recipientSocketURI
                                .orElseThrow(() -> new MissingMessagePropertyException(WONMSG.recipientSocket)));
            } else {
                // if the message comes from the owner, the original sender is also
                // the sender of the response
                builder.senderSocket(senderSocketURI
                                .orElseThrow(() -> new MissingMessagePropertyException(WONMSG.senderSocket)));
            }
            builder.recipientSocket(senderSocketURI
                            .orElseThrow(() -> new MissingMessagePropertyException(WONMSG.senderSocket)));
        }
        builder
                        .respondingToMessage(toRespondto.getMessageURIRequired())
                        .respondingToMessageType(toRespondto.getMessageTypeRequired())
                        .direction(WonMessageDirection.FROM_SYSTEM);
        builder.timestampNow();
        return builder.build();
    }
}