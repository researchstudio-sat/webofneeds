package won.protocol.model;

/**
 * User: Alan Tus
 * Date: 17.04.13
 * Time: 13:38
 */
public enum Event {

    ACCEPT("Accept"),
    CLOSE("Close"),
    PREPARE("Prepare"),
    OPEN("Open"),
    HINT("Hint");

    private String name;

    private Event(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return WON.BASE_URI + name;
    }
}
