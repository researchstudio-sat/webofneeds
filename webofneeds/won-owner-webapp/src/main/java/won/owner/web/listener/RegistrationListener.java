package won.owner.web.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import won.owner.model.EmailVerificationToken;
import won.owner.web.WonOwnerMailSender;
import won.owner.web.events.OnRegistrationCompleteEvent;
import won.owner.model.User;
import won.owner.service.impl.UserService;

/**
 * Created by fsuda on 27.11.2018.
 */
@Component
public class RegistrationListener implements ApplicationListener<OnRegistrationCompleteEvent> {
    @Autowired
    private UserService userService;

    @Autowired
    private WonOwnerMailSender emailSender;

    @Override
    public void onApplicationEvent(OnRegistrationCompleteEvent event) {
        User user = event.getUser();
        EmailVerificationToken emailVerificationToken = userService.createEmailVerificationToken(user);

        emailSender.sendVerificationMessage(user, emailVerificationToken);
    }
}
