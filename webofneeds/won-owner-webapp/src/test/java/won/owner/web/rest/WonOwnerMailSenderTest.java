package won.owner.web.rest;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import won.owner.web.WonOwnerMailSender;

/**
 * User: ypanchenko
 * Date: 17.02.2015
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/spring/owner-mailer.xml"})
public class WonOwnerMailSenderTest {

  @Autowired
  private WonOwnerMailSender emailSender;

  @Autowired
  private JavaMailSenderImpl baseEmailSender;

  // This test is useful for manual testing:
  // uncomment @Test, provide your email address as receiver e-mail address,
  // then go there and verify manually if the test message was delivered correctly
  // @Test
  public void sendPrivateLinkTest() {
    emailSender.sendPrivateLink("***@example.com", "owner-url/private-link/test-link");
  }

  // a test fails if mail account data were not specified in the owner.properties
  @Test
  public void emailSenderAccountPropertiesTest() {
    Assert.assertTrue(baseEmailSender.getUsername() != null && baseEmailSender.getUsername().length() > 1);
    Assert.assertNotNull(baseEmailSender.getPassword() != null && baseEmailSender.getPassword().length() > 1);
  }

}
