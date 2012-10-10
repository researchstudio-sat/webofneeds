package won.server.need;

/**
 * Created with IntelliJ IDEA.
 * User: fsalcher
 * Date: 10.10.12
 * Time: 11:27
 * To change this template use File | Settings | File Templates.
 */
public interface Need {

    public String getID();

    public void setState(NeedState state);

}
