package won.protocol.repository;

import java.net.URI;
import java.util.Date;
import java.util.List;

import javax.persistence.LockModeType;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import won.protocol.model.Need;
import won.protocol.model.NeedState;

/**
 * User: Gabriel Date: 02.11.12 Time: 15:28
 */

public interface NeedRepository extends WonRepository<Need> {

    List<Need> findByNeedURI(URI URI);

    @Query("select needURI from Need")
    List<URI> getAllNeedURIs();

    @Query("select needURI from Need need")
    Slice<URI> getAllNeedURIs(Pageable pageable);

    @Query("select needURI from Need need where need.state = :needState")
    Slice<URI> getAllNeedURIs(@Param("needState") NeedState needState, Pageable pageable);

    Need findOneByNeedURI(URI needURI);

    Need findOneByNeedURIAndVersionNot(URI needURI, int version);

    @Query("select needURI from Need need where need.creationDate < :referenceDate")
    Slice<URI> getNeedURIsBefore(@Param("referenceDate") Date referenceDate, Pageable pageable);

    @Query("select needURI from Need need where need.creationDate < :referenceDate and need.state = :needState")
    Slice<URI> getNeedURIsBefore(@Param("referenceDate") Date referenceDate, @Param("needState") NeedState needState,
            Pageable pageable);

    @Query("select needURI from Need need where need.creationDate > :referenceDate")
    Slice<URI> getNeedURIsAfter(@Param("referenceDate") Date referenceDate, Pageable pageable);

    @Query("select needURI from Need need where need.creationDate > :referenceDate and need.state = :needState")
    Slice<URI> getNeedURIsAfter(@Param("referenceDate") Date referenceDate, @Param("needState") NeedState needState,
            Pageable pageable);

    @Query("select needURI from Need need where need.lastUpdate > :modifiedDate")
    List<URI> findModifiedNeedURIsAfter(@Param("modifiedDate") Date modifiedDate);

    @Query("select state, count(*) from Connection where needURI = :need group by state")
    List<Object[]> getCountsPerConnectionState(@Param("need") URI needURI);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select need from Need need where needURI= :uri")
    Need findOneByNeedURIForUpdate(@Param("uri") URI uri);

    /**
     * Finds needs that have been inactive between start and end date
     * 
     * @param start
     * @param end
     * @param pageable
     * @return
     */
    @Query("select distinct need from Need need "
            + "join Connection c on ( c.needURI = need.needURI ) join MessageEventPlaceholder mep on (mep.parentURI = need.needURI or mep.parentURI = c.connectionURI) "
            + "where " + "need.state = 'ACTIVE' " + "and " + "mep.messageType <> 'NEED_MESSAGE' " + "and "
            + " (select count(*) from Connection con where con.needURI = need.needURI and con.state = 'CONNECTED') = 0"
            + "and " + "( mep.senderURI = c.connectionURI or mep.senderNeedURI = need.needURI)" + "group by need "
            + "having max(mep.creationDate) > :startDate and max(mep.creationDate) < :endDate ")
    Slice<Need> findNeedsInactiveBetweenAndNotConnected(@Param("startDate") Date start, @Param("endDate") Date end,
            Pageable pageable);

    /**
     * Finds needs that have been inactive between start and end date
     * 
     * @param start
     * @param end
     * @param pageable
     * @return
     */
    @Query("select distinct need from Need need "
            + "join Connection c on ( c.needURI = need.needURI ) join MessageEventPlaceholder mep on (mep.parentURI = need.needURI or mep.parentURI = c.connectionURI) "
            + "where " + "need.state = 'ACTIVE' " + "and " + "mep.messageType <> 'NEED_MESSAGE' " + "and "
            + "( mep.senderURI = c.connectionURI or mep.senderNeedURI = need.needURI)" + "group by need "
            + "having max(mep.creationDate) > :startDate and max(mep.creationDate) < :endDate ")
    Slice<Need> findNeedsInactiveBetween(@Param("startDate") Date start, @Param("endDate") Date end, Pageable pageable);

    @Query("select distinct need from Need need "
            + "join Connection c on ( c.needURI = need.needURI ) join MessageEventPlaceholder mep on (mep.parentURI = need.needURI or mep.parentURI = c.connectionURI) "
            + "where " + "need.state = 'ACTIVE' " + "and " + "mep.messageType <> 'NEED_MESSAGE' " + "and "
            + " (select count(*) from Connection con where con.needURI = need.needURI and con.state = 'CONNECTED') = 0"
            + "and " + "( mep.senderURI = c.connectionURI or mep.senderNeedURI = need.needURI)" + "group by need "
            + "having max(mep.creationDate) < :sinceDate")
    Slice<Need> findNeedsInactiveSinceAndNotConnected(@Param("sinceDate") Date since, Pageable pageable);

    @Query("select distinct need from Need need "
            + "join Connection c on ( c.needURI = need.needURI ) join MessageEventPlaceholder mep on (mep.parentURI = need.needURI or mep.parentURI = c.connectionURI) "
            + "where " + "need.state = 'ACTIVE' " + "and " + "mep.messageType <> 'NEED_MESSAGE' " + "and "
            + "( mep.senderURI = c.connectionURI or mep.senderNeedURI = need.needURI)" + "group by need "
            + "having max(mep.creationDate) < :sinceDate")
    Slice<Need> findNeedsInactiveSince(@Param("sinceDate") Date since, Pageable pageable);
}
