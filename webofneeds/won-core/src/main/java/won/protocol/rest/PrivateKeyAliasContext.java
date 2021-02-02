package won.protocol.rest;

public class PrivateKeyAliasContext {
    private static ThreadLocal<String> privateKeyAlias = new ThreadLocal<String>();

    public static String getPrivateKeyAlias() {
        return privateKeyAlias.get();
    }

    public static void setPrivateKeyAlias(String alias) {
        privateKeyAlias.set(alias);
    }
}
