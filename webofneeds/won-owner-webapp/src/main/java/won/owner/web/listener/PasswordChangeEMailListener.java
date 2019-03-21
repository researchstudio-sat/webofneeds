package won.owner.web.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import won.owner.model.User;
import won.owner.service.impl.UserService;
import won.owner.web.WonOwnerMailSender;
import won.owner.web.events.OnPasswordChangedEvent;

@Component public class PasswordChangeEMailListener implements ApplicationListener<OnPasswordChangedEvent> {
  @Autowired private UserService userService;

  @Autowired private WonOwnerMailSender emailSender;

  @Override public void onApplicationEvent(OnPasswordChangedEvent event) {
    User user = event.getUser();
    emailSender.sendPasswordChangedMessage(user);
  }
}
