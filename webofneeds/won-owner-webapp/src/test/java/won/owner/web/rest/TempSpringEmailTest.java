package won.owner.web.rest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * User: ypanchenko
 * Date: 17.02.2015
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/spring/owner-mailer.xml"})
public class TempSpringEmailTest
{

  @Autowired
  private TempSpringEmail emailSender;

  @Test
  public void autowiredSpringEmailTest() {
    emailSender.sendPrivateLink("yana.panchenko@researchstudio.at", "test-link");
  }

}
