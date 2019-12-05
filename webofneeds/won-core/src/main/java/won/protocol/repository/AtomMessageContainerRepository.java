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

import won.protocol.model.AtomMessageContainer;

/**
 * Created by fkleedorfer on 05.12.2016.
 */
public interface AtomMessageContainerRepository extends WonRepository<AtomMessageContainer> {
    AtomMessageContainer findOneByParentUri(URI parentUri);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from AtomMessageContainer c where c.parentUri = :parentUri")
    AtomMessageContainer findOneByParentUriForUpdate(@Param("parentUri") URI parentUri);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select atom, c from Atom atom join AtomMessageContainer c on atom.atomURI = c.parentUri where c.parentUri = :parentUri")
    void lockParentAndContainerByParentUriForUpdate(@Param("parentUri") URI parentUri);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from AtomMessageContainer c join MessageEvent msg on msg.parentURI = c.parentUri where msg.messageURI = :messageUri")
    AtomMessageContainer findOneByContainedMessageUriForUpdate(@Param("messageUri") URI messageUri);

    @Query("select case when (count(n) > 0) then true else false end "
                    + "from Atom n left outer join Connection con on (n.atomURI = con.atomURI) "
                    + " where n.atomURI = :atomUri and " + "( " + "   n.atomURI = :webId or "
                    + "   con.targetAtomURI = :webId " + ")")
    boolean isReadPermittedForWebID(@Param("atomUri") URI connectionUri, @Param("webId") URI webId);
}
