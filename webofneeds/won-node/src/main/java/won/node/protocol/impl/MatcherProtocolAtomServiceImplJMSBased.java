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
package won.node.protocol.impl;

import org.apache.camel.Header;
import org.apache.jena.riot.Lang;
import org.springframework.stereotype.Service;
import won.protocol.matcher.MatcherProtocolAtomService;
import won.protocol.message.WonMessageDecoder;
import won.protocol.util.RdfUtils;

import java.net.URI;

/**
 * User: fkleedorfer Date: 02.11.12
 */
@Service
public class MatcherProtocolAtomServiceImplJMSBased// implements MatcherProtocolAtomService
{
    private MatcherProtocolAtomService delegate;

    public void hint(@Header("atomURI") final String atomURI, @Header("otherAtomURI") final String otherAtomURI,
                    @Header("score") final String score, @Header("originator") final String originator,
                    @Header("content") final String content, @Header("wonMessage") final String wonMessage)
                    throws Exception {
        delegate.hint(URI.create(atomURI), URI.create(otherAtomURI), Double.valueOf(score), URI.create(originator),
                        RdfUtils.toModel(content), WonMessageDecoder.decode(Lang.TRIG, wonMessage));
    }

    public void setDelegate(MatcherProtocolAtomService delegate) {
        this.delegate = delegate;
    }
}
