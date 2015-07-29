package won.cryptography.utils;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.path.Path;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import won.cryptography.rdfsign.WonKeysReaderWriter;
import won.cryptography.service.KeyStoreService;
import won.protocol.util.DefaultPrefixUtils;
import won.protocol.util.linkeddata.LinkedDataSource;

import java.io.File;
import java.net.URI;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: ypanchenko
 * Date: 12.04.2015
 */
public class TestingDataSource implements LinkedDataSource
{

  Map<String,PublicKey> pubKeysMap = new HashMap<String,PublicKey>();
  public TestingDataSource() {

    //load public  keys:
    File keysFile = new File(this.getClass().getResource(TestSigningUtils.KEYS_FILE).getFile());
    KeyStoreService storeService = new KeyStoreService(keysFile);

    pubKeysMap.put(TestSigningUtils.needCertUri, storeService.getCertificate(TestSigningUtils.needCertUri).getPublicKey());
    pubKeysMap.put(TestSigningUtils.ownerCertUri, storeService.getCertificate(TestSigningUtils.ownerCertUri).getPublicKey());
    pubKeysMap.put(TestSigningUtils.nodeCertUri, storeService.getCertificate(TestSigningUtils.nodeCertUri).getPublicKey());
  }

  @Override
  public Dataset getDataForResource(final URI resourceURI) {
    Dataset dataset = DatasetFactory.createMem();
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
  public Dataset getDataForResource(final URI resourceURI, final List<URI> properties, final int maxRequest, final int maxDepth) {
    throw new NotImplementedException();
  }

  @Override
  public Dataset getDataForResourceWithPropertyPath(final URI resourceURI, final List<Path> properties, final int maxRequest, final int maxDepth, final boolean moveAllTriplesInDefaultGraph) {
    throw new NotImplementedException();
  }
}
