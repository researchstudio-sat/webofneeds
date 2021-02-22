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

/**
 * Created by fkleedorfer on 05.09.2014.
 */
import { is } from "../utils.js";

import jsonld from "jsonld/dist/jsonld.min.js";
import vocab from "~/app/service/vocab";
import won from "./won.js";

import linkedDataWorker from "workerize-loader?[name].[contenthash:8]!../../ld-worker.js";

(function() {
  /**
   * Loads the atom-data without following up
   * with a request for the connection-container
   * to get the connection-uris. Thus it's faster.
   */
  won.fetchAtom = (atomUri, requestCredentials) =>
    linkedDataWorker().fetchAtom(atomUri, requestCredentials, vocab);

  won.validateEnvelopeDataForAtom = atomUri => {
    if (typeof atomUri === "undefined" || atomUri == null) {
      throw {
        message: "validateEnvelopeDataForAtom: atomUri must not be null",
      };
    }

    return Promise.resolve();
  };

  won.validateEnvelopeDataForConnection = (socketUri, targetSocketUri) => {
    if (
      typeof socketUri === "undefined" ||
      socketUri == null ||
      typeof targetSocketUri === "undefined" ||
      targetSocketUri == null
    ) {
      throw {
        message: "getEnvelopeDataforConnection: socketUris must not be null",
      };
    }

    return Promise.resolve();
  };

  /**
   * Fetches all MetaConnections of the given connectionContainerUri
   * @param connectionContainerUri
   * @param requesterWebId -> usually unset or set to the atomUri itself, this needs to be adapted to include the correct requesterWebId that has credentials to view the connectionContainer of the given atom
   * @param connectedOnly -> if set to true, only connections with the state "Connected" will be returned
   * @returns {Promise<never>}
   */
  won.fetchConnectionUrisWithStateByAtomUri = (
    connectionContainerUri,
    requestCredentials
  ) =>
    linkedDataWorker().fetchConnectionUrisWithStateByAtomUri(
      connectionContainerUri,
      requestCredentials,
      vocab
    );

  /**
   * @param connectionUri
   * @param fetchParams: optional paramters
   *        * requesterWebId: the WebID used to access the ressource (used
   *            by the owner-server to pick the right key-pair)
   *        * queryParams: GET-params as documented for ld-worker.js `queryString`
   *        * pagingSize: if specified the server will return the first
   *            page (unless e.g. `queryParams.p=2` is specified when
   *            it will return the second page of size N)
   * @return {*} the connections predicates
   */
  won.fetchConnection = (connectionUri, fetchParams) => {
    if (!is("String", connectionUri)) {
      throw new Error(
        "Tried to request connection infos for sthg that isn't an uri: " +
          connectionUri
      );
    }

    return linkedDataWorker().fetchConnection(
      connectionUri,
      fetchParams,
      vocab
    );
  };

  /**
   * @param connectionUri
   * @param messageContainerUri, if this parameter is present we do not fetch the connection at all, we fetch the containerUri directly
   * @param fetchParams: optional paramters
   *        * requesterWebId: the WebID used to access the ressource (used
   *            by the owner-server to pick the right key-pair)
   *        * queryParams: GET-params as documented for ld-worker.js `queryString`
   *        * pagingSize: if specified the server will return the first
   *            page (unless e.g. `queryParams.p=2` is specified when
   *            it will return the second page of size N)
   * @return {nextPage: nextPageLink Object, messages: arrayOfMessages}
   */
  won.fetchMessagesOfConnection = (
    connectionUri,
    messageContainerUri,
    fetchParams
  ) => {
    if (!is("String", connectionUri)) {
      throw new Error(
        "Tried to request connection infos for sthg that isn't an uri: " +
          connectionUri
      );
    }

    //TODO: MOVE THE SUBSEQUENT PROMISES TO ld-worker once we figured out how to parse a message within a worker with wonMessageFromJsonLd
    return linkedDataWorker()
      .fetchMessagesOfConnection(
        connectionUri,
        messageContainerUri,
        fetchParams,
        vocab
      )
      .then(responseObject =>
        jsonld.expand(responseObject.jsonLdData).then(jsonLdData => {
          const messages = {};

          jsonLdData &&
            jsonLdData
              .filter(graph => graph["@id"].indexOf("wm:/") === 0)
              .forEach(graph => {
                const msgUri = graph["@id"].split("#")[0];
                const singleMessage = messages[msgUri];

                if (singleMessage) {
                  singleMessage["@graph"].push(graph);
                } else {
                  messages[msgUri] = { "@graph": [graph] };
                }
              });

          const promiseArray = [];
          for (const msgUri in messages) {
            const msg = messages[msgUri];
            promiseArray.push(
              won
                .wonMessageFromJsonLd(msg, msgUri)
                .then(wonMessage => ({
                  msgUri: msgUri,
                  wonMessage: wonMessage,
                }))
                .catch(error => {
                  console.error(
                    "Could not parse msg to wonMessage: ",
                    msg,
                    "error: ",
                    error
                  );
                  return { msgUri: msgUri, wonMessage: undefined };
                })
            );
          }
          return Promise.all([
            Promise.resolve(responseObject.nextPage),
            Promise.all(promiseArray),
          ]);
        })
      )
      .then(([nextPage, messages]) => ({
        nextPage: nextPage,
        messages: messages,
      }));
  };

  /**
   * @param senderSocketUri
   * @param targetSocketUri
   * @param fetchParams: optional paramters
   *        * requesterWebId: the WebID used to access the ressource (used
   *            by the owner-server to pick the right key-pair)
   *        * queryParams: GET-params as documented for ld-worker.js `queryString`
   *        * pagingSize: if specified the server will return the first
   *            page (unless e.g. `queryParams.p=2` is specified when
   *            it will return the second page of size N)
   * @return {*} the connections predicates along with the uris of associated events
   */
  won.fetchConnectionUrisBySocket = (
    senderSocketUri,
    targetSocketUri,
    fetchParams
  ) => {
    if (!is("String", senderSocketUri) || !is("String", targetSocketUri)) {
      throw new Error(
        "Tried to request connection infos for sthg that isn't an uri: " +
          senderSocketUri +
          " or " +
          targetSocketUri
      );
    }

    return linkedDataWorker().fetchConnectionUrisBySocket(
      senderSocketUri,
      targetSocketUri,
      fetchParams,
      vocab
    );
  };

  /**
   * @param senderSocketUri
   * @param targetSocketUri
   * @param fetchParams: optional paramters
   *        * requesterWebId: the WebID used to access the ressource (used
   *            by the owner-server to pick the right key-pair)
   *        * queryParams: GET-params as documented for ld-worker.js `queryString`
   *        * pagingSize: if specified the server will return the first
   *            page (unless e.g. `queryParams.p=2` is specified when
   *            it will return the second page of size N)
   * @return {*} the connections predicates
   */
  won.fetchConnectionBySocket = (
    senderSocketUri,
    targetSocketUri,
    fetchParams
  ) => {
    if (!is("String", senderSocketUri) || !is("String", targetSocketUri)) {
      throw new Error(
        "Tried to request connection infos for sthg that isn't an uri: " +
          senderSocketUri +
          " or " +
          targetSocketUri
      );
    }

    return linkedDataWorker().fetchConnectionBySocket(
      senderSocketUri,
      targetSocketUri,
      fetchParams,
      vocab
    );
  };
})();

window.jsonld4dbg = jsonld;
