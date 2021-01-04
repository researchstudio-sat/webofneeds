/*
 * Copyright 2012 Research Studios Austria Forschungsges.m.b.H. Licensed under
 * the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package won.protocol.repository;

import java.net.URI;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import java.util.Set;
import javax.persistence.LockModeType;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import won.protocol.message.WonMessageType;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionState;

/**
 * Created with IntelliJ IDEA. User: Gabriel Date: 04.11.12 Time: 16:56 To
 * change this template use File | Settings | File Templates.
 */
public interface ConnectionRepository extends WonRepository<Connection> {
    List<Connection> findByConnectionURI(URI URI);

    Optional<Connection> findOneByConnectionURI(URI URI);

    Optional<Connection> findOneBySocketURIAndTargetSocketURI(URI socketURI, URI remoteSocketURI);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select con from Connection con where socketURI = :socketUri and targetSocketURI = :targetSocketUri")
    Optional<Connection> findOneBySocketURIAndTargetSocketURIForUpdate(@Param("socketUri") URI socketURI,
                    @Param("targetSocketUri") URI targetSocketURI);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select con from Connection con where connectionURI = :uri")
    Optional<Connection> findOneByConnectionURIForUpdate(@Param("uri") URI uri);

    Connection findOneByConnectionURIAndVersionNot(URI URI, int version);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select con from Connection con where atomURI = :atomUri and targetAtomURI = :targetAtomUri and socketURI = :socketUri and targetSocketURI = :targetSocketUri")
    Optional<Connection> findOneByAtomURIAndTargetAtomURIAndSocketURIAndTargetSocketURIForUpdate(
                    @Param("atomUri") URI atomURI, @Param("targetAtomUri") URI targetAtomURI,
                    @Param("socketUri") URI socketUri, @Param("targetSocketUri") URI targetSocketURI);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select con from Connection con where atomURI = :atomUri and targetAtomURI = :targetAtomUri and socketURI = :socketUri and targetSocketURI is null")
    Optional<Connection> findOneByAtomURIAndTargetAtomURIAndSocketURIAndNullTargetSocketForUpdate(
                    @Param("atomUri") URI atomURI, @Param("targetAtomUri") URI targetAtomURI,
                    @Param("socketUri") URI socketUri);

    List<Connection> findByAtomURI(URI URI);

    List<Connection> findByAtomURIAndStateAndTypeURI(URI atomURI, ConnectionState connectionState, URI socketType);

    List<Connection> findByAtomURIAndState(URI atomURI, ConnectionState connectionState);

    List<Connection> findBySocketURIAndState(URI socketURI, ConnectionState connectionState);

    List<Connection> findByAtomURIAndTypeURI(URI atomURI, URI socketType);

    @Query("select count (*) from Connection con where socketURI = :socketUri")
    long countBySocketUri(@Param("socketUri") URI socketURI);

    long countByAtomURIAndState(URI atomURI, ConnectionState connectionState);

    long countBySocketURIAndState(URI socketURI, ConnectionState connectionState);

    @Query("select connectionURI from Connection")
    List<URI> getAllConnectionURIs();

    @Query("select conn from Connection conn")
    List<Connection> getAllConnections();

    @Query("select connectionURI from Connection where atomURI = ?1")
    List<URI> getAllConnectionURIsForAtomURI(URI atomURI);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Connection c where c.atomURI = ?1 and c.state != ?2")
    List<Connection> findByAtomURIAndNotStateForUpdate(URI atomURI, ConnectionState connectionState);

    @Query("select c from Connection c where c.atomURI = ?1 and c.state != ?2")
    List<Connection> findByAtomURIAndNotState(URI atomURI, ConnectionState connectionState);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Connection c where c.atomURI = ?1 and c.socketURI = ?2 and c.state != ?3")
    List<Connection> findByAtomURIAndSocketURIAndNotStateForUpdate(URI atomURI, URI socketURI,
                    ConnectionState connectionState);

    @Query("select c from Connection c where c.atomURI = ?1 and c.socketURI = ?2 and c.state != ?3")
    List<Connection> findByAtomURIAndSocketURIAndNotState(URI atomURI, URI socketURI, ConnectionState connectionState);

    @Query("select case when (count(c) > 0) then true else false end from Connection c where c.atomURI = ?1 and c.socketURI = ?2 and c.state = ?3")
    boolean existsWithtomURIAndSocketURIAndState(URI atomURI, URI socketURI, ConnectionState connectionState);

    @Query("select conn from Connection conn where lastUpdate > :modifiedAfter")
    List<Connection> findModifiedConnectionsAfter(@Param("modifiedAfter") Date modifiedAfter);

    /**
     * Obtains connectionURIs grouped by the connectionURI itself and with message
     * properties attached. The paging request therefore can use criteria based on
     * aggregated messages properties of the connection, such as
     * min(msg.creationDate). For example:
     * <code>new PageRequest(0, 1, Sort.Direction.DESC, "min(msg.creationDate)"))</code>
     * 
     * @param pageable
     * @return
     */
    @Query("select msg.parentURI from MessageEvent msg "
                    + "where (msg.senderURI = msg.parentURI or msg.recipientURI = msg.parentURI) "
                    + "group by msg.parentURI")
    Slice<URI> getConnectionURIByActivityDate(Pageable pageable);

    /**
     * Obtains connectionURIs grouped by the connectionURI itself and with message
     * properties attached. The paging request therefore can use criteria based on
     * aggregated messages properties of the connection, such as
     * min(msg.creationDate). For example:
     * <code>new PageRequest(0, 1, Sort.Direction.DESC, "min(msg.creationDate)"))</code>
     * 
     * @param pageable
     * @return
     */
    @Query("select conn from Connection conn where conn.connectionURI in (select distinct msg.parentURI from MessageEvent msg "
                    + "where (msg.senderURI = msg.parentURI or msg.recipientURI = msg.parentURI))")
    Slice<Connection> getConnectionsByActivityDate(Pageable pageable);

    @Query("select msg.parentURI from MessageEvent msg "
                    + "where ((msg.senderURI = msg.parentURI or msg.recipientURI = msg.parentURI) and (msg.creationDate < :referenceDate))"
                    + "group by msg.parentURI")
    Slice<URI> getConnectionURIByActivityDate(@Param("referenceDate") Date referenceDate, Pageable pageable);

    @Query("select conn from Connection conn where conn.connectionURI in (select distinct msg.parentURI from MessageEvent msg "
                    + "where ((msg.senderURI = msg.parentURI or msg.recipientURI = msg.parentURI) and (msg.creationDate < :referenceDate)))")
    Slice<Connection> getConnectionsByActivityDate(@Param("referenceDate") Date referenceDate, Pageable pageable);

    @Query("select conn from Connection conn where conn.connectionURI in (select msg.parentURI from MessageEvent msg "
                    + "where ((msg.senderURI = msg.parentURI or msg.recipientURI = msg.parentURI) and (msg.creationDate < :referenceDate))"
                    + "group by msg.parentURI having max(msg.creationDate) < :resumeDate)")
    Slice<Connection> getConnectionsBeforeByActivityDate(@Param("resumeDate") Date resumeEventDate,
                    @Param("referenceDate") Date referenceDate, Pageable pageable);

    @Query("select conn from Connection conn where conn.connectionURI in (select msg.parentURI from MessageEvent msg "
                    + "where ((msg.senderURI = msg.parentURI or msg.recipientURI = msg.parentURI) and (msg.creationDate < :referenceDate))"
                    + "group by msg.parentURI having max(msg.creationDate) > :resumeDate)")
    Slice<Connection> getConnectionsAfterByActivityDate(@Param("resumeDate") Date resumeEventDate,
                    @Param("referenceDate") Date referenceDate, Pageable pageable);

    /**
     * Obtains connectionURIs of the provided Atom grouped by the connectionURI
     * itself and with message properties attached. The paging request therefore can
     * use criteria based on aggregated messages properties of the connection, such
     * as min(msg.creationDate). For example:
     * <code>new PageRequest(0, 1, Sort.Direction.DESC, "min(msg.creationDate)"))</code>
     * 
     * @param atomURI
     * @param pageable
     * @return
     */
    @Query("select msg.parentURI from MessageEvent msg "
                    + "where (msg.senderAtomURI = :atom and msg.senderURI = msg.parentURI) "
                    + "   or (msg.recipientAtomURI = :atom and msg.recipientURI = msg.parentURI) "
                    + "group by msg.parentURI")
    Slice<URI> getConnectionURIByActivityDate(@Param("atom") URI atomURI, Pageable pageable);

    /**
     * Obtains connections of the provided Atom grouped by the connectionURI itself
     * and with message properties attached. The paging request therefore can use
     * criteria based on aggregated messages properties of the connection, such as
     * min(msg.creationDate). For example:
     * <code>new PageRequest(0, 1, Sort.Direction.DESC, "min(msg.creationDate)"))</code>
     *
     * @param atomURI
     * @param pageable
     * @return
     */
    @Query("select conn from Connection conn where conn.connectionURI in (select distinct msg.parentURI  from MessageEvent msg "
                    + "where (msg.senderAtomURI = :atom and msg.senderURI = msg.parentURI) "
                    + "   or (msg.recipientAtomURI = :atom and msg.recipientURI = msg.parentURI))")
    Slice<Connection> getConnectionsByActivityDate(@Param("atom") URI atomURI, Pageable pageable);

    @Query("select msg.parentURI from MessageEvent msg "
                    + "where (((msg.senderAtomURI = :atom and msg.senderURI = msg.parentURI) "
                    + "   or (msg.recipientAtomURI = :atom and msg.recipientURI = msg.parentURI)) and (msg.creationDate < :referenceDate)) "
                    + "group by msg.parentURI")
    Slice<URI> getConnectionURIByActivityDate(@Param("atom") URI atomURI, @Param("referenceDate") Date referenceDate,
                    Pageable pageable);

    @Query("select conn from Connection conn where conn.atomURI = :atom and conn.connectionURI in (select distinct msg.parentURI  from MessageEvent msg "
                    + "where msg.parentURI = conn.connectionURI and msg.creationDate < :referenceDate)")
    Slice<Connection> getConnectionsByActivityDate(@Param("atom") URI atomURI,
                    @Param("referenceDate") Date referenceDate, Pageable pageable);

    @Query("select msg.parentURI from MessageEvent msg "
                    + "where (((msg.senderAtomURI = :atom and msg.senderURI = msg.parentURI) "
                    + "   or (msg.recipientAtomURI = :atom and msg.recipientURI = msg.parentURI)) and (msg.creationDate < :referenceDate)"
                    + "   and (msg.messageType = :messageType)) " + "group by msg.parentURI")
    Slice<URI> getConnectionURIByActivityDate(@Param("atom") URI atomURI,
                    @Param("messageType") WonMessageType messageType, @Param("referenceDate") Date referenceDate,
                    Pageable pageable);

    @Query("select conn from Connection conn where conn.connectionURI in (select distinct msg.parentURI  from MessageEvent msg "
                    + "where (((msg.senderAtomURI = :atom and msg.senderURI = msg.parentURI) "
                    + "   or (msg.recipientAtomURI = :atom and msg.recipientURI = msg.parentURI)) and (msg.creationDate < :referenceDate)"
                    + "   and (msg.messageType = :messageType)))")
    Slice<Connection> getConnectionsByActivityDate(@Param("atom") URI atomURI,
                    @Param("messageType") WonMessageType messageType, @Param("referenceDate") Date referenceDate,
                    Pageable pageable);

    @Query("select msg.parentURI from MessageEvent msg "
                    + "where (((msg.senderAtomURI = :atom and msg.senderURI = msg.parentURI) "
                    + "   or (msg.recipientAtomURI = :atom and msg.recipientURI = msg.parentURI)) "
                    + "   and (msg.messageType = :messageType)) " + "group by msg.parentURI")
    Slice<URI> getConnectionURIByActivityDate(@Param("atom") URI atomURI,
                    @Param("messageType") WonMessageType messageType, Pageable pageable);

    @Query("select conn from Connection conn where conn.connectionURI in (select distinct msg.parentURI  from MessageEvent msg "
                    + "where (((msg.senderAtomURI = :atom and msg.senderURI = msg.parentURI) "
                    + "   or (msg.recipientAtomURI = :atom and msg.recipientURI = msg.parentURI)) "
                    + "   and (msg.messageType = :messageType)))")
    Slice<Connection> getConnectionsByActivityDate(@Param("atom") URI atomURI,
                    @Param("messageType") WonMessageType messageType, Pageable pageable);

    @Query("select conn from Connection conn where conn.connectionURI in (select msg.parentURI from MessageEvent msg "
                    + "where (((msg.senderAtomURI = :atom and msg.senderURI = msg.parentURI) "
                    + "   or (msg.recipientAtomURI = :atom and msg.recipientURI = msg.parentURI)) and (msg.creationDate < :referenceDate))"
                    + "group by msg.parentURI having max(msg.creationDate) < :resumeDate)")
    Slice<Connection> getConnectionsBeforeByActivityDate(@Param("atom") URI atomURI,
                    @Param("resumeDate") Date resumeEventDate, @Param("referenceDate") Date referenceDate,
                    Pageable pageable);

    @Query("select conn from Connection conn where conn.connectionURI in (select msg.parentURI from MessageEvent msg "
                    + "where (((msg.senderAtomURI = :atom and msg.senderURI = msg.parentURI) or (msg.recipientAtomURI = :atom and msg.recipientURI = msg.parentURI)) "
                    + "   and (msg.creationDate < :referenceDate) and (msg.messageType = :messageType))"
                    + "group by msg.parentURI having max(msg.creationDate) < :resumeDate)")
    Slice<Connection> getConnectionsBeforeByActivityDate(@Param("atom") URI atomURI,
                    @Param("resumeDate") Date resumeEventDate, @Param("messageType") WonMessageType messageType,
                    @Param("referenceDate") Date referenceDate, Pageable pageable);

    @Query("select conn from Connection conn where conn.connectionURI in (select msg.parentURI from MessageEvent msg "
                    + "where (((msg.senderAtomURI = :atom and msg.senderURI = msg.parentURI) "
                    + "   or (msg.recipientAtomURI = :atom and msg.recipientURI = msg.parentURI)) and (msg.creationDate < :referenceDate))"
                    + "group by msg.parentURI having max(msg.creationDate) > :resumeDate)")
    Slice<Connection> getConnectionsAfterByActivityDate(@Param("atom") URI atomURI,
                    @Param("resumeDate") Date resumeEventDate, @Param("referenceDate") Date referenceDate,
                    Pageable pageable);

    @Query("select conn from Connection conn where conn.connectionURI in (select msg.parentURI from MessageEvent msg "
                    + "where (((msg.senderAtomURI = :atom and msg.senderURI = msg.parentURI) or (msg.recipientAtomURI = :atom and msg.recipientURI = msg.parentURI)) "
                    + "   and (msg.creationDate < :referenceDate) and (msg.messageType = :messageType))"
                    + "group by msg.parentURI having max(msg.creationDate) > :resumeDate)")
    Slice<Connection> getConnectionsAfterByActivityDate(@Param("atom") URI atomURI,
                    @Param("resumeDate") Date resumeEventDate, @Param("messageType") WonMessageType messageType,
                    @Param("referenceDate") Date referenceDate, Pageable pageable);

    @Query("select case when (count(con) > 0) then true else false end "
                    + "from Connection con where con.atomURI = :fromAtom and con.targetAtomURI = :toAtom")
    boolean existsWithAtomAndTargetAtom(@Param("fromAtom") URI fromAtom, @Param("toAtom") URI toAtom);

    @Query("select case when (count(con) > 0) then true else false end "
                    + "from Connection con where "
                    + "con.atomURI = :fromAtom "
                    + "and con.targetAtomURI = :toAtom "
                    + "and con.state in :allowedStates ")
    boolean existsWithAtomAndTargetAtomAndStates(
                    @Param("fromAtom") URI fromAtom,
                    @Param("toAtom") URI toAtom,
                    @Param("allowedStates") Collection<URI> allowedStates);

    @Query("select case when (count(con) > 0) then true else false end "
                    + "from Connection con where "
                    + "con.atomURI = :fromAtom "
                    + "and con.targetAtomURI = :toAtom "
                    + "and con.typeURI in :allowedSocketTypes ")
    boolean existsWithAtomAndTargetAtomAndSocketTypes(
                    @Param("fromAtom") URI fromAtom,
                    @Param("toAtom") URI toAtom,
                    @Param("allowedSocketTypes") Collection<URI> allowedSocketTypes);

    @Query("select case when (count(con) > 0) then true else false end "
                    + "from Connection con where "
                    + "con.atomURI = :fromAtom "
                    + "and con.targetAtomURI = :toAtom "
                    + "and con.socketURI in :allowedSockets ")
    boolean existsWithAtomAndTargetAtomAndSockets(
                    @Param("fromAtom") URI fromAtom,
                    @Param("toAtom") URI toAtom,
                    @Param("allowedSockets") Collection<URI> allowedSockets);

    @Query("select case when (count(con) > 0) then true else false end "
                    + "from Connection con where "
                    + "con.atomURI = :fromAtom "
                    + "and con.targetAtomURI = :toAtom "
                    + "and con.state in :allowedStates "
                    + "and con.typeURI in :allowedSocketTypes ")
    boolean existsWithAtomAndTargetAtomAndStatesAndSocketTypes(
                    @Param("fromAtom") URI fromAtom,
                    @Param("toAtom") URI toAtom,
                    @Param("allowedStates") Collection<URI> allowedStates,
                    @Param("allowedSocketTypes") Collection<URI> allowedSocketTypes);

    @Query("select case when (count(con) > 0) then true else false end "
                    + "from Connection con where "
                    + "con.atomURI = :fromAtom "
                    + "and con.targetAtomURI = :toAtom "
                    + "and con.state in :allowedStates "
                    + "and con.socketURI in :allowedSockets ")
    boolean existsWithAtomAndTargetAtomAndStatesAndSockets(
                    @Param("fromAtom") URI fromAtom,
                    @Param("toAtom") URI toAtom,
                    @Param("allowedStates") Collection<URI> allowedStates,
                    @Param("allowedSockets") Collection<URI> allowedSockets);

    @Query("select case when (count(con) > 0) then true else false end "
                    + "from Connection con where "
                    + "con.atomURI = :fromAtom "
                    + "and con.targetAtomURI = :toAtom "
                    + "and con.typeURI in :allowedSocketTypes "
                    + "and con.socketURI in :allowedSockets ")
    boolean existsWithAtomAndTargetAtomAndSocketTypesAndSockets(
                    @Param("fromAtom") URI fromAtom,
                    @Param("toAtom") URI toAtom,
                    @Param("allowedSocketTypes") Collection<URI> allowedSocketTypes,
                    @Param("allowedSockets") Collection<URI> allowedSockets);

    @Query("select case when (count(con) > 0) then true else false end "
                    + "from Connection con where "
                    + "con.atomURI = :fromAtom "
                    + "and con.targetAtomURI = :toAtom "
                    + "and con.state in :allowedStates "
                    + "and con.typeURI in :allowedSocketTypes "
                    + "and con.socketURI in :allowedSockets ")
    boolean existsWithAtomAndTargetAtomAndStatesAndSocketTypesAndSockets(
                    @Param("fromAtom") URI fromAtom,
                    @Param("toAtom") URI toAtom,
                    @Param("allowedStates") Collection<URI> allowedStates,
                    @Param("allowedSocketTypes") Collection<URI> allowedSocketTypes,
                    @Param("allowedSockets") Collection<URI> allowedSockets);
}
