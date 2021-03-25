package won.cryptography.ssl;

import org.apache.http.ssl.PrivateKeyDetails;
import org.apache.http.ssl.PrivateKeyStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.net.Socket;
import java.util.Map;
import java.util.Objects;

public class PredefinedAliasStrategy implements PrivateKeyStrategy {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private String alias;

    public PredefinedAliasStrategy(String alias) {
        this.alias = alias;
    }

    @Override
    public String chooseAlias(Map<String, PrivateKeyDetails> map, Socket socket) {
        // it is tempting to check if the map actually contains a key for the alias and
        // throw an exception if it doesn't. However, this will cause an exception where
        // none is due. This strategy is called for many key types until it's the one
        // type of our key, so we cannot perform this kind of check here.
        return alias;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PredefinedAliasStrategy that = (PredefinedAliasStrategy) o;
        return Objects.equals(alias, that.alias);
    }

    @Override
    public int hashCode() {
        return Objects.hash(alias);
    }
}
