import urljoin from "url-join";
import _ from "lodash";
import { ownerBaseUrl } from "~/config/default.js";

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
  console.debug("queryOnOwner: ", queryOnOwner);

  if (queryParams.requesterWebId) {
    queryOnOwner +=
      "requester=" + encodeURIComponent(queryParams.requesterWebId) + "&";
  }

  const paramsString = generateQueryParamsString({
    ...queryParams,
    requesterWebId: undefined,
    token: undefined,
  });

  console.debug(
    "generateLinkedDataQueryString: ",
    (
      queryOnOwner +
      "uri=" +
      encodeURIComponent(dataUri + (paramsString ? paramsString : ""))
    ).replace(new RegExp("%3A", "g"), ":")
  );

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
