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
package won.owner.repository;

import java.net.URI;
import java.util.List;

import org.springframework.data.jpa.repository.Query;

import won.owner.model.UserNeed;
import won.protocol.repository.WonRepository;

/**
 * User: fkleedorfer Date: 15.10.2014
 */
public interface UserNeedRepository extends WonRepository<UserNeed> {
    @Query(value = "SELECT n from UserNeed n where n.uri = ?1")
    public UserNeed findByNeedUri(URI needUri);

    @Query(value = "SELECT n from UserNeed n order by n.creationDate DESC")
    public List<UserNeed> findAllNeeds();
}
