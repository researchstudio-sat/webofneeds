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
import { is, getIn, extractAtomUriBySocketUri } from "../utils.js";

import * as ownerApi from "../api/owner-api.js";
import jsonld from "jsonld/dist/jsonld.min.js";
import won from "./won.js";
import vocab from "./vocab.js";

(function() {
  /**
   * Loads the atom-data without following up
   * with a request for the connection-container
   * to get the connection-uris. Thus it's faster.
   */
  won.fetchAtom = (atomUri, requestCredentials) =>
    ownerApi
      .fetchJsonLdDataset(atomUri, requestCredentials)
      .then(jsonLdData =>
        Promise.all([
          jsonld.frame(jsonLdData, {
            "@id": atomUri, // start the framing from this uri. Otherwise will generate all possible nesting-variants.
            "@context": won.defaultContext,
            "@embed": "@always",
          }),
          jsonld.frame(jsonLdData, {
            "@type": vocab.AUTH.AuthorizationCompacted,
            "@context": won.defaultContext,
            "@embed": "@always",
          }),
        ])
      )
      .then(([atomJsonLd, authsJsonLd]) => {
        // usually the atom-data will be in a single object in the '@graph' array.
        // We can flatten this and still have valid json-ld
        const flattenedAtomJsonLd = getIn(atomJsonLd, ["@graph", 0])
          ? getIn(atomJsonLd, ["@graph", 0])
          : atomJsonLd;
        flattenedAtomJsonLd["@context"] = atomJsonLd["@context"]; // keep context
        if (
          !flattenedAtomJsonLd ||
          getIn(flattenedAtomJsonLd, ["@graph", "length"]) === 0
        ) {
          console.error(
            "Received empty graph ",
            atomJsonLd,
            " for atom ",
            atomUri
          );
          return {
            atom: { "@context": flattenedAtomJsonLd["@context"] },
            auth: authsJsonLd,
          };
        }
        return { atom: flattenedAtomJsonLd, auth: authsJsonLd };
      })
      .catch(e => {
        const msg = "Failed to get atom " + atomUri + ".";
        e.message += msg;
        console.error(e.message);
        throw e;
      });

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
    ownerApi
      .fetchJsonLdDataset(connectionContainerUri, requestCredentials)
      .then(jsonLdData =>
        jsonld.frame(jsonLdData, {
          "@id": connectionContainerUri,
          "@context": won.defaultContext,
          "@embed": "@always",
        })
      )
      .then(
        connectionContainer =>
          connectionContainer && connectionContainer["@id"]
            ? jsonld.frame(connectionContainer, {
                "@id": connectionContainer["@id"],
                "@embed": "@always",
              })
            : undefined
      )
      .then(connectionContainerFramed => {
        if (connectionContainerFramed) {
          const parseJsonLdConnection = jsonLdConnection => ({
            uri: jsonLdConnection["@id"],
            type: jsonLdConnection["@type"],
            modified:
              jsonLdConnection["http://purl.org/dc/terms/modified"]["@value"],
            socket: jsonLdConnection[vocab.WON.socket]["@id"],
            connectionState: jsonLdConnection[vocab.WON.connectionState]["@id"],
            sourceAtom: jsonLdConnection[vocab.WON.sourceAtom]["@id"],
            targetAtom: jsonLdConnection[vocab.WON.targetAtom]["@id"],
            targetSocket: jsonLdConnection[vocab.WON.targetSocket]["@id"],
            hasEvents: [],
          });
          if (is("Array", connectionContainerFramed[vocab.RDFS.member])) {
            return connectionContainerFramed[vocab.RDFS.member].map(
              parseJsonLdConnection
            );
          } else if (connectionContainerFramed[vocab.RDFS.member]) {
            return [
              parseJsonLdConnection(
                connectionContainerFramed[vocab.RDFS.member]
              ),
            ];
          } else {
            return [];
          }
        } else {
          return [];
        }
      });

  /**
   * @param connectionUri
   * @param fetchParams: optional paramters
   *        * requesterWebId: the WebID used to access the ressource (used
   *            by the owner-server to pick the right key-pair)
   *        * queryParams: GET-params as documented for ownerApi.js `queryString`
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

    return (
      //add the eventUris
      ownerApi
        .fetchJsonLdDataset(connectionUri, fetchParams)
        .then(jsonLdData =>
          jsonld.frame(jsonLdData, {
            "@id": connectionUri,
            "@context": won.defaultContext,
            "@embed": "@always",
          })
        )
        .then(jsonLdConnection => jsonld.expand(jsonLdConnection))
        .then(jsonLdConnection => {
          const connectionContentGraph = jsonLdConnection[0];
          return {
            uri: connectionContentGraph["@id"],
            type: connectionContentGraph["@type"][0],
            modified:
              connectionContentGraph["http://purl.org/dc/terms/modified"][0][
                "@value"
              ],
            messageContainer:
              connectionContentGraph[vocab.WON.messageContainer][0]["@id"],
            socket: connectionContentGraph[vocab.WON.socket][0]["@id"],
            wonNode: connectionContentGraph[vocab.WON.wonNode][0]["@id"],
            connectionState:
              connectionContentGraph[vocab.WON.connectionState][0]["@id"],
            sourceAtom: connectionContentGraph[vocab.WON.sourceAtom][0]["@id"],
            targetAtom: connectionContentGraph[vocab.WON.targetAtom][0]["@id"],
            targetSocket:
              connectionContentGraph[vocab.WON.targetSocket][0]["@id"],
            previousConnectionState:
              connectionContentGraph[vocab.WON.previousConnectionState] &&
              connectionContentGraph[vocab.WON.previousConnectionState][0][
                "@id"
              ],
            targetConnection:
              connectionContentGraph[vocab.WON.targetConnection] &&
              connectionContentGraph[vocab.WON.targetConnection][0]["@id"],
            hasEvents: [],
          };
        })
        .catch(e => {
          const msg = "Failed to get connection " + connectionUri + ".";
          e.message += msg;
          console.error(e.message);
          throw e;
        })
    );
  };

  /**
   * @param connectionUri
   * @param messageContainerUri, if this parameter is present we do not fetch the connection at all, we fetch the containerUri directly
   * @param fetchParams: optional paramters
   *        * requesterWebId: the WebID used to access the ressource (used
   *            by the owner-server to pick the right key-pair)
   *        * queryParams: GET-params as documented for ownerApi.js `queryString`
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

    const connectionContainerPromise = messageContainerUri
      ? Promise.resolve(messageContainerUri)
      : won
          .fetchConnection(connectionUri)
          .then(connection => connection.messageContainer);

    return connectionContainerPromise
      .then(messageContainerUri =>
        ownerApi.fetchJsonLdDataset(messageContainerUri, fetchParams, true)
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
   *        * queryParams: GET-params as documented for ownerApi.js `queryString`
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

    fetchParams.socket = senderSocketUri;
    fetchParams.targetSocket = targetSocketUri;

    return ownerApi
      .fetchJsonLdDataset(
        extractAtomUriBySocketUri(senderSocketUri) + "/c",
        fetchParams
      )
      .then(jsonLdData =>
        jsonld.frame(jsonLdData, {
          "@type": vocab.WON.Connection,
          "@context": won.defaultContext,
          "@embed": "@always",
        })
      )
      .then(jsonResp => jsonResp && jsonResp["@id"]);
  };

  /**
   * @param senderSocketUri
   * @param targetSocketUri
   * @param fetchParams: optional paramters
   *        * requesterWebId: the WebID used to access the ressource (used
   *            by the owner-server to pick the right key-pair)
   *        * queryParams: GET-params as documented for ownerApi.js `queryString`
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

    fetchParams.socket = senderSocketUri;
    fetchParams.targetSocket = targetSocketUri;

    return (
      ownerApi
        .fetchJsonLdDataset(
          extractAtomUriBySocketUri(senderSocketUri) + "/c",
          fetchParams
        )
        .then(jsonLdData =>
          jsonld.frame(jsonLdData, {
            "@type": vocab.WON.Connection,
            "@context": won.defaultContext,
            "@embed": "@always",
          })
        )
        //add the eventUris
        .then(jsonResp => jsonResp && jsonResp["@id"])
        .then(connUri =>
          won.fetchConnection(connUri, {
            requesterWebId: fetchParams.requesterWebId,
          })
        )
    );
  };
})();

window.jsonld4dbg = jsonld;
