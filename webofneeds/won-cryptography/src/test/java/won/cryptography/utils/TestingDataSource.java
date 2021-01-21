package won.cryptography.utils;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.path.Path;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.http.HttpHeaders;
import won.cryptography.rdfsign.WonKeysReaderWriter;
import won.cryptography.service.keystore.FileBasedKeyStoreService;
import won.protocol.rest.DatasetResponseWithStatusCodeAndHeaders;
import won.protocol.util.DefaultPrefixUtils;
import won.protocol.util.linkeddata.LinkedDataSource;

import java.io.File;
import java.net.URI;
import java.security.PublicKey;
import java.security.Security;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * User: ypanchenko Date: 12.04.2015
 */
public class TestingDataSource implements LinkedDataSource {
    private final Map<String, PublicKey> pubKeysMap = new HashMap<>();

    public TestingDataSource() throws Exception {
        // load public keys:
        Security.addProvider(new BouncyCastleProvider());
        File keysFile = new File(this.getClass().getResource(TestSigningUtils.KEYS_FILE).getFile());
        FileBasedKeyStoreService storeService = new FileBasedKeyStoreService(keysFile, "temp");
        storeService.init();
        pubKeysMap.put(TestSigningUtils.atomCertUri,
                        storeService.getCertificate(TestSigningUtils.atomCertUri).getPublicKey());
        pubKeysMap.put(TestSigningUtils.ownerCertUri,
                        storeService.getCertificate(TestSigningUtils.ownerCertUri).getPublicKey());
        pubKeysMap.put(TestSigningUtils.nodeCertUri,
                        storeService.getCertificate(TestSigningUtils.nodeCertUri).getPublicKey());
    }

    @Override
    public Dataset getDataForPublicResource(final URI resourceURI) {
        Dataset dataset = DatasetFactory.createGeneral();
        DefaultPrefixUtils.setDefaultPrefixes(dataset.getDefaultModel());
        WonKeysReaderWriter keyWriter = new WonKeysReaderWriter();
        Model model = dataset.getDefaultModel();
        Resource subj = model.createResource(resourceURI.toString());
        try {
            keyWriter.writeToModel(model, subj, pubKeysMap.get(resourceURI.toString()));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return dataset;
    }

    @Override
    public Dataset getDataForResource(final URI resourceURI, final URI requesterWebID) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Dataset getDataForPublicResource(final URI resourceURI, final List<URI> properties, final int maxRequest,
                    final int maxDepth) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Dataset getDataForResource(final URI resourceURI, final URI requesterWebID, final List<URI> properties,
                    final int maxRequest, final int maxDepth) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Dataset getDataForPublicResourceWithPropertyPath(final URI resourceURI, final List<Path> properties,
                    final int maxRequest, final int maxDepth, final boolean moveAllTriplesInDefaultGraph) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Dataset getDataForResourceWithPropertyPath(URI resourceURI, URI requesterWebID, List<Path> properties,
                    int maxRequest, int maxDepth) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Dataset getDataForResourceWithPropertyPath(URI resourceURI, Optional<URI> requesterWebID,
                    List<Path> properties, int maxRequest, int maxDepth) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DatasetResponseWithStatusCodeAndHeaders getDatasetWithHeadersForResource(URI resource,
                    HttpHeaders httpHeaders) {
        throw new UnsupportedOperationException();
    }
}
