package won.protocol.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import won.protocol.message.WonMessageType;
import won.protocol.model.MessageEventPlaceholder;

import javax.persistence.LockModeType;
import java.net.URI;
import java.util.Date;
import java.util.List;

public interface MessageEventRepository extends WonRepository<MessageEventPlaceholder> {

    MessageEventPlaceholder findOneByMessageURI(URI URI);

    //read is permitted iff any of these conditions apply:
    // * the WebId is the sender need
    // * the WebId is the recipient need
    // * the WebId is a need that has a connection to the sender need AND the message is in the need's event container
    /*
    sql:
     select msg.*, con.*
            from message_event msg left outer join connection con on (
             msg.parentURI = con.connectionURI  -- msg is in connection. need and remote need have access
             or msg.parentURI = con.needURI -- msg not in connection but belongs to need. We allow all remote needs access
             -- if con is null, there are no connections for the msg's need, and therefore no remote needs. only allow the local need access
            )
             where (
		msg.messageURI = 'https://satvm05.researchstudio.at/won/resource/event/2x27qe2l4jt6obgxiqij' -- ok now we have the event and maybe a connection
		and (
                 (con is null and msg.parentURI = 'https://satvm05.researchstudio.at/won/resource/need/6825071433651196000') -- the event belongs to the need: fine
                 or con.needURI = 'https://satvm05.researchstudio.at/won/resource/need/6825071433651196000' -- grant access to the local need
                 or con.remoteNeedURI = 'https://satvm05.researchstudio.at/won/resource/need/6825071433651196000' -- grant access to the remote need
		 or msg.senderNodeURI = 'https://satvm05.researchstudio.at/won/resource/need/6825071433651196000' -- grant access to the sender node
		 or msg.receiverNodeURI = 'https://satvm05.researchstudio.at/won/resource/need/6825071433651196000' -- grant access to the receiver node
		) )
     */
    @Query("select case when (count(msg) > 0) then true else false end " +
            "from MessageEventPlaceholder msg left outer join Connection con on (" +
            " msg.parentURI = con.connectionURI or " +
            " msg.parentURI = con.needURI " +
            " ) " +
            " where msg.messageURI = :messageUri and (" +
            "   ( con is null and msg.parentURI = :webId )" +
            "   or con.needURI = :webId " +
            "   or con.remoteNeedURI = :webId " +
            "   or msg.receiverNodeURI = :webId " +
            "   or msg.senderNodeURI = :webId " +
            ")")
    public boolean isReadPermittedForWebID(@Param("messageUri") URI messageUri, @Param("webId") URI webId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n,c from NeedEventContainer c join MessageEventPlaceholder msg on msg.parentURI = c.parentUri join Need n on c.parentUri = n.needURI where msg.messageURI = :messageUri")
    public void lockNeedAndEventContainerByContainedMessageForUpdate(@Param("messageUri") URI messageUri);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select con,c from NeedEventContainer c join MessageEventPlaceholder msg on msg.parentURI = c.parentUri join Connection con on c.parentUri = con.needURI where msg.messageURI = :messageUri")
    public void lockConnectionAndEventContainerByContainedMessageForUpdate(@Param("messageUri") URI messageUri);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select msg from MessageEventPlaceholder msg where msg.messageURI = :uri")
    MessageEventPlaceholder findOneByMessageURIforUpdate(@Param("uri") URI uri);

    List<MessageEventPlaceholder> findByParentURI(URI URI);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select msg from MessageEventPlaceholder msg where msg.parentURI = :parent and msg.messageType = :messageType")
    List<MessageEventPlaceholder> findByParentURIAndMessageTypeForUpdate(
            @Param("parent") URI parentURI,
            @Param("messageType") WonMessageType messageType);

    @Query("select msg from MessageEventPlaceholder msg where msg.parentURI = :parent and msg.messageType = :messageType")
    List<MessageEventPlaceholder> findByParentURIAndMessageType(
            @Param("parent") URI parentURI,
            @Param("messageType") WonMessageType messageType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select msg from MessageEventPlaceholder msg left outer join MessageEventPlaceholder msg2 on msg.parentURI = msg2.parentURI and msg.creationDate < msg2.creationDate where msg.parentURI = :parent and msg2.id is null")
    MessageEventPlaceholder findNewestByParentURIforUpdate(@Param("parent") URI parentUri);

    @Query("select msg from MessageEventPlaceholder msg left outer join MessageEventPlaceholder msg2 on msg.parentURI = msg2.parentURI and msg.creationDate < msg2.creationDate where msg.parentURI = :parent and msg2.id is null")
    MessageEventPlaceholder findNewestByParentURI(@Param("parent") URI parentUri);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select msg from MessageEventPlaceholder msg left outer join MessageEventPlaceholder msg2 on msg.parentURI = msg2.parentURI and msg.creationDate > msg2.creationDate where msg.parentURI = :parent and msg2.id is null")
    MessageEventPlaceholder findOldestByParentURIforUpdate(@Param("parent") URI parentUri);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select msg from MessageEventPlaceholder msg where msg.parentURI = :parent and " +
            "referencedByOtherMessage = false")
    List<MessageEventPlaceholder> findByParentURIAndNotReferencedByOtherMessageForUpdate(
            @Param("parent") URI parentURI);
    @Query("select msg from MessageEventPlaceholder msg where msg.parentURI = :parent and " +
            "referencedByOtherMessage = false")
    List<MessageEventPlaceholder> findByParentURIAndNotReferencedByOtherMessage(
            @Param("parent") URI parentURI);

    @Query("select msg from MessageEventPlaceholder msg where msg.parentURI = :parent")
    Slice<MessageEventPlaceholder> findByParentURI(@Param("parent") URI parentURI, Pageable pageable);

    @Query("select msg from MessageEventPlaceholder msg left join fetch msg.datasetHolder where msg.parentURI = :parent")
    Slice<MessageEventPlaceholder> findByParentURIFetchDatasetEagerly(@Param("parent") URI parentURI, Pageable pageable);

    @Query("select messageURI from MessageEventPlaceholder msg where msg.parentURI = :parent and msg.messageType = :messageType")
    Slice<URI> getMessageURIsByParentURI(
            @Param("parent") URI parentURI,
            @Param("messageType") WonMessageType messageType,
            Pageable pageable);

    @Query("select msg from MessageEventPlaceholder msg where msg.parentURI = :parent and msg.messageType = :messageType")
    Slice<MessageEventPlaceholder> findByParentURIAndType(
            @Param("parent") URI parentURI,
            @Param("messageType") WonMessageType messageType,
            Pageable pageable);

    @Query("select msg from MessageEventPlaceholder msg left join fetch msg.datasetHolder where msg.parentURI = :parent and msg.messageType = :messageType")
    Slice<MessageEventPlaceholder> findByParentURIAndTypeFetchDatasetEagerly(
            @Param("parent") URI parentURI,
            @Param("messageType") WonMessageType messageType,
            Pageable pageable);

    @Query("select messageURI from MessageEventPlaceholder msg where msg.parentURI = :parent and msg.creationDate < :referenceDate")
    Slice<URI> getMessageURIsByParentURIBefore(
            @Param("parent") URI parentURI,
            @Param("referenceDate") Date referenceDate,
            Pageable pageable);

    @Query("select msg from MessageEventPlaceholder msg where msg.parentURI = :parent and msg.creationDate < :referenceDate")
    Slice<MessageEventPlaceholder> findByParentURIBefore(
            @Param("parent") URI parentURI,
            @Param("referenceDate") Date referenceDate,
            Pageable pageable);

    @Query("select msg from MessageEventPlaceholder msg left join fetch msg.datasetHolder where msg.parentURI = :parent and msg.creationDate < :referenceDate")
    Slice<MessageEventPlaceholder> findByParentURIBeforeFetchDatasetEagerly(
            @Param("parent") URI parentURI,
            @Param("referenceDate") Date referenceDate,
            Pageable pageable);

    @Query("select msg from MessageEventPlaceholder msg left join fetch msg.datasetHolder where msg.parentURI = :parent and msg.creationDate < (select msg2.creationDate from MessageEventPlaceholder msg2 where msg2.messageURI = :referenceMessageUri )")
    Slice<MessageEventPlaceholder> findByParentURIBeforeFetchDatasetEagerly(
            @Param("parent") URI parentURI,
            @Param("referenceMessageUri") URI referenceMessageUri,
            Pageable pageable);


    @Query("select messageURI from MessageEventPlaceholder msg where msg.parentURI = :parent and msg.creationDate < :referenceDate and msg.messageType = :messageType")
    Slice<URI> getMessageURIsByParentURIBefore(
            @Param("parent") URI parentURI,
            @Param("referenceDate") Date referenceDate,
            @Param("messageType") WonMessageType messageType,
            Pageable pageable);

    @Query("select msg from MessageEventPlaceholder msg where msg.parentURI = :parent and msg.creationDate < :referenceDate and msg.messageType = :messageType")
    Slice<MessageEventPlaceholder> findByParentURIAndTypeBefore(
            @Param("parent") URI parentURI,
            @Param("referenceDate") Date referenceDate,
            @Param("messageType") WonMessageType messageType,
            Pageable pageable);

    @Query("select msg from MessageEventPlaceholder msg left join fetch msg.datasetHolder where msg.parentURI = :parent and msg.creationDate < :referenceDate and msg.messageType = :messageType")
    Slice<MessageEventPlaceholder> findByParentURIAndTypeBeforeFetchDatasetEagerly(
            @Param("parent") URI parentURI,
            @Param("referenceDate") Date referenceDate,
            @Param("messageType") WonMessageType messageType,
            Pageable pageable);

    @Query("select msg from MessageEventPlaceholder msg left join fetch msg.datasetHolder where msg.parentURI = :parent and msg.messageType = :messageType and msg.creationDate < (select msg2.creationDate from MessageEventPlaceholder msg2 where msg2.messageURI = :referenceMessageUri )")
    Slice<MessageEventPlaceholder> findByParentURIAndTypeBeforeFetchDatasetEagerly(
            @Param("parent") URI parentURI,
            @Param("referenceMessageUri") URI referenceMessageURI,
            @Param("messageType") WonMessageType messageType,
            Pageable pageable);

    @Query("select messageURI from MessageEventPlaceholder msg where msg.parentURI = :parent and msg.creationDate > :referenceDate")
    Slice<URI> getMessageURIsByParentURIAfter(
            @Param("parent") URI parentURI,
            @Param("referenceDate") Date referenceDate,
            Pageable pageable);

    @Query("select msg from MessageEventPlaceholder msg where msg.parentURI = :parent and msg.creationDate > " +
            ":referenceDate")
    Slice<MessageEventPlaceholder> findByParentURIAfter(
            @Param("parent") URI parentURI,
            @Param("referenceDate") Date referenceDate,
            Pageable pageable);

    @Query("select msg from MessageEventPlaceholder msg left join fetch msg.datasetHolder where msg.parentURI = :parent and msg.creationDate > " +
            ":referenceDate")
    Slice<MessageEventPlaceholder> findByParentURIAfterFetchDatasetEagerly(
            @Param("parent") URI parentURI,
            @Param("referenceDate") Date referenceDate,
            Pageable pageable);

    @Query("select messageURI from MessageEventPlaceholder msg where msg.parentURI = :parent and msg.creationDate > :referenceDate and msg.messageType = :messageType")
    Slice<URI> getMessageURIsByParentURIAfter(
            @Param("parent") URI parentURI,
            @Param("referenceDate") Date referenceDate,
            @Param("messageType") WonMessageType messageType,
            Pageable pageable);


    @Query("select msg from MessageEventPlaceholder msg left join fetch msg.datasetHolder where msg.parentURI = :parent and msg.creationDate > :referenceDate and msg.messageType = :messageType")
    Slice<MessageEventPlaceholder> findByParentURIAndTypeAfter(
            @Param("parent") URI parentURI,
            @Param("referenceDate") Date referenceDate,
            @Param("messageType") WonMessageType messageType,
            Pageable pageable);

    @Query("select msg from MessageEventPlaceholder msg where msg.parentURI = :parent and msg.creationDate > :referenceDate and msg.messageType = :messageType")
    Slice<MessageEventPlaceholder> findByParentURIAndTypeAfterFetchDatasetEagerly(
            @Param("parent") URI parentURI,
            @Param("referenceDate") Date referenceDate,
            @Param("messageType") WonMessageType messageType,
            Pageable pageable);


    @Query("select msg from MessageEventPlaceholder msg where msg.correspondingRemoteMessageURI = :uri")
    MessageEventPlaceholder findOneByCorrespondingRemoteMessageURI(@Param("uri") URI uri);


    @Query("select max(msg.creationDate) from MessageEventPlaceholder msg where msg.creationDate <= :referenceDate and " +
            "parentURI = :parent")
    Date findMaxActivityDateOfParentAtTime(@Param("parent") URI parentURI, @Param("referenceDate") Date referenceDate);

    @Query("select max(msg.creationDate) from MessageEventPlaceholder msg where msg.creationDate <= :referenceDate and " +
            "parentURI = :parent and msg.messageType = :messageType")
    Date findMaxActivityDateOfParentAtTime(@Param("parent") URI parentURI, @Param("messageType") WonMessageType
            messageType, @Param("referenceDate") Date referenceDate);

}
