package won.node.service.nodeconfig;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URI;
import java.util.List;

public class URIServiceTest {
    private static URIService uriService;
    private static List<URI> messageURIs = List.of(URI.create("https://wonnode/won/resource/msg/abc123"),
                    URI.create("https://wonnode/won/resource/msg/abc123?some=param"),
                    URI.create("https://wonnode/won/resource/msg/abc123?some=param&another=param"));
    private static List<URI> atomUris = List.of(URI.create("https://wonnode/won/resource/atom/abc123"),
                    URI.create("https://wonnode/won/resource/atom/abc123?some=param"),
                    URI.create("https://wonnode/won/resource/atom/abc123?some=param&another=param"));
    private static List<URI> atomMessagesUris = List
                    .of(URI.create("https://wonnode/won/resource/atom/abc123/msg"),
                                    URI.create("https://wonnode/won/resource/atom/abc123/msg?some=param"),
                                    URI.create("https://wonnode/won/resource/atom/abc123/msg?some=param&another=param"));
    private static List<URI> tokenEndpointUris = List.of(URI.create("https://wonnode/won/resource/atom/abc123/token"),
                    URI.create("https://wonnode/won/resource/atom/abc123/token?some=param"),
                    URI.create("https://wonnode/won/resource/atom/abc123/token?some=param&another=param"));
    private static List<URI> connectionContainerUris = List.of(URI.create("https://wonnode/won/resource/atom/abc123/c"),
                    URI.create("https://wonnode/won/resource/atom/abc123/c?some=param"),
                    URI.create("https://wonnode/won/resource/atom/abc123/c?some=param&another=param"));
    private static List<URI> connectionUris = List.of(URI.create("https://wonnode/won/resource/atom/abc123/c/def456"),
                    URI.create("https://wonnode/won/resource/atom/abc123/c/def456?some=param"),
                    URI.create("https://wonnode/won/resource/atom/abc123/c/def456?some=param&another=param"));
    private static List<URI> connectionMessagesUris = List
                    .of(URI.create("https://wonnode/won/resource/atom/abc123/c/def456/msg/"),
                                    URI.create("https://wonnode/won/resource/atom/abc123/c/def456/msg?some=param"),
                                    URI.create("https://wonnode/won/resource/atom/abc123/c/def456/msg?some=param&another=param"));
    private static List<URI> fragmentUris = List.of(
                    URI.create("https://wonnode/won/resource/atom/abc123#sub"),
                    URI.create("https://wonnode/won/resource/atom/abc123/token#sub"),
                    URI.create("https://wonnode/won/resource/atom/abc123/msg#sub"),
                    URI.create("https://wonnode/won/resource/atom/abc123/c#sub"),
                    URI.create("https://wonnode/won/resource/atom/abc123/c/def456#sub"),
                    URI.create("https://wonnode/won/resource/atom/abc123/c/def456/msg#sub"),
                    URI.create("https://wonnode/won/resource/msg/abc123#sub"));

    @BeforeClass
    public static void prepareUriService() throws Exception {
        uriService = new URIService();
        uriService.setGeneralURIPrefix("https://wonnode/won");
        uriService.setDataURIPrefix("https://wonnode/won/data");
        uriService.setPageURIPrefix("https://wonnode/won/page");
        uriService.setResourceURIPrefix("https://wonnode/won/resource");
        uriService.afterPropertiesSet();
    }

    @Test
    public void isMessageURI() {
        atomUris.forEach(u -> Assert.assertFalse("test failed for: " + u, uriService.isMessageURI(u)));
        atomMessagesUris.forEach(u -> Assert.assertFalse("test failed for: " + u, uriService.isMessageURI(u)));
        tokenEndpointUris.forEach(u -> Assert.assertFalse("test failed for: " + u, uriService.isMessageURI(u)));
        connectionContainerUris.forEach(u -> Assert.assertFalse("test failed for: " + u, uriService.isMessageURI(u)));
        connectionUris.forEach(u -> Assert.assertFalse("test failed for: " + u, uriService.isMessageURI(u)));
        connectionMessagesUris.forEach(u -> Assert.assertFalse("test failed for: " + u, uriService.isMessageURI(u)));
        messageURIs.forEach(u -> Assert.assertTrue("test failed for: " + u, uriService.isMessageURI(u)));
        fragmentUris.forEach(u -> Assert.assertFalse("test failed for: " + u, uriService.isMessageURI(u)));
    }

    @Test
    public void isAtomURI() {
        atomUris.forEach(u -> Assert.assertTrue("test failed for: " + u, uriService.isAtomURI(u)));
        atomMessagesUris.forEach(u -> Assert.assertFalse("test failed for: " + u, uriService.isAtomURI(u)));
        tokenEndpointUris.forEach(u -> Assert.assertFalse("test failed for: " + u, uriService.isAtomURI(u)));
        connectionContainerUris.forEach(u -> Assert.assertFalse("test failed for: " + u, uriService.isAtomURI(u)));
        connectionUris.forEach(u -> Assert.assertFalse("test failed for: " + u, uriService.isAtomURI(u)));
        connectionMessagesUris.forEach(u -> Assert.assertFalse("test failed for: " + u, uriService.isAtomURI(u)));
        messageURIs.forEach(u -> Assert.assertFalse("test failed for: " + u, uriService.isAtomURI(u)));
        fragmentUris.forEach(u -> Assert.assertFalse("test failed for: " + u, uriService.isAtomURI(u)));
    }

    @Test
    public void isAtomMessagesURI() {
        atomUris.forEach(u -> Assert.assertFalse("test failed for: " + u, uriService.isAtomMessagesURI(u)));
        atomMessagesUris.forEach(u -> Assert.assertTrue("test failed for: " + u, uriService.isAtomMessagesURI(u)));
        tokenEndpointUris.forEach(u -> Assert.assertFalse("test failed for: " + u, uriService.isAtomMessagesURI(u)));
        connectionContainerUris
                        .forEach(u -> Assert.assertFalse("test failed for: " + u, uriService.isAtomMessagesURI(u)));
        connectionUris.forEach(u -> Assert.assertFalse("test failed for: " + u, uriService.isAtomMessagesURI(u)));
        connectionMessagesUris
                        .forEach(u -> Assert.assertFalse("test failed for: " + u, uriService.isAtomMessagesURI(u)));
        messageURIs.forEach(u -> Assert.assertFalse("test failed for: " + u, uriService.isAtomMessagesURI(u)));
        fragmentUris.forEach(u -> Assert.assertFalse("test failed for: " + u, uriService.isAtomMessagesURI(u)));
    }

    @Test
    public void isAtomUnreadURI() {
        uriService.isAtomUnreadURI(null);
    }

    @Test
    public void isTokenEndpointURI() {
        atomUris.forEach(u -> Assert.assertFalse("test failed for: " + u, uriService.isTokenEndpointURI(u)));
        atomMessagesUris.forEach(u -> Assert.assertFalse("test failed for: " + u, uriService.isTokenEndpointURI(u)));
        tokenEndpointUris.forEach(u -> Assert.assertTrue("test failed for: " + u, uriService.isTokenEndpointURI(u)));
        connectionContainerUris
                        .forEach(u -> Assert.assertFalse("test failed for: " + u, uriService.isTokenEndpointURI(u)));
        connectionUris.forEach(u -> Assert.assertFalse("test failed for: " + u, uriService.isTokenEndpointURI(u)));
        connectionMessagesUris
                        .forEach(u -> Assert.assertFalse("test failed for: " + u, uriService.isTokenEndpointURI(u)));
        messageURIs.forEach(u -> Assert.assertFalse("test failed for: " + u, uriService.isTokenEndpointURI(u)));
        fragmentUris.forEach(u -> Assert.assertFalse("test failed for: " + u, uriService.isTokenEndpointURI(u)));
    }

    @Test
    public void isConnectionContainerURI() {
        atomUris.forEach(u -> Assert.assertFalse("test failed for: " + u, uriService.isConnectionContainerURI(u)));
        atomMessagesUris.forEach(
                        u -> Assert.assertFalse("test failed for: " + u, uriService.isConnectionContainerURI(u)));
        tokenEndpointUris.forEach(
                        u -> Assert.assertFalse("test failed for: " + u, uriService.isConnectionContainerURI(u)));
        connectionContainerUris.forEach(
                        u -> Assert.assertTrue("test failed for: " + u, uriService.isConnectionContainerURI(u)));
        connectionUris.forEach(
                        u -> Assert.assertFalse("test failed for: " + u, uriService.isConnectionContainerURI(u)));
        connectionMessagesUris.forEach(
                        u -> Assert.assertFalse("test failed for: " + u, uriService.isConnectionContainerURI(u)));
        messageURIs.forEach(u -> Assert.assertFalse("test failed for: " + u, uriService.isConnectionContainerURI(u)));
        fragmentUris.forEach(u -> Assert.assertFalse("test failed for: " + u, uriService.isConnectionContainerURI(u)));
    }

    @Test
    public void isConnectionURI() {
        atomUris.forEach(u -> Assert.assertFalse("test failed for: " + u, uriService.isConnectionURI(u)));
        atomMessagesUris.forEach(u -> Assert.assertFalse("test failed for: " + u, uriService.isConnectionURI(u)));
        tokenEndpointUris.forEach(u -> Assert.assertFalse("test failed for: " + u, uriService.isConnectionURI(u)));
        connectionContainerUris
                        .forEach(u -> Assert.assertFalse("test failed for: " + u, uriService.isConnectionURI(u)));
        connectionUris.forEach(u -> Assert.assertTrue("test failed for: " + u, uriService.isConnectionURI(u)));
        connectionMessagesUris.forEach(u -> Assert.assertFalse("test failed for: " + u, uriService.isConnectionURI(u)));
        messageURIs.forEach(u -> Assert.assertFalse("test failed for: " + u, uriService.isConnectionURI(u)));
        fragmentUris.forEach(u -> Assert.assertFalse("test failed for: " + u, uriService.isConnectionURI(u)));
    }

    @Test
    public void isConnectionMessagesURI() {
        atomUris.forEach(u -> Assert.assertFalse("test failed for: " + u, uriService.isConnectionMessagesURI(u)));
        atomMessagesUris.forEach(
                        u -> Assert.assertFalse("test failed for: " + u, uriService.isConnectionMessagesURI(u)));
        tokenEndpointUris.forEach(
                        u -> Assert.assertFalse("test failed for: " + u, uriService.isConnectionMessagesURI(u)));
        connectionContainerUris.forEach(
                        u -> Assert.assertFalse("test failed for: " + u, uriService.isConnectionMessagesURI(u)));
        connectionUris.forEach(u -> Assert.assertFalse("test failed for: " + u, uriService.isConnectionMessagesURI(u)));
        connectionMessagesUris.forEach(
                        u -> Assert.assertTrue("test failed for: " + u, uriService.isConnectionMessagesURI(u)));
        messageURIs.forEach(u -> Assert.assertFalse("test failed for: " + u, uriService.isConnectionMessagesURI(u)));
        fragmentUris.forEach(u -> Assert.assertFalse("test failed for: " + u, uriService.isConnectionMessagesURI(u)));
    }

    @Test
    public void getConnectionURIofConnectionMessagesURI() {
        Assert.assertEquals(URI.create("https://wonnode/won/resource/atom/abc123/c/def456"),
                        uriService.getConnectionURIofConnectionMessagesURI(
                                        URI.create("https://wonnode/won/resource/atom/abc123/c/def456/msg/")));
    }

    @Test
    public void getAtomURIofAtomMessagesURI() {
        Assert.assertEquals(URI.create("https://wonnode/won/resource/atom/abc123"),
                        uriService.getAtomURIofAtomMessagesURI(
                                        URI.create("https://wonnode/won/resource/atom/abc123/msg")));
    }

    @Test
    public void getAtomURIofSubURI() {
        Assert.assertEquals(URI.create("https://wonnode/won/resource/atom/abc123"),
                        uriService.getAtomURIofSubURI(
                                        URI.create("https://wonnode/won/resource/atom/abc123#sub")));
    }

    @Test
    public void getAtomURIofAtomUnreadURI() {
        Assert.assertEquals(URI.create("https://wonnode/won/resource/atom/abc123"),
                        uriService.getAtomURIofAtomUnreadURI(
                                        URI.create("https://wonnode/won/resource/atom/abc123/unread?some=param")));
    }

    @Test
    public void toDataURIIfPossible() {
        Assert.assertEquals(URI.create("https://wonnode/won/data/atom/abc123/unread?some=param"),
                        uriService.toDataURIIfPossible(
                                        URI.create("https://wonnode/won/resource/atom/abc123/unread?some=param")));
    }

    @Test
    public void toPageURIIfPossible() {
        Assert.assertEquals(URI.create("https://wonnode/won/page/atom/abc123/unread?some=param"),
                        uriService.toPageURIIfPossible(
                                        URI.create("https://wonnode/won/resource/atom/abc123/unread?some=param")));
    }

    @Test
    public void toResourceURIIfPossible() {
        Assert.assertEquals(URI.create("https://wonnode/won/resource/atom/abc123/unread?some=param"),
                        uriService.toResourceURIIfPossible(
                                        URI.create("https://wonnode/won/data/atom/abc123/unread?some=param")));
    }

    @Test
    public void createAtomURIForId() {
        Assert.assertEquals(URI.create("https://wonnode/won/resource/atom/abc123"),
                        uriService.createAtomURIForId("abc123"));
    }

    @Test
    public void createConnectionURIForId() {
        Assert.assertEquals(URI.create("https://wonnode/won/resource/atom/abc123/c/def456"),
                        uriService.createConnectionURIForId("abc123", "def456"));
    }

    @Test
    public void createConnectionContainerURIForAtom() {
        Assert.assertEquals(URI.create("https://wonnode/won/resource/atom/abc123/c"),
                        uriService.createConnectionContainerURIForAtom(
                                        URI.create("https://wonnode/won/resource/atom/abc123")));
    }

    @Test
    public void createMessageContainerURIForConnection() {
        Assert.assertEquals(URI.create("https://wonnode/won/resource/atom/abc123/c/def456/msg"),
                        uriService.createMessageContainerURIForConnection(
                                        URI.create("https://wonnode/won/resource/atom/abc123/c/def456")));
    }

    @Test
    public void createMessageURIForId() {
        Assert.assertEquals(URI.create("wm:/xyz789"),
                        uriService.createMessageURIForId("xyz789"));
    }

    @Test
    public void createAttachmentURIForId() {
        uriService.createAttachmentURIForId(null);
    }

    @Test
    public void createAclGraphURIForAtomURI() {
        Assert.assertEquals(URI.create("https://wonnode/won/resource/atom/abc123#acl"),
                        uriService.createAclGraphURIForAtomURI(
                                        URI.create("https://wonnode/won/resource/atom/abc123")));
    }

    @Test
    public void createSysInfoGraphURIForAtomURI() {
        Assert.assertEquals(URI.create("https://wonnode/won/resource/atom/abc123#sysinfo"),
                        uriService.createSysInfoGraphURIForAtomURI(
                                        URI.create("https://wonnode/won/resource/atom/abc123")));
    }

    public void toLocalMessageURI() {
        Assert.assertEquals(URI.create("https://wonnode/won/resource/msg/xyz789"),
                        uriService.createMessageURIForId("wm:/xyz789"));
    }

    @Test
    public void toGenericMessageURI() {
        Assert.assertEquals(URI.create("wm:/xyz789"),
                        uriService.toGenericMessageURI(URI.create("https://wonnode/won/resource/msg/xyz789")));
    }
}
