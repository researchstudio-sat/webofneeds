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
package won.node.springsecurity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.util.StopWatch;

import won.cryptography.webid.AccessControlRules;

/**
 * Created by fkleedorfer on 28.11.2016.
 */
public class DefaultWoNAccessDecisionVoter implements AccessDecisionVoter {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    AccessControlRules defaultAccessControlRules;

    public DefaultWoNAccessDecisionVoter() {
    }

    @Override
    public boolean supports(final ConfigAttribute attribute) {
        return true;
    }

    @Override
    public boolean supports(final Class clazz) {
        return FilterInvocation.class.equals(clazz);
    }

    @Override
    public int vote(final Authentication authentication, final Object object, final Collection collection) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        if (!(authentication instanceof PreAuthenticatedAuthenticationToken))
            return ACCESS_ABSTAIN;
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof WebIdUserDetails))
            return ACCESS_ABSTAIN;
        WebIdUserDetails userDetails = (WebIdUserDetails) principal;
        if (!(object instanceof FilterInvocation))
            return ACCESS_ABSTAIN;
        String webId = userDetails.getUsername();
        String resource = ((FilterInvocation) object).getRequest().getRequestURL().toString();
        if (authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                        .filter(r -> "ROLE_WEBID".equals(r)).findAny().isPresent()) {
            // perform our hard coded access control checks
            List<String> webIDs = new ArrayList<>(1);
            webIDs.add(webId);
            if (defaultAccessControlRules.isAccessPermitted(resource, webIDs)) {
                stopWatch.stop();
                logger.debug("access control check took " + stopWatch.getLastTaskTimeMillis() + " millis");
                return ACCESS_GRANTED;
            }
            return ACCESS_DENIED;
        }
        return ACCESS_DENIED;
    }

    public void setDefaultAccessControlRules(final WonDefaultAccessControlRules defaultAccessControlRules) {
        this.defaultAccessControlRules = defaultAccessControlRules;
    }
}
