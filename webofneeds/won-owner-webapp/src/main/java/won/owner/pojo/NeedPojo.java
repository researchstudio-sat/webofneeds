package won.owner.pojo;

/**
 * Created with IntelliJ IDEA.
 * User: Gabriel
 * Date: 19.12.12
 * Time: 11:44
 */
public class NeedPojo {
    private String needURI;
    private String wonNode;
    private boolean active;

    public String getNeedURI() {
        return needURI;
    }

    public void setWonNode(String wonNode) {
        this.wonNode = wonNode;
    }

    public String getWonNode() {
        return wonNode;
    }

    public void setNeedURI(String needURI) {
        this.needURI = needURI;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
