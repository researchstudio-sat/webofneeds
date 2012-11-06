/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package won.protocol.repository;

import org.springframework.stereotype.Repository;
import won.protocol.model.Connection;

import java.net.URI;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Gabriel
 * Date: 04.11.12
 * Time: 16:56
 * To change this template use File | Settings | File Templates.
 */
@Repository
public interface ConnectionRepository extends WonRepository<Connection> {
  List<Connection> findByConnectionURI(URI URI);
  List<Connection> findByNeedURI(URI URI);
}
