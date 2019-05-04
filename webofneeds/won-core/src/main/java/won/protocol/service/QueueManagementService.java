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
package won.protocol.service;

import java.util.List;

import won.protocol.model.OwnerApplication;

/**
 * User: sbyim Date: 13.11.13
 */
public interface QueueManagementService {
    public List<String> generateQueueNamesForOwnerApplication(OwnerApplication ownerApplication);

    public String getEndpointForMessage(String methodName, String ownerApplicationID);
}
