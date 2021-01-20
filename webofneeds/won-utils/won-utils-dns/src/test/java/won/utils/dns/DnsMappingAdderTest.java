package won.utils.dns;

import org.junit.Assert;
import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

public class DnsMappingAdderTest {
    @Test(expected = IllegalArgumentException.class)
    public void testDnsMapping_wrongAddressLength() {
        DnsMappingAdder.addDnsMapping("xyz", new byte[] { 123 });
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDnsMapping_nullHostname_wrongAddressLength() {
        DnsMappingAdder.addDnsMapping(null, new byte[] { 123 });
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDnsMapping_nullAddress() {
        DnsMappingAdder.addDnsMapping("xyz", (byte[]) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDnsMapping_nullArgsBytes() {
        DnsMappingAdder.addDnsMapping(null, (byte[]) null);
    }

    @Test(expected = NullPointerException.class)
    public void testDnsMapping_nullArgsString() {
        DnsMappingAdder.addDnsMapping(null, (String) null);
    }

    @Test
    public void testDnsMapping_ok_bytes_address() throws UnknownHostException {
        byte[] address = new byte[] { 10, 0, 0, 71 };
        String hostname = "example.com";
        DnsMappingAdder.addDnsMapping(hostname, address);
        InetAddress[] entries = InetAddress.getAllByName("example.com");
        Assert.assertNotNull(entries);
        Assert.assertEquals(1, entries.length);
        Assert.assertTrue("address not equal", Arrays.equals(address, entries[0].getAddress()));
        Assert.assertEquals("hostname not equal", hostname, entries[0].getHostName());
    }

    @Test
    public void testDnsMapping_ok_string_address() throws UnknownHostException {
        byte[] address = new byte[] { 10, 0, 0, 71 };
        String addressString = "10.0.0.71";
        String hostname = "example.com";
        DnsMappingAdder.addDnsMapping(hostname, addressString);
        InetAddress[] entries = InetAddress.getAllByName("example.com");
        Assert.assertNotNull(entries);
        Assert.assertEquals(1, entries.length);
        Assert.assertTrue("address not equal", Arrays.equals(address, entries[0].getAddress()));
        Assert.assertEquals("hostname not equal", hostname, entries[0].getHostName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDnsMapping_wrong_string_address() throws UnknownHostException {
        String addressString = "10.0.71";
        String hostname = "example.com";
        DnsMappingAdder.addDnsMapping(hostname, addressString);
    }
}
