package won.protocol.repository;

import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.persistence.LockModeType;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import won.protocol.model.Atom;
import won.protocol.model.AtomState;

/**
 * User: Gabriel Date: 02.11.12 Time: 15:28
 */
public interface AtomRepository extends WonRepository<Atom> {
    List<Atom> findByAtomURI(URI URI);

    @Query("select atomURI from Atom atom where :atomState is null or atom.state = :atomState")
    List<URI> getAllAtomURIs(@Param("atomState") AtomState atomState);

    @Query("select atomURI from Atom atom where :atomState is null or atom.state = :atomState")
    Slice<URI> getAllAtomURIs(@Param("atomState") AtomState atomState, Pageable pageable);

    Optional<Atom> findOneByAtomURI(URI atomURI);

    Atom findOneByAtomURIAndVersionNot(URI atomURI, int version);

    @Query("select atomURI from Atom atom where atom.creationDate < :referenceDate")
    Slice<URI> getAtomURIsBefore(@Param("referenceDate") Date referenceDate, Pageable pageable);

    @Query("select atomURI from Atom atom where atom.creationDate < :referenceDate and atom.state = :atomState")
    Slice<URI> getAtomURIsBefore(@Param("referenceDate") Date referenceDate, @Param("atomState") AtomState atomState,
                    Pageable pageable);

    @Query("select atomURI from Atom atom where atom.creationDate > :referenceDate")
    Slice<URI> getAtomURIsAfter(@Param("referenceDate") Date referenceDate, Pageable pageable);

    @Query("select atomURI from Atom atom where atom.creationDate > :referenceDate and atom.state = :atomState")
    Slice<URI> getAtomURIsAfter(@Param("referenceDate") Date referenceDate, @Param("atomState") AtomState atomState,
                    Pageable pageable);

    @Query("select atomURI from Atom atom where atom.lastUpdate > :modifiedDate and (:atomState is null or atom.state = :atomState)")
    List<URI> getAllAtomURIsModifiedAfter(@Param("modifiedDate") Date modifiedDate,
                    @Param("atomState") AtomState atomState);

    @Query("select atomURI from Atom atom where atom.creationDate > :createdDate and (:atomState is null or atom.state = :atomState)")
    List<URI> getAllAtomURIsCreatedAfter(@Param("createdDate") Date createdDate,
                    @Param("atomState") AtomState atomState);

    @Query("select state, count(*) from Connection where atomURI = :atom group by state")
    List<Object[]> getCountsPerConnectionState(@Param("atom") URI atomURI);

    @Query("select state, connectionURI from Connection where atomURI = :atom")
    List<Object[]> getConnectionUrisAndState(@Param("atom") URI atomUri);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select atom from Atom atom where atomURI= :uri")
    Optional<Atom> findOneByAtomURIForUpdate(@Param("uri") URI uri);

    /**
     * Finds atoms that have been inactive between start and end date
     * 
     * @param start
     * @param end
     * @param pageable
     * @return
     */
    @Query("select distinct atom from Atom atom "
                    + "join Connection c on ( c.atomURI = atom.atomURI ) join MessageEvent mep on (mep.parentURI = atom.atomURI or mep.parentURI = c.connectionURI) "
                    + "where " + "atom.state = 'ACTIVE' " + "and " + "mep.messageType <> 'ATOM_MESSAGE' " + "and "
                    + " (select count(*) from Connection con where con.atomURI = atom.atomURI and con.state = 'CONNECTED') = 0"
                    + "and " + "( mep.senderURI = c.connectionURI or mep.senderAtomURI = atom.atomURI)"
                    + "group by atom "
                    + "having max(mep.creationDate) > :startDate and max(mep.creationDate) < :endDate ")
    Slice<Atom> findAtomsInactiveBetweenAndNotConnected(@Param("startDate") Date start, @Param("endDate") Date end,
                    Pageable pageable);

    /**
     * Finds atoms that have been inactive between start and end date
     * 
     * @param start
     * @param end
     * @param pageable
     * @return
     */
    @Query("select distinct atom from Atom atom "
                    + "join Connection c on ( c.atomURI = atom.atomURI ) join MessageEvent mep on (mep.parentURI = atom.atomURI or mep.parentURI = c.connectionURI) "
                    + "where " + "atom.state = 'ACTIVE' " + "and " + "mep.messageType <> 'ATOM_MESSAGE' " + "and "
                    + "( mep.senderURI = c.connectionURI or mep.senderAtomURI = atom.atomURI)" + "group by atom "
                    + "having max(mep.creationDate) > :startDate and max(mep.creationDate) < :endDate ")
    Slice<Atom> findAtomsInactiveBetween(@Param("startDate") Date start, @Param("endDate") Date end, Pageable pageable);

    @Query("select distinct atom from Atom atom "
                    + "join Connection c on ( c.atomURI = atom.atomURI ) join MessageEvent mep on (mep.parentURI = atom.atomURI or mep.parentURI = c.connectionURI) "
                    + "where " + "atom.state = 'ACTIVE' " + "and " + "mep.messageType <> 'ATOM_MESSAGE' " + "and "
                    + " (select count(*) from Connection con where con.atomURI = atom.atomURI and con.state = 'CONNECTED') = 0"
                    + "and " + "( mep.senderURI = c.connectionURI or mep.senderAtomURI = atom.atomURI)"
                    + "group by atom " + "having max(mep.creationDate) < :sinceDate")
    Slice<Atom> findAtomsInactiveSinceAndNotConnected(@Param("sinceDate") Date since, Pageable pageable);

    @Query("select distinct atom from Atom atom "
                    + "join Connection c on ( c.atomURI = atom.atomURI ) join MessageEvent mep on (mep.parentURI = atom.atomURI or mep.parentURI = c.connectionURI) "
                    + "where " + "atom.state = 'ACTIVE' " + "and " + "mep.messageType <> 'ATOM_MESSAGE' " + "and "
                    + "( mep.senderURI = c.connectionURI or mep.senderAtomURI = atom.atomURI)" + "group by atom "
                    + "having max(mep.creationDate) < :sinceDate")
    Slice<Atom> findAtomsInactiveSince(@Param("sinceDate") Date since, Pageable pageable);
}
