package won.protocol.vocabulary;

import java.lang.invoke.MethodHandles;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User: atus Date: 23.04.13
 */
public enum GRDeliveryMethod {
    DELIVERY_MODE_DIRECT_DOWNLOAD("DeliveryModeDirectDownload"), DELIVERY_MODE_FREIGHT("DeliveryModeFreight"),
    DELIVERY_MODE_MAIL("DeliveryModeMail"), DELIVERY_MODE_OWN_FLEET("DeliveryModeOwnFleet"),
    DELIVERY_MODE_PICK_UP("DeliveryModePickUp");
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private String name;

    GRDeliveryMethod(String name) {
        this.name = name;
    }

    public URI getURI() {
        return URI.create(GR.BASE_URI + name);
    }

    /**
     * Tries to match the given string against all enum values.
     *
     * @param fragment string to match
     * @return matched enum, null otherwise
     */
    public static GRDeliveryMethod parseString(final String fragment) {
        for (GRDeliveryMethod state : values())
            if (state.name.equals(fragment))
                return state;
        logger.warn("No enum could be matched for: {}", fragment);
        return null;
    }
}
