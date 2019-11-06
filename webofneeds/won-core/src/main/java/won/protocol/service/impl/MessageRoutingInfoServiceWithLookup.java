package won.protocol.service.impl;

import java.net.URI;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import won.protocol.message.WonMessage;
import won.protocol.service.MessageRoutingInfoService;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.linkeddata.WonLinkedDataUtils;

/**
 * Determines sender|recipient atom|node for a given message by looking up
 * linked data.
 * 
 * @author fkleedorfer
 */
@Component
public class MessageRoutingInfoServiceWithLookup implements MessageRoutingInfoService {
    @Autowired
    LinkedDataSource linkedDataSource;

    @Override
    public Optional<URI> senderAtom(WonMessage msg) {
        URI atomUri = msg.getSenderAtomURI();
        if (atomUri == null) {
            URI socketUri = msg.getSenderSocketURI();
            if (socketUri != null) {
                atomUri = WonLinkedDataUtils.getAtomOfSocket(socketUri, linkedDataSource).orElse(null);
            }
        }
        if (atomUri == null) {
            URI senderUri = msg.getSenderURI();
            if (senderUri != null) {
                atomUri = WonLinkedDataUtils.getAtomURIforConnectionURI(senderUri, linkedDataSource);
            }
        }
        return Optional.ofNullable(atomUri);
    }

    @Override
    public Optional<URI> senderSocketType(WonMessage msg) {
        URI socketURI = msg.getSenderSocketURI();
        if (socketURI == null) {
            URI possiblyConnectionURI = msg.getSenderURI();
            if (possiblyConnectionURI != null) {
                socketURI = WonLinkedDataUtils.getSocketURIForConnectionURI(possiblyConnectionURI, linkedDataSource);
            }
        }
        return Optional.ofNullable(socketURI).map(
                        uri -> WonLinkedDataUtils.getTypeOfSocket(uri, linkedDataSource).orElse(null));
    }

    @Override
    public Optional<URI> recipientSocketType(WonMessage msg) {
        URI socketURI = msg.getRecipientSocketURI();
        if (socketURI == null) {
            URI possiblyConnectionURI = msg.getRecipientURI();
            if (possiblyConnectionURI != null) {
                socketURI = WonLinkedDataUtils.getSocketURIForConnectionURI(possiblyConnectionURI, linkedDataSource);
            }
        }
        return Optional.ofNullable(socketURI).map(
                        uri -> WonLinkedDataUtils.getTypeOfSocket(uri, linkedDataSource).orElse(null));
    }

    @Override
    public Optional<URI> recipientAtom(WonMessage msg) {
        URI atomUri = msg.getRecipientAtomURI();
        if (atomUri == null) {
            URI socketUri = msg.getRecipientSocketURI();
            if (socketUri != null) {
                atomUri = WonLinkedDataUtils.getAtomOfSocket(socketUri, linkedDataSource).orElse(null);
            }
        }
        if (atomUri == null) {
            URI senderUri = msg.getRecipientURI();
            if (senderUri != null) {
                atomUri = WonLinkedDataUtils.getAtomURIforConnectionURI(senderUri, linkedDataSource);
            }
        }
        return Optional.ofNullable(atomUri);
    }

    @Override
    public Optional<URI> senderNode(WonMessage msg) {
        Optional<URI> atomURI = senderAtom(msg);
        return atomURI.map(uri -> WonLinkedDataUtils.getWonNodeURIForAtomOrConnectionURI(uri, linkedDataSource));
    }

    @Override
    public Optional<URI> recipientNode(WonMessage msg) {
        Optional<URI> atomURI = recipientAtom(msg);
        return atomURI.map(uri -> WonLinkedDataUtils.getWonNodeURIForAtomOrConnectionURI(uri, linkedDataSource));
    }
}
