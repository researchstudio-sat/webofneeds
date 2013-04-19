package won.owner.pojo;

/**
 * Created with IntelliJ IDEA.
 * User: Gabriel
 * Date: 19.12.12
 * Time: 11:44
 */
public class NeedPojo {
    private String wonNode;
    private boolean active;
    private String textDescription;
    private String needURI;

    public void setWonNode(String wonNode) {
        this.wonNode = wonNode;
    }

    public String getWonNode() {
        return wonNode;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getTextDescription() {
        return textDescription;
    }

    public void setTextDescription(String textDescription) {
        this.textDescription = textDescription;
    }

    public String getNeedURI() {
        return needURI;
    }

    public void setNeedURI(String needURI) {
        this.needURI = needURI;
    }
}
