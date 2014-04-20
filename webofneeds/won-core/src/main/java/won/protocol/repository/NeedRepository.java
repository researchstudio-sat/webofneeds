package won.protocol.repository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.Query;
import won.protocol.model.Need;

import java.net.URI;
import java.util.List;

/**
 * User: Gabriel
 * Date: 02.11.12
 * Time: 15:28
 */

public interface NeedRepository extends WonRepository<Need> {

  List<Need> findByNeedURI(URI URI);

  @Query("select needURI from Need")
  List<URI> getAllNeedURIs();

  @Query("select needURI from Need")
  List<URI> getAllNeedURIs(PageRequest pageRequest);
}
