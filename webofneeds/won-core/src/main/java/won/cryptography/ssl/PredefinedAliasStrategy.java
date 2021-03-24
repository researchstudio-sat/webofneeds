package won.cryptography.ssl;

import org.apache.http.ssl.PrivateKeyDetails;
import org.apache.http.ssl.PrivateKeyStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.net.Socket;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class PredefinedAliasStrategy implements PrivateKeyStrategy {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private String alias;

    public PredefinedAliasStrategy(String alias) {
        this.alias = alias;
    }

    @Override
    public String chooseAlias(Map<String, PrivateKeyDetails> map, Socket socket) {
        if (!map.containsKey(alias)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Error choosing private key alias {}, available values are:\n {}", alias,
                                map.keySet().stream().collect(
                                                Collectors.joining("\n")));
            }
            throw new IllegalStateException("Trying to select private key alias " + alias + " that is not available");
        }
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
