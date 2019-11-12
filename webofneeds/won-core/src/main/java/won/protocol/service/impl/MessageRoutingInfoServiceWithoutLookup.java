package won.protocol.service.impl;

import java.net.URI;
import java.util.Optional;

import org.springframework.stereotype.Component;

import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageUtils;
import won.protocol.service.MessageRoutingInfoService;

/**
 * Determines sender|recipient atom|node for a given message by inspecting the
 * message only.
 * 
 * @author fkleedorfer
 */
@Component
public class MessageRoutingInfoServiceWithoutLookup implements MessageRoutingInfoService {
    @Override
    public Optional<URI> senderAtom(WonMessage msg) {
        return WonMessageUtils.getSenderAtomURI(msg);
    }

    @Override
    public Optional<URI> senderSocketType(WonMessage msg) {
        return Optional.empty();
    }

    @Override
    public Optional<URI> recipientSocketType(WonMessage msg) {
        return Optional.empty();
    }

    @Override
    public Optional<URI> recipientAtom(WonMessage msg) {
        return WonMessageUtils.getRecipientAtomURI(msg);
    }

    @Override
    public Optional<URI> senderNode(WonMessage msg) {
        return senderAtom(msg).map(WonMessageUtils::stripAtomSuffix);
    }

    @Override
    public Optional<URI> recipientNode(WonMessage msg) {
        return recipientAtom(msg).map(WonMessageUtils::stripAtomSuffix);
    }
}
