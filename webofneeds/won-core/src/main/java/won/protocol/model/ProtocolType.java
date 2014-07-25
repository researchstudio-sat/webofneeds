package won.protocol.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.vocabulary.WON;

import java.net.URI;

/**
 * Created with IntelliJ IDEA.
 * User: gabriel
 * Date: 12.09.13
 * Time: 18:11
 * To change this template use File | Settings | File Templates.
 */
public enum ProtocolType {
    OwnerProtocol("OwnerProtocol"),
    NeedProtocol("NeedProtocol"),
    MatcherProtocol("MatcherProtocol");

    private static final Logger logger = LoggerFactory.getLogger(BasicNeedType.class);

    private String name;

    private ProtocolType(String name)
    {
        this.name = name;
    }

}
