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
     * @return
     */
    public URI toGenericMessageURI(URI nodeSpecificMessageUri, Optional<URI> requesterWebID,
                    LinkedDataSource linkedDataSource) {
        if (nodeSpecificMessageUri == null) {
            return null;
        }
        if (isMessageURI(nodeSpecificMessageUri)) {
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
     * Same as resolve(URI, URI, LinkedDataSource) but try to resolve without
     * guessing the WoN node based on the referring document.
     * 
     * @param toResolve
     * @param linkedDataSource
     * @return
     */
    public URI toLocalMessageURI(URI toResolve, LinkedDataSource linkedDataSource) {
        if (toResolve == null) {
            return null;
        }
        if (!isMessageURI(toResolve)) {
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
     */
    public URI toLocalMessageURI(URI toResolve, Optional<URI> referringDocument, Optional<URI> requesterWebID,
                    LinkedDataSource linkedDataSource) {
        if (toResolve == null) {
            return null;
        }
        if (!isMessageURI(toResolve)) {
            return toResolve;
        }
        if (referringDocument.isPresent() && !isMessageURI(referringDocument.get())) {
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

    private boolean isMessageURI(URI toResolve) {
        return WonMessageUriHelper.isGenericMessageURI(toResolve);
    }

    public URI toLocalMessageURI(URI toResolve, WonNodeInfo wonNodeInfo) {
        return WonMessageUriHelper.toLocalMessageURI(toResolve, wonNodeInfo.getEventURIPrefix());
    }

    public URI toGenericMessageURI(URI toUnResolve, WonNodeInfo wonNodeInfo) {
        return WonMessageUriHelper.toGenericMessageURI(toUnResolve, wonNodeInfo.getEventURIPrefix());
    }
}
