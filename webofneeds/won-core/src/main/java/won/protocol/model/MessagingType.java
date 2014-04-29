package won.protocol.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: gabriel
 * Date: 12.09.13
 * Time: 18:11
 * To change this template use File | Settings | File Templates.
 */
public enum MessagingType
{
    Queue("Queue"),
    Topic("Topic");

    private static final Logger logger = LoggerFactory.getLogger(BasicNeedType.class);

    private String type;

    private MessagingType(String type)
    {
        this.type = type;
    }

}
