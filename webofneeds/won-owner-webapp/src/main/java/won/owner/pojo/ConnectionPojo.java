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
package won.owner.pojo;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

import won.protocol.vocabulary.WON;

/**
 * User: LEIH-NB Date: 23.07.14
 */
public class ConnectionPojo {
    private long connectionId = -1;
    private String connectionURI;
    private String remoteConnectionURI;
    private String needURI;
    private String remoteNeedURI;
    private String connectionState;
    private String[] facetURIs;
    private String[] remoteFacetURIs;
    private String wonNodeURI;

    public ConnectionPojo(URI connectionURI, final Model model) {
        this.connectionURI = connectionURI.toString();
        Resource connection = model.getResource(connectionURI.toString());
        Statement remoteConnectionStatement = connection.getProperty(WON.HAS_REMOTE_CONNECTION);
        if (remoteConnectionStatement != null) {
            remoteConnectionURI = remoteConnectionStatement.getString();
        }
        Statement needURIStatement = connection.getProperty(WON.BELONGS_TO_NEED);
        if (needURIStatement != null) {
            needURI = needURIStatement.getString();
        }
        Statement connectionStateStatement = connection.getProperty(WON.HAS_CONNECTION_STATE);
        if (connectionStateStatement != null) {
            connectionState = connectionStateStatement.getString();
        }
        StmtIterator facetIter = connection.listProperties(WON.HAS_FACET);
        List<String> facets = new ArrayList<String>(10);
        while (facetIter.hasNext()) {
            Statement stmt = facetIter.nextStatement();
            facets.add(stmt.getObject().toString());
        }
        this.facetURIs = facets.toArray(new String[facets.size()]);
        StmtIterator remoteFacetIter = connection.listProperties(WON.HAS_REMOTE_FACET);
        List<String> remoteFacets = new ArrayList<String>(10);
        while (remoteFacetIter.hasNext()) {
            Statement stmt = remoteFacetIter.nextStatement();
            remoteFacets.add(stmt.getObject().toString());
        }
        this.remoteFacetURIs = remoteFacets.toArray(new String[remoteFacets.size()]);
        Statement wonNodeStatement = connection.getProperty(WON.HAS_WON_NODE);
        if (wonNodeStatement != null) {
            wonNodeURI = wonNodeStatement.getString();
        }
    }

    public String getRemoteConnectionURI() {
        return remoteConnectionURI;
    }

    public void setRemoteConnectionURI(final String remoteConnectionURI) {
        this.remoteConnectionURI = remoteConnectionURI;
    }

    public String getConnectionURI() {
        return connectionURI;
    }

    public void setConnectionURI(final String connectionURI) {
        this.connectionURI = connectionURI;
    }

    public String getNeedURI() {
        return needURI;
    }

    public void setNeedURI(final String needURI) {
        this.needURI = needURI;
    }

    public String getConnectionState() {
        return connectionState;
    }

    public void setConnectionState(final String connectionState) {
        this.connectionState = connectionState;
    }

    public String[] getFacetURIs() {
        return facetURIs;
    }

    public void setFacetURIs(final String[] facetURIs) {
        this.facetURIs = facetURIs;
    }

    public String[] getRemoteFacetURIs() {
        return remoteFacetURIs;
    }

    public void setRemoteFacetURIs(final String[] remoteFacetURIs) {
        this.remoteFacetURIs = remoteFacetURIs;
    }

    public String getWonNodeURI() {
        return wonNodeURI;
    }

    public void setWonNodeURI(final String wonNodeURI) {
        this.wonNodeURI = wonNodeURI;
    }

    public String getRemoteNeedURI() {
        return remoteNeedURI;
    }

    public void setRemoteNeedURI(final String remoteNeedURI) {
        this.remoteNeedURI = remoteNeedURI;
    }

    public long getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(final long connectionId) {
        this.connectionId = connectionId;
    }
}
