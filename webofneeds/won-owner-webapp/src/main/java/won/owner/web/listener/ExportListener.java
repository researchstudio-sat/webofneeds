package won.owner.web.listener;

import org.apache.jena.query.Dataset;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import won.owner.model.User;
import won.owner.service.impl.KeystoreEnabledUserDetails;
import won.owner.web.WonOwnerMailSender;
import won.owner.web.events.OnExportUserEvent;
import won.protocol.agreement.AgreementProtocolState;
import won.protocol.rest.LinkedDataFetchingException;
import won.protocol.util.AuthenticationThreadLocal;
import won.protocol.util.linkeddata.CachingLinkedDataSource;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.linkeddata.WonLinkedDataUtils;

import java.io.*;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
public class ExportListener implements ApplicationListener<OnExportUserEvent> {
    private static final Logger logger = LoggerFactory.getLogger(AgreementProtocolState.class);


    @Autowired
    private LinkedDataSource linkedDataSourceOnBehalfOfNeed;

    @Autowired
    private WonOwnerMailSender emailSender;


    @Override
    public void onApplicationEvent(OnExportUserEvent onExportUserEvent) {
        try {
            Authentication authentication = onExportUserEvent.getAuthentication();
            User authUser = ((KeystoreEnabledUserDetails) authentication.getPrincipal()).getUser();
            User user = onExportUserEvent.getUser();

            File tmpFile = File.createTempFile("won", null);
            tmpFile.deleteOnExit();

            ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(tmpFile));

            ZipEntry needsEntry = new ZipEntry("export.nq");
            zip.putNextEntry(needsEntry);
            user.getUserNeeds()
                    .stream()
                    .parallel()
                    .map(userNeed -> fetchNeedData(authentication, userNeed.getUri()))
                    .forEach(dataset -> {
                        RDFDataMgr.write(zip, dataset, RDFFormat.NQUADS_UTF8);
                    });
            zip.closeEntry();

            ZipEntry keystoreEntry = new ZipEntry("keystore.jks");
            zip.putNextEntry(keystoreEntry);
            zip.write(authUser.getKeystoreHolder().getKeystoreBytes());
            zip.closeEntry();

            zip.close();
            emailSender.sendExportMessage(onExportUserEvent.getResponseEmail(), tmpFile);
            tmpFile.delete();
        } catch (IOException e) {
            logger.warn(e.getMessage());
        }
    }

    private static void invalidate(URI uri, URI webId, LinkedDataSource linkedDataSource) {
        if (!(linkedDataSource instanceof CachingLinkedDataSource)) {
            return;
        }
        if (uri != null) {
            ((CachingLinkedDataSource)linkedDataSource).invalidate(uri);
            if (webId != null) {
                ((CachingLinkedDataSource)linkedDataSource).invalidate(uri, webId);
            }
        }
    }

    private static void refreshData(URI needUri, LinkedDataSource linkedDataSource){
        // we may have tried to crawl a conversation dataset of which messages
        // were still in-flight. we allow one re-crawl attempt per exception before
        // we throw the exception on:
        if (!(linkedDataSource instanceof CachingLinkedDataSource)) {
            return;
        }
        invalidate(needUri, needUri, linkedDataSource);
    }

    private static boolean recrawl(Set<URI> recrawled, URI needUri, LinkedDataSource linkedDataSource, URI... uris) {
        Set<URI> urisToCrawl = new HashSet<URI>();
        Arrays.stream(uris)
                .filter(x -> ! recrawled.contains(x))
                .forEach(urisToCrawl::add);
        if (urisToCrawl.isEmpty()) {
            if (logger.isDebugEnabled()) {
                logger.debug("need {}: not recrawling again: {}", needUri, Arrays.toString(uris));
            }
            return false;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("need {}, recrawling: {}", needUri, urisToCrawl);
        }
        if (linkedDataSource instanceof CachingLinkedDataSource) {
            urisToCrawl.stream().forEach(uri -> {
                invalidate(uri, needUri, linkedDataSource);
            });
        }
        recrawled.addAll(urisToCrawl);
        return true;
    }

    public Dataset fetchNeedData(Authentication authentication, URI needUri) {
        //allow each resource to be re-crawled once for each reason
        Set<URI> recrawledForFailedFetch = new HashSet<>();
        AuthenticationThreadLocal.setAuthentication(authentication);
        while(true) {
            //we leave the loop either with a runtime exception or with the result
            try {
                Dataset needDataset= WonLinkedDataUtils.getFullNeedDataset(needUri, linkedDataSourceOnBehalfOfNeed);
                return needDataset;
            } catch (LinkedDataFetchingException e) {
                // we may have tried to crawl a conversation dataset of which messages
                // were still in-flight. we allow one re-crawl attempt per exception before
                // we throw the exception on:
                refreshData(needUri, linkedDataSourceOnBehalfOfNeed);
                if (!recrawl(recrawledForFailedFetch, needUri, linkedDataSourceOnBehalfOfNeed, e.getResourceUri())){
                    throw e;
                }
            }
        }
    }
}
