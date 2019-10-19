package won.protocol.util.linkeddata.uriresolver;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import won.protocol.service.WonNodeInfo;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.WonMessageUriHelper;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.linkeddata.WonLinkedDataUtils;

@Component
public class WonMessageUriResolver {
    private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Autowired
    WonNodeInformationService wonNodeInformationService;

    /**
     * Reverse operation: get the message uri for a node-specific one.
     * 
     * @param nodeSpecificMessageUri
     * @param requesterWebID
     * @param linkedDataSource
     * @return a generic, non-dereferencable message URI, or <code>toResolve</code>
     * if it could not be converted
     */
    public URI toGenericMessageURI(URI nodeSpecificMessageUri, Optional<URI> requesterWebID,
                    LinkedDataSource linkedDataSource) {
        if (nodeSpecificMessageUri == null) {
            return null;
        }
        if (isGenericMessageURI(nodeSpecificMessageUri)) {
            return nodeSpecificMessageUri;
        }
        Optional<WonNodeInfo> wonNodeInfo = Optional.empty();
        if (requesterWebID.isPresent()) {
            try {
                wonNodeInfo = WonLinkedDataUtils.findWonNode(nodeSpecificMessageUri, requesterWebID,
                                linkedDataSource);
            } catch (Exception e) {
                logger.debug("caught exception trying to fetch " + nodeSpecificMessageUri + " with requresterWebID "
                                + requesterWebID);
            }
        }
        if (wonNodeInfo.isPresent()) {
            return toGenericMessageURI(nodeSpecificMessageUri, wonNodeInfo.get());
        }
        return toGenericMessageURI(nodeSpecificMessageUri, wonNodeInformationService.getDefaultWonNodeInfo());
    }

    /**
     * Converts the <code>localMessageUri</code> that is dereferencable on the
     * specified WoN node to a generic one.
     * 
     * @param localMessageUri
     * @param wonNodeInfo
     * @return a generic, non-dereferencable message URI, or <code>toResolve</code>
     * if it could not be converted
     */
    public URI toGenericMessageURI(URI localMessageUri, WonNodeInfo wonNodeInfo) {
        return WonMessageUriHelper.toGenericMessageURI(localMessageUri, wonNodeInfo.getEventURIPrefix());
    }

    /**
     * Same as resolve(URI, URI, LinkedDataSource) but try to resolve without
     * guessing the WoN node based on the referring document.
     * 
     * @param toResolve
     * @param linkedDataSource
     * @return a local, dereferencable message URI, or <code>toResolve</code> if it
     * could not be converted
     */
    public URI toLocalMessageURI(URI toResolve, LinkedDataSource linkedDataSource) {
        if (toResolve == null) {
            return null;
        }
        if (!isGenericMessageURI(toResolve)) {
            return toResolve;
        }
        Optional<WonNodeInfo> wonNodeInfo = Optional.of(wonNodeInformationService.getDefaultWonNodeInfo());
        return toLocalMessageURI(toResolve, wonNodeInfo.get());
    }

    /**
     * Assuming that the URI <code>toResolve</code> was found in a document
     * identified by <code>referringDocument</code>, use this information to map the
     * possibly non-dereferencable URI <code>toResolve</code> to a dereferencable
     * one.
     * 
     * @param referringDocument The document containing <code>toResolve</code>
     * @param toResolve The URI to be mapped to a dereferencable one.
     * @return a dereferencable URI that will provide and appropriate representation
     * of <code>toResolve</code>
     * @return a local, dereferencable message URI, or <code>toResolve</code> if it
     * could not be converted
     */
    public URI toLocalMessageURI(URI toResolve, Optional<URI> referringDocument, Optional<URI> requesterWebID,
                    LinkedDataSource linkedDataSource) {
        if (toResolve == null) {
            return null;
        }
        if (!isGenericMessageURI(toResolve)) {
            return toResolve;
        }
        if (referringDocument.isPresent() && !isGenericMessageURI(referringDocument.get())) {
            // try to guess the WoN node from the referring document - which is not a
            // message
            Optional<WonNodeInfo> wonNodeInfo = WonLinkedDataUtils.findWonNode(referringDocument.get(), requesterWebID,
                            linkedDataSource);
            if (wonNodeInfo.isPresent()) {
                return toLocalMessageURI(toResolve, wonNodeInfo.get());
            }
        }
        return toLocalMessageURI(toResolve, linkedDataSource);
    }

    /**
     * Converts the specified uri <code>toResolve</code> to a local message uri on
     * the WoN node identified by the specified <code>wonNodeUri</code>. If that
     * fails, the default WoN node is used instead.
     * 
     * @param toResolve
     * @param wonNodeUri
     * @param requesterWebID
     * @param linkedDataSource
     * @return a local, dereferencable message URI, or <code>toResolve</code> if it
     * could not be converted
     */
    public URI toLocalMessageURIForWonNode(URI toResolve, Optional<URI> wonNodeUri,
                    LinkedDataSource linkedDataSource) {
        if (toResolve == null) {
            return null;
        }
        if (!isGenericMessageURI(toResolve)) {
            return toResolve;
        }
        if (wonNodeUri.isPresent() && !isGenericMessageURI(wonNodeUri.get())) {
            // try to guess the WoN node from the referring document - which is not a
            // message
            Optional<WonNodeInfo> wonNodeInfo = WonLinkedDataUtils.getWonNodeInfo(wonNodeUri.get(),
                            linkedDataSource);
            if (wonNodeInfo.isPresent()) {
                return toLocalMessageURI(toResolve, wonNodeInfo.get());
            }
        }
        return toLocalMessageURI(toResolve, linkedDataSource);
    }

    private boolean isGenericMessageURI(URI toResolve) {
        return WonMessageUriHelper.isGenericMessageURI(toResolve);
    }

    /**
     * Converts the specified URI <code>genericMessageUri</code> to a message URI
     * that is dereferencable on the specified WoN node.
     * 
     * @param genericMessageUri
     * @param wonNodeInfo
     * @return a local, dereferencable message URI, or
     * <code>genericMessageUri</code> if it could not be converted
     */
    public URI toLocalMessageURI(URI genericMessageUri, WonNodeInfo wonNodeInfo) {
        return WonMessageUriHelper.toLocalMessageURI(genericMessageUri, wonNodeInfo.getEventURIPrefix());
    }
}
