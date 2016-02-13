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

  @Query("select messageURI from MessageEventPlaceholder msg where msg.parentURI = :parent")
  Slice<URI> getMessageURIsByParentURI(@Param("parent") URI parentURI, Pageable pageable);

  @Query("select messageURI from MessageEventPlaceholder msg where msg.parentURI = :parent and msg.messageType = :messageType")
  Slice<URI> getMessageURIsByParentURI(
    @Param("parent") URI parentURI,
    @Param("messageType") WonMessageType messageType,
    Pageable pageable);

  @Query("select messageURI from MessageEventPlaceholder msg where msg.parentURI = :parent and msg.creationDate < :referenceDate")
  Slice<URI> getMessageURIsByParentURIBefore(
    @Param("parent") URI parentURI,
    @Param("referenceDate") Date referenceDate,
    Pageable pageable);

  @Query("select messageURI from MessageEventPlaceholder msg where msg.parentURI = :parent and msg.creationDate < :referenceDate and msg.messageType = :messageType")
  Slice<URI> getMessageURIsByParentURIBefore(
    @Param("parent") URI parentURI,
    @Param("referenceDate") Date referenceDate,
    @Param("messageType") WonMessageType messageType,
    Pageable pageable);

  @Query("select messageURI from MessageEventPlaceholder msg where msg.parentURI = :parent and msg.creationDate > :referenceDate")
  Slice<URI> getMessageURIsByParentURIAfter(
    @Param("parent") URI parentURI,
    @Param("referenceDate") Date referenceDate,
    Pageable pageable);

  @Query("select messageURI from MessageEventPlaceholder msg where msg.parentURI = :parent and msg.creationDate > :referenceDate and msg.messageType = :messageType")
  Slice<URI> getMessageURIsByParentURIAfter(
    @Param("parent") URI parentURI,
    @Param("referenceDate") Date referenceDate,
    @Param("messageType") WonMessageType messageType,
    Pageable pageable);

  MessageEventPlaceholder findOneByCorrespondingRemoteMessageURI(URI uri);

}
