package won.protocol.service;

import java.net.URI;
import java.util.Optional;

import won.protocol.message.WonMessage;

public interface MessageRoutingInfoService {
    Optional<URI> senderAtom(WonMessage msg);

    Optional<URI> senderSocketType(WonMessage msg);

    Optional<URI> recipientSocketType(WonMessage msg);

    Optional<URI> recipientAtom(WonMessage msg);

    Optional<URI> senderNode(WonMessage msg);

    Optional<URI> recipientNode(WonMessage msg);
}