package won.protocol.model;

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

    private String name;

    private ProtocolType(String name)
    {
        this.name = name;
    }

}
