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
import { is, getIn } from "../utils.js";

import * as ownerApi from "../api/owner-api.js";
import jsonld from "jsonld/dist/jsonld.js";
import won from "./won.js";
import vocab from "./vocab.js";

(function() {
  /**
   * Loads the atom-data without following up
   * with a request for the connection-container
   * to get the connection-uris. Thus it's faster.
   */
  won.getAtom = atomUri =>
    ownerApi
      .getJsonLdDataset(atomUri)
      .then(jsonLdData =>
        /* JsonLd seems to have a framing issue when it comes to @type -> to ensure all the attributes are used in the data
         we need to replace @type with the rdf:type, and also replace @value with rdf:value otherwise the framing would not work
         */
        JSON.parse(
          JSON.stringify(jsonLdData)
            .replace(/@type/g, "rdf:xtype")
            .replace(/@value/g, "rdf:xvalue")
        )
      )
      .then(jsonLdData =>
        jsonld.promises.frame(jsonLdData, {
          "@id": atomUri, // start the framing from this uri. Otherwise will generate all possible nesting-variants.
          "@context": won.defaultContext,
        })
      )
      .then(jsonLdData =>
        /* After framing we replace the rdf:type and rdf:value with @type and @value again
         */
        JSON.parse(
          JSON.stringify(jsonLdData)
            .replace(/rdf:xtype/g, "@type")
            .replace(/rdf:xvalue/g, "@value")
        )
      )
      .then(jsonLdData =>
        //Compacting is necessary because our replacement did not compact all uris, before because it assumed literals
        jsonld.promises.compact(jsonLdData, won.defaultContext)
      )
      .then(atomJsonLd => {
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
          return { "@context": flattenedAtomJsonLd["@context"] };
        }

        return flattenedAtomJsonLd;
      });

  won.validateEnvelopeDataForAtom = function(atomUri) {
    if (typeof atomUri === "undefined" || atomUri == null) {
      throw {
        message: "validateEnvelopeDataForAtom: atomUri must not be null",
      };
    }

    return Promise.resolve();
  };

  won.validateEnvelopeDataForConnection = async function(
    socketUri,
    targetSocketUri
  ) {
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

  won.getConnectionUrisWithStateByAtomUri = (atomUri, requesterWebId) => {
    return won
      .getJsonLdNode(atomUri, requesterWebId)
      .then(jsonLdAtom => jsonld.promises.expand(jsonLdAtom))
      .then(jsonLdAtom => {
        const jsonLdContentGraph = jsonLdAtom[0];

        return jsonLdContentGraph[vocab.WON.connections][0]["@id"];
      })
      .then(connectionContainerUri =>
        won.getJsonLdNode(connectionContainerUri, requesterWebId)
      )
      .then(connectionContainer => {
        const connectionsGraph =
          connectionContainer &&
          connectionContainer["@graph"] &&
          connectionContainer["@graph"][0];
        const connections = connectionsGraph && connectionsGraph["rdfs:member"];

        if (!connections) {
          return [];
        } else if (is("Array", connections)) {
          return Promise.all(
            connections.map(connection => getConnection(connection["@id"]))
          );
        } else {
          return Promise.all([getConnection(connections["@id"])]);
        }
      });
  };

  function getConnection(connectionUri, fetchParams) {
    return (
      won
        //add the eventUris
        .getJsonLdNode(connectionUri, fetchParams)
        .then(jsonLdConnection => jsonld.promises.expand(jsonLdConnection))
        .then(jsonLdConnection => {
          const connectionContentGraph = jsonLdConnection[0];
          const connection = {
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

          return connection;
        })
    );
  }

  /**
   * @param connectionUri
   * @param fetchParams: optional paramters
   *        * requesterWebId: the WebID used to access the ressource (used
   *            by the owner-server to pick the right key-pair)
   *        * queryParams: GET-params as documented for ownerApi.js `queryString`
   *        * pagingSize: if specified the server will return the first
   *            page (unless e.g. `queryParams.p=2` is specified when
   *            it will return the second page of size N)
   * @return {*} the connections predicates along with the uris of associated events
   */
  won.getConnectionWithEventUris = function(connectionUri, fetchParams) {
    if (!is("String", connectionUri)) {
      throw new Error(
        "Tried to request connection infos for sthg that isn't an uri: " +
          connectionUri
      );
    }
    return getConnection(connectionUri, fetchParams)
      .then(connection =>
        Promise.all([
          Promise.resolve(connection),
          won.getJsonLdNode(connection.messageContainer, fetchParams),
        ])
      )
      .then(([connection, messageContainer]) => {
        const messageContainerGraph =
          messageContainer &&
          messageContainer["@graph"] &&
          messageContainer["@graph"][0];
        const messages =
          messageContainerGraph && messageContainerGraph["rdfs:member"];

        /*
           * if there's only a single rdfs:member in the event
           * container, getJsonLdNode will not return an array, so we
           * need to make sure it's one from here on out.
           */
        if (!messages) {
          connection.hasEvents = [];
        } else if (is("Array", messages)) {
          connection.hasEvents = messages.map(message => message["@id"]);
        } else {
          connection.hasEvents = [messages["@id"]];
        }
        return connection;
      });
  };

  /**
   * @param connectionUri
   * @param fetchParams: optional paramters
   *        * requesterWebId: the WebID used to access the ressource (used
   *            by the owner-server to pick the right key-pair)
   *        * queryParams: GET-params as documented for ownerApi.js `queryString`
   *        * pagingSize: if specified the server will return the first
   *            page (unless e.g. `queryParams.p=2` is specified when
   *            it will return the second page of size N)
   * @return {*} the connections predicates along with the fetched messages
   */
  won.getMessagesOfConnection = function(connectionUri, fetchParams) {
    if (!is("String", connectionUri)) {
      throw new Error(
        "Tried to request connection infos for sthg that isn't an uri: " +
          connectionUri
      );
    }
    return getConnection(connectionUri)
      .then(connection =>
        won.getJsonLdNode(connection.messageContainer, fetchParams)
      )
      .then(messageContainer => {
        console.debug(
          "Received MessageContainer For Connection(",
          connectionUri,
          ") with fetchParams: ",
          fetchParams,
          "RESULT: ",
          messageContainer
        );

        const messageContainerGraph =
          messageContainer &&
          messageContainer["@graph"] &&
          messageContainer["@graph"][0];
        const messages =
          messageContainerGraph && messageContainerGraph["rdfs:member"];

        let rawMessageArray;
        /*
           * if there's only a single rdfs:member in the event
           * container, getJsonLdNode will not return an array, so we
           * need to make sure it's one from here on out.
           */
        if (!messages) {
          rawMessageArray = [];
        } else if (is("Array", messages)) {
          rawMessageArray = messages;
        } else {
          rawMessageArray = [messages];
        }

        return Promise.all(
          rawMessageArray.map(rawMessage => {
            const msgUri = rawMessage["@id"];

            console.debug("rawMessage: ", rawMessage);
            return jsonld.promises
              .frame(messageContainer, {
                "@id": msgUri,
                "@context": won.defaultContext,
              })
              .then(jsonLdMessage => {
                console.debug(
                  "won.getMessagesOfConnection framedMessage: ",
                  jsonLdMessage
                );
                return won.wonMessageFromJsonLd(jsonLdMessage);
              })
              .then(wonMessage => {
                console.debug(
                  "won.getMessagesOfConnection wonMessageFromJsonLd: ",
                  wonMessage
                );
                return {
                  msgUri: msgUri,
                  wonMessage: wonMessage,
                };
              })
              .catch(e => {
                const msg =
                  "Failed to frame or parse to wonMessage " + msgUri + ".";
                e.message += msg;
                console.error(e.message);
                return { msgUri: msgUri, wonMessage: undefined };
              });
          })
        );
      });
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
  won.getConnectionUrisBySocket = function(
    senderSocketUri,
    targetSocketUri,
    fetchParams
  ) {
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
      won
        .getJsonLdNode(senderSocketUri.split("#")[0] + "/c", fetchParams)
        //add the eventUris
        .then(jsonResp => {
          console.debug("won.getConnectionUrisBySocket jsonResp:", jsonResp);
          return jsonResp["@graph"][0]["rdfs:member"];
        })
        .then(connUris => {
          let _connUris;
          if (is("Array", connUris)) {
            _connUris = connUris.map(connUri => connUri["@id"]);
          } else {
            _connUris = [connUris["@id"]];
          }

          return _connUris[0];
        })
    );
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
  won.getConnectionWithEventUrisBySocket = function(
    senderSocketUri,
    targetSocketUri,
    fetchParams
  ) {
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
      won
        .getJsonLdNode(senderSocketUri.split("#")[0] + "/c", fetchParams)
        //add the eventUris
        .then(jsonResp => {
          console.debug(
            "won.getConnectionWithEventUrisBySocket jsonResp:",
            jsonResp
          );
          return jsonResp["@graph"][0]["rdfs:member"];
        })
        .then(connUris => {
          let _connUris;
          if (is("Array", connUris)) {
            _connUris = connUris.map(connUri => connUri["@id"]);
          } else {
            _connUris = [connUris["@id"]];
          }

          const newFetchParams = {
            requesterWebId: fetchParams.requesterWebId,
          };

          return _connUris.map(connUri => {
            return won.getConnectionWithEventUris(connUri, newFetchParams);
          });
        })
    );
  };

  won.getWonMessage = (msgUri, fetchParams) => {
    return ownerApi
      .getJsonLdDataset(msgUri, fetchParams)
      .then(rawEvent => won.wonMessageFromJsonLd(rawEvent))
      .catch(e => {
        const msg = "Failed to get wonMessage " + msgUri + ".";
        e.message += msg;
        console.error(e.message);
        throw e;
      });
  };

  window.getWonMessage4dbg = won.getWonMessage;
  window.wonMessageFromJsonLd4dbg = won.wonMessageFromJsonLd;

  /**
   * Fetches the triples where URI is subject and add objects of those triples to the
   * resulting structure by the localname of the predicate.
   * The URI is added as property 'uri'.
   *
   * If a predicate occurs multiple times the objects (in the rdf sense) will be
   * grouped as an array. This is usually the case for rdfs:member, to give an example.
   *
   * NOTE: Atm it ignores prefixes which might lead to clashes.
   *
   * @param uri
   * @param fetchParams: optional paramters
   *        * requesterWebId: the WebID used to access the ressource (used
   *            by the owner-server to pick the right key-pair)
   *        * queryParams: GET-params as documented for ownerApi.js `queryString`
   *        * pagingSize: if specified the server will return the first
   *            page (unless e.g. `queryParams.p=2` is specified when
   *            it will return the second page of size N)
   */
  won.getJsonLdNode = function(uri, fetchParams) {
    if (!uri) {
      return Promise.reject({ message: "getJsonLdNode: uri must not be null" });
    }

    return ownerApi
      .getJsonLdDataset(uri, fetchParams)
      .then(jsonLdData =>
        jsonld.promises.frame(jsonLdData, {
          "@id": uri,
          "@context": won.defaultContext,
        })
      )
      .then(jsonLdDataFramed => {
        return jsonLdDataFramed;
      });
  };
})();

/*
{
    "@id" : "conn:aavakck2xsm39hr9kxi2",
    "@type" : "won:Connection",
    "http://purl.org/dc/terms/modified" : {
      "@type" : "xsd:dateTime",
      "@value" : "2019-06-18T06:27:20.989Z"
    },
    "won:connectionState" : {
      "@id" : "won:Connected"
    },
    "won:socket" : {
      "@id" : "atom:z6ne170yrf0z#holdableSocket"
    },
    "won:sourceAtom" : {
      "@id" : "atom:z6ne170yrf0z"
    },
    "won:targetAtom" : {
      "@id" : "atom:sxxxgf2necv6"
    },
    "won:targetConnection" : {
      "@id" : "conn:totztqjd99h1vt73mi5n"
    },
    "won:targetSocket" : {
      "@id" : "atom:sxxxgf2necv6#holderSocket"
    }
  }, {
    "@id" : "conn:aboy09l0txxkewixlqtq",
    "@type" : "won:Connection",
    "http://purl.org/dc/terms/modified" : {
      "@type" : "xsd:dateTime",
      "@value" : "2019-06-18T06:27:46.278Z"
    },
    "won:connectionState" : {
      "@id" : "won:Suggested"
    },
    "won:socket" : {
      "@id" : "atom:z6ne170yrf0z#chatSocket"
    },
    "won:sourceAtom" : {
      "@id" : "atom:z6ne170yrf0z"
    },
    "won:targetAtom" : {
      "@id" : "atom:xbszwgx0ey23a9kcm283"
    },
    "won:targetSocket" : {
      "@id" : "atom:xbszwgx0ey23a9kcm283#ChatSocket"
    }
  }
* */

window.jsonld4dbg = jsonld;
