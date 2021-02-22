import urljoin from "url-join";
import _ from "lodash";
import { ownerBaseUrl } from "~/config/default.js";
import jsonld from "jsonld";

export const fetchAtom = (atomUri, requestCredentials, vocab) =>
  fetchJsonLdDataset(atomUri, requestCredentials)
    .then(jsonLdData =>
      Promise.all([
        jsonld.frame(jsonLdData, {
          "@id": atomUri, // start the framing from this uri. Otherwise will generate all possible nesting-variants.
          "@context": vocab.defaultContext,
          "@embed": "@always",
        }),
        jsonld.frame(jsonLdData, {
          "@type": vocab.AUTH.AuthorizationCompacted,
          "@context": vocab.defaultContext,
          "@embed": "@always",
        }),
      ])
    )
    .then(([atomJsonLd, authsJsonLd]) => {
      // usually the atom-data will be in a single object in the '@graph' array.
      // We can flatten this and still have valid json-ld
      const flattenedAtomJsonLd =
        atomJsonLd && atomJsonLd["@graph"]
          ? atomJsonLd["@graph"][0]
          : atomJsonLd;
      flattenedAtomJsonLd["@context"] = atomJsonLd["@context"]; // keep context
      if (
        !flattenedAtomJsonLd ||
        (flattenedAtomJsonLd["@graph"] &&
          flattenedAtomJsonLd["@graph"].length === 0)
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

export const fetchConnectionUrisWithStateByAtomUri = (
  connectionContainerUri,
  requestCredentials,
  vocab
) =>
  fetchJsonLdDataset(connectionContainerUri, requestCredentials)
    .then(jsonLdData =>
      jsonld.frame(jsonLdData, {
        "@id": connectionContainerUri,
        "@context": vocab.defaultContext,
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
            parseJsonLdConnection(connectionContainerFramed[vocab.RDFS.member]),
          ];
        } else {
          return [];
        }
      } else {
        return [];
      }
    });

export const fetchMessagesOfConnection = (
  connectionUri,
  messageContainerUri,
  fetchParams,
  vocab
) => {
  const connectionContainerPromise = messageContainerUri
    ? Promise.resolve(messageContainerUri)
    : fetchConnection(connectionUri, fetchParams, vocab).then(
        connection => connection.messageContainer
      );

  return connectionContainerPromise.then(messageContainerUri =>
    fetchJsonLdDataset(messageContainerUri, fetchParams, true)
  );
};

export const fetchConnectionUrisBySocket = (
  senderSocketUri,
  targetSocketUri,
  fetchParams,
  vocab
) => {
  fetchParams.socket = senderSocketUri;
  fetchParams.targetSocket = targetSocketUri;

  return fetchJsonLdDataset(
    extractAtomUriBySocketUri(senderSocketUri) + "/c",
    fetchParams
  )
    .then(jsonLdData =>
      jsonld.frame(jsonLdData, {
        "@type": vocab.WON.Connection,
        "@context": vocab.defaultContext,
        "@embed": "@always",
      })
    )
    .then(jsonResp => jsonResp && jsonResp["@id"]);
};

export const fetchConnectionBySocket = (
  senderSocketUri,
  targetSocketUri,
  fetchParams,
  vocab
) => {
  fetchParams.socket = senderSocketUri;
  fetchParams.targetSocket = targetSocketUri;

  return (
    fetchJsonLdDataset(
      extractAtomUriBySocketUri(senderSocketUri) + "/c",
      fetchParams
    )
      .then(jsonLdData =>
        jsonld.frame(jsonLdData, {
          "@type": vocab.WON.Connection,
          "@context": vocab.defaultContext,
          "@embed": "@always",
        })
      )
      //add the eventUris
      .then(jsonResp => jsonResp && jsonResp["@id"])
      .then(connUri =>
        fetchConnection(
          connUri,
          {
            requesterWebId: fetchParams.requesterWebId,
          },
          vocab
        )
      )
  );
};

export const fetchConnection = (connectionUri, fetchParams, vocab) =>
  fetchJsonLdDataset(connectionUri, fetchParams)
    .then(jsonLdData =>
      jsonld.frame(jsonLdData, {
        "@id": connectionUri,
        "@context": vocab.defaultContext,
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
        targetSocket: connectionContentGraph[vocab.WON.targetSocket][0]["@id"],
        previousConnectionState:
          connectionContentGraph[vocab.WON.previousConnectionState] &&
          connectionContentGraph[vocab.WON.previousConnectionState][0]["@id"],
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
    });

export const fetchJsonLdDataset = (
  uri,
  params = {},
  //ownerBaseUrl,
  includeLinkHeader = false
) => {
  // bestfetch(requestUri, {
  return fetch(generateLinkedDataQueryString(uri, params), {
    method: "get",
    credentials: "same-origin",
    headers: {
      // cachePolicy: "network-only",
      Accept: "application/ld+json",
      Authorization: params.token ? "Bearer " + params.token : undefined,
      Prefer: params.pagingSize
        ? `return=representation; max-member-count="${params.pagingSize}"`
        : undefined,
    },
  })
    .then(checkHttpStatus(uri, params))
    .then(response => {
      if (includeLinkHeader) {
        const linkHeaderString =
          response.headers && response.headers.get("Link");
        const linkHeaders = parseHeaderLinks(linkHeaderString);

        const nextPageLinkObject =
          linkHeaders && linkHeaders.next && getLinkAndParams(linkHeaders.next);
        return Promise.all([
          response.json(),
          Promise.resolve(nextPageLinkObject),
        ]).then(([jsonLdData, nextPage]) => ({
          jsonLdData: jsonLdData,
          nextPage: nextPage,
        }));
      } else {
        return response.json();
      }
    });
};

function extractAtomUriBySocketUri(socketUri) {
  return socketUri && socketUri.substring(0, socketUri.lastIndexOf("#"));
}

/**
 * Throws an error if this isn't a good http-response
 * @param response
 * @returns {*}
 */
const checkHttpStatus = (uri, params = {}) => response => {
  if (
    (response.status >= 200 && response.status < 300) ||
    response.status === 304
  ) {
    return response;
  } else {
    let error = new Error(
      `${response.status} - ${
        response.statusText
      } for request ${uri}, ${JSON.stringify(params)}`
    );

    error.response = response;
    error.status = response.status;
    throw error;
  }
};

/**
 * This function is used to generate the query-strings.
 * Should anything about the way the API is accessed changed,
 * adapt this function.
 * @param dataUri
 * @param queryParams a config object whose fields get appended as get parameters.
 *     important parameters include:
 *         * requesterWebId: the WebID used to access the ressource (used
 *                 by the owner-server to pick the right key-pair)
 *         * deep: 'true' to automatically resolve containers (e.g.
 *                 the message-container)
 *         * paging parameters as found
 *           [here](https://github.com/researchstudio-sat/webofneeds/blob/master/webofneeds/won-node-webapp/doc/linked-data-paging.md)
 *         * "p",
 *         * "resumebefore",
 *         * "resumeafter",
 *         * "type",
 *         * "state",
 *         * "socket",
 *         * "targetSocket",
 *         * "timeof",
 *         * "deep",
 *         * "state",
 *         * "scope"
 * @param includeLinkHeader if set to true, the response will be a json object of the json response and a link to the next page (if not present, the link will be undefined)
 * @returns {string}
 */
/**
 * paging parameters as found
 * [here](https://github.com/researchstudio-sat/webofneeds/blob/master/webofneeds/won-node-webapp/doc/linked-data-paging.md)
 * @type {string[]}
 */
function generateLinkedDataQueryString(dataUri, queryParams) {
  //let queryOnOwner = ownerBaseUrl + "/rest/linked-data/?";
  let queryOnOwner = urljoin(ownerBaseUrl, "/rest/linked-data/?");

  if (queryParams.requesterWebId) {
    queryOnOwner +=
      "requester=" + encodeURIComponent(queryParams.requesterWebId) + "&";
  }

  const paramsString = generateQueryParamsString({
    ...queryParams,
    requesterWebId: undefined,
    token: undefined,
  });

  return (
    queryOnOwner +
    "uri=" +
    encodeURIComponent(dataUri + (paramsString ? paramsString : ""))
  ).replace(new RegExp("%3A", "g"), ":"); // server can't resolve uri-encoded colons. revert the encoding done in `queryString`.
}

/**
 * parses a json object out of a url, that puts the url/query params within a json object
 * @param url
 * @returns {{params: {}, url: (*|string)}|{params: any, url: (*|string)}|undefined}
 */
function getLinkAndParams(url) {
  const array = url && url.split("?");

  if (array) {
    if (array.length === 1) {
      return {
        url: array[0],
        params: {},
      };
    } else if (array.length === 2) {
      return {
        url: array[0],
        params: getParamsObject(array[1]),
      };
    }
  }
  return undefined;
}

/**
 * generates a json object out of the paramsString of a url (everything after the ?)
 * @param paramsString
 * @returns {any}
 */
function getParamsObject(paramsString) {
  let pairs = paramsString.split("&");
  let result = {};
  pairs.forEach(function(pair) {
    pair = pair.split("=");
    result[pair[0]] = pair[1] ? decodeURIComponent(pair[1]) : undefined;
  });

  return JSON.parse(JSON.stringify(result));
}

function generateQueryParamsString(params) {
  if (params) {
    const keyValueArray = [];

    for (const key in params) {
      const value = params[key];

      if (value) {
        keyValueArray.push(key + "=" + encodeURIComponent(value));
      }
    }

    if (keyValueArray.length > 0) {
      return "?" + keyValueArray.join("&");
    }
  }
  return undefined;
}

function parseHeaderLinks(linkHeaderString) {
  return (
    linkHeaderString &&
    _.chain(linkHeaderString)
      .split(",")
      .map(link => {
        return {
          ref: link
            .split(";")[1]
            .replace(/rel="(.*)"/, "$1")
            .trim(),
          url: link
            .split(";")[0]
            .replace(/<(.*)>/, "$1")
            .trim(),
        };
      })
      .keyBy("ref")
      .mapValues("url")
      .value()
  );
}

function is(type, obj) {
  const clas = Object.prototype.toString.call(obj).slice(8, -1);
  return obj !== undefined && obj !== null && clas === type;
}
