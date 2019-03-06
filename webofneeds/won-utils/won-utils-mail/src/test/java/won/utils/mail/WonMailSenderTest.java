package won.utils.mail;

import javax.mail.Message;
import javax.mail.MessagingException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;

/**
 * User: ypanchenko
 * Date: 17.02.2015
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/mail-sender.xml"})
public class WonMailSenderTest {

  private GreenMail testSmtp;

  @Autowired
  private WonMailSender wonEmailSender;

  @Autowired
  private JavaMailSenderImpl baseMailSender;

  @Test
  public void sendTextMessage() throws MessagingException {
    SimpleMailMessage message = new SimpleMailMessage();

    // replace sender specified in mail-sender.xml with the test sender
    message.setFrom("test@sender.com");

    wonEmailSender.sendTextMessage("test@receiver.com", "test subject", "test message");

    Message[] messages = testSmtp.getReceivedMessages();
    Assert.assertTrue(messages.length == 1);
    Assert.assertEquals("test subject", messages[0].getSubject());
    String body = GreenMailUtil.getBody(messages[0]).replaceAll("=\r?\n", "");
    Assert.assertEquals("test message", body);
  }

  @Before
  public void testSmtpInit(){

    testSmtp = new GreenMail(ServerSetupTest.SMTP);
    testSmtp.start();

    // replace port and host specified in mail-sender.xml with the test port and host
    baseMailSender.setPort(3025);
    baseMailSender.setHost("localhost");
  }

  @After
  public void cleanup(){
    testSmtp.stop();
  }

}
