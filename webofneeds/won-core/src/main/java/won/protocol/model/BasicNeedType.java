package won.protocol.model;

/**
 * User: Alan Tus
 * Date: 17.04.13
 * Time: 13:38
 */
public enum BasicNeedType {

    TAKE("Take"),
    GIVE("Give"),
    DO("Do"),;

    private String name;

    private BasicNeedType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return WON.BASE_URI + name;
    }
}
