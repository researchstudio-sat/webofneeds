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
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import won.protocol.model.DatasetHolder;

/**
 * Repository for jena datasets (wrapped in DatasetHolders)
 */
public interface DatasetHolderRepository extends CrudRepository<DatasetHolder, URI> {
    DatasetHolder findOneByUriAndVersionNot(URI uri, Integer version);

    DatasetHolder findOneByUri(URI uri);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DatasetHolder d where d.uri = :uri")
    DatasetHolder findOneByUriForUpdate(@Param("uri") URI uri);
}
