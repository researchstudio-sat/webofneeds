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

import javax.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import won.protocol.model.ConnectionMessageContainer;

/**
 * Created by fkleedorfer on 05.12.2016.
 */
public interface ConnectionMessageContainerRepository extends WonRepository<ConnectionMessageContainer> {
    public ConnectionMessageContainer findOneByParentUri(URI parentUri);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ConnectionMessageContainer c where c.parentUri = :parentUri")
    public ConnectionMessageContainer findOneByParentUriForUpdate(@Param("parentUri") URI parentUri);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select connection, c from Connection connection join ConnectionMessageContainer c on connection.connectionURI = c.parentUri where c.parentUri = :parentUri")
    public void lockParentAndContainerByParentUriForUpdate(@Param("parentUri") URI parentUri);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ConnectionMessageContainer c join  MessageEventPlaceholder msg on msg.parentURI = c.parentUri where msg.messageURI = :messageUri")
    public ConnectionMessageContainer findOneByContainedMessageUriForUpdate(@Param("messageUri") URI messageUri);

    @Query("select case when (count(con) > 0) then true else false end " + "from Connection con "
                    + " where con.connectionURI = :connectionUri and ( " + "   con.atomURI = :webId "
                    + "   or con.targetAtomURI = :webId " + ")")
    public boolean isReadPermittedForWebID(@Param("connectionUri") URI connectionUri, @Param("webId") URI webId);
}
