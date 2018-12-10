package won.owner.web.rest;

import org.junit.Assert;
import org.junit.Ignore;
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
@ContextConfiguration(locations = {"classpath:/spring/owner-context.xml"})
public class WonOwnerMailSenderTest {

  private String email = null; // TODO: insert email address for testing here
  private String localNeedUri = null; // TODO: insert some local need URI for testing here
  private String remoteNeedUri = null; // TODO: insert some remote need URI for testing here
  private String localConnectionUri = "testLocalConnection";
  private String textMessage = "This is a test text message!";

  @Autowired
  private WonOwnerMailSender emailSender;

  @Autowired
  private JavaMailSenderImpl baseEmailSender;

  // This test is useful for manual testing:
  // uncomment @Test, provide your email address as receiver e-mail address as well as the local and remote need uris
  // to test, then go there and verify manually if the test message was delivered correctly
  //@Test
  public void sendConversationNotificationMessageTest() {
    emailSender.sendConversationNotificationMessage(email, localNeedUri, remoteNeedUri, localConnectionUri, textMessage);
  }

  //@Test
  public void sendConnectNotificationMessageTest() {
    emailSender.sendConnectNotificationMessage(email, localNeedUri, remoteNeedUri, localConnectionUri, textMessage);
  }

  //@Test
  public void sendCloseNotificationMessageTest() {
    emailSender.sendCloseNotificationMessage(email, localNeedUri, remoteNeedUri, localConnectionUri, textMessage);
  }

  //@Test
  public void sendHintNotificationMessageTest() {
    emailSender.sendHintNotificationMessage(email, localNeedUri, remoteNeedUri, localConnectionUri);
  }

  // a test fails if mail account data were not specified in the owner.properties
  @Test
  @Ignore //we don't want to require setting WON_CONFIG_DIR during the build
  public void emailSenderAccountPropertiesTest() {
    Assert.assertTrue(baseEmailSender.getUsername() != null && baseEmailSender.getUsername().length() > 1);
    Assert.assertNotNull(baseEmailSender.getPassword() != null && baseEmailSender.getPassword().length() > 1);
  }

}
