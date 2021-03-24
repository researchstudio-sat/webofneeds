package won.node.protocol.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import won.protocol.message.WonMessage;
import won.protocol.service.MessageRoutingInfoService;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.linkeddata.WonLinkedDataUtils;
import won.protocol.util.linkeddata.uriresolver.WonRelativeUriHelper;

import java.net.URI;
import java.util.Optional;

/**
 * Determines sender|recipient atom|node for a given message by looking up
 * linked data.
 * 
 * @author fkleedorfer
 */
@Component
public class MessageRoutingInfoServiceWithLookup implements MessageRoutingInfoService {
    @Autowired
    private LinkedDataSource linkedDataSource;
    @Value("${http.client.requesterWebId}")
    private URI requesterWebId;

    @Override
    public Optional<URI> senderAtom(WonMessage msg) {
        URI atomUri = msg.getSenderAtomURI();
        if (atomUri == null) {
            URI socketUri = msg.getSenderSocketURI();
            if (socketUri != null) {
                atomUri = WonRelativeUriHelper.stripFragment(socketUri);
            }
        }
        if (atomUri == null) {
            URI senderUri = msg.getConnectionURI();
            if (senderUri != null) {
                atomUri = WonRelativeUriHelper.stripConnectionSuffix(senderUri);
            }
        }
        return Optional.ofNullable(atomUri);
    }

    @Override
    public Optional<URI> senderSocketType(WonMessage msg) {
        URI socketURI = msg.getSenderSocketURI();
        if (socketURI == null) {
            URI possiblyConnectionURI = msg.getConnectionURI();
            if (possiblyConnectionURI != null) {
                socketURI = WonLinkedDataUtils.getSocketURIForConnectionURI(possiblyConnectionURI, requesterWebId,
                                linkedDataSource);
            }
        }
        return Optional.ofNullable(socketURI).map(
                        uri -> WonLinkedDataUtils.getTypeOfSocket(uri, requesterWebId, linkedDataSource).orElse(null));
    }

    @Override
    public Optional<URI> recipientSocketType(WonMessage msg) {
        URI socketURI = msg.getRecipientSocketURI();
        if (socketURI == null) {
            URI possiblyConnectionURI = msg.getConnectionURI();
            if (possiblyConnectionURI != null) {
                socketURI = WonLinkedDataUtils.getSocketURIForConnectionURI(possiblyConnectionURI, requesterWebId,
                                linkedDataSource);
            }
        }
        return Optional.ofNullable(socketURI).map(
                        uri -> WonLinkedDataUtils.getTypeOfSocket(uri, requesterWebId, linkedDataSource).orElse(null));
    }

    @Override
    public Optional<URI> recipientAtom(WonMessage msg) {
        URI atomUri = msg.getRecipientAtomURI();
        if (atomUri == null) {
            URI socketUri = msg.getRecipientSocketURI();
            if (socketUri != null) {
                atomUri = WonRelativeUriHelper.stripFragment(socketUri);
            }
        }
        if (atomUri == null) {
            URI senderUri = msg.getConnectionURI();
            if (senderUri != null) {
                atomUri = WonRelativeUriHelper.stripConnectionSuffix(senderUri);
            }
        }
        return Optional.ofNullable(atomUri);
    }

    @Override
    public Optional<URI> senderNode(WonMessage msg) {
        Optional<URI> atomURI = senderAtom(msg);
        return atomURI.map(uri -> WonRelativeUriHelper.stripAtomSuffix(uri));
    }

    @Override
    public Optional<URI> recipientNode(WonMessage msg) {
        Optional<URI> atomURI = recipientAtom(msg);
        return atomURI.map(uri -> WonRelativeUriHelper.stripAtomSuffix(uri));
    }
}
