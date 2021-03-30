import urljoin from "url-join";
import _ from "lodash";
import { ownerBaseUrl } from "~/config/default.js";
import jsonld from "jsonld";
import * as N3 from "n3";

export const fetchDefaultNodeUri = () => {
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
};

/**
 * Resend the verification mail.
 *
 */
export const resendEmailVerification = email => {
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
    .then(resp => resp.json());
};

/**
 * Send Anonymous Link Email
 *
 */
export const sendAnonymousLinkEmail = (email, privateId) => {
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
    .then(resp => resp.json());
};

/**
 * Change the password of the user currently logged in.
 * @param credentials { email, oldPassword, newPassword }
 * @returns {*}
 */
export const changePassword = credentials => {
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
};

/**
 * Reset password with email and recoveryKey
 * @param credentials { email, newPassword, recoveryKey }
 * @returns {*}
 */
export const resetPassword = credentials => {
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
  })
    .then(checkHttpStatus("/owner/rest/users/resetPassword"))
    .then(() => ({ ok: true }));
};

/**
 * @param credentials either {email, password} or {privateId}
 * @returns {*}
 */
export const login = credentials => {
  const { email, password, rememberMe, privateId } = parseCredentials(
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
};

export const logout = () => {
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
};

export const exportAccount = dataEncryptionPassword => {
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
};

/**
 * Checks whether the user has a logged-in session.
 * Returns a promise with the user-object if successful
 * or a failing promise if an error has occured.
 *
 * @returns {*}
 */
export const checkLoginStatus = () => {
  return fetch("rest/users/isSignedIn", { credentials: "include" })
    .then(checkHttpStatus("rest/users/isSignedIn")) // will reject if not logged in
    .then(resp => resp.json());
};

/**
 * Registers the account with the server.
 * The returned promise fails if something went
 * wrong during creation.
 *
 * @param credentials either {email, password} or {privateId}
 * @returns {*}
 */
export const registerAccount = credentials => {
  const { email, password } = parseCredentials(credentials);
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
  })
    .then(checkHttpStatus(url))
    .then(response => response.json());
};

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
    .then(resp => resp.json())
    .catch(error => error.json());

/**
 * Confirm the Registration with the verificationToken-link provided in the registration-email
 */
export const confirmRegistration = verificationToken => {
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
    .then(resp => resp.json());
};

/**
 * Transfer an existing privateId User,
 * to a non existing User
 * @param credentials {email, password, privateId}
 * @returns {*}
 */
export const transferPrivateAccount = credentials => {
  const { email, password, privateId } = credentials;
  const privateUsername = privateId2Credentials(privateId).email;
  const privatePassword = privateId2Credentials(privateId).password;

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
};

export const serverSideConnect = (
  fromSocketUri,
  toSocketUri,
  fromPending = false,
  toPending = false,
  autoOpen = false,
  message
) => {
  const params = {
    fromSocket: fromSocketUri,
    toSocket: toSocketUri,
    fromPending: fromPending,
    toPending: toPending,
    autoOpen: autoOpen,
    message: message,
  };

  return fetch("rest/action/connect", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(params),
    credentials: "include",
  })
    .then(checkHttpStatus("rest/action/connect", params))
    .then(() => ({ ok: true }));
};

/**
 * Send Subscription for pushNotifications to Server
 */
export const sendSubscriptionToServer = subscription => {
  const url = urljoin(ownerBaseUrl, "/rest/users/subscribeNotifications");
  return fetch(url, {
    method: "POST",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
    },
    credentials: "include",
    body: JSON.stringify(subscription.toJSON()),
  })
    .then(checkHttpStatus())
    .then(response => response.json());
};

/**
 * Returns all stored Atoms including MetaData (e.g. type, creationDate, location, state) as a Map
 * @param state either "ACTIVE" or "INACTIVE"
 * @returns {*}
 */
export const fetchOwnedMetaAtoms = state => {
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
};

//FIXME: This might be a problem if messages from non owned atoms are fetched (only requesterWebId is used not token)
export const fetchMessage = (atomUri, messageUri) => {
  const url = urljoin(
    ownerBaseUrl,
    "/rest/linked-data/",
    `?requester=${encodeURIComponent(atomUri)}`,
    `&uri=${encodeURIComponent(messageUri)}`
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
};

export const fetchAllMetaAtoms = (
  createdAfterDate,
  state = "ACTIVE",
  limit = 600
) => {
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
};

export const fetchAllActiveMetaPersonas = vocab => {
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
};

export const fetchAllMetaAtomsNear = (
  createdAfterDate,
  location,
  maxDistance = 5000,
  limit = 500,
  state = "ACTIVE"
) => {
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
};

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

const fetchMetaAtoms = (
  modifiedAfterDate,
  createdAfterDate,
  state,
  filterBySocketTypeUri,
  filterByAtomTypeUri,
  location,
  maxDistance,
  limit
) => {
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
};

export const fetchMessageEffects = (connectionUri, messageUri) => {
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
};

export const fetchAgreementProtocolUris = connectionUri => {
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
};

export const fetchAgreementProtocolDataset = connectionUri => {
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
};

/**
 * Send a message to the endpoint and return a promise with a json payload with messageUri and message as props
 * @param msg
 * @returns {*}
 */
export const sendMessage = msg => {
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
};

export const fetchPetriNetUris = connectionUri => {
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
};

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
          "@type": vocab.AUTH.Authorization,
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
  if (!is("String", connectionUri)) {
    throw new Error(
      "Tried to request connection infos for sthg that isn't an uri: " +
        connectionUri
    );
  }

  const connectionContainerPromise = messageContainerUri
    ? Promise.resolve(messageContainerUri)
    : fetchConnection(connectionUri, fetchParams, vocab).then(
        connection => connection.messageContainer
      );

  return connectionContainerPromise
    .then(messageContainerUri =>
      fetchJsonLdDataset(messageContainerUri, fetchParams, true)
    )
    .then(responseObject =>
      Promise.all([
        Promise.resolve(responseObject.nextPage),
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
              wonMessageFromJsonLd(msg, msgUri, vocab)
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
          return Promise.all(promiseArray);
        }),
      ])
    );
};

export const fetchConnectionUrisBySocket = (
  senderSocketUri,
  targetSocketUri,
  fetchParams,
  vocab
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
      //add the messageUris
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

export const fetchConnection = (connectionUri, fetchParams, vocab) => {
  if (!is("String", connectionUri)) {
    throw new Error(
      "Tried to request connection infos for sthg that isn't an uri: " +
        connectionUri
    );
  }
  return fetchJsonLdDataset(connectionUri, fetchParams)
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
      console.error("Failed to get connection " + connectionUri + ".");
      throw e;
    });
};

export const rdfToJsonLd = rdf => {
  /**
   * An wrapper for N3's writer that returns a promise
   * @param {*} quads list of quads following the rdfjs interface (http://rdf.js.org/)
   *   e.g.:
   * ```
   *  [ Quad {
   *      graph: DefaultGraph {id: ""}
   *      object: NamedNode {id: "http://example.org/cartoons#Cat"}
   *      predicate: NamedNode {id: "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"}
   *      subject: NamedNode {id: "http://example.org/cartoons#Tom"}
   * }, Quad {...}, ...]
   * ```
   * See here for ways to create them using N3: https://www.npmjs.com/package/n3#creating-triplesquads
   * @param {*} writerArgs the arguments for intializing the writer.
   *   e.g. `{format: 'application/trig'}`. See the writer-documentation
   *   (https://github.com/RubenVerborgh/N3.js#writing) for more details.
   */
  const n3Write = async (quads, writerArgs) => {
    const writer = new N3.Writer(writerArgs);
    return new Promise((resolve, reject) => {
      writer.addQuads(quads);
      writer.end((error, result) => {
        if (error) reject(error);
        else resolve(result);
      });
    });
  };

  /**
   * A wrapper for N3's parse that returns a promise
   * @param {*} rdf a rdf-string to be parsed
   * @param {*} parserArgs arguments for initializing the parser,
   *   e.g. `{format: 'application/n-quads'}` if you want to make
   *   parser stricter about what it accepts. See the parser-documentation
   *   (https://github.com/RubenVerborgh/N3.js#parsing) for more details.
   */
  const n3Parse = (rdf, parserArgs) => {
    const rdfParser = parserArgs ? new N3.Parser(parserArgs) : new N3.Parser();
    return rdfParser.parse(rdf);
  };

  return n3Write(n3Parse(rdf), { format: "application/n-quads" })
    .then(quadString =>
      jsonld.fromRDF(quadString, { format: "application/n-quads" })
    )
    .catch(e => {
      e.message =
        "error while parsing the following turtle:\n\n" +
        rdf +
        "\n\n----\n\n" +
        e.message;
      throw e;
    });
};

export const wonMessageFromJsonLd = (rawMessageJsonLd, msgUri, vocab) =>
  jsonld
    .frame(rawMessageJsonLd, { "@id": msgUri, "@embed": "@always" })
    .then(framedMessageJsonLd => ({
      framedMessage: framedMessageJsonLd,
      rawMessage: rawMessageJsonLd,
      messageStructure: {
        messageUri: framedMessageJsonLd["@id"],
        messageDirection: framedMessageJsonLd["@type"],
      },
    }))
    .then(wonMessage => {
      //Only generate compactedFramedMessage if it is not a response or not from Owner
      const getProperty = (wonMessage, property) => {
        const jsonldUtilsGetProperty = (jsonld, property) => {
          if (jsonld && property) {
            if (jsonld["@graph"] && jsonld["@graph"][0]) {
              return jsonld["@graph"][0][property];
            } else {
              return jsonld[property];
            }
          }
          return undefined;
        };

        let val = jsonldUtilsGetProperty(wonMessage.framedMessage, property);
        if (val) {
          const __singleValueOrArray = (wonMessage, val) => {
            const getSafeJsonLdValue = dataItem => {
              if (dataItem == null) return null;
              if (typeof dataItem === "object") {
                if (dataItem["@id"]) return dataItem["@id"];
                if (dataItem["@value"]) return dataItem["@value"];
              } else {
                return dataItem;
              }
              return null;
            };

            if (!val) return null;
            if (is("Array", val)) {
              if (val.length === 1) {
                return getSafeJsonLdValue(val);
              }
              return val.map(x => getSafeJsonLdValue(x));
            }
            return getSafeJsonLdValue(val);
          };

          return __singleValueOrArray(wonMessage, val);
        }
        return null;
      };

      const messageType = getProperty(wonMessage, vocab.WONMSG.messageType);
      const messageDirection =
        wonMessage.messageStructure &&
        wonMessage.messageStructure.messageDirection;

      if (
        messageType === vocab.WONMSG.successResponse ||
        messageType === vocab.WONMSG.failureResponse ||
        messageDirection === vocab.WONMSG.FromOwner
      ) {
        if (wonMessage.compactFramedMessage) {
          return wonMessage;
        }
        if (wonMessage.framedMessage && wonMessage.rawMessage) {
          return Promise.all([
            jsonld
              .compact(wonMessage.framedMessage, vocab.defaultContext)
              .then(
                compactFramedMessage =>
                  (wonMessage.compactFramedMessage = compactFramedMessage)
              ),
            jsonld
              .compact(wonMessage.rawMessage, vocab.defaultContext)
              .then(
                compactRawMessage =>
                  (wonMessage.compactRawMessage = compactRawMessage)
              ),
          ])
            .catch(e =>
              console.error(
                "Failed to generate jsonld for message " +
                  wonMessage.getMessageUri() +
                  "\n\n" +
                  e.message +
                  "\n\n" +
                  e.stack
              )
            )
            .then(() => wonMessage);
        }
      }
      return wonMessage;
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
    return response
      .json()
      .then(jsonResponse => ({
        response: jsonResponse,
        status: response.status,
        params: params,
        message: `ERROR for request: ${response.status} - ${
          response.statusText
        } for request ${uri}`,
      }))
      .catch(err => {
        console.debug(
          "checkHttpStatus response, does not have json content",
          response.status,
          err
        );

        return {
          response: {},
          status: response.status,
          params: params,
          message: `ERROR for request: ${response.status} - ${
            response.statusText
          } for request ${uri}`,
        };
      })
      .then(errorPayload => {
        throw new Error(JSON.stringify(errorPayload));
      });
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
    obtainedFrom: undefined,
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

/**
 * @param credentials either {email, password} or {privateId}
 * @returns {email, password}
 */
function parseCredentials(credentials) {
  return credentials.privateId
    ? privateId2Credentials(credentials.privateId)
    : credentials;
}

/**
 * Parses a given privateId into a fake email address and a password.
 * @param privateId
 * @returns {{email: string, password: *}}
 */
function privateId2Credentials(privateId) {
  const [usernameFragment, password] = privateId.split("-");
  const email = usernameFragment + "@matchat.org";
  return {
    email,
    password,
    privateId,
  };
}
