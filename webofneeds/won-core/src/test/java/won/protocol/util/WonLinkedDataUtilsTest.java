package won.protocol.util;

import java.net.URI;
import java.util.Arrays;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;

import won.protocol.util.linkeddata.WonLinkedDataUtils;

public class WonLinkedDataUtilsTest {
    private static final String[] linkHeaders = {
                    "<http://example.org/customer-relations?p=3>; rel=\"next\"",
                    "<http://example.org/customer-relations?p=1>; rel=\"prev\"",
    };

    @Test
    public void testNextLink() {
        Optional<URI> nextLink = WonLinkedDataUtils.extractLDPNextPageLinkFromLinkHeaders(Arrays.asList(linkHeaders));
        Assert.assertTrue("No next link found", nextLink.isPresent());
        Assert.assertEquals("Extracting next link failed", URI.create("http://example.org/customer-relations?p=3"),
                        nextLink.get());
    }

    @Test
    public void testPrevLink() {
        Optional<URI> prevLink = WonLinkedDataUtils.extractLDPPrevPageLinkFromLinkHeaders(Arrays.asList(linkHeaders));
        Assert.assertTrue("No prev link found", prevLink.isPresent());
        Assert.assertEquals("Extracting prev link failed", URI.create("http://example.org/customer-relations?p=1"),
                        prevLink.get());
    }
}
