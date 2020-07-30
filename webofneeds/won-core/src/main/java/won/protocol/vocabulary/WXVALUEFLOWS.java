package won.protocol.vocabulary;

public class WXVALUEFLOWS {
    public static final String BASE_URI = "https://w3id.org/won/ext/valueflows#";
    public static final String DEFAULT_PREFIX = "wx-vf";
    public static final ResourceWrapper PrimaryAccountableSocket = ResourceWrapper
                    .create(BASE_URI + "PrimaryAccountableSocket");
    public static final ResourceWrapper PrimaryAccountableOfSocket = ResourceWrapper
                    .create(BASE_URI + "PrimaryAccountableOfSocket");
    public static final ResourceWrapper CustodianSocket = ResourceWrapper.create(BASE_URI + "CustodianSocket");
    public static final ResourceWrapper CustodianOfSocket = ResourceWrapper.create(BASE_URI + "CustodianOfSocket");
    public static final ResourceWrapper ActorActivitySocket = ResourceWrapper.create(BASE_URI + "ActorActivitySocket");
    public static final ResourceWrapper ResourceSocket = ResourceWrapper.create(BASE_URI + "ResourceSocket");
    public static final ResourceWrapper ResourceActivitySocket = ResourceWrapper
                    .create(BASE_URI + "ResourceActivitySocket");
    public static final ResourceWrapper ActorSocket = ResourceWrapper.create(BASE_URI + "ActorSocket");
    public static final ResourceWrapper PartnerActivitySocket = ResourceWrapper
                    .create(BASE_URI + "PartnerActivitySocket");
    public static final ResourceWrapper SupporterSocket = ResourceWrapper.create(BASE_URI + "SupporterSocket");
    public static final ResourceWrapper SupportableSocket = ResourceWrapper.create(BASE_URI + "SupportableSocket");
}
