package won.protocol.service.impl;

import java.net.URI;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import won.protocol.message.WonMessage;
import won.protocol.service.MessageRoutingInfoService;

@Component
@Primary // prefer this bean when autowiring
public class MessageRoutingInfoServiceImpl implements MessageRoutingInfoService {
    @Autowired
    private MessageRoutingInfoServiceWithLookup withLookup;
    @Autowired
    private MessageRoutingInfoServiceWithoutLookup withoutLookup;

    @Override
    public Optional<URI> senderAtom(WonMessage msg) {
        return Optional.ofNullable(
                        withoutLookup.senderAtom(msg)
                                        .orElseGet(() -> withLookup.senderAtom(msg).orElse(null)));
    }

    @Override
    public Optional<URI> senderSocketType(WonMessage msg) {
        return Optional.ofNullable(
                        withoutLookup.senderSocketType(msg)
                                        .orElseGet(() -> withLookup.senderSocketType(msg).orElse(null)));
    }

    @Override
    public Optional<URI> recipientSocketType(WonMessage msg) {
        return Optional.ofNullable(
                        withoutLookup.recipientSocketType(msg)
                                        .orElseGet(() -> withLookup.recipientSocketType(msg).orElse(null)));
    }

    @Override
    public Optional<URI> recipientAtom(WonMessage msg) {
        return Optional.ofNullable(
                        withoutLookup.recipientAtom(msg)
                                        .orElseGet(() -> withLookup.recipientAtom(msg).orElse(null)));
    }

    @Override
    public Optional<URI> senderNode(WonMessage msg) {
        return Optional.ofNullable(
                        withoutLookup.senderNode(msg)
                                        .orElseGet(() -> withLookup.senderNode(msg).orElse(null)));
    }

    @Override
    public Optional<URI> recipientNode(WonMessage msg) {
        return Optional.ofNullable(
                        withoutLookup.recipientNode(msg)
                                        .orElseGet(() -> withLookup.recipientNode(msg).orElse(null)));
    }
}
