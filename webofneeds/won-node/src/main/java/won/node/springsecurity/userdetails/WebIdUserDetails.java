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
package won.node.springsecurity.userdetails;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by fkleedorfer on 24.11.2016.
 */
public class WebIdUserDetails extends AbstractUserDetails {
    private URI webId;

    public WebIdUserDetails(final URI webId, Collection<? extends GrantedAuthority> grantedAuthorities) {
        super(grantedAuthorities);
        this.webId = webId;
    }

    /**
     * Returns the webId.
     *
     * @return
     */
    @Override
    public String getUsername() {
        return webId.toString();
    }
}
