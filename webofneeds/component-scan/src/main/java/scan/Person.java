package scan;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sat.SAT;

/**
 * User: syim
 * Date: 13.03.2015
 */
@Component
public class Person
{
  public void setWorld(final World world) {
    this.world = world;
  }

  @Autowired
  World world;

  @Autowired
  SAT sat;

  public void sayHello(){
    System.out.println("Hello, " + world.getWorld() + "!");
  }
}
