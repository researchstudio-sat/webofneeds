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

package won.bot.framework.component.nodeurisource.impl;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import won.bot.framework.component.nodeurisource.NodeURISource;

/**
 * NodeUriSource that is given a list of URIs and returns each element in a round robin fashion.
 */
public class RoundRobinMultiNodeUriSource implements NodeURISource {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private List<URI> nodeURIs = null;
    private int lastIndex = -1;

    @Override
    public URI getNodeURI() {
        if (this.nodeURIs == null || this.nodeURIs.isEmpty())
            return null;
        int index = lastIndex + 1;
        if (index >= this.nodeURIs.size()) {
            index = 0;
        }
        URI nodeUri = this.nodeURIs.get(index);
        this.lastIndex = index;
        logger.debug("using node URI '{}'", nodeUri);
        return nodeUri;
    }

    public void setNodeURIs(final Collection<URI> nodeURIs) {
        if (nodeURIs == null) {
            this.nodeURIs = new ArrayList<URI>();
        } else {
            this.nodeURIs = new ArrayList<URI>(nodeURIs.size());
            this.nodeURIs.addAll(nodeURIs);
        }
    }

}
