/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package won.protocol.repository;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import won.protocol.model.NeedEventContainer;

import javax.persistence.LockModeType;
import java.net.URI;

/**
 * Created by fkleedorfer on 05.12.2016.
 */
public interface NeedEventContainerRepository extends WonRepository<NeedEventContainer> {
  public NeedEventContainer findOneByParentUri(URI parentUri);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from NeedEventContainer c where c.parentUri = :parentUri")
  public NeedEventContainer findOneByParentUriForUpdate(@Param("parentUri") URI parentUri);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select need, c from Need need join NeedEventContainer c on need.needURI = c.parentUri where c.parentUri = :parentUri")
  public void lockParentAndContainerByParentUriForUpdate(@Param("parentUri") URI parentUri);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from NeedEventContainer c join MessageEventPlaceholder msg on msg.parentURI = c.parentUri where msg.messageURI = :messageUri")
  public NeedEventContainer findOneByContainedMessageUriForUpdate(@Param("messageUri") URI messageUri);

  @Query("select case when (count(n) > 0) then true else false end "
      + "from Need n left outer join Connection con on (n.needURI = con.needURI) " + " where n.needURI = :needUri and "
      + "( " + "   n.needURI = :webId or " + "   con.remoteNeedURI = :webId " + ")")
  public boolean isReadPermittedForWebID(@Param("needUri") URI connectionUri, @Param("webId") URI webId);

}
