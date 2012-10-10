package won.server.need;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: fsalcher
 * Date: 10.10.12
 * Time: 11:27
 * To change this template use File | Settings | File Templates.
 */
public interface NeedService {

    public Need createNeed(String title, String description);

    public Need getNeed(String needID);

    public List<Match> getMatchList(Need need);
    public List<Match> getMatchList(String needID);

    public Match getMatch(String matchID);

    public Match matchNotification(String localNeedID, String remoteNeedID, String remoteNeedServerURL);

}
