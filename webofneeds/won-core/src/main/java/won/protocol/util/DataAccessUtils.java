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

package won.protocol.util;

import java.net.URI;
import java.text.MessageFormat;
import java.util.List;

import won.protocol.exception.NoSuchConnectionException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.exception.NoSuchOwnerApplicationException;
import won.protocol.model.Connection;
import won.protocol.model.Need;
import won.protocol.model.OwnerApplication;
import won.protocol.model.WonNode;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.NeedRepository;
import won.protocol.repository.OwnerApplicationRepository;
import won.protocol.repository.WonNodeRepository;

/**
 * User: fkleedorfer Date: 05.11.12
 */
public class DataAccessUtils {

    /**
     * Loads the specified need from the database and raises an exception if it is not found.
     *
     * @param needURI
     * @throws won.protocol.exception.NoSuchNeedException
     * @return the connection
     */
    public static Need loadNeed(NeedRepository needRepository, final URI needURI) throws NoSuchNeedException {
        List<Need> needs = needRepository.findByNeedURI(needURI);
        if (needs.size() == 0)
            throw new NoSuchNeedException(needURI);
        if (needs.size() > 1)
            throw new IllegalStateException(MessageFormat
                    .format("Inconsistent database state detected: multiple needs found with URI {0}", needURI));
        return needs.get(0);
    }

    /**
     * Loads the specified wonNode from the database.
     *
     * @param wonNodeURI
     * @throws won.protocol.exception.NoSuchNeedException
     * @return the wonNode or null if none is found
     */
    public static WonNode loadWonNode(WonNodeRepository repository, final URI wonNodeURI) {
        List<WonNode> nodes = repository.findByWonNodeURI(wonNodeURI);
        if (nodes.size() == 0)
            return null;
        if (nodes.size() > 1)
            throw new IllegalStateException(MessageFormat
                    .format("Inconsistent database state detected: multiple needs found with URI {0}", wonNodeURI));
        return nodes.get(0);
    }

    public static String loadOwnerApplication(OwnerApplicationRepository ownerApplicationRepository,
            final String ownerApplicationId) throws NoSuchOwnerApplicationException {
        List<OwnerApplication> ownerApplications = ownerApplicationRepository
                .findByOwnerApplicationId(ownerApplicationId);
        if (ownerApplications.size() == 0)
            throw new NoSuchOwnerApplicationException();
        if (ownerApplications.size() > 1)
            throw new IllegalStateException(MessageFormat.format(
                    "Inconsistent database state detected: multiple connections found with URI {0}",
                    ownerApplicationId));
        return ownerApplications.get(0).getOwnerApplicationId();

    }

    public static Connection loadConnection(ConnectionRepository connectionRepository, final Long id)
            throws NoSuchConnectionException {
        List<Connection> connections = connectionRepository.findById(id);
        if (connections.size() == 0)
            throw new NoSuchConnectionException(id);
        if (connections.size() > 1)
            throw new IllegalStateException(MessageFormat
                    .format("Inconsistent database state detected: multiple connections found with URI {0}", id));
        return connections.get(0);
    }

    /**
     * Loads the specified connection from the database and raises an exception if it is not found.
     * 
     * @param connectionRepository
     * @param connectionURI
     * @return
     * @throws NoSuchConnectionException
     */
    public static Connection loadConnection(ConnectionRepository connectionRepository, final URI connectionURI)
            throws NoSuchConnectionException {
        List<Connection> connections = connectionRepository.findByConnectionURI(connectionURI);
        if (connections.size() == 0)
            throw new NoSuchConnectionException(connectionURI);
        if (connections.size() > 1)
            throw new IllegalStateException(MessageFormat.format(
                    "Inconsistent database state detected: multiple connections found with URI {0}", connectionURI));
        return connections.get(0);
    }

}
