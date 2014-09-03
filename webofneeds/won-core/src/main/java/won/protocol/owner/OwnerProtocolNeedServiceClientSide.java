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

package won.protocol.owner;

import com.google.common.util.concurrent.ListenableFuture;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import org.springframework.context.ApplicationContextAware;
import won.protocol.exception.*;
import won.protocol.message.WonMessage;

import java.net.URI;
import java.util.concurrent.ExecutionException;

/**
 * User: LEIH-NB
 * Date: 17.10.13
 */
public interface OwnerProtocolNeedServiceClientSide extends ApplicationContextAware{

    /**
     * registers the owner application on WON Node and receive client ID
     *
     * @param endpointURI
     * */
    // ToDo (FS): this one shouldn't be here, right?
     public String register(URI endpointURI) throws Exception;

    /**
     * Creates a new need with the specified content, ownerURI and active state.
     *
     * @param content
     * @param activate
     * @param wonMessage contains the whole message as messageEvent RDF
     * @return the URI of the newly created need
     */
    public ListenableFuture<URI> createNeed(
            Model content,
            final boolean activate,
            WonMessage wonMessage) throws Exception;

    /**
     * Activates the need object.
     *
     * @param needURI
     * @param messageEvent contains the whole message as messageEvent RDF
     * @throws won.protocol.exception.NoSuchNeedException if needURI does not refer to an existing need
     */
    public void activate(URI needURI, Dataset messageEvent) throws Exception;

    /**
     * Deactivates the need object, closing all its established connections.
     *
     * @param needURI
     * @param messageEvent contains the whole message as messageEvent RDF
     * @throws NoSuchNeedException if needURI does not refer to an existing need
     */
    public void deactivate(URI needURI, Dataset messageEvent)
            throws NoSuchNeedException, Exception;

    public ListenableFuture<URI> createNeed(
            Model content,
            boolean activate,
            URI wonNodeUri,
            WonMessage wonMessage) throws Exception;
    /**
     * Opens a connection identified by connectionURI. A rdf graph can be sent along with the request.
     *
     * @param connectionURI the URI of the connection
     * @param content a rdf graph describing properties of the event. The null releative URI ('<>') inside that graph,
     *                as well as the base URI of the graph will be attached to the resource identifying the event.
     * @param messageEvent contains the whole message as messageEvent RDF
     * @throws won.protocol.exception.NoSuchConnectionException if connectionURI does not refer to an existing connection
     * @throws won.protocol.exception.IllegalMessageForConnectionStateException if the message is not allowed in the current state of the connection
     */
    public void open(URI connectionURI, Model content, Dataset messageEvent)
            throws Exception;

    /**
     * Closes the connection identified by the specified URI.
     *
     * @param connectionURI the URI of the connection
     * @param content a rdf graph describing properties of the event. The null releative URI ('<>') inside that graph,
     *                as well as the base URI of the graph will be attached to the resource identifying the event.
     * @param messageEvent contains the whole message as messageEvent RDF
     * @throws NoSuchConnectionException if connectionURI does not refer to an existing connection
     * @throws IllegalMessageForConnectionStateException if the message is not allowed in the current state of the connection
     */
    public void close(URI connectionURI, Model content, Dataset messageEvent)
            throws NoSuchConnectionException, IllegalMessageForConnectionStateException, Exception;

    /**
     * Sends a chat message via the local connection identified by the specified connectionURI
     * to the remote partner.
     * TODO: text messages are currently being saved on the owner side without checking whether the messages are succesfully processed on the node side. should be changed.
     *
     * @param connectionURI the local connection
     * @param message       the chat message
     * @param messageEvent contains the whole message as messageEvent RDF
     * @throws NoSuchConnectionException if connectionURI does not refer to an existing connection
     * @throws IllegalMessageForConnectionStateException if the message is not allowed in the current state of the connection
     */
    public void sendMessage(URI connectionURI, Model message, Dataset messageEvent) throws Exception;

    public ListenableFuture<URI> connect(
            URI needURI,
            URI otherNeedURI,
            Model content,
            Dataset messageEvent)
            throws NoSuchNeedException,
            IllegalMessageForNeedStateException, ConnectionAlreadyExistsException,
            ExecutionException, InterruptedException, CamelConfigurationFailedException, Exception;

    public void sendWonMessage(WonMessage wonMessage) throws Exception;

}
