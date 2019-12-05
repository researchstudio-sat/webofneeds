package won.node.service.nodebehaviour;

import java.util.Objects;
import java.util.Optional;

import won.protocol.model.ConnectionState;

public class ConnectionStateChange {
    private Optional<ConnectionState> oldState = Optional.empty();
    private ConnectionState newState;

    /**
     * Creates a ConnectionStatChange object for two states, which must be non-null.
     */
    public ConnectionStateChange(ConnectionState oldState, ConnectionState newState) {
        super();
        Objects.nonNull(oldState);
        Objects.nonNull(newState);
        this.oldState = Optional.of(oldState);
        this.newState = newState;
    }

    /**
     * Creates a ConnectionStatChange object for situations in which the connection
     * is new and there is no prior state.
     */
    public ConnectionStateChange(ConnectionState newState) {
        super();
        Objects.nonNull(newState);
        this.newState = newState;
    }

    public boolean isConnect() {
        return newState.equals(ConnectionState.CONNECTED) && (newState != oldState.orElse(null));
    }

    public boolean isDisconnect() {
        return oldState.isPresent() && oldState.get() == ConnectionState.CONNECTED && newState != oldState.orElse(null);
    }

    public Optional<ConnectionState> getOldState() {
        return oldState;
    }

    public ConnectionState getNewState() {
        return newState;
    }
}
