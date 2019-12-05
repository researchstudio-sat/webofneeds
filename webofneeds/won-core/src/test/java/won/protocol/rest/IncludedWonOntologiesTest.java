package won.protocol.rest;

import java.net.URI;
import java.util.Optional;

import org.apache.jena.rdf.model.Model;
import org.junit.Assert;
import org.junit.Test;

import won.protocol.util.linkeddata.IncludedWonOntologies;

public class IncludedWonOntologiesTest {
    @Test
    public void testCore() {
        String uri = "https://w3id.org/won/core";
        String name = "won-core";
        assertPresent(uri, name);
    }

    @Test
    public void testHold() {
        assertPresent("https://w3id.org/won/ext/hold", "ext-hold");
    }

    @Test
    public void testInexistent() {
        Optional<Model> m = IncludedWonOntologies.get(URI.create("https://example.com/dontfindthis"));
        Assert.assertFalse("Should not have found an ontology for uri https://example.com/dontfindthis", m.isPresent());
    }

    public void assertPresent(String uri, String name) {
        Optional<Model> m = IncludedWonOntologies.get(URI.create(uri));
        Assert.assertTrue("Could not load " + name + "(" + uri + ")", m.isPresent());
        Assert.assertFalse("Returned model is empty " + name + "(" + uri + ")", m.get().isEmpty());
    }
}
