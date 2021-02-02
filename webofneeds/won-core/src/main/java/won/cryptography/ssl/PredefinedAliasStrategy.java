package won.cryptography.ssl;

import org.apache.http.ssl.PrivateKeyDetails;
import org.apache.http.ssl.PrivateKeyStrategy;

import java.net.Socket;
import java.util.Map;
import java.util.Objects;

public class PredefinedAliasStrategy implements PrivateKeyStrategy {
    private String alias;

    public PredefinedAliasStrategy(String alias) {
        this.alias = alias;
    }

    @Override
    public String chooseAlias(Map<String, PrivateKeyDetails> map, Socket socket) {
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
