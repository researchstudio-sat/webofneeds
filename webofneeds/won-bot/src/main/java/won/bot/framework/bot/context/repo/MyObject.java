package won.bot.framework.bot.context.repo;

import org.springframework.data.annotation.Id;

/**
 * Created by hfriedrich on 24.10.2016.
 */
public class MyObject
{
  @Id
  private String id;

  private Object object;

  public String getId() {
    return id;
  }

  public void setId(final String id) {
    this.id = id;
  }

  public Object getObject() {
    return object;
  }

  public void setObject(final Object object) {
    this.object = object;
  }
}
