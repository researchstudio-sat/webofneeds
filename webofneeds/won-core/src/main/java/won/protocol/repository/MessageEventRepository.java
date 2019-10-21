package won.protocol.repository;

import java.net.URI;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.persistence.LockModeType;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import won.protocol.message.WonMessageType;
import won.protocol.model.MessageEvent;
import won.protocol.model.unread.UnreadMessageInfoForConnection;

public interface MessageEventRepository extends WonRepository<MessageEvent> {
    Optional<MessageEvent> findOneByMessageURI(URI URI);

    // read is permitted iff any of these conditions apply:
    // * the WebId is the sender atom
    // * the WebId is the recipient atom
    // * the WebId is an atom that has a connection to the sender atom AND the
    // message is in the atom's event container
    /*
     * sql: select msg.*, con.* from message_event msg left outer join connection
     * con on ( msg.parentURI = con.connectionURI -- msg is in connection. atom and
     * remote atom have access or msg.parentURI = con.atomURI -- msg not in
     * connection but belongs to atom. We allow all remote atoms access -- if con is
     * null, there are no connections for the msg's atom, and therefore no remote
     * atoms. only allow the local atom access ) where ( msg.messageURI =
     * 'https://satvm05.researchstudio.at/won/resource/event/2x27qe2l4jt6obgxiqij'
     * -- ok now we have the event and maybe a connection and ( (con is null and
     * msg.parentURI =
     * 'https://satvm05.researchstudio.at/won/resource/atom/6825071433651196000') --
     * the event belongs to the atom: fine or con.atomURI =
     * 'https://satvm05.researchstudio.at/won/resource/atom/6825071433651196000' --
     * grant access to the local atom or con.targetAtomURI =
     * 'https://satvm05.researchstudio.at/won/resource/atom/6825071433651196000' --
     * grant access to the remote atom or msg.senderNodeURI =
     * 'https://satvm05.researchstudio.at/won/resource/atom/6825071433651196000' --
     * grant access to the sender node or msg.recipientNodeURI =
     * 'https://satvm05.researchstudio.at/won/resource/atom/6825071433651196000' --
     * grant access to the receiver node ) )
     */
    @Query("select case when (count(msg) > 0) then true else false end "
                    + "from MessageEvent msg left outer join Connection con on ("
                    + " msg.parentURI = con.connectionURI or " + " msg.parentURI = con.atomURI " + " ) "
                    + " where msg.messageURI = :messageUri and (" + "   ( con is null and msg.parentURI = :webId )"
                    + "   or con.atomURI = :webId " + "   or con.targetAtomURI = :webId "
                    + "   or msg.recipientNodeURI = :webId " + "   or msg.senderNodeURI = :webId " + ")")
    boolean isReadPermittedForWebID(@Param("messageUri") URI messageUri, @Param("webId") URI webId);

    /*
     * @Query( "select " +
     * "	new won.protocol.model.unread.UnreadMessageInfoForConnection(c.connectionuri, c.state, new won.protocol.model.unread.UnreadMessageInfo(count(*), max(m.creationdate), min(m.creationdate)) "
     * + "    from MessageEvent m join MessageEvent last on ( " +
     * "        m.parenturi = last.parenturi n" +
     * "    	and m.messagetype not in ('SUCCESS_RESPONSE', 'FAILURE_RESPONSE') "
     * + "    	and m.creationdate > last.creationdate " + "    ) " +
     * "    join MessageContainer e on ( " + "    	last.parenturi = e.parent_uri "
     * + "    ) " + "    join Connection c on ( " +
     * "    	c.connectionuri = last.parenturi " + "    ) " +
     * "    and e.parent_type = 'Connection' " + "    where  " +
     * "    	last.messageuri in :lastSeenMessageUris " +
     * "        and c.atomuri = :atomUri " +
     * "    group by (c.connectionuri, c.state) " )
     */
    @Query("select \n" + "	new won.protocol.model.unread.UnreadMessageInfoForConnection(\n"
                    + "		c.connectionURI, \n" + "		c.state, \n" + "		count(*), \n"
                    + "		max(m.creationDate), \n" + "		min(m.creationDate) \n" + "  ) \n"
                    + "    from Connection c join MessageEvent m on \n"
                    + "    	c.connectionURI = m.parentURI\n" + "    left join MessageEvent last on \n"
                    + "        m.parentURI = last.parentURI\n"
                    + "        and last.messageURI in :lastSeenMessageUris \n" + "    where \n"
                    + "        c.atomURI = :atomUri \n"
                    + "		and m.messageType not in ('SUCCESS_RESPONSE', 'FAILURE_RESPONSE') \n"
                    + "        and (last is null or m.creationDate > last.creationDate) \n"
                    + "    group by c.connectionURI, c.state \n")
    List<UnreadMessageInfoForConnection> getUnreadInfoForAtom(@Param("atomUri") URI atomURI,
                    @Param("lastSeenMessageUris") Collection<URI> lastSeenMessageURIs);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n,c from AtomMessageContainer c join MessageEvent msg on msg.parentURI = c.parentUri join Atom n on c.parentUri = n.atomURI where msg.messageURI = :messageUri")
    void lockAtomAndMessageContainerByContainedMessageForUpdate(@Param("messageUri") URI messageUri);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select con,c from AtomMessageContainer c join MessageEvent msg on msg.parentURI = c.parentUri join Connection con on c.parentUri = con.connectionURI where msg.messageURI = :messageUri")
    void lockConnectionAndMessageContainerByContainedMessageForUpdate(@Param("messageUri") URI messageUri);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select msg from MessageEvent msg where msg.messageURI = :uri")
    MessageEvent findOneByMessageURIforUpdate(@Param("uri") URI uri);

    List<MessageEvent> findByParentURI(URI URI);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select msg from MessageEvent msg where msg.parentURI = :parent and msg.messageType = :messageType")
    List<MessageEvent> findByParentURIAndMessageTypeForUpdate(@Param("parent") URI parentURI,
                    @Param("messageType") WonMessageType messageType);

    @Query("select count(*) from MessageEvent msg where msg.parentURI = :parent and msg.messageType = :messageType")
    long countByParentURIAndMessageType(@Param("parent") URI parentURI,
                    @Param("messageType") WonMessageType messageType);

    @Query("select msg from MessageEvent msg where msg.parentURI = :parent and msg.messageType = :messageType")
    List<MessageEvent> findByParentURIAndMessageType(@Param("parent") URI parentURI,
                    @Param("messageType") WonMessageType messageType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select msg from MessageEvent msg left outer join MessageEvent msg2 on msg.parentURI = msg2.parentURI and msg.creationDate < msg2.creationDate where msg.parentURI = :parent and msg2.id is null")
    MessageEvent findNewestByParentURIforUpdate(@Param("parent") URI parentUri);

    @Query("select msg from MessageEvent msg left outer join MessageEvent msg2 on msg.parentURI = msg2.parentURI and msg.creationDate < msg2.creationDate where msg.parentURI = :parent and msg2.id is null")
    MessageEvent findNewestByParentURI(@Param("parent") URI parentUri);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select msg from MessageEvent msg left outer join MessageEvent msg2 on msg.parentURI = msg2.parentURI and msg.creationDate > msg2.creationDate where msg.parentURI = :parent and msg2.id is null")
    MessageEvent findOldestByParentURIforUpdate(@Param("parent") URI parentUri);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select msg from MessageEvent msg where msg.parentURI = :parent and "
                    + "referencedByOtherMessage = false")
    List<MessageEvent> findByParentURIAndNotReferencedByOtherMessageForUpdate(
                    @Param("parent") URI parentURI);

    @Query("select msg from MessageEvent msg where msg.parentURI = :parent and "
                    + "referencedByOtherMessage = false")
    List<MessageEvent> findByParentURIAndNotReferencedByOtherMessage(@Param("parent") URI parentURI);

    @Query("select msg from MessageEvent msg where msg.parentURI = :parent")
    Slice<MessageEvent> findByParentURI(@Param("parent") URI parentURI, Pageable pageable);

    @Query("select msg from MessageEvent msg left join fetch msg.datasetHolder where msg.parentURI = :parent")
    Slice<MessageEvent> findByParentURIFetchDatasetEagerly(@Param("parent") URI parentURI,
                    Pageable pageable);

    @Query("select messageURI from MessageEvent msg where msg.parentURI = :parent and msg.messageType = :messageType")
    Slice<URI> getMessageURIsByParentURI(@Param("parent") URI parentURI,
                    @Param("messageType") WonMessageType messageType, Pageable pageable);

    @Query("select msg from MessageEvent msg where msg.parentURI = :parent and msg.messageType = :messageType")
    Slice<MessageEvent> findByParentURIAndType(@Param("parent") URI parentURI,
                    @Param("messageType") WonMessageType messageType, Pageable pageable);

    @Query("select msg from MessageEvent msg left join fetch msg.datasetHolder where msg.parentURI = :parent and msg.messageType = :messageType")
    Slice<MessageEvent> findByParentURIAndTypeFetchDatasetEagerly(@Param("parent") URI parentURI,
                    @Param("messageType") WonMessageType messageType, Pageable pageable);

    @Query("select messageURI from MessageEvent msg where msg.parentURI = :parent and msg.creationDate < :referenceDate")
    Slice<URI> getMessageURIsByParentURIBefore(@Param("parent") URI parentURI,
                    @Param("referenceDate") Date referenceDate, Pageable pageable);

    @Query("select msg from MessageEvent msg where msg.parentURI = :parent and msg.creationDate < :referenceDate")
    Slice<MessageEvent> findByParentURIBefore(@Param("parent") URI parentURI,
                    @Param("referenceDate") Date referenceDate, Pageable pageable);

    @Query("select msg from MessageEvent msg left join fetch msg.datasetHolder where msg.parentURI = :parent and msg.creationDate < :referenceDate")
    Slice<MessageEvent> findByParentURIBeforeFetchDatasetEagerly(@Param("parent") URI parentURI,
                    @Param("referenceDate") Date referenceDate, Pageable pageable);

    @Query("select msg from MessageEvent msg left join fetch msg.datasetHolder where msg.parentURI = :parent and msg.creationDate < (select msg2.creationDate from MessageEvent msg2 where msg2.messageURI = :referenceMessageUri )")
    Slice<MessageEvent> findByParentURIBeforeFetchDatasetEagerly(@Param("parent") URI parentURI,
                    @Param("referenceMessageUri") URI referenceMessageUri, Pageable pageable);

    @Query("select messageURI from MessageEvent msg where msg.parentURI = :parent and msg.creationDate < :referenceDate and msg.messageType = :messageType")
    Slice<URI> getMessageURIsByParentURIBefore(@Param("parent") URI parentURI,
                    @Param("referenceDate") Date referenceDate, @Param("messageType") WonMessageType messageType,
                    Pageable pageable);

    @Query("select msg from MessageEvent msg where msg.parentURI = :parent and msg.creationDate < :referenceDate and msg.messageType = :messageType")
    Slice<MessageEvent> findByParentURIAndTypeBefore(@Param("parent") URI parentURI,
                    @Param("referenceDate") Date referenceDate, @Param("messageType") WonMessageType messageType,
                    Pageable pageable);

    @Query("select msg from MessageEvent msg left join fetch msg.datasetHolder where msg.parentURI = :parent and msg.creationDate < :referenceDate and msg.messageType = :messageType")
    Slice<MessageEvent> findByParentURIAndTypeBeforeFetchDatasetEagerly(@Param("parent") URI parentURI,
                    @Param("referenceDate") Date referenceDate, @Param("messageType") WonMessageType messageType,
                    Pageable pageable);

    @Query("select msg from MessageEvent msg left join fetch msg.datasetHolder where msg.parentURI = :parent and msg.messageType = :messageType and msg.creationDate < (select msg2.creationDate from MessageEvent msg2 where msg2.messageURI = :referenceMessageUri )")
    Slice<MessageEvent> findByParentURIAndTypeBeforeFetchDatasetEagerly(@Param("parent") URI parentURI,
                    @Param("referenceMessageUri") URI referenceMessageURI,
                    @Param("messageType") WonMessageType messageType, Pageable pageable);

    @Query("select messageURI from MessageEvent msg where msg.parentURI = :parent and msg.creationDate > :referenceDate")
    Slice<URI> getMessageURIsByParentURIAfter(@Param("parent") URI parentURI,
                    @Param("referenceDate") Date referenceDate, Pageable pageable);

    @Query("select msg from MessageEvent msg where msg.parentURI = :parent and msg.creationDate > "
                    + ":referenceDate")
    Slice<MessageEvent> findByParentURIAfter(@Param("parent") URI parentURI,
                    @Param("referenceDate") Date referenceDate, Pageable pageable);

    @Query("select msg from MessageEvent msg left join fetch msg.datasetHolder where msg.parentURI = :parent and msg.creationDate > "
                    + ":referenceDate")
    Slice<MessageEvent> findByParentURIAfterFetchDatasetEagerly(@Param("parent") URI parentURI,
                    @Param("referenceDate") Date referenceDate, Pageable pageable);

    @Query("select messageURI from MessageEvent msg where msg.parentURI = :parent and msg.creationDate > :referenceDate and msg.messageType = :messageType")
    Slice<URI> getMessageURIsByParentURIAfter(@Param("parent") URI parentURI,
                    @Param("referenceDate") Date referenceDate, @Param("messageType") WonMessageType messageType,
                    Pageable pageable);

    @Query("select msg from MessageEvent msg left join fetch msg.datasetHolder where msg.parentURI = :parent and msg.creationDate > :referenceDate and msg.messageType = :messageType")
    Slice<MessageEvent> findByParentURIAndTypeAfter(@Param("parent") URI parentURI,
                    @Param("referenceDate") Date referenceDate, @Param("messageType") WonMessageType messageType,
                    Pageable pageable);

    @Query("select msg from MessageEvent msg where msg.parentURI = :parent and msg.creationDate > :referenceDate and msg.messageType = :messageType")
    Slice<MessageEvent> findByParentURIAndTypeAfterFetchDatasetEagerly(@Param("parent") URI parentURI,
                    @Param("referenceDate") Date referenceDate, @Param("messageType") WonMessageType messageType,
                    Pageable pageable);

    @Query("select msg from MessageEvent msg where msg.correspondingRemoteMessageURI = :uri")
    MessageEvent findOneByCorrespondingRemoteMessageURI(@Param("uri") URI uri);

    @Query("select max(msg.creationDate) from MessageEvent msg where msg.creationDate <= :referenceDate and "
                    + "parentURI = :parent")
    Date findMaxActivityDateOfParentAtTime(@Param("parent") URI parentURI, @Param("referenceDate") Date referenceDate);

    @Query("select max(msg.creationDate) from MessageEvent msg where msg.creationDate <= :referenceDate and "
                    + "parentURI = :parent and msg.messageType = :messageType")
    Date findMaxActivityDateOfParentAtTime(@Param("parent") URI parentURI,
                    @Param("messageType") WonMessageType messageType, @Param("referenceDate") Date referenceDate);

    /**
     * For a specified message (msg in the query), return true iff there is an
     * earlier message with the same recipient and the same innermost message uri
     * that is not the corresponding remote message of the specified one
     * 
     * @param messageUri
     * @return
     */
    @Query("select case when (count(otherMsg) > 0) then true else false end "
                    + "from MessageEvent msg, MessageEvent otherMsg join Connection otherCon "
                    + "on (otherMsg.parentURI = otherCon.connectionURI) " + "where "
                    + "msg.messageURI = :messageUri and " + "otherMsg.messageURI <> msg.messageURI and "
                    + "otherCon.atomURI = msg.recipientAtomURI and "
                    + "otherMsg.recipientAtomURI = msg.recipientAtomURI and "
                    + "otherMsg.innermostMessageURI = msg.innermostMessageURI and " + "( " +
                    // either the other message is earlier - then we lose
                    "    msg.creationDate > otherMsg.creationDate " + "    or (" +
                    // if both messages happen at the same instant, we need a tie-breaker: db id
                    "        msg.creationDate = otherMsg.creationDate and " + "        msg.id > otherMsg.id " + "    )"
                    + ")")
    boolean existEarlierMessageWithSameInnermostMessageURIAndRecipientAtomURI(
                    @Param("messageUri") URI messageUri);

    /**
     * When we want to forward a message to recipient r, we first check if we have
     * received a message with the same innermost message from r. If that's the
     * case, we don't forward it to r. Here is the check for that
     *
     * @param messageUri
     * @return
     */
    @Query("select case when (count(otherMsg) > 0) then true else false end "
                    + "from MessageEvent msg, MessageEvent otherMsg join Connection otherCon "
                    + "on (otherMsg.parentURI = otherCon.connectionURI) " + "where "
                    + "msg.messageURI = :messageUri and " + "otherMsg.senderAtomURI = :senderAtomUri and "
                    + "otherMsg.messageURI <> msg.messageURI and " + "otherCon.atomURI = msg.recipientAtomURI and "
                    + "otherMsg.recipientAtomURI = msg.recipientAtomURI and "
                    + "otherMsg.innermostMessageURI = msg.innermostMessageURI")
    boolean isReceivedSameInnermostMessageFromSender(@Param("messageUri") URI messageUri,
                    @Param("senderAtomUri") URI senderAtomURI);

    void deleteByParentURI(URI parentUri);
}
