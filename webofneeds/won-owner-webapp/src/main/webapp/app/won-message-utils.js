/**
 * Created by ksinger on 03.12.2015.
 */

import won from "./won-es6.js";
import Immutable from "immutable";
import { checkHttpStatus, urisToLookupMap, is } from "./utils.js";

import { ownerBaseUrl } from "config";
import urljoin from "url-join";

import { getRandomWonId } from "./won-utils.js";
import * as useCaseUtils from "./usecase-utils.js";
import { isConnUriClosed } from "./won-localstorage.js";
import { actionTypes } from "./actions/actions.js";

/**
 * Checks if a wonMessage contains content/references that make it necessary for us to check which effects
 * this message has caused (in relation to other messages, necessary e.g. AgreementData
 * @param wonMsg
 * @returns {*}
 */
export function isFetchMessageEffectsNeeded(wonMsg) {
  return (
    wonMsg &&
    (wonMsg.getProposedMessageUris() ||
      wonMsg.getRetractsMessageUris() ||
      wonMsg.getRejectsMessageUris() ||
      wonMsg.getAcceptsMessageUris() ||
      wonMsg.getProposedToCancelMessageUris() ||
      wonMsg.getClaimsMessageUris())
  );
}

export function buildRateMessage(
  msgToRateFor,
  ownedNeedUri,
  theirNeedUri,
  ownNodeUri,
  theirNodeUri,
  theirConnectionUri,
  rating
) {
  return new Promise(resolve => {
    const buildMessage = function(envelopeData) {
      //TODO: use event URI pattern specified by WoN node
      const eventUri =
        envelopeData[won.WONMSG.hasSenderNode] + "/event/" + getRandomWonId();
      const message = new won.MessageBuilder(won.WONMSG.feedbackMessage) //TODO: Looks like a copy-paste-leftover from connect
        .eventURI(eventUri)
        .hasOwnerDirection()
        .forEnvelopeData(envelopeData)
        .hasSentTimestamp(new Date().getTime().toString())
        .addRating(rating, msgToRateFor.connection.uri)
        .build();
      //const callback = createMessageCallbackForRemoteNeedMessage(eventUri, won.EVENT.OPEN_SENT);
      return { eventUri: eventUri, message: message };
    };

    //fetch all data needed
    won
      .getEnvelopeDataforConnection(
        msgToRateFor.connection.uri,
        ownedNeedUri,
        theirNeedUri,
        ownNodeUri,
        theirNodeUri,
        theirConnectionUri
      )
      .then(function(envelopeData) {
        resolve(buildMessage(envelopeData, msgToRateFor.event));
      }, won.reportError(
        "cannot open connection " + msgToRateFor.connection.uri
      ));
  });
}

export function buildCloseMessage(
  connectionUri,
  ownedNeedUri,
  theirNeedUri,
  ownNodeUri,
  theirNodeUri,
  theirConnectionUri
) {
  const buildMessage = function(envelopeData) {
    //TODO: use event URI pattern specified by WoN node
    const eventUri =
      envelopeData[won.WONMSG.hasSenderNode] + "/event/" + getRandomWonId();
    const message = new won.MessageBuilder(won.WONMSG.closeMessage)
      .eventURI(eventUri)
      .forEnvelopeData(envelopeData)
      .hasOwnerDirection()
      .hasSentTimestamp(new Date().getTime().toString())
      .build();
    //const callback = createMessageCallbackForRemoteNeedMessage(eventUri, won.EVENT.CLOSE_SENT);
    return { eventUri: eventUri, message: message };
  };

  //fetch all data needed
  return won
    .getEnvelopeDataforConnection(
      connectionUri,
      ownedNeedUri,
      theirNeedUri,
      ownNodeUri,
      theirNodeUri,
      theirConnectionUri
    )
    .then(envelopeData => buildMessage(envelopeData))
    .catch(err => {
      won.reportError(
        "cannot close connection " + connectionUri + ": " + JSON.stringify(err)
      );
      throw err;
    });
}
export function buildCloseNeedMessage(needUri, wonNodeUri) {
  const buildMessage = function(envelopeData) {
    const eventUri =
      envelopeData[won.WONMSG.hasSenderNode] + "/event/" + getRandomWonId();
    const message = new won.MessageBuilder(won.WONMSG.closeNeedMessage)
      .eventURI(eventUri)
      .hasReceiverNode(wonNodeUri)
      .hasOwnerDirection()
      .hasSentTimestamp(new Date().getTime().toString())
      .forEnvelopeData(envelopeData)
      .build();

    return { eventUri: eventUri, message: message };
  };

  return won
    .getEnvelopeDataForNeed(needUri, wonNodeUri)
    .then(
      envelopeData => buildMessage(envelopeData),
      () => won.reportError("cannot close need " + needUri)
    );
}

export function buildDeleteNeedMessage(needUri, wonNodeUri) {
  const buildMessage = function(envelopeData) {
    const eventUri =
      envelopeData[won.WONMSG.hasSenderNode] + "/event/" + getRandomWonId();
    const message = new won.MessageBuilder(won.WONMSG.deleteNeedMessage)
      .eventURI(eventUri)
      .hasReceiverNode(wonNodeUri)
      .hasOwnerDirection()
      .hasSentTimestamp(new Date().getTime().toString())
      .forEnvelopeData(envelopeData)
      .build();

    return { eventUri: eventUri, message: message };
  };

  return won
    .getEnvelopeDataForNeed(needUri, wonNodeUri)
    .then(
      envelopeData => buildMessage(envelopeData),
      () => won.reportError("cannot delete need " + needUri)
    );
}

export function buildOpenNeedMessage(needUri, wonNodeUri) {
  const buildMessage = function(envelopeData) {
    const eventUri =
      envelopeData[won.WONMSG.hasSenderNode] + "/event/" + getRandomWonId();
    const message = new won.MessageBuilder(won.WONMSG.activateNeedMessage)
      .eventURI(eventUri)
      .hasReceiverNode(wonNodeUri)
      .hasOwnerDirection()
      .hasSentTimestamp(new Date().getTime().toString())
      .forEnvelopeData(envelopeData)
      .build();

    return { eventUri: eventUri, message: message };
  };

  return won
    .getEnvelopeDataForNeed(needUri, wonNodeUri)
    .then(
      envelopeData => buildMessage(envelopeData),
      () => won.reportError("cannot close need " + needUri)
    );
}

/**
 * Builds json-ld for a connect-message in reaction to a need.
 * @param connectionUri
 * @param textMessage
 * @returns {{eventUri, message}|*}
 */
export function buildConnectMessage({
  ownedNeedUri,
  theirNeedUri,
  ownNodeUri,
  theirNodeUri,
  connectMessage,
  optionalOwnConnectionUri,
  ownFacet,
  theirFacet,
}) {
  const envelopeData = won.getEnvelopeDataforNewConnection(
    ownedNeedUri,
    theirNeedUri,
    ownNodeUri,
    theirNodeUri
  );
  if (optionalOwnConnectionUri) {
    envelopeData[won.WONMSG.hasSender] = optionalOwnConnectionUri;
  }
  if (ownFacet) {
    envelopeData[won.WONMSG.hasSenderFacet] = ownFacet;
  }
  if (theirFacet) {
    envelopeData[won.WONMSG.hasReceiverFacet] = theirFacet;
  }
  //TODO: use event URI pattern specified by WoN node
  const eventUri =
    envelopeData[won.WONMSG.hasSenderNode] + "/event/" + getRandomWonId();
  const messageBuilder = new won.MessageBuilder(won.WONMSG.connectMessage);
  messageBuilder.eventURI(eventUri);
  messageBuilder.forEnvelopeData(envelopeData);
  //do not set facets: connect the default facets with each other
  if (connectMessage && typeof connectMessage === "string") {
    messageBuilder.hasTextMessage(connectMessage);
  } else if (connectMessage) {
    messageBuilder.mergeIntoContentGraph(connectMessage);
  }
  messageBuilder.hasOwnerDirection();
  messageBuilder.hasSentTimestamp(new Date().getTime().toString());
  const message = messageBuilder.build();

  return { eventUri: eventUri, message: message };
}

export function buildChatMessage({
  chatMessage,
  additionalContent,
  referencedContentUris, //this is a map of corresponding uris to be e.g. proposes or retracted... (it already includes the correct uri -> remoteUri for received messages, and uri for sent messages)
  connectionUri,
  ownedNeedUri,
  theirNeedUri,
  ownNodeUri,
  theirNodeUri,
  theirConnectionUri,
  isTTL,
}) {
  let jsonldGraphPayloadP = isTTL
    ? won.ttlToJsonLd(won.defaultTurtlePrefixes + "\n" + chatMessage)
    : Promise.resolve();

  const envelopeDataP = won.getEnvelopeDataforConnection(
    connectionUri,
    ownedNeedUri,
    theirNeedUri,
    ownNodeUri,
    theirNodeUri,
    theirConnectionUri
  );

  const messageP = Promise.all([envelopeDataP, jsonldGraphPayloadP]).then(
    ([envelopeData, graphPayload]) => {
      const eventUri =
        envelopeData[won.WONMSG.hasSenderNode] + "/event/" + getRandomWonId();

      /*
             * Build the json-ld message that's signed on the owner-server
             * and then send to the won-node.
             */
      const wonMessageBuilder = new won.MessageBuilder(
        won.WONMSG.connectionMessage
      )
        .forEnvelopeData(envelopeData)
        .hasOwnerDirection()
        .hasSentTimestamp(new Date().getTime().toString());

      if (isTTL && graphPayload) {
        wonMessageBuilder.mergeIntoContentGraph(graphPayload);
      } else if (
        !isTTL &&
        (chatMessage || additionalContent || referencedContentUris)
      ) {
        //add the chatMessage as normal text message
        if (chatMessage) {
          wonMessageBuilder.addContentGraphData(
            won.WON.hasTextMessage,
            chatMessage
          );
        }

        if (additionalContent) {
          const contentNode = wonMessageBuilder.getContentGraphNode();
          const contentNodes = wonMessageBuilder.getContentGraphNodes();
          const detailList = useCaseUtils.getAllDetails();
          additionalContent.forEach((value, key) => {
            const detail = detailList[key];
            const detailRDF =
              detail &&
              detail.parseToRDF({
                value: value,
                identifier: detail.identifier,
                contentUri: eventUri,
              });

            if (detailRDF) {
              const detailRDFArray = Array.isArray(detailRDF)
                ? detailRDF
                : [detailRDF];

              for (const i in detailRDFArray) {
                const detailRDFToAdd = detailRDFArray[i];

                if (detailRDFToAdd["@id"]) {
                  contentNodes.push(detailRDFToAdd);
                } else {
                  for (const key in detailRDFToAdd) {
                    //if contentNode[key] and detailRDF[key] both have values we ommit adding new content (until we implement a merge function)
                    if (contentNode[key]) {
                      if (!Array.isArray(contentNode[key]))
                        contentNode[key] = Array.of(contentNode[key]);

                      contentNode[key] = contentNode[key].concat(
                        detailRDFToAdd[key]
                      );
                    } else {
                      contentNode[key] = detailRDFToAdd[key];
                    }
                  }
                }
              }
            }
          });
        }

        if (referencedContentUris) {
          const contentNode = wonMessageBuilder.getContentGraphNode();
          referencedContentUris.forEach((uris, key) => {
            if (uris && uris.length > 0) {
              switch (key) {
                case "retracts":
                  contentNode[
                    "http://purl.org/webofneeds/modification#retracts"
                  ] = uris;
                  break;
                case "rejects":
                  contentNode[
                    "http://purl.org/webofneeds/agreement#rejects"
                  ] = uris;
                  break;
                case "proposes":
                  contentNode[
                    "http://purl.org/webofneeds/agreement#proposes"
                  ] = uris;
                  break;
                case "claims":
                  contentNode[
                    "http://purl.org/webofneeds/agreement#claims"
                  ] = uris;
                  break;
                case "proposesToCancel":
                  contentNode[
                    "http://purl.org/webofneeds/agreement#proposesToCancel"
                  ] = uris;
                  break;
                case "accepts":
                  contentNode[
                    "http://purl.org/webofneeds/agreement#accepts"
                  ] = uris;
                  break;
                default:
                  console.error(
                    "key[",
                    key,
                    "] is not a valid reference omitting uris[",
                    uris,
                    "] in message"
                  );
                  break;
              }
            }
          });
        }
      } else {
        throw new Error(
          "No textmessage or valid graph as payload of chat message:" +
            JSON.stringify(chatMessage) +
            " " +
            JSON.stringify(graphPayload)
        );
      }

      wonMessageBuilder.eventURI(eventUri); // replace placeholders with proper event-uri
      const message = wonMessageBuilder.build();

      return {
        eventUri,
        message,
      };
    }
  );
  return messageP;
}

export function buildOpenMessage(
  connectionUri,
  ownedNeedUri,
  theirNeedUri,
  ownNodeUri,
  theirNodeUri,
  theirConnectionUri,
  chatMessage
) {
  const messageP = won
    .getEnvelopeDataforConnection(
      connectionUri,
      ownedNeedUri,
      theirNeedUri,
      ownNodeUri,
      theirNodeUri,
      theirConnectionUri
    )
    .then(envelopeData => {
      //TODO: use event URI pattern specified by WoN node
      const eventUri =
        envelopeData[won.WONMSG.hasSenderNode] + "/event/" + getRandomWonId();
      const message = new won.MessageBuilder(won.WONMSG.openMessage)
        .eventURI(eventUri)
        .forEnvelopeData(envelopeData)
        .hasTextMessage(chatMessage)
        .hasOwnerDirection()
        .hasSentTimestamp(new Date().getTime().toString())
        .build();

      return {
        eventUri,
        message,
      };
    });

  return messageP;
}

/**
 *
 * @param needData
 * @param wonNodeUri
 * @return {{
 *    message: (
 *      {
 *          @id,
 *          msg:hasDestinationUri,
 *          msg:hasAttachmentGraphUri
 *      }|
 *      {@id}|
 *      {@graph, @context}
 *    ),
 *    eventUri: string,
 *    needUri: string
 * }}
 */
export async function buildCreateMessage(needData, wonNodeUri) {
  //Check for is and seeks
  /*
    if(!needData.type || !needData.title)
        throw new Error('Tried to create post without type or title. ', needData);
    */

  const publishedContentUri = wonNodeUri + "/need/" + getRandomWonId();

  const imgs = needData.images;
  let attachmentUris = [];
  if (imgs) {
    imgs.forEach(function(img) {
      img.uri = wonNodeUri + "/attachment/" + getRandomWonId();
    });
    attachmentUris = imgs.map(function(img) {
      return img.uri;
    });
  }

  //if type  create -> use needBuilder as well
  const prepareContentNodeData = async needData => ({
    // Adds all fields from needDataIsOrSeeks:
    // title, description, tags, matchingContext, location,...
    ...needData,

    publishedContentUri: publishedContentUri, //mandatory
    //TODO attach to either is or seeks?
    attachmentUris: attachmentUris, //optional, should be same as in `attachments` below
    arbitraryJsonLd: needData.ttl ? await won.ttlToJsonLd(needData.ttl) : [],
  });

  let contentRdf = won.buildNeedRdf({
    content: needData.content
      ? await prepareContentNodeData(needData.content)
      : undefined,
    seeks: needData.seeks
      ? await prepareContentNodeData(needData.seeks)
      : undefined,
    useCase: needData.useCase ? needData.useCase : undefined, //only needed for need building
    // FIXME: find a better way to include need details that are not part of is or seeks
    matchingContext: needData.matchingContext
      ? needData.matchingContext
      : undefined,
    facet: needData.facet,
  });

  const msgUri = wonNodeUri + "/event/" + getRandomWonId(); //mandatory
  const msgJson = won.buildMessageRdf(contentRdf, {
    receiverNode: wonNodeUri, //mandatory
    senderNode: wonNodeUri, //mandatory
    msgType: won.WONMSG.createMessage, //mandatory
    publishedContentUri: publishedContentUri, //mandatory
    msgUri: msgUri,
    attachments: imgs, //optional, should be same as in `attachmentUris` above
  });
  //add the @base definition to the @context so we can use #fragments in the need structure
  msgJson["@context"]["@base"] = publishedContentUri;
  return {
    message: msgJson,
    eventUri: msgUri,
    needUri: publishedContentUri,
  };
}

export function fetchDataForNonOwnedNeedOnly(needUri, dispatch) {
  dispatch({
    type: actionTypes.needs.storeTheirUrisInLoading,
    payload: Immutable.fromJS({ uris: [needUri] }),
  });
  return fetchTheirNeedAndDispatch(needUri, dispatch);
}

export function fetchUnloadedData(dispatch) {
  return fetchOwnedInactiveNeedUris().then(needUris => {
    dispatch({
      type: actionTypes.needs.storeOwnedInactiveUrisInLoading,
      payload: Immutable.fromJS({ uris: needUris }),
    });
    return fetchDataForOwnedNeeds(needUris, dispatch);
  });
}

export function fetchOwnedData(dispatch) {
  return fetchOwnedInactiveNeedUris()
    .then(inactiveNeedUris =>
      dispatch({
        type: actionTypes.needs.storeOwnedInactiveUris,
        payload: Immutable.fromJS({ uris: inactiveNeedUris }),
      })
    )
    .then(() => fetchOwnedActiveNeedUris())
    .then(needUris => {
      dispatch({
        type: actionTypes.needs.storeOwnedActiveUris,
        payload: Immutable.fromJS({ uris: needUris }),
      });
      return needUris;
    })
    .then(needUris => fetchDataForOwnedNeeds(needUris, dispatch));
}

function fetchOwnedInactiveNeedUris() {
  return fetch(urljoin(ownerBaseUrl, "/rest/needs?state=INACTIVE"), {
    method: "get",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
    },
    credentials: "include",
  })
    .then(checkHttpStatus)
    .then(response => response.json());
}

function fetchOwnedActiveNeedUris() {
  return fetch(urljoin(ownerBaseUrl, "/rest/needs?state=ACTIVE"), {
    method: "get",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
    },
    credentials: "include",
  })
    .then(checkHttpStatus)
    .then(response => response.json());
}

export function fetchAgreementProtocolUris(connectionUri) {
  console.debug("fetchAgreementProtocolUris: ", connectionUri);
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
    .then(checkHttpStatus)
    .then(response => response.json());
}

export function fetchPetriNetUris(connectionUri) {
  console.debug("fetchPetriNetUris: ", connectionUri);
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
    .then(checkHttpStatus)
    .then(response => response.json());
}

export function fetchMessageEffects(connectionUri, messageUri) {
  console.debug(
    "fetchMessageEffects: ",
    connectionUri,
    " messageUri:",
    messageUri
  );

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
    .then(checkHttpStatus)
    .then(response => response.json());
}

export function fetchMessage(needUri, eventUri) {
  console.debug("fetchMessage: ", needUri, " eventUri: ", eventUri);
  const url = urljoin(
    ownerBaseUrl,
    "/rest/linked-data/",
    `?requester=${encodeURI(needUri)}`,
    `&uri=${encodeURI(eventUri)}`
  );
  const httpOptions = {
    method: "get",
    headers: {
      Accept: "application/ld+json",
      "Content-Type": "application/ld+json",
    },
    credentials: "include",
  };
  return fetch(url, httpOptions)
    .then(checkHttpStatus)
    .then(response => response.json());
}

export async function fetchDataForOwnedNeeds(ownedNeedUris, dispatch) {
  if (!is("Array", ownedNeedUris) || ownedNeedUris.length === 0) {
    return;
  }

  return urisToLookupMap(ownedNeedUris, uri =>
    fetchOwnedNeedAndDispatch(uri, dispatch)
  )
    .then(() =>
      urisToLookupMap(ownedNeedUris, needUri =>
        fetchConnectionsOfNeedAndDispatch(needUri, dispatch)
      )
    )
    .then(needConnectionMap => {
      const theirNeedUris = Immutable.fromJS(needConnectionMap)
        .filter(connections => connections.size > 0)
        .flatMap(entry => entry)
        .map(conn => conn.get("hasRemoteNeed"))
        .toSet();

      const theirNeedUrisArray = theirNeedUris.toArray();

      dispatch({
        type: actionTypes.needs.storeTheirUrisInLoading,
        payload: Immutable.fromJS({ uris: theirNeedUrisArray }),
      });
      return theirNeedUrisArray;
    })
    .then(theirNeedUris =>
      urisToLookupMap(theirNeedUris, uri =>
        fetchTheirNeedAndDispatch(uri, dispatch)
      )
    );
}

function fetchConnectionsOfNeedAndDispatch(needUri, dispatch) {
  return won
    .getConnectionUrisOfNeed(needUri, needUri, true)
    .then(connectionUrisOfNeed => {
      const activeConnectionUris = connectionUrisOfNeed.filter(
        connUri => !isConnUriClosed(connUri)
      );
      dispatch({
        type: actionTypes.connections.storeActiveUrisInLoading,
        payload: Immutable.fromJS({
          needUri: needUri,
          connUris: activeConnectionUris,
        }),
      });

      return urisToLookupMap(activeConnectionUris, connUri =>
        fetchActiveConnectionAndDispatch(connUri, needUri, dispatch)
      );
    });
}

function fetchOwnedNeedAndDispatch(needUri, dispatch) {
  return won
    .getNeed(needUri)
    .then(need => {
      dispatch({
        type: actionTypes.needs.storeOwned,
        payload: Immutable.fromJS({ needs: { [needUri]: need } }),
      });
      return need;
    })
    .catch(() => {
      dispatch({
        type: actionTypes.needs.storeUriFailed,
        payload: Immutable.fromJS({ uri: needUri }),
      });
      return;
    });
}

export function fetchActiveConnectionAndDispatch(connUri, needUri, dispatch) {
  return won
    .getConnectionWithEventUris(connUri, { requesterWebId: needUri })
    .then(connection => {
      dispatch({
        type: actionTypes.connections.storeActive,
        payload: Immutable.fromJS({ connections: { [connUri]: connection } }),
      });
      return connection;
    })
    .catch(() => {
      dispatch({
        type: actionTypes.connections.storeUriFailed,
        payload: Immutable.fromJS({ uri: connUri }),
      });
      return;
    });
}

export function fetchTheirNeedAndDispatch(needUri, dispatch) {
  return won
    .getNeed(needUri)
    .then(need => {
      if (need["won:heldBy"] && need["won:heldBy"]["@id"]) {
        const personaUri = need["won:heldBy"]["@id"];
        dispatch({
          type: actionTypes.personas.storeTheirUrisInLoading,
          payload: Immutable.fromJS({ uris: [personaUri] }),
        });
        return won
          .getNeed(personaUri)
          .then(personaNeed => {
            dispatch({
              type: actionTypes.personas.storeTheirs,
              payload: Immutable.fromJS({
                needs: { [personaUri]: personaNeed },
              }),
            });
            return need;
          })
          .catch(() => {
            dispatch({
              type: actionTypes.personas.storeUriFailed,
              payload: Immutable.fromJS({ uri: personaUri }),
            });
            return need;
          });
      } else {
        return need;
      }
    })
    .then(need => {
      dispatch({
        type: actionTypes.needs.storeTheirs,
        payload: Immutable.fromJS({ needs: { [needUri]: need } }),
      });
      return need;
    })
    .catch(() => {
      dispatch({
        type: actionTypes.needs.storeUriFailed,
        payload: Immutable.fromJS({ uri: needUri }),
      });
      return;
    });
}
