package component.scan;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import scan.Person;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/component-scan.xml"})
public class ComponentScanTest
{

  @Autowired
  Person person;

  public void setTest(final Person person) {
    this.person = person;
  }

  @Test
  public void testCreateFromOwner() throws Exception {
    person.sayHello();
  }

}
