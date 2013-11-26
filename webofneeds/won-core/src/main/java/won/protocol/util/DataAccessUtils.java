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

import won.protocol.exception.NoSuchConnectionException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.model.Connection;
import won.protocol.model.Need;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.NeedRepository;

import java.net.URI;
import java.text.MessageFormat;
import java.util.List;

/**
 * User: fkleedorfer
 * Date: 05.11.12
 */
public class DataAccessUtils {

    /**
    * Loads the specified need from the database and raises an exception if it is not found.
    *
    * @param needURI
    * @throws won.protocol.exception.NoSuchNeedException
    * @return the connection
    */
    public static Need loadNeed(NeedRepository needRepository, final URI needURI) throws NoSuchNeedException
    {
        List<Need> needs = needRepository.findByNeedURI(needURI);
        if (needs.size() == 0) throw new NoSuchNeedException(needURI);
        if (needs.size() > 1) throw new IllegalStateException(MessageFormat.format("Inconsistent database state detected: multiple needs found with URI {0}", needURI));
        return needs.get(0);
    }

    /**
    * Loads the specified connection from the database and raises an exception if it is not found.
    * @param connectionRepository
    * @param connectionURI
    * @return
    * @throws NoSuchConnectionException
    */
    public static Connection loadConnection(ConnectionRepository connectionRepository, final URI connectionURI) throws NoSuchConnectionException
    {
        List<Connection> connections = connectionRepository.findByConnectionURI(connectionURI);
        if (connections.size() == 0) throw new NoSuchConnectionException(connectionURI);
        if (connections.size() > 1) throw new IllegalStateException(MessageFormat.format("Inconsistent database state detected: multiple connections found with URI {0}",connectionURI));
        return connections.get(0);
    }
}
