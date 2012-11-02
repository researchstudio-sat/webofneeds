package won.protocol.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
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
@Repository
public interface NeedRepository extends JpaRepository<Need, Long> {
    List<Need> findByNeedURI(URI URI);
}
