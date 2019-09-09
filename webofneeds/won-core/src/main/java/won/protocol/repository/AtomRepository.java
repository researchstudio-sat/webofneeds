package won.protocol.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import won.protocol.model.Atom;
import won.protocol.model.AtomState;

import javax.persistence.LockModeType;
import java.net.URI;
import java.util.Date;
import java.util.List;

/**
 * User: Gabriel Date: 02.11.12 Time: 15:28
 */
public interface AtomRepository extends WonRepository<Atom> {
    List<Atom> findByAtomURI(URI URI);

    @Query("select atomURI from Atom")
    List<URI> getAllAtomURIs();

    @Query("select atomURI from Atom atom where atom.state = :atomState")
    List<URI> getAllAtomURIs(@Param("atomState") AtomState atomState);

    @Query("select atomURI from Atom atom")
    Slice<URI> getAllAtomURIs(Pageable pageable);

    @Query("select atomURI from Atom atom where atom.state = :atomState")
    Slice<URI> getAllAtomURIs(@Param("atomState") AtomState atomState, Pageable pageable);

    Atom findOneByAtomURI(URI atomURI);

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

    @Query("select atomURI from Atom atom where atom.lastUpdate > :modifiedDate")
    List<URI> findModifiedAtomURIsAfter(@Param("modifiedDate") Date modifiedDate);

    @Query("select state, count(*) from Connection where atomURI = :atom group by state")
    List<Object[]> getCountsPerConnectionState(@Param("atom") URI atomURI);

    @Query("select state, connectionURI from Connection where atomURI = :atom")
    List<Object[]> getConnectionUrisAndState(@Param("atom") URI atomUri);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select atom from Atom atom where atomURI= :uri")
    Atom findOneByAtomURIForUpdate(@Param("uri") URI uri);

    /**
     * Finds atoms that have been inactive between start and end date
     * 
     * @param start
     * @param end
     * @param pageable
     * @return
     */
    @Query("select distinct atom from Atom atom "
                    + "join Connection c on ( c.atomURI = atom.atomURI ) join MessageEventPlaceholder mep on (mep.parentURI = atom.atomURI or mep.parentURI = c.connectionURI) "
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
                    + "join Connection c on ( c.atomURI = atom.atomURI ) join MessageEventPlaceholder mep on (mep.parentURI = atom.atomURI or mep.parentURI = c.connectionURI) "
                    + "where " + "atom.state = 'ACTIVE' " + "and " + "mep.messageType <> 'ATOM_MESSAGE' " + "and "
                    + "( mep.senderURI = c.connectionURI or mep.senderAtomURI = atom.atomURI)" + "group by atom "
                    + "having max(mep.creationDate) > :startDate and max(mep.creationDate) < :endDate ")
    Slice<Atom> findAtomsInactiveBetween(@Param("startDate") Date start, @Param("endDate") Date end, Pageable pageable);

    @Query("select distinct atom from Atom atom "
                    + "join Connection c on ( c.atomURI = atom.atomURI ) join MessageEventPlaceholder mep on (mep.parentURI = atom.atomURI or mep.parentURI = c.connectionURI) "
                    + "where " + "atom.state = 'ACTIVE' " + "and " + "mep.messageType <> 'ATOM_MESSAGE' " + "and "
                    + " (select count(*) from Connection con where con.atomURI = atom.atomURI and con.state = 'CONNECTED') = 0"
                    + "and " + "( mep.senderURI = c.connectionURI or mep.senderAtomURI = atom.atomURI)"
                    + "group by atom " + "having max(mep.creationDate) < :sinceDate")
    Slice<Atom> findAtomsInactiveSinceAndNotConnected(@Param("sinceDate") Date since, Pageable pageable);

    @Query("select distinct atom from Atom atom "
                    + "join Connection c on ( c.atomURI = atom.atomURI ) join MessageEventPlaceholder mep on (mep.parentURI = atom.atomURI or mep.parentURI = c.connectionURI) "
                    + "where " + "atom.state = 'ACTIVE' " + "and " + "mep.messageType <> 'ATOM_MESSAGE' " + "and "
                    + "( mep.senderURI = c.connectionURI or mep.senderAtomURI = atom.atomURI)" + "group by atom "
                    + "having max(mep.creationDate) < :sinceDate")
    Slice<Atom> findAtomsInactiveSince(@Param("sinceDate") Date since, Pageable pageable);
}
