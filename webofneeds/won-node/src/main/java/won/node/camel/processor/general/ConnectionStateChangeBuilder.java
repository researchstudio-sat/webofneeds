package won.node.camel.processor.general;

import won.node.facet.ConnectionStateChange;
import won.protocol.model.ConnectionState;

import java.util.Objects;
import java.util.Optional;

/**
 * Simple helper class that creates a ConnectionStateChange object in two steps:
 * First, set the old state (or none if there is no connection yet), second set the
 * new state.
 */
public class ConnectionStateChangeBuilder {
  private Optional<ConnectionState> oldState = Optional.empty();
  private Optional<ConnectionState> newState = Optional.empty();

  /**
   * If there is no connection, use this constructor.
   */
  public ConnectionStateChangeBuilder() {
  }

  /**
   * Set the old connection state. If this method is not used, we'll assume there was no old connection.
   */
  public ConnectionStateChangeBuilder oldState(ConnectionState oldState) {
    Objects.nonNull(oldState);
    this.oldState = Optional.of(oldState);
    return this;
  }

  /**
   * Provide the new connection state.
   */
  public ConnectionStateChangeBuilder newState(ConnectionState newState) {
    Objects.nonNull(newState);
    this.newState = Optional.of(newState);
    return this;
  }

  public boolean canBuild() {
    return newState.isPresent();
  }

  public ConnectionStateChange build() {
    if (!canBuild()) {
      throw new IllegalStateException("Cannot build ConnectionStateChange without a new state");
    }
    if (oldState.isPresent()) {
      return new ConnectionStateChange(oldState.get(), newState.get());
    } else {
      return new ConnectionStateChange(newState.get());
    }
  }
}
