package won.utils.dns;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DnsMappingAdder {
    private static Pattern IP_ADDRESS = Pattern.compile("(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})");

    /**
     * Inserts a mapping into the JVM's dns cache, as if the entry was found in a
     * '/etc/hosts' file.
     *
     * @param hostname a legal host name
     * @param address four byte address, e.g. {127,0,0,1}
     */
    public static void addDnsMapping(final String hostname, final byte[] address) {
        try {
            Objects.requireNonNull(hostname);
            Objects.requireNonNull(address);
            if (address.length != 4) {
                throw new IllegalArgumentException("address must be of length 4, but was " + Arrays.toString(address));
            }
            Class<?> addressesClass = Class.forName("java.net.InetAddress$Addresses");
            Field cacheField = InetAddress.class.getDeclaredField("cache");
            cacheField.setAccessible(true);
            ConcurrentMap cache = (ConcurrentMap) cacheField.get(InetAddress.class);
            InvocationHandler inetAddressesProxy = new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if (method.getName().equals("get")) {
                        InetAddress localhost = InetAddress.getByAddress(hostname, address);
                        return new InetAddress[] { localhost };
                    } else {
                        throw new UnsupportedOperationException(
                                        "Cannot handle method call '" + method + "' on proxy " + proxy);
                    }
                }
            };
            Object addressesProxy = Proxy.newProxyInstance(InetAddress.class.getClassLoader(),
                            new Class[] { addressesClass }, inetAddressesProxy);
            cache.put(hostname, addressesProxy);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot set hostname mapping ' " + hostname + "' -> '" + Arrays
                            .toString(address) + "':", e);
        }
    }

    /**
     * Inserts a mapping into the JVM's dns cache, as if the entry was found in a
     * '/etc/hosts' file.
     *
     * @param hostname a legal host name
     * @param address four byte address as a string, e.g. "127.0.0.1"
     */
    public static void addDnsMapping(final String hostname, final String address) {
        Matcher m = IP_ADDRESS.matcher(address);
        if (!m.matches()) {
            throw new IllegalArgumentException("Not an IP address: " + address);
        }
        addDnsMapping(hostname, new byte[] {
                        Byte.parseByte(m.group(1)),
                        Byte.parseByte(m.group(2)),
                        Byte.parseByte(m.group(3)),
                        Byte.parseByte(m.group(4)) });
    }
}
