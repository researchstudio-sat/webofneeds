package won.protocol.model.util.linkeddata;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collection;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WON;

public class WonRdfUtilsTest {

    private InputStream getResourceAsStream(String name) {
        return getClass().getClassLoader().getResourceAsStream(name);
    }
    
    private String getResourceAsString(String name) throws Exception {
        byte[] buffer = new byte[256];
        StringWriter sw = new StringWriter();
        try (InputStream in = getResourceAsStream(name)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int bytesRead = 0;
            while ((bytesRead = in.read(buffer)) > -1) {
                baos.write(buffer,0,bytesRead);
            }
            return new String(baos.toByteArray(),Charset.defaultCharset());
        }
        
    }
    
    private Dataset loadTestDatasetFromClasspathResource(String resource) {
        Dataset dataset = DatasetFactory.createGeneral();
        RDFDataMgr.read(dataset,  getResourceAsStream(resource), Lang.TRIG);
        return dataset;
    }
    
    @BeforeClass
    public static void setLogLevel() {
        Logger root = (Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);  
    }
    
    @Test 
    public void testGetNeedURI() {
        Dataset needDataset = loadTestDatasetFromClasspathResource("wonrdfutils/need1.trig");
        URI needURI = WonRdfUtils.NeedUtils.getNeedURI(needDataset);
        Assert.assertEquals(URI.create("https://192.168.124.49:8443/won/resource/need/cbfgi37je6kr"), needURI);
    }
    
    @Test 
    public void testGetFacetsOfTypeOneFacet() {
        Dataset needDataset = loadTestDatasetFromClasspathResource("wonrdfutils/need1.trig");
        URI needURI = WonRdfUtils.NeedUtils.getNeedURI(needDataset);
        Collection<URI> facets = WonRdfUtils.FacetUtils.getFacetsOfType(needDataset, needURI, URI.create(WON.CHAT_FACET_STRING));
        Assert.assertEquals(1, facets.size());
        Assert.assertEquals(URI.create("https://192.168.124.49:8443/won/resource/need/cbfgi37je6kr#chatFacet"), facets.stream().findFirst().get());
        
    }
    
    @Test 
    public void testGetFacetsOfTypeTwoFacets() {
        Dataset needDataset = loadTestDatasetFromClasspathResource("wonrdfutils/need1.trig");
        URI needURI = WonRdfUtils.NeedUtils.getNeedURI(needDataset);
        Collection<URI> facets = WonRdfUtils.FacetUtils.getFacetsOfType(needDataset, needURI, URI.create(WON.GROUP_FACET_STRING));
        Assert.assertEquals(2, facets.size());
        Assert.assertTrue(facets.contains(URI.create("https://192.168.124.49:8443/won/resource/need/cbfgi37je6kr#groupFacet1")));
        Assert.assertTrue(facets.contains(URI.create("https://192.168.124.49:8443/won/resource/need/cbfgi37je6kr#groupFacet1")));
        
    }
}
