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

/*
 * This file is subject to the terms and conditions defined in file 'LICENSE.txt', which is part of this source code package.
 */

package won.owner.repository;

import won.owner.model.Draft;
import won.protocol.repository.WonRepository;

import java.net.URI;
import java.util.List;

/**
 * User: syim
 * Date: 11/7/13
 */
public interface DraftRepository extends WonRepository<Draft> {

  public List<Draft> findById(long id);

  public Draft findOneByDraftURI(URI draftURI);

  public List<Draft> findByDraftURI(URI draftURI);

}
