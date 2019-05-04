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
package won.node.socket.impl;

import java.net.URI;

import org.apache.jena.rdf.model.Model;

import won.protocol.exception.ConnectionAlreadyExistsException;
import won.protocol.exception.IllegalMessageForConnectionStateException;
import won.protocol.exception.IllegalMessageForAtomStateException;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.exception.NoSuchAtomException;
import won.protocol.message.WonMessage;
import won.protocol.model.Connection;
import won.protocol.model.SocketType;

/**
 * User: fkleedorfer Date: 25.03.14
 */
public interface SocketLogic {
    SocketType getSocketType();

    void openFromOwner(Connection con, Model content, WonMessage wonMessage)
                    throws NoSuchConnectionException, IllegalMessageForConnectionStateException;

    void closeFromOwner(Connection con, Model content, WonMessage wonMessage)
                    throws NoSuchConnectionException, IllegalMessageForConnectionStateException;

    void sendMessageFromOwner(Connection con, Model message, WonMessage wonMessage)
                    throws NoSuchConnectionException, IllegalMessageForConnectionStateException;

    void openFromAtom(Connection con, Model content, WonMessage wonMessage)
                    throws NoSuchConnectionException, IllegalMessageForConnectionStateException;

    void closeFromAtom(Connection con, Model content, WonMessage wonMessage)
                    throws NoSuchConnectionException, IllegalMessageForConnectionStateException;

    void sendMessageFromAtom(Connection con, Model message, WonMessage wonMessage)
                    throws NoSuchConnectionException, IllegalMessageForConnectionStateException;

    void hint(Connection con, double score, URI originator, Model content, WonMessage wonMessage)
                    throws NoSuchAtomException, IllegalMessageForAtomStateException;

    void connectFromAtom(Connection con, Model content, WonMessage wonMessage)
                    throws NoSuchAtomException, IllegalMessageForAtomStateException, ConnectionAlreadyExistsException;

    void connectFromOwner(Connection con, Model content, WonMessage wonMessage)
                    throws NoSuchAtomException, IllegalMessageForAtomStateException, ConnectionAlreadyExistsException;
}
