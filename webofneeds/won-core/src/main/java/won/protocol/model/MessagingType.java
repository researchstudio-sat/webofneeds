package won.protocol.model;

/**
 * Created with IntelliJ IDEA. User: gabriel Date: 12.09.13 Time: 18:11 To
 * change this template use File | Settings | File Templates.
 */
public enum MessagingType {
    Queue("Queue"), Topic("Topic");
    private String type;

    MessagingType(String type) {
        this.type = type;
    }
}
