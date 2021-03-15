import * as ownerApi from "../api/owner-api.js";
import Immutable from "immutable";
import { actionTypes } from "../actions/actions.js";
import * as generalSelectors from "./selectors/general-selectors.js";
import * as atomUtils from "./utils/atom-utils.js";
import * as connectionUtils from "./utils/connection-utils.js";
import * as accountUtils from "./utils/account-utils.js";
import * as processUtils from "./utils/process-utils.js";
import * as useCaseUtils from "~/app/usecase-utils";
import { isUriDeleted } from "~/app/won-localstorage";
import {
  parseMetaAtom,
  parseAtomContent,
} from "../reducers/atom-reducer/parse-atom.js";
import {
  extractAtomUriFromConnectionUri,
  get,
  is,
  getUri,
  getIn,
  parseWorkerError,
} from "../utils.js";
import won from "../won-es6";
import vocab from "../service/vocab.js";
import { fetchWikiData } from "~/app/api/wikidata-api";
import cf from "clownface";
import { actionCreators } from "../actions/actions";
import paWorker from "workerize-loader?[name].[contenthash:8]!../../parseAtom-worker.js";
import fakeNames from "~/app/fakeNames.json";

const parseAtomWorker = paWorker();

export const fetchOwnedMetaData = dispatch =>
  ownerApi.fetchOwnedMetaAtoms().then(metaAtoms => {
    const atomsImm = Immutable.fromJS(metaAtoms);
    dispatch({
      type: actionTypes.atoms.storeOwnedMetaAtoms,
      payload: Immutable.fromJS({
        metaAtoms: atomsImm ? atomsImm : Immutable.Map(),
      }),
    });

    const activeAtomsImm =
      atomsImm &&
      atomsImm.filter(metaAtom => atomUtils.isActive(parseMetaAtom(metaAtom)));

    return [...activeAtomsImm.keys()];
  });

export const fetchActiveConnectionAndDispatch = (
  connUri,
  requestCredentials,
  dispatch
) =>
  won
    .fetchConnection(connUri, requestCredentials)
    .then(connection => {
      dispatch({
        type: actionTypes.connections.storeActive,
        payload: Immutable.fromJS({ connections: { [connUri]: connection } }),
      });
      return connection;
    })
    .catch(error => {
      dispatch({
        type: actionTypes.connections.storeUriFailed,
        payload: Immutable.fromJS({
          uri: connUri,
          request: {
            code: error.status,
            message: error.message,
            requestCredentials: requestCredentials,
          },
        }),
      });
    });

export const fetchConnectionUriBySocketUris = (
  senderSocketUri,
  targetSocketUri,
  requestCredentials
) =>
  won
    .fetchConnectionUrisBySocket(
      senderSocketUri,
      targetSocketUri,
      requestCredentials
    )
    .catch(() => {
      console.error(
        "Fetch of ConnectionUri of sockets",
        senderSocketUri,
        "<->",
        targetSocketUri,
        "failed"
      );
    });

export const fetchActiveConnectionAndDispatchBySocketUris = (
  senderSocketUri,
  targetSocketUri,
  requestCredentials,
  dispatch
) =>
  won
    .fetchConnectionBySocket(
      senderSocketUri,
      targetSocketUri,
      requestCredentials
    )
    .then(conn => {
      dispatch({
        type: actionTypes.connections.storeActive,
        payload: Immutable.fromJS({ connections: { [conn.uri]: conn } }),
      });
      return conn;
    })
    .catch(() => {
      console.error(
        "Store Connection of sockets",
        senderSocketUri,
        "<->",
        targetSocketUri,
        "failed"
      );
    });

export const determineRequestCredentials = (state, atomUri) => {
  console.debug("## Determine requesterWebId for", atomUri);

  const validRequestCredentials = generalSelectors.getAllValidRequestCredentialsForAtom(
    atomUri
  )(state);

  if (validRequestCredentials.size > 0) {
    if (validRequestCredentials.size > 1) {
      console.debug(
        "## more than one (seemingly) valid requestCredentials entry found to fetch (",
        atomUri,
        "), using the first one for now, here is the rest as Immutable: ",
        validRequestCredentials
      );
    }

    return Promise.resolve(validRequestCredentials.first().toJS());
  } else {
    //TODO: Figure out a way to handle this within the selecter we use above
    const accountState = generalSelectors.getAccountState(state);
    const nonOwnedConnectionsToTargetAtom = generalSelectors
      .getConnectionsWithTargetAtomUri(atomUri)(state)
      .filterNot((_, connUri) =>
        accountUtils.isAtomOwned(
          accountState,
          extractAtomUriFromConnectionUri(connUri)
        )
      );

    if (nonOwnedConnectionsToTargetAtom.size > 0) {
      const consideredAtom = generalSelectors.getAtom(
        extractAtomUriFromConnectionUri(
          getUri(nonOwnedConnectionsToTargetAtom.first())
        )
      )(state);

      if (nonOwnedConnectionsToTargetAtom.size > 1) {
        console.debug(
          "## more than one connection found for this non owned Atom, using first one for token-access consideration: ",
          consideredAtom,
          nonOwnedConnectionsToTargetAtom
        );
      } else {
        console.debug(
          "## one connection found for this non owned Atom, using first one as token-access consideration: ",
          consideredAtom
        );
      }

      const tokenAuths = atomUtils.getTokenAuth(consideredAtom);

      for (const tokenAuth of tokenAuths) {
        console.debug("tokenAuth: ", tokenAuth);
        const authTokenOperations = tokenAuth
          .get(vocab.AUTH.grant)
          .flatMap(grant => get(grant, vocab.AUTH.operation))
          .map(op => get(op, vocab.AUTH.requestToken))
          .filter(op => !!op);
        console.debug("authOperations: ", authTokenOperations);

        for (const authTokenOperation of authTokenOperations) {
          const tokenScopeUri = getIn(authTokenOperation, [
            vocab.AUTH.tokenScope,
            "@id",
          ]);
          console.debug("### ", tokenScopeUri);

          //TODO: THIS IS NEXT PART PRETTY HARDCODED (which isnt great but works for now):
          if (tokenScopeUri === vocab.HOLD.ScopeReadHeldAtoms) {
            const ownedBuddyUrisOfConsideredAtom = atomUtils
              .getConnectedConnections(
                consideredAtom,
                vocab.BUDDY.BuddySocketCompacted
              )
              .filter(conn =>
                accountUtils.isAtomOwned(
                  accountState,
                  connectionUtils.getTargetAtomUri(conn)
                )
              )
              .map(connectionUtils.getTargetAtomUri)
              .valueSeq()
              .toSet();
            console.debug(
              "ownedBuddyUrisOfConsideredAtom: ",
              ownedBuddyUrisOfConsideredAtom
            );

            if (ownedBuddyUrisOfConsideredAtom.size > 0) {
              const fetchTokenRequesterId = ownedBuddyUrisOfConsideredAtom.first();

              if (ownedBuddyUrisOfConsideredAtom.size > 1) {
                console.debug(
                  "## more than one connection found for this non owned Atom, using first one as the tokenRequester: ",
                  fetchTokenRequesterId,
                  ownedBuddyUrisOfConsideredAtom
                );
              }

              return ownerApi
                .fetchTokenForAtom(getUri(consideredAtom), {
                  requesterWebId: fetchTokenRequesterId,
                  scopes: tokenScopeUri,
                })
                .then(tokens => {
                  console.debug("tokens: ", tokens);
                  return { token: tokens[0] };
                })
                .catch(error => console.debug("error: ", error)); //TODO: FIX ERROR
            }
          }

          //******************************************************************************
        }
      }

      return Promise.resolve({ requesterWebId: undefined, token: undefined });
    } else {
      console.debug(
        "## no connections found for this non owned atom, using nothing as requesterWebId/token maybe we are lucky..."
      );
      return Promise.resolve({ requesterWebId: undefined, token: undefined });
    }
  }
};
export const fetchConnectionsContainerAndDispatch = (
  atomUri,
  dispatch,
  getState
) => {
  const state = getState();
  const processState = generalSelectors.getProcessState(state);

  if (processUtils.isConnectionContainerLoaded(processState, atomUri)) {
    console.debug(
      "Omit Fetch of ConnectionContainer<",
      atomUri,
      ">, it is already loaded..."
    );
    if (processUtils.isConnectionContainerToLoad(processState, atomUri)) {
      dispatch({
        type: actionTypes.atoms.markConnectionContainerAsLoaded,
        payload: Immutable.fromJS({ uri: atomUri }),
      });
    }
    return Promise.resolve();
  } else if (processUtils.isConnectionContainerLoading(processState, atomUri)) {
    console.debug(
      "Omit Fetch of ConnectionContainer<",
      atomUri,
      ">, it is currently loading..."
    );
    return Promise.resolve();
  } else if (processUtils.isProcessingInitialLoad(processState)) {
    console.debug(
      "Omit Fetch of ConnectionContainer<",
      atomUri,
      ">, initial Load still in progress..."
    );
    return Promise.resolve();
  }

  const isOwned = generalSelectors.isAtomOwned(atomUri)(state);
  console.debug(
    "Proceed Fetch of",
    isOwned ? "Owned" : "",
    "ConnectionContainer<",
    atomUri,
    ">"
  );

  dispatch({
    type: actionTypes.atoms.storeConnectionContainerInLoading,
    payload: Immutable.fromJS({ uri: atomUri }),
  });

  return determineRequestCredentials(state, atomUri).then(requestCredentials =>
    won
      .fetchConnectionUrisWithStateByAtomUri(
        atomUtils.getConnectionContainerUri(
          generalSelectors.getAtom(atomUri)(state)
        ),
        requestCredentials
      )
      .then(connectionsWithStateAndSocket => {
        dispatch({
          type: actionTypes.connections.storeMetaConnections,
          payload: Immutable.fromJS({
            atomUri: atomUri,
            connections: connectionsWithStateAndSocket,
            request: {
              code: 200,
              requestCredentials: requestCredentials,
            },
          }),
        });
        if (isOwned) {
          const activeConnectionUris = connectionsWithStateAndSocket
            .filter(
              conn =>
                !isUriDeleted(conn.uri) &&
                conn.connectionState !== vocab.WON.Closed &&
                conn.connectionState !== vocab.WON.Suggested
            )
            .map(conn => conn.uri);

          dispatch({
            type: actionTypes.connections.storeActiveUrisInLoading,
            payload: Immutable.fromJS({
              atomUri: atomUri,
              connUris: activeConnectionUris,
            }),
          });
          return activeConnectionUris;
        }
        return undefined;
      })
      .then(
        activeConnectionUris =>
          activeConnectionUris &&
          urisToLookupMap(activeConnectionUris, connUri =>
            fetchActiveConnectionAndDispatch(
              connUri,
              requestCredentials,
              dispatch
            )
          )
      )
      .catch(error => {
        let errorParsed = parseWorkerError(error);

        if (errorParsed.status && errorParsed.status === 410) {
          dispatch({
            type: actionTypes.atoms.delete,
            payload: Immutable.fromJS({ uri: atomUri }),
          });
        } else {
          dispatch({
            type: actionTypes.atoms.storeConnectionContainerFailed,
            payload: Immutable.fromJS({
              uri: atomUri,
              request: {
                code: errorParsed.status,
                params: errorParsed.params || {},
                message: errorParsed.message || error.message,
                response: errorParsed.response,
                requestCredentials: requestCredentials,
              },
            }),
          });
        }
      })
  );
};

/**
 * Fetches an atom (incl. the persona that holds it), the fetch is omitted if:
 * - the atom is already loaded
 * - the atom is currently loading
 * Omit fetching the persona attached to the atom if:
 * - there is no attached persona
 * - the persona is already loaded
 * - the persona is currently loading
 *
 * If update is set to true, the loaded status of an atom does NOT omit the fetch
 * @param {String} atomUri
 * @param dispatch
 * @param {function} getState
 * @param {boolean} update, defaults to false
 * @param {Object} overrideCredentials, can be used to override the credentialDetermination
 * @returns {*}
 */
export const fetchAtomAndDispatch = (
  atomUri,
  dispatch,
  getState,
  update = false,
  overrideCredentials
) => {
  const state = getState();
  const processState = generalSelectors.getProcessState(state);

  if (!update && processUtils.isAtomLoaded(processState, atomUri)) {
    console.debug("Omit Fetch of Atom<", atomUri, ">, it is already loaded...");
    if (processUtils.isAtomToLoad(processState, atomUri)) {
      dispatch({
        type: actionTypes.atoms.markAsLoaded,
        payload: Immutable.fromJS({ uri: atomUri }),
      });
    }
    return Promise.resolve();
  } else if (processUtils.isAtomLoading(processState, atomUri)) {
    console.debug(
      "Omit Fetch of Atom<",
      atomUri,
      ">, it is currently loading..."
    );
    return Promise.resolve();
  } else if (processUtils.isProcessingInitialLoad(processState)) {
    console.debug(
      "Omit Fetch of Atom<",
      atomUri,
      ">, initial Load still in progress..."
    );
    return Promise.resolve();
  }

  const isOwned = generalSelectors.isAtomOwned(atomUri)(state);
  console.debug(
    "Proceed Fetch of",
    isOwned ? "Owned" : "",
    "Atom<",
    atomUri,
    ">"
  );
  dispatch({
    type: actionTypes.atoms.storeUriInLoading,
    payload: Immutable.fromJS({ uri: atomUri }),
  });

  return (
    (overrideCredentials
      ? Promise.resolve(overrideCredentials)
      : determineRequestCredentials(state, atomUri)
    )
      // TODO: INCLUDE GRANTS FETCH SOMEHOW
      // .then(requestCredentials => {
      //   ownerApi
      //     .fetchGrantsForAtom(atomUri, requestCredentials)
      //     .then(response => {
      //       console.debug("fetchGrantsForAtom Response: ", response);
      //     })
      //     .catch(error => console.debug("fetchGrantsForAtom Error:", error));
      //   return requestCredentials;
      // })
      .then(requestCredentials =>
        won
          .fetchAtom(atomUri, requestCredentials)
          .then(atom => parseAtomWorker.parse(atom, fakeNames, vocab))
          .then(partiallyParsedAtom => {
            const parsedAtomImm = parseAtomContent(partiallyParsedAtom);
            if (parsedAtomImm) {
              dispatch({
                type: actionTypes.atoms.store,
                payload: Immutable.fromJS({
                  atom: parsedAtomImm,
                  request: {
                    code: 200,
                    requestCredentials: requestCredentials,
                  },
                }),
              });
            }
            return parsedAtomImm;
          })
          .catch(error => {
            let errorParsed = parseWorkerError(error);

            if (
              (errorParsed.status && errorParsed.status === 410) ||
              (errorParsed.message && errorParsed.message.startsWith("410"))
            ) {
              dispatch({
                type: actionTypes.atoms.delete,
                payload: Immutable.fromJS({ uri: atomUri }),
              });
            } else {
              dispatch({
                type: actionTypes.atoms.storeUriFailed,
                payload: Immutable.fromJS({
                  uri: atomUri,
                  request: {
                    code: errorParsed.status,
                    params: errorParsed.params || {},
                    message: errorParsed.message,
                    requestCredentials: requestCredentials,
                  },
                }),
              });
            }
          })
      )
  );
};

export const fetchPersonas = (dispatch /*, getState,*/) =>
  ownerApi.fetchAllActiveMetaPersonas().then(atoms => {
    const atomsImm = Immutable.fromJS(atoms);
    const atomUris = [...atomsImm.keys()];

    dispatch({
      type: actionTypes.atoms.storeMetaAtoms,
      payload: Immutable.fromJS({ metaAtoms: atoms }),
    });

    return atomUris;
  });

export const fetchWhatsNew = (
  dispatch,
  getState,
  createdAfterDate = new Date(Date.now() - 30 /*Days before*/ * 86400000)
) =>
  ownerApi.fetchAllMetaAtoms(createdAfterDate).then(atoms => {
    const atomsImm = Immutable.fromJS(atoms);
    const atomUris = [...atomsImm.keys()];

    dispatch({
      type: actionTypes.atoms.storeWhatsNew,
      payload: Immutable.fromJS({ metaAtoms: atoms }),
    });
    return atomUris;
  });

export const fetchWhatsAround = (
  dispatch,
  getState,
  createdAfterDate,
  location,
  maxDistance
) =>
  ownerApi
    .fetchAllMetaAtomsNear(createdAfterDate, location, maxDistance)
    .then(atoms => {
      const atomsImm = Immutable.fromJS(atoms);
      const atomUris = [...atomsImm.keys()];

      dispatch({
        type: actionTypes.atoms.storeWhatsAround,
        payload: Immutable.fromJS({
          metaAtoms: atoms,
          location: location,
          maxDistance: maxDistance,
        }),
      });
      return atomUris;
    });

export const fetchMessages = (
  dispatch,
  state,
  connectionUri,
  requestCredentials,
  numberOfMessages,
  resumeAfter /*msgUri: load numberOfEvents before this msgUri*/
) => {
  const fetchParams = {
    ...requestCredentials,
    pagingSize: numberOfMessages * 3, // `*3*` to compensate for the *roughly* 2 additional success messages per chat message
    deep: true,
    resumeafter: resumeAfter,
  };

  dispatch({
    type: actionTypes.connections.fetchMessagesStart,
    payload: Immutable.fromJS({ connectionUri: connectionUri }),
  });

  const atom = generalSelectors.getAtomByConnectionUri(connectionUri)(state);
  const messageContainerUri = get(
    atomUtils.getConnection(atom, connectionUri),
    "messageContainerUri"
  );

  return won
    .fetchMessagesOfConnection(connectionUri, messageContainerUri, fetchParams)
    .then(({ nextPage, messages }) => {
      const lookupMap = { success: {}, failed: {} };
      const loadingArray = [];
      messages.map(message => {
        loadingArray.push(message.msgUri);

        if (message.wonMessage) {
          lookupMap["success"][message.msgUri] = message.wonMessage;
        } else {
          lookupMap["failed"][message.msgUri] = undefined;
        }
      });

      return { nextPage: nextPage, messages: lookupMap };
    })
    .then(({ nextPage, messages }) => {
      if (messages) {
        const messagesImm = Immutable.fromJS(messages);
        const successMessages = get(messagesImm, "success");
        const failedMessages = get(messagesImm, "failed");

        if (successMessages.size > 0) {
          dispatch({
            type: actionTypes.connections.fetchMessagesSuccess,
            payload: Immutable.fromJS({
              connectionUri: connectionUri,
              events: successMessages,
              nextPage: nextPage,
            }),
          });
        }

        if (failedMessages.size > 0) {
          dispatch({
            type: actionTypes.connections.fetchMessagesFailed,
            payload: Immutable.fromJS({
              connectionUri: connectionUri,
              events: failedMessages,
              nextPage: nextPage,
            }),
          });
        }

        /*If neither succes nor failed has any elements we simply say that fetching Ended, that way
        we can ensure that there is not going to be a lock on the connection because loadingMessages was complete but never
        reset its status
        */
        if (successMessages.size === 0 && failedMessages.size === 0) {
          dispatch({
            type: actionTypes.connections.fetchMessagesEnd,
            payload: Immutable.fromJS({
              connectionUri: connectionUri,
              nextPage: nextPage,
            }),
          });
        }
      }
    });
};

/**
 * Takes a single uri or an array of uris, performs the lookup function on each
 * of them seperately, collects the results and builds an map/object
 * with the uris as keys and the results as values.
 * If any call to the asyncLookupFunction fails, the corresponding
 * key-value-pair will not be contained in the result.
 * @param uris
 * @param asyncLookupFunction
 * @param excludeUris uris to exclude from lookup
 * @param abortOnError -> abort the whole crawl by breaking the promisechain instead of ignoring the failures
 * @return {*}
 */
const urisToLookupMap = (
  uris,
  asyncLookupFunction,
  excludeUris = [],
  abortOnError = false
) => {
  //make sure we have an array and not a single uri.
  const urisAsArray = is("Array", uris) ? uris : [uris];
  const excludeUrisAsArray = is("Array", excludeUris)
    ? excludeUris
    : [excludeUris];

  const urisAsArrayWithoutExcludes = urisAsArray.filter(
    uri => excludeUrisAsArray.indexOf(uri) < 0
  );

  const asyncLookups = urisAsArrayWithoutExcludes.map(uri =>
    asyncLookupFunction(uri).catch(error => {
      if (abortOnError) {
        throw new Error(error);
      } else {
        console.error(
          `failed lookup for ${uri} in utils.js:urisToLookupMap ` +
            error.message,
          "\n\n",
          error.stack,
          "\n\n",
          urisAsArrayWithoutExcludes,
          "\n\n",
          uris,
          "\n\n",
          error
        );
        return undefined;
      }
    })
  );
  return Promise.all(asyncLookups).then(dataObjects => {
    const lookupMap = {};
    //make sure there's the same
    uris.forEach((uri, i) => {
      if (dataObjects[i]) {
        lookupMap[uri] = dataObjects[i];
      }
    });
    return lookupMap;
  });
};

export const storeWikiData = (uri, dispatch, getState) => {
  const processState = generalSelectors.getProcessState(getState());

  if (processUtils.isExternalDataLoading(processState, uri)) {
    console.debug(
      "Omit Fetch of WikiData<",
      uri,
      ">, it is currently loading..."
    );
    return Promise.resolve();
  }
  console.debug("Proceed Fetch of WikiData<", uri, ">");

  dispatch({
    type: actionTypes.externalData.storeUriInLoading,
    payload: Immutable.fromJS({ uri: uri }),
  });

  return fetchWikiData(uri)
    .then(dataset => {
      const cfData = cf({ dataset });
      const detailsToParse = useCaseUtils.getAllDetails();
      const cfEntity = cfData.namedNode(uri);

      const generateContentFromCF = (cfEntityData, detailsToParse) => {
        let content = {};
        if (cfEntityData && detailsToParse) {
          for (const detailKey in detailsToParse) {
            const detailToParse = detailsToParse[detailKey];
            const detailIdentifier = detailToParse && detailToParse.identifier;
            const detailValue =
              detailToParse &&
              detailToParse.parseFromCF &&
              detailToParse.parseFromCF(cfEntityData);

            if (detailIdentifier && detailValue) {
              content[detailIdentifier] = detailValue;
            }
          }
        }

        return content;
      };

      return generateContentFromCF(cfEntity, detailsToParse);
    })
    .then(parsedContent => {
      dispatch(
        actionCreators.externalData__store(
          Immutable.fromJS({ [uri]: parsedContent })
        )
      );
    });
};
