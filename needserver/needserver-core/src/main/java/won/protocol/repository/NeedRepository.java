package won.protocol.repository;

import won.protocol.model.Need;

import java.net.URI;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Gabriel
 * Date: 02.11.12
 * Time: 15:28
 * To change this template use File | Settings | File Templates.
 */

public interface NeedRepository extends WonRepository<Need> {
    List<Need> findByNeedURI(URI URI);
}
