package won.owner.web.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import won.owner.model.User;
import won.owner.service.impl.UserService;
import won.owner.web.WonOwnerMailSender;
import won.owner.web.events.OnRecoveryKeyGeneratedEvent;

@Component
public class RecoveryKeyGeneratedEMailListener implements ApplicationListener<OnRecoveryKeyGeneratedEvent> {
  @Autowired
  private UserService userService;

  @Autowired
  private WonOwnerMailSender emailSender;

  @Override
  public void onApplicationEvent(OnRecoveryKeyGeneratedEvent event) {
    User user = event.getUser();
    String recoveryKey = event.getRecoveryKey();
    emailSender.sendRecoveryKeyGeneratedMessage(user, recoveryKey);
  }
}
