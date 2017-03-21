package won.protocol.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import won.protocol.message.WonMessageType;
import won.protocol.model.MessageEventPlaceholder;

import java.net.URI;
import java.util.Date;
import java.util.List;

public interface MessageEventRepository extends WonRepository<MessageEventPlaceholder> {

  MessageEventPlaceholder findOneByMessageURI(URI URI);

  List<MessageEventPlaceholder> findByParentURI(URI URI);

  List<URI> getMessageURIsByParentURI(URI parentURI);

  List<MessageEventPlaceholder> findByParentURIAndMessageType(URI parentURI, WonMessageType messageType);

  @Query("select msg from MessageEventPlaceholder msg where msg.parentURI = :parent and " +
    "referencedByOtherMessage = false")
  List<MessageEventPlaceholder> findByParentURIAndNotReferencedByOtherMessage(
    @Param("parent") URI parentURI);

  @Query("select messageURI from MessageEventPlaceholder msg where msg.parentURI = :parent")
  Slice<URI> getMessageURIsByParentURI(@Param("parent") URI parentURI, Pageable pageable);

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


  MessageEventPlaceholder findOneByCorrespondingRemoteMessageURI(URI uri);

  @Query("select max(msg.creationDate) from MessageEventPlaceholder msg where msg.creationDate <= :referenceDate and " +
    "parentURI = :parent")
  Date findMaxActivityDateOfParentAtTime(@Param("parent") URI parentURI, @Param("referenceDate") Date referenceDate);

  @Query("select max(msg.creationDate) from MessageEventPlaceholder msg where msg.creationDate <= :referenceDate and " +
    "parentURI = :parent and msg.messageType = :messageType")
  Date findMaxActivityDateOfParentAtTime(@Param("parent") URI parentURI, @Param("messageType") WonMessageType
    messageType, @Param("referenceDate") Date referenceDate);

}
