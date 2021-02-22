import urljoin from "url-join";
import { ownerBaseUrl } from "~/config/default.js";
import * as wonUtils from "../won-utils.js";
import { generateQueryParamsString } from "../utils.js";
import vocab from "../service/vocab.js";
import * as N3 from "n3";
// import { bestfetch } from "bestfetch";

/**
 * Created by quasarchimaere on 11.06.2019.
 */

export function fetchDefaultNodeUri() {
  /* this allows the owner-app-server to dynamically switch default nodes. */
  return fetch(/*relativePathToConfig=*/ "appConfig/getDefaultWonNodeUri")
    .then(checkHttpStatus("appConfig/getDefaultWonNodeUri"))
    .then(resp => resp.json())
    .catch((/*err*/) => {
      const defaultNodeUri = `${location.protocol}://${
        location.host
      }/won/resource`;
      console.warn(
        "Failed to fetch default node uri at the relative path `",
        "appConfig/getDefaultWonNodeUri",
        "` (is the API endpoint there up and reachable?) -> falling back to the default ",
        defaultNodeUri
      );
      return defaultNodeUri;
    });
}

/**
 * @param credentials either {email, password} or {privateId}
 * @returns {*}
 */
export function login(credentials) {
  const { email, password, rememberMe, privateId } = wonUtils.parseCredentials(
    credentials
  );
  const loginUrl = urljoin(ownerBaseUrl, "/rest/users/signin");
  const params =
    "username=" +
    encodeURIComponent(email) +
    "&password=" +
    encodeURIComponent(password) +
    (rememberMe ? "&remember-me=true" : "") +
    (privateId ? "&privateId=" + privateId : "");

  return fetch(loginUrl, {
    method: "post",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8",
    },
    body: params,
    credentials: "include",
  })
    .then(checkHttpStatus(loginUrl))
    .then(resp => resp.json());
}

export function logout() {
  const url = urljoin(ownerBaseUrl, "/rest/users/signout");
  return fetch(url, {
    method: "post",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
    },
    credentials: "include",
    body: JSON.stringify({}),
  }).then(checkHttpStatus(url));
}

export function exportAccount(dataEncryptionPassword) {
  const url = urljoin(
    ownerBaseUrl,
    `/rest/users/exportAccount?keyStorePassword=${dataEncryptionPassword}`
  );
  return fetch(url, {
    method: "post",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
    },
    credentials: "include",
  }).then(checkHttpStatus(url));
}
/**
 * Checks whether the user has a logged-in session.
 * Returns a promise with the user-object if successful
 * or a failing promise if an error has occured.
 *
 * @returns {*}
 */
export function checkLoginStatus() {
  return fetch("rest/users/isSignedIn", { credentials: "include" })
    .then(checkHttpStatus("rest/users/isSignedIn")) // will reject if not logged in
    .then(resp => resp.json());
}

/**
 * Registers the account with the server.
 * The returned promise fails if something went
 * wrong during creation.
 *
 * @param credentials either {email, password} or {privateId}
 * @returns {*}
 */
export function registerAccount(credentials) {
  const { email, password } = wonUtils.parseCredentials(credentials);
  const url = urljoin(ownerBaseUrl, "/rest/users/");

  return fetch(url, {
    method: "post",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
    },
    credentials: "include",
    body: JSON.stringify({
      username: email,
      password: password,
      privateId: credentials.privateId,
    }),
  }).then(checkHttpStatus(url));
}

/**
 * Accept the Terms Of Service
 */
export const acceptTermsOfService = () =>
  fetch(urljoin(ownerBaseUrl, "/rest/users/acceptTermsOfService"), {
    method: "post",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
    },
    credentials: "include",
  })
    .then(resp => {
      return resp.json();
    })
    .catch(error => {
      return error.json();
    });

/**
 * Confirm the Registration with the verificationToken-link provided in the registration-email
 */
export function confirmRegistration(verificationToken) {
  const url = urljoin(ownerBaseUrl, "/rest/users/confirmRegistration");
  return fetch(url, {
    method: "post",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      token: verificationToken,
    }),
  })
    .then(checkHttpStatus(url))
    .then(resp => {
      return resp.json();
    })
    .catch(error =>
      error.response.json().then(errorMessage => {
        //FIXME: MOVE THIS ERROR HANDLINNG INTO THE ACTION
        const verificationError = new Error();
        verificationError.jsonResponse = errorMessage;
        throw verificationError;
      })
    );
}

/**
 * Resend the verification mail.
 *
 */
export function resendEmailVerification(email) {
  const url = urljoin(ownerBaseUrl, "/rest/users/resendVerificationEmail");
  return fetch(url, {
    method: "post",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      username: email,
    }),
  })
    .then(checkHttpStatus(url))
    .then(resp => {
      return resp.json();
    })
    .catch(error =>
      error.response.json().then(errorMessage => {
        //FIXME: MOVE THIS ERROR HANDLINNG INTO THE ACTION
        const resendError = new Error();
        resendError.jsonResponse = errorMessage;
        throw resendError;
      })
    );
}

/**
 * Resend the verification mail.
 *
 */
export function sendAnonymousLinkEmail(email, privateId) {
  const url = urljoin(ownerBaseUrl, "/rest/users/sendAnonymousLinkEmail");
  return fetch(url, {
    method: "post",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
    },
    credentials: "include",
    body: JSON.stringify({
      email: email,
      privateId: privateId,
    }),
  })
    .then(checkHttpStatus(url))
    .then(resp => {
      return resp.json();
    })
    .catch(error =>
      error.response.json().then(errorMessage => {
        //FIXME: MOVE THIS ERROR HANDLINNG INTO THE ACTION
        const sendError = new Error();
        sendError.jsonResponse = errorMessage;
        throw sendError;
      })
    );
}

/**
 * Change the password of the user currently logged in.
 * @param credentials { email, oldPassword, newPassword }
 * @returns {*}
 */
export function changePassword(credentials) {
  const { email, oldPassword, newPassword } = credentials;

  return fetch("/owner/rest/users/changePassword", {
    method: "post",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
    },
    credentials: "include",
    body: JSON.stringify({
      username: email,
      oldPassword: oldPassword,
      newPassword: newPassword,
    }),
  }).then(checkHttpStatus("/owner/rest/users/changePassword"));
}

/**
 * Transfer an existing privateId User,
 * to a non existing User
 * @param credentials {email, password, privateId}
 * @returns {*}
 */
export function transferPrivateAccount(credentials) {
  const { email, password, privateId } = credentials;
  const privateUsername = wonUtils.privateId2Credentials(privateId).email;
  const privatePassword = wonUtils.privateId2Credentials(privateId).password;

  return fetch("/owner/rest/users/transfer", {
    method: "post",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
    },
    credentials: "include",
    body: JSON.stringify({
      username: email,
      password: password,
      privateUsername: privateUsername,
      privatePassword: privatePassword,
    }),
  }).then(checkHttpStatus("/owner/rest/users/transfer"));
}

/**
 * Change the password of the user currently logged in.
 * @param credentials { email, oldPassword, newPassword }
 * @returns {*}
 */
export function resetPassword(credentials) {
  const { email, recoveryKey, newPassword } = credentials;

  return fetch("/owner/rest/users/resetPassword", {
    method: "post",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
    },
    credentials: "include",
    body: JSON.stringify({
      username: email,
      recoveryKey: recoveryKey,
      newPassword: newPassword,
      verificationToken: "",
    }),
  }).then(checkHttpStatus("/owner/rest/users/resetPassword"));
}

export function serverSideConnect(
  socketUri1,
  socketUri2,
  pending1 = false,
  pending2 = false
) {
  return fetch("rest/action/connect", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify([
      {
        pending: pending1,
        socket: socketUri1,
      },
      {
        pending: pending2,
        socket: socketUri2,
      },
    ]),
    credentials: "include",
  });
}

/**
 * Returns all stored Atoms including MetaData (e.g. type, creationDate, location, state) as a Map
 * @param state either "ACTIVE" or "INACTIVE"
 * @returns {*}
 */
export function fetchOwnedMetaAtoms(state) {
  let paramString = "";
  if (state === "ACTIVE" || state === "INACTIVE") {
    paramString = "?state=" + state;
  }
  const url = urljoin(ownerBaseUrl, "/rest/atoms" + paramString);

  return fetch(url, {
    method: "get",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
    },
    credentials: "include",
  })
    .then(checkHttpStatus(url))
    .then(response => response.json());
}

export function fetchMessage(atomUri, eventUri) {
  const url = urljoin(
    ownerBaseUrl,
    "/rest/linked-data/",
    `?requester=${encodeURIComponent(atomUri)}`,
    `&uri=${encodeURIComponent(eventUri)}`
  );

  return fetch(url, {
    method: "get",
    headers: {
      Accept: "application/ld+json",
      "Content-Type": "application/ld+json",
    },
    credentials: "include",
  })
    .then(checkHttpStatus(url))
    .then(response => response.json());
}

export function fetchAllMetaAtoms(
  createdAfterDate,
  state = "ACTIVE",
  limit = 600
) {
  return fetchMetaAtoms(
    undefined,
    createdAfterDate,
    state,
    undefined,
    undefined,
    undefined,
    undefined,
    limit
  );
}

export function fetchAllActiveMetaPersonas() {
  return fetchMetaAtoms(
    undefined,
    undefined,
    "ACTIVE",
    vocab.BUDDY.BuddySocket,
    vocab.WON.Persona,
    undefined,
    undefined,
    undefined
  );
}

export const fetchTokenForAtom = (atomUri, params) =>
  fetch(generateLinkedDataQueryString(atomUri + "/token", params), {
    method: "get",
    credentials: "same-origin",
    headers: {
      // cachePolicy: "network-only",
      Authorization: params.token ? "Bearer " + params.token : undefined,
      Accept: "application/json",
    },
  })
    .then(checkHttpStatus(atomUri + "/token", params))
    .then(response => response.json());

export const fetchGrantsForAtom = (atomUri, params) =>
  fetch(generateLinkedDataQueryString(atomUri + "/grants", params), {
    method: "get",
    credentials: "same-origin",
    headers: {
      // cachePolicy: "network-only",
      Authorization: params.token ? "Bearer " + params.token : undefined,
      Accept: "application/ld+json",
    },
  })
    .then(checkHttpStatus(atomUri + "/grants", params))
    .then(response => response.json());

function fetchMetaAtoms(
  modifiedAfterDate,
  createdAfterDate,
  state,
  filterBySocketTypeUri,
  filterByAtomTypeUri,
  location,
  maxDistance,
  limit
) {
  const url = urljoin(
    ownerBaseUrl,
    "/rest/atoms/all?" +
      (state ? "state=" + state + "&" : "") +
      (limit ? "limit=" + limit + "&" : "") +
      (modifiedAfterDate
        ? "modifiedafter=" + modifiedAfterDate.toISOString() + "&"
        : "") +
      (createdAfterDate
        ? "createdAfterDate=" + createdAfterDate.toISOString() + "&"
        : "") +
      (filterBySocketTypeUri
        ? "filterBySocketTypeUri=" +
          encodeURIComponent(filterBySocketTypeUri) +
          "&"
        : "") +
      (filterByAtomTypeUri
        ? "filterByAtomTypeUri=" + encodeURIComponent(filterByAtomTypeUri) + "&"
        : "") +
      (location &&
      location.lat &&
      location.lng &&
      (maxDistance || maxDistance === 0)
        ? "latitude=" + location.lat + "&"
        : "") +
      (location &&
      location.lat &&
      location.lng &&
      (maxDistance || maxDistance === 0)
        ? "longitude=" + location.lng + "&"
        : "") +
      (location &&
      location.lat &&
      location.lng &&
      (maxDistance || maxDistance === 0)
        ? "maxDistance=" + maxDistance + "&"
        : "")
  );
  return fetch(url, {
    method: "get",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
    },
    body: JSON.stringify(),
    credentials: "include",
  })
    .then(checkHttpStatus(url))
    .then(response => response.json());
}

export function fetchAllMetaAtomsNear(
  createdAfterDate,
  location,
  maxDistance = 5000,
  limit = 500,
  state = "ACTIVE"
) {
  if (location && location.lat && location.lng) {
    return fetchMetaAtoms(
      undefined,
      createdAfterDate,
      state,
      undefined,
      undefined,
      location,
      maxDistance,
      limit
    );
  } else {
    return Promise.reject();
  }
}

export function fetchMessageEffects(connectionUri, messageUri) {
  const url = urljoin(
    ownerBaseUrl,
    "/rest/agreement/getMessageEffects",
    `?connectionUri=${connectionUri}`,
    `&messageUri=${messageUri}`
  );

  return fetch(url, {
    method: "get",
    headers: {
      Accept: "application/ld+json",
      "Content-Type": "application/ld+json",
    },
    credentials: "include",
  })
    .then(checkHttpStatus(url))
    .then(response => response.json());
}

export function fetchAgreementProtocolUris(connectionUri) {
  const url = urljoin(
    ownerBaseUrl,
    "/rest/agreement/getAgreementProtocolUris",
    `?connectionUri=${connectionUri}`
  );

  return fetch(url, {
    method: "get",
    headers: {
      Accept: "application/ld+json",
      "Content-Type": "application/ld+json",
    },
    credentials: "include",
  })
    .then(checkHttpStatus(url))
    .then(response => response.json());
}

export function fetchAgreementProtocolDataset(connectionUri) {
  const url = urljoin(
    ownerBaseUrl,
    "/rest/agreement/getAgreementProtocolDataset",
    `?connectionUri=${connectionUri}`
  );

  return fetch(url, {
    method: "get",
    headers: {
      Accept: "application/trig",
      "Content-Type": "application/trig",
    },
    credentials: "include",
  })
    .then(checkHttpStatus(url))
    .then(response => response.text())
    .then(textResponse => {
      const trigParser = new N3.Parser({ format: "application/trig" });

      return trigParser.parse(textResponse);
    });
}

export function fetchPetriNetUris(connectionUri) {
  const url = urljoin(
    ownerBaseUrl,
    "/rest/petrinet/getPetriNetUris",
    `?connectionUri=${connectionUri}`
  );

  return fetch(url, {
    method: "get",
    headers: {
      Accept: "application/ld+json",
      "Content-Type": "application/ld+json",
    },
    credentials: "include",
  })
    .then(checkHttpStatus(url))
    .then(response => response.json());
}

/**
 * Send a message to the endpoint and return a promise with a json payload with messageUri and message as props
 * @param msg
 * @returns {*}
 */
export function sendMessage(msg) {
  return fetch("/owner/rest/messages/send", {
    body: JSON.stringify(msg),
    method: "POST",
    headers: new Headers({
      "Content-Type": "application/ld+json;charset=UTF-8",
    }),
    credentials: "include",
  })
    .then(checkHttpStatus("/owner/rest/messages/send"))
    .then(response => response.json())
    .then(jsonResponse => ({
      messageUri: jsonResponse.messageUri,
      message: msg,
    }));
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
  let queryOnOwner = urljoin(ownerBaseUrl, "/rest/linked-data/") + "?";

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
