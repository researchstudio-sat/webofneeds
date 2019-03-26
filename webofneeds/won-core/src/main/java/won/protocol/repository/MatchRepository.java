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

import java.net.URI;
import java.util.List;

import org.springframework.data.domain.Sort;

import won.protocol.model.Match;

/**
 * User: fkleedorfer Date: 05.11.12
 */
public interface MatchRepository extends WonRepository<Match> {
  public List<Match> findByFromNeed(URI fromNeed);

  public List<Match> findByFromNeed(URI fromNeed, Sort sort);

  public List<Match> findByFromNeedAndToNeedAndOriginator(URI fromNeed, URI toNeed, URI originator);
}
