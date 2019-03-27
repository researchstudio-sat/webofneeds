package won.protocol.util;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.jena.query.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import won.protocol.agreement.AgreementProtocolState;
import won.protocol.agreement.IncompleteConversationDataException;
import won.protocol.rest.LinkedDataFetchingException;
import won.protocol.util.linkeddata.CachingLinkedDataSource;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.linkeddata.WonLinkedDataUtils;

public class WonConversationUtils {
    private static final Logger logger = LoggerFactory.getLogger(WonConversationUtils.class);

    private static <T> T getFirstOrNull(Dataset dataset, Function<Dataset, List<T>> function) {
        // RDFDataMgr.write(System.err, dataset, Lang.TRIG);
        List<T> results = function.apply(dataset);
        if (results.size() > 0)
            return results.get(0);
        return null;
    }

    public static URI getNthLatestMessage(Dataset conversationDataset, Predicate predicate, int n) {
        return AgreementProtocolState.of(conversationDataset).getNthLatestMessage(predicate, n);
    }

    public static URI getLatestMessageOfNeed(Dataset conversationDataset, URI senderNeed) {
        return AgreementProtocolState.of(conversationDataset).getLatestMessageSentByNeed(senderNeed);
    }

    public static URI getNthLatestMessageOfNeed(Dataset conversationDataset, URI senderNeed, int n) {
        return AgreementProtocolState.of(conversationDataset).getNthLatestMessageSentByNeed(senderNeed, n);
    }

    public static URI getLatestAcceptsMessageOfNeed(Dataset conversationDataset, URI senderNeed) {
        return AgreementProtocolState.of(conversationDataset).getLatestAcceptsMessageSentByNeed(senderNeed);
    }

    public static URI getLatestAcceptsMessage(Dataset conversationDataset) {
        return AgreementProtocolState.of(conversationDataset).getLatestAcceptsMessage();
    }

    public static URI getLatestRetractsMessageOfNeed(Dataset conversationDataset, URI senderNeed) {
        return AgreementProtocolState.of(conversationDataset).getLatestRetractsMessageSentByNeed(senderNeed);
    }

    public static URI getLatestProposesMessageOfNeed(Dataset conversationDataset, URI senderNeed) {
        return AgreementProtocolState.of(conversationDataset).getLatestProposesMessageSentByNeed(senderNeed);
    }

    public static URI getLatestProposesToCancelMessageOfNeed(Dataset conversationDataset, URI senderNeed) {
        return AgreementProtocolState.of(conversationDataset).getLatestProposesToCancelMessageSentByNeed(senderNeed);
    }

    public static URI getLatestRejectsMessageOfNeed(Dataset conversationDataset, URI senderNeed, int n) {
        return AgreementProtocolState.of(conversationDataset).getLatestRejectsMessageSentByNeed(senderNeed);
    }

    public static AgreementProtocolState getAgreementProtocolState(URI connectionUri,
                    LinkedDataSource linkedDataSource) {
        URI needUri = WonLinkedDataUtils.getNeedURIforConnectionURI(connectionUri, linkedDataSource);
        // allow each resource to be re-crawled once for each reason
        Set<URI> recrawledForIncompleteness = new HashSet<>();
        Set<URI> recrawledForFailedFetch = new HashSet<>();
        while (true) {
            // we leave the loop either with a runtime exception or with the result
            try {
                Dataset conversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri,
                                linkedDataSource);
                return AgreementProtocolState.of(conversationDataset);
            } catch (IncompleteConversationDataException e) {
                // we may have tried to crawl a conversation dataset of which messages
                // were still in-flight. we allow one re-crawl attempt per exception before
                // we throw the exception on:
                refreshDataForConnection(connectionUri, needUri, linkedDataSource);
                if (!recrawl(recrawledForIncompleteness, connectionUri, needUri, linkedDataSource,
                                e.getMissingMessageUri(), e.getReferringMessageUri())) {
                    throw e;
                }
            } catch (LinkedDataFetchingException e) {
                // we may have tried to crawl a conversation dataset of which messages
                // were still in-flight. we allow one re-crawl attempt per exception before
                // we throw the exception on:
                refreshDataForConnection(connectionUri, needUri, linkedDataSource);
                if (!recrawl(recrawledForFailedFetch, connectionUri, needUri, linkedDataSource, e.getResourceUri())) {
                    throw e;
                }
            }
        }
    }

    private static void refreshDataForConnection(URI connectionUri, URI needUri, LinkedDataSource linkedDataSource) {
        // we may have tried to crawl a conversation dataset of which messages
        // were still in-flight. we allow one re-crawl attempt per exception before
        // we throw the exception on:
        if (!(linkedDataSource instanceof CachingLinkedDataSource)) {
            return;
        }
        if (connectionUri == null) {
            return;
        }
        invalidate(connectionUri, needUri, linkedDataSource);
        URI connectionEventContainerUri = WonLinkedDataUtils.getEventContainerURIforConnectionURI(connectionUri,
                        linkedDataSource);
        invalidate(connectionEventContainerUri, needUri, linkedDataSource);
        URI remoteConnectionUri = WonLinkedDataUtils.getRemoteConnectionURIforConnectionURI(connectionUri,
                        linkedDataSource);
        if (remoteConnectionUri == null) {
            return;
        }
        invalidate(remoteConnectionUri, needUri, linkedDataSource);
        URI remoteConnectionEventContainerUri = WonLinkedDataUtils
                        .getEventContainerURIforConnectionURI(remoteConnectionUri, linkedDataSource);
        invalidate(remoteConnectionEventContainerUri, needUri, linkedDataSource);
    }

    private static boolean recrawl(Set<URI> recrawled, URI connectionUri, URI needUri,
                    LinkedDataSource linkedDataSource, URI... uris) {
        Set<URI> urisToCrawl = new HashSet<URI>();
        Arrays.stream(uris).filter(x -> !recrawled.contains(x)).forEach(urisToCrawl::add);
        if (urisToCrawl.isEmpty()) {
            if (logger.isDebugEnabled()) {
                logger.debug("connection {}: not recrawling again: {}", connectionUri, Arrays.toString(uris));
            }
            return false;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("connection {}, recrawling: {}", connectionUri, urisToCrawl);
        }
        if (linkedDataSource instanceof CachingLinkedDataSource) {
            urisToCrawl.stream().forEach(uri -> {
                invalidate(uri, needUri, linkedDataSource);
            });
        }
        recrawled.addAll(urisToCrawl);
        return true;
    }

    private static void invalidate(URI uri, URI webId, LinkedDataSource linkedDataSource) {
        if (!(linkedDataSource instanceof CachingLinkedDataSource)) {
            return;
        }
        if (uri != null) {
            ((CachingLinkedDataSource) linkedDataSource).invalidate(uri);
            if (webId != null) {
                ((CachingLinkedDataSource) linkedDataSource).invalidate(uri, webId);
            }
        }
    }
}
