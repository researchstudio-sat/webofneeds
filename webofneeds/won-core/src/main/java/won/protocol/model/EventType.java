package won.protocol.model;

import won.protocol.vocabulary.WON;

import java.net.URI;

/**
 * User: Alan Tus
 * Date: 17.04.13
 * Time: 13:38
 */
public enum EventType {

    ACCEPT("Accept"),
    CLOSE("Close"),
    PREPARE("Prepare"),
    OPEN("Open"),
    HINT("Hint");

    private String name;

    private EventType(String name) {
        this.name = name;
    }

    public URI getURI() {
        return URI.create(WON.BASE_URI + name);
    }
}
