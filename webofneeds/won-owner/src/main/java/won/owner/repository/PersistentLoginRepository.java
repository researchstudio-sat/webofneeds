package won.owner.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import won.owner.model.PersistentLogin;

public interface PersistentLoginRepository extends CrudRepository<PersistentLogin, String> {
  public void deleteByUsername(String username);

  @Query("SELECT pl FROM PersistentLogin pl JOIN FETCH pl.keystorePasswordHolder WHERE pl.id = (:id)")
  public PersistentLogin findOneEagerly(@Param("id") String id);
}
