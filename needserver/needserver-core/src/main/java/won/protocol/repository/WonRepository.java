package won.protocol.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Gabriel
 * Date: 04.11.12
 * Time: 17:05
 * To change this template use File | Settings | File Templates.
 */
public interface WonRepository<M> extends JpaRepository<M, Long> {
    List<M> findById(Long id);
}
