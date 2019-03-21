package won.bot.framework.bot.context;

import org.springframework.data.annotation.Id;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by hfriedrich on 21.11.2016.
 */
public class MongoContextObjectList {
  @Id
  private String id;

  private List<Object> objectList;

  public MongoContextObjectList() {

    id = null;
    objectList = null;
  }

  public MongoContextObjectList(String id) {
    this.id = id;
    objectList = new ArrayList<>();
  }

  public String getId() {
    return id;
  }

  public List<Object> getList() {
    return objectList;
  }
}
