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

package won.protocol.owner;

import won.protocol.exception.NoSuchNeedException;
import won.protocol.model.Match;
import won.protocol.service.ConnectionCommunicationService;
import won.protocol.service.NeedInformationService;
import won.protocol.service.NeedManagementService;
import won.protocol.service.OwnerFacingNeedCommunicationService;

import java.net.URI;
import java.util.Collection;

/**
 * User: fkleedorfer
 * Date: 31.10.12
 */
public interface OwnerProtocolNeedService extends NeedManagementService, NeedInformationService,
        ConnectionCommunicationService, OwnerFacingNeedCommunicationService { }
