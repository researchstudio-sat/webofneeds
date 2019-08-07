package won.owner.web.listener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.jena.query.Dataset;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import won.owner.model.User;
import won.owner.service.impl.KeystoreEnabledUserDetails;
import won.owner.service.impl.UserService;
import won.owner.web.WonOwnerMailSender;
import won.owner.web.events.OnExportUserEvent;
import won.protocol.rest.LinkedDataFetchingException;
import won.protocol.util.AuthenticationThreadLocal;
import won.protocol.util.linkeddata.CachingLinkedDataSource;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.linkeddata.WonLinkedDataUtils;

@Component
public class ExportListener implements ApplicationListener<OnExportUserEvent> {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Autowired
    private LinkedDataSource linkedDataSourceOnBehalfOfAtom;
    @Autowired
    private WonOwnerMailSender emailSender;
    @Autowired
    private UserService userService;

    @Override
    public void onApplicationEvent(OnExportUserEvent onExportUserEvent) {
        Authentication authentication = onExportUserEvent.getAuthentication();
        KeystoreEnabledUserDetails userDetails = ((KeystoreEnabledUserDetails) authentication.getPrincipal());
        String password = onExportUserEvent.getKeyStorePassword();
        User user = userService.getByUsername(userDetails.getUsername());
        String responseMail = onExportUserEvent.getResponseEmail();
        File tmpFile = null;
        try {
            tmpFile = File.createTempFile("won", null);
            tmpFile.deleteOnExit();
            ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(tmpFile), Charset.forName("UTF-8"));
            ZipEntry atomsEntry = new ZipEntry("export.nq");
            zip.putNextEntry(atomsEntry);
            user.getUserAtoms().stream().parallel().map(userAtom -> fetchAtomData(authentication, userAtom.getUri()))
                            .forEach(dataset -> {
                                RDFDataMgr.write(zip, dataset, RDFFormat.NQUADS_UTF8);
                            });
            zip.closeEntry();
            ZipEntry keystoreEntry = new ZipEntry("keystore.jks");
            zip.putNextEntry(keystoreEntry);
            if (password != null && !password.isEmpty()) {
                ByteArrayOutputStream tmpStream = new ByteArrayOutputStream();
                userDetails.getKeyStore().store(tmpStream, password.toCharArray());
                tmpStream.writeTo(zip);
            } else {
                zip.write("You need to supply a keyStorePassword to get your keystore for security reasons".getBytes());
            }
            zip.closeEntry();
            zip.close();
            emailSender.sendExportMessage(onExportUserEvent.getResponseEmail(), tmpFile);
        } catch (LinkedDataFetchingException e) {
            logger.warn(e.getMessage());
            emailSender.sendExportFailedMessage(responseMail);
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
            emailSender.sendExportFailedMessage(responseMail);
            throw new RuntimeException(e);
        } catch (Exception e) {
            emailSender.sendExportFailedMessage(responseMail);
            throw e;
        } finally {
            if (tmpFile != null) {
                tmpFile.delete();
            }
        }
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

    private static void refreshData(URI atomUri, LinkedDataSource linkedDataSource) {
        // we may have tried to crawl a conversation dataset of which messages
        // were still in-flight. we allow one re-crawl attempt per exception before
        // we throw the exception on:
        if (!(linkedDataSource instanceof CachingLinkedDataSource)) {
            return;
        }
        invalidate(atomUri, atomUri, linkedDataSource);
    }

    private static boolean recrawl(Set<URI> recrawled, URI atomUri, LinkedDataSource linkedDataSource, URI... uris) {
        Set<URI> urisToCrawl = new HashSet<>();
        Arrays.stream(uris).filter(x -> !recrawled.contains(x)).forEach(urisToCrawl::add);
        if (urisToCrawl.isEmpty()) {
            if (logger.isDebugEnabled()) {
                logger.debug("atom {}: not recrawling again: {}", atomUri, Arrays.toString(uris));
            }
            return false;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("atom {}, recrawling: {}", atomUri, urisToCrawl);
        }
        if (linkedDataSource instanceof CachingLinkedDataSource) {
            urisToCrawl.stream().forEach(uri -> {
                invalidate(uri, atomUri, linkedDataSource);
            });
        }
        recrawled.addAll(urisToCrawl);
        return true;
    }

    public Dataset fetchAtomData(Authentication authentication, URI atomUri) {
        // allow each resource to be re-crawled once for each reason
        Set<URI> recrawledForFailedFetch = new HashSet<>();
        AuthenticationThreadLocal.setAuthentication(authentication);
        while (true) {
            // we leave the loop either with a runtime exception or with the result
            try {
                return WonLinkedDataUtils.getFullAtomDataset(atomUri, linkedDataSourceOnBehalfOfAtom);
            } catch (LinkedDataFetchingException e) {
                // we may have tried to crawl a conversation dataset of which messages
                // were still in-flight. we allow one re-crawl attempt per exception before
                // we throw the exception on:
                refreshData(atomUri, linkedDataSourceOnBehalfOfAtom);
                if (!recrawl(recrawledForFailedFetch, atomUri, linkedDataSourceOnBehalfOfAtom, e.getResourceUri())) {
                    throw e;
                }
            }
        }
    }
}
