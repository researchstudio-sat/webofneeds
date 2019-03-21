package won.owner.web.events;

import org.springframework.context.ApplicationEvent;
import won.owner.model.User;

/**
 * Created by fsuda on 27.11.2018. This Event is used/published as a Result of a
 * successful password change.
 */
public class OnRecoveryKeyGeneratedEvent extends ApplicationEvent {
  private User user;
  private String recoveryKey;

  public OnRecoveryKeyGeneratedEvent(User user, String recoveryKey) {
    super(user);
    this.user = user;
    this.recoveryKey = recoveryKey;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public String getRecoveryKey() {
    return recoveryKey;
  }

  public void setRecoveryKey(String recoveryKey) {
    this.recoveryKey = recoveryKey;
  }
}
