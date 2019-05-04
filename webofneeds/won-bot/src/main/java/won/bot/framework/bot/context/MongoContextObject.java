package won.bot.framework.bot.context;

import org.springframework.data.annotation.Id;

/**
 * Wrapper object for storing generic java objects in the mongo db database.
 * This objects allows to specify the the id and the value (java object) to
 * store. Created by hfriedrich on 24.10.2016.
 */
public class MongoContextObject {
    @Id
    private String id;
    private Object object;

    public MongoContextObject(String id, Object object) {
        this.id = id;
        this.object = object;
    }

    public String getId() {
        return id;
    }

    public Object getObject() {
        return object;
    }
}
