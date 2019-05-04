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
    private String targetConnectionURI;
    private String atomURI;
    private String targetAtomURI;
    private String connectionState;
    private String[] socketURIs;
    private String[] targetSocketURIs;
    private String wonNodeURI;

    public ConnectionPojo(URI connectionURI, final Model model) {
        this.connectionURI = connectionURI.toString();
        Resource connection = model.getResource(connectionURI.toString());
        Statement targetConnectionStatement = connection.getProperty(WON.targetConnection);
        if (targetConnectionStatement != null) {
            targetConnectionURI = targetConnectionStatement.getString();
        }
        Statement atomURIStatement = connection.getProperty(WON.sourceAtom);
        if (atomURIStatement != null) {
            atomURI = atomURIStatement.getString();
        }
        Statement connectionStateStatement = connection.getProperty(WON.connectionState);
        if (connectionStateStatement != null) {
            connectionState = connectionStateStatement.getString();
        }
        StmtIterator socketIter = connection.listProperties(WON.socket);
        List<String> sockets = new ArrayList<String>(10);
        while (socketIter.hasNext()) {
            Statement stmt = socketIter.nextStatement();
            sockets.add(stmt.getObject().toString());
        }
        this.socketURIs = sockets.toArray(new String[sockets.size()]);
        StmtIterator targetSocketIter = connection.listProperties(WON.targetSocket);
        List<String> targetSockets = new ArrayList<String>(10);
        while (targetSocketIter.hasNext()) {
            Statement stmt = targetSocketIter.nextStatement();
            targetSockets.add(stmt.getObject().toString());
        }
        this.targetSocketURIs = targetSockets.toArray(new String[targetSockets.size()]);
        Statement wonNodeStatement = connection.getProperty(WON.wonNode);
        if (wonNodeStatement != null) {
            wonNodeURI = wonNodeStatement.getString();
        }
    }

    public String getTargetConnectionURI() {
        return targetConnectionURI;
    }

    public void setTargetConnectionURI(final String targetConnectionURI) {
        this.targetConnectionURI = targetConnectionURI;
    }

    public String getConnectionURI() {
        return connectionURI;
    }

    public void setConnectionURI(final String connectionURI) {
        this.connectionURI = connectionURI;
    }

    public String getAtomURI() {
        return atomURI;
    }

    public void setAtomURI(final String atomURI) {
        this.atomURI = atomURI;
    }

    public String getConnectionState() {
        return connectionState;
    }

    public void setConnectionState(final String connectionState) {
        this.connectionState = connectionState;
    }

    public String[] getSocketURIs() {
        return socketURIs;
    }

    public void setSocketURIs(final String[] socketURIs) {
        this.socketURIs = socketURIs;
    }

    public String[] getTargetSocketURIs() {
        return targetSocketURIs;
    }

    public void setTargetSocketURIs(final String[] targetSocketURIs) {
        this.targetSocketURIs = targetSocketURIs;
    }

    public String getWonNodeURI() {
        return wonNodeURI;
    }

    public void setWonNodeURI(final String wonNodeURI) {
        this.wonNodeURI = wonNodeURI;
    }

    public String getTargetAtomURI() {
        return targetAtomURI;
    }

    public void setTargetAtomURI(final String targetAtomURI) {
        this.targetAtomURI = targetAtomURI;
    }

    public long getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(final long connectionId) {
        this.connectionId = connectionId;
    }
}
