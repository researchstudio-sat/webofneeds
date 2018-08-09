/**
 * Created by ksinger on 03.12.2015.
 */

import won from "./won-es6.js";
import Immutable from "immutable";
import { checkHttpStatus, urisToLookupMap, is } from "./utils.js";

import { ownerBaseUrl } from "config";
import urljoin from "url-join";

import { getRandomWonId, getAllDetails } from "./won-utils.js";
import { isConnUriClosed } from "./won-localstorage.js";

export const emptyDataset = Immutable.fromJS({
  ownNeeds: {},
  connections: {},
  events: {},
  theirNeeds: {},
  inactiveNeedUris: [],
  activeNeedUris: [],
  inactiveNeedUrisLoading: [],
  needUriForConnections: {},
  activeConnectionUrisLoading: [],
  inactiveConnectionUris: [],
  theirNeedUrisInLoading: [],
});

export function wellFormedPayload(payload) {
  return emptyDataset.mergeDeep(Immutable.fromJS(payload));
}

export function buildRateMessage(
  msgToRateFor,
  ownNeedUri,
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
        ownNeedUri,
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
  ownNeedUri,
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
      ownNeedUri,
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
  ownNeedUri,
  theirNeedUri,
  ownNodeUri,
  theirNodeUri,
  textMessage,
  optionalOwnConnectionUri,
}) {
  const envelopeData = won.getEnvelopeDataforNewConnection(
    ownNeedUri,
    theirNeedUri,
    ownNodeUri,
    theirNodeUri
  );
  if (optionalOwnConnectionUri) {
    envelopeData[won.WONMSG.hasSender] = optionalOwnConnectionUri;
  }
  //TODO: use event URI pattern specified by WoN node
  const eventUri =
    envelopeData[won.WONMSG.hasSenderNode] + "/event/" + getRandomWonId();
  const message = new won.MessageBuilder(won.WONMSG.connectMessage)
    .eventURI(eventUri)
    .forEnvelopeData(envelopeData)
    .hasFacet(won.WON.OwnerFacet)
    .hasRemoteFacet(won.WON.OwnerFacet)
    .hasTextMessage(textMessage)
    .hasOwnerDirection()
    .hasSentTimestamp(new Date().getTime().toString())
    .build();

  return { eventUri: eventUri, message: message };
}

export function buildChatMessage({
  chatMessage,
  additionalContent,
  connectionUri,
  ownNeedUri,
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
    ownNeedUri,
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
      } else if (!isTTL && (chatMessage || additionalContent)) {
        //add the chatMessage as normal text message
        if (chatMessage) {
          wonMessageBuilder.addContentGraphData(
            won.WON.hasTextMessage,
            chatMessage
          );
        }

        if (additionalContent) {
          const contentNode = wonMessageBuilder.getContentGraphNode();
          const detailList = getAllDetails();
          additionalContent.forEach((value, key) => {
            const detail = detailList[key];
            const detailRDF =
              detail &&
              detail.parseToRDF({
                value: value,
                identifier: detail.identifier,
              });

            if (detailRDF) {
              for (const key in detailRDF) {
                //if contentNode[key] and detailRDF[key] both have values we ommit adding new content (until we implement a merge function)
                if (contentNode[key]) {
                  if (!Array.isArray(contentNode[key]))
                    contentNode[key] = Array.of(contentNode[key]);

                  contentNode[key] = contentNode[key].concat(detailRDF[key]);
                } else {
                  contentNode[key] = detailRDF[key];
                }
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
  ownNeedUri,
  theirNeedUri,
  ownNodeUri,
  theirNodeUri,
  theirConnectionUri,
  chatMessage
) {
  const messageP = won
    .getEnvelopeDataforConnection(
      connectionUri,
      ownNeedUri,
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
        .hasFacet(won.WON.OwnerFacet) //TODO: check. looks like a copy-paste-leftover from connect
        .hasRemoteFacet(won.WON.OwnerFacet) //TODO: check. looks like a copy-paste-leftover from connect
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
  const prepareContentNodeData = async needDataIsOrSeeks => ({
    // Adds all fields from needDataIsOrSeeks:
    // title, description, tags, matchingContext, location,...
    ...needDataIsOrSeeks,

    publishedContentUri: publishedContentUri, //mandatory
    type: won.toCompacted(needDataIsOrSeeks.type), //mandatory
    //TODO attach to either is or seeks?
    attachmentUris: attachmentUris, //optional, should be same as in `attachments` below
    arbitraryJsonLd: needDataIsOrSeeks.ttl
      ? await won.ttlToJsonLd(needDataIsOrSeeks.ttl)
      : [],
  });

  let contentRdf = won.buildNeedRdf({
    is: needData.is ? await prepareContentNodeData(needData.is) : undefined,
    seeks: needData.seeks
      ? await prepareContentNodeData(needData.seeks)
      : undefined,
    // FIXME: find a better way to include need details that are not part of is or seeks
    matchingContext: needData.matchingContext
      ? needData.matchingContext
      : undefined,
    searchString: needData.searchString ? needData.searchString : undefined,
    useCase: needData.useCase ? needData.useCase : undefined, //only needed for need building
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
  return {
    message: msgJson,
    eventUri: msgUri,
    needUri: publishedContentUri,
  };
}

export function buildProposalMessage(uri, type, text) {
  const msgP = won.WONMSG.uriPlaceholder.event;
  const sc = "http://purl.org/webofneeds/agreement#" + type;
  const whM = "\n won:hasTextMessage ";
  return (
    "<" +
    msgP +
    "> <" +
    sc +
    "> <" +
    uri +
    ">;" +
    whM +
    " '''" +
    text.replace(/'/g, "///'") +
    "'''."
  );
}

export function buildModificationMessage(uri, type, text) {
  const msgP = won.WONMSG.uriPlaceholder.event;
  const sc = "http://purl.org/webofneeds/modification#" + type;
  const whM = "\n won:hasTextMessage ";
  return (
    "<" +
    msgP +
    "> <" +
    sc +
    "> <" +
    uri +
    ">;" +
    whM +
    " '''" +
    text.replace(/'/g, "///'") +
    "'''."
  );
}

export function isSuccessMessage(event) {
  return event.hasMessageType === won.WONMSG.successResponseCompacted;
}

export function fetchDataForNonOwnedNeedOnly(needUri) {
  console.log("fetchDataForNonOwnedNeedOnly");
  return won
    .getNeed(needUri)
    .then(need =>
      emptyDataset
        .setIn(["theirNeeds", needUri], Immutable.fromJS(need))
        .set("loggedIn", false)
    );
}

export function fetchUnloadedData(curriedDispatch) {
  console.log("fetchUnloadedData");

  return fetchOwnedInactiveNeedUris().then(needUris => {
    curriedDispatch(wellFormedPayload({ inactiveNeedUrisLoading: needUris }));
    return fetchDataForOwnedNeeds(needUris, curriedDispatch, []);
  });
}

export function fetchOwnedData(email, curriedDispatch) {
  console.log("fetchOwnedData");
  return fetchOwnedInactiveNeedUris().then(inactiveNeedUris => {
    curriedDispatch(wellFormedPayload({ inactiveNeedUris: inactiveNeedUris }));

    return fetchOwnedActiveNeedUris().then(needUris => {
      curriedDispatch(wellFormedPayload({ activeNeedUris: needUris }));
      return fetchDataForOwnedNeeds(needUris, curriedDispatch);
    });
  });
}
//export function fetchDataForOwnedNeeds(needUris, curriedDispatch) {
//    return fetchAllAccessibleAndRelevantData(needUris, curriedDispatch)
//        .catch(error => {
//            throw({msg: 'user needlist retrieval failed', error});
//        });
//}
//function fetchOwnedNeedUris() {
//  console.log("fetchOwnedNeedUris");
//  return fetch(urljoin(ownerBaseUrl, "/rest/needs/"), {
//    method: "get",
//    headers: {
//      Accept: "application/json",
//      "Content-Type": "application/json",
//    },
//    credentials: "include",
//  })
//    .then(checkHttpStatus)
//    .then(response => response.json());
//}

function fetchOwnedInactiveNeedUris() {
  console.log("fetchOwnedInactiveNeedUris");
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
  console.log("fetchOwnedActiveNeedUris");
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

// API call to get agreements data for a connection
export function fetchAgreementData(url) {
  console.log("fetchAgreementData: ", url);
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
  console.log("fetchMessage: ", needUri, " eventUri: ", eventUri);
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

window.fetchAll4dbg = fetchDataForOwnedNeeds;
export async function fetchDataForOwnedNeeds(
  ownNeedUris,
  curriedDispatch = () => undefined
) {
  if (!is("Array", ownNeedUris) || ownNeedUris.length === 0) {
    return emptyDataset;
  }

  console.log("fetchOwnNeedAndDispatch for: ", ownNeedUris);
  const allOwnNeeds = await urisToLookupMap(ownNeedUris, uri =>
    fetchOwnNeedAndDispatch(uri, curriedDispatch)
  );

  // wait for the own needs to be dispatched then load connections
  //[{ uri -> cnct }]
  const connectionMaps = await Promise.all(
    ownNeedUris.map(needUri =>
      fetchConnectionsOfNeedAndDispatch(needUri, curriedDispatch)
    )
  );

  // flatten into one lookup map
  const allConnections = connectionMaps.reduce(
    (a, b) => Object.assign(a, b),
    {}
  );

  const theirNeedUris = Object.values(allConnections).map(
    cnct => cnct.hasRemoteNeed
  );

  const theirNeedUris_ = Immutable.Set(theirNeedUris).toArray();
  curriedDispatch(
    wellFormedPayload({ theirNeedUrisInLoading: theirNeedUris_ })
  );
  console.log("fetchTheirNeedAndDispatch for: ", theirNeedUris_);
  const allTheirNeeds = await urisToLookupMap(theirNeedUris_, uri =>
    fetchTheirNeedAndDispatch(uri, curriedDispatch)
  );

  const allDataRawPromise = Promise.all([
    allOwnNeeds,
    allConnections,
    allTheirNeeds,
  ]);

  return allDataRawPromise.then(
    ([ownNeeds, connections, /* events, */ theirNeeds]) => {
      wellFormedPayload({
        ownNeeds,
        connections,
        events: {
          /* will be loaded later when connection is accessed */
        },
        theirNeeds,
      });
    }
  );

  /**
     const allAccessibleAndRelevantData = {
        ownNeeds: {
            <needUri> : {
                *:*,
                connections: [<connectionUri>, <connectionUri>]
            }
            <needUri> : {
                *:*,
                connections: [<connectionUri>, <connectionUri>]
            }
        },
        theirNeeds: {
            <needUri>: {
                *:*,
                connections: [<connectionUri>, <connectionUri>] <--?
            }
        },
        connections: {
            <connectionUri> : {
                *:*,
                events: [<eventUri>, <eventUri>]
            }
            <connectionUri> : {
                *:*,
                events: [<eventUri>, <eventUri>]
            }
        }
        events: {
            <eventUri> : { *:* },
            <eventUri> : { *:* }
        }
     }
     */
}

async function fetchConnectionsOfNeedAndDispatch(
  needUri,
  curriedDispatch = () => undefined
) {
  const connectionUrisOfNeed = await won.getConnectionUrisOfNeed(
    needUri,
    needUri,
    true
  );
  const activeConnectionUris = connectionUrisOfNeed.filter(
    connUri => !isConnUriClosed(connUri)
  );
  curriedDispatch(
    wellFormedPayload({
      needUriForConnections: needUri,
      activeConnectionUrisLoading: activeConnectionUris,
    })
  );
  console.log("fetchConnectionAndDispatch for: ", activeConnectionUris);
  return urisToLookupMap(activeConnectionUris, uri =>
    fetchConnectionAndDispatch(uri, curriedDispatch)
  );
}

function fetchOwnNeedAndDispatch(needUri, curriedDispatch = () => undefined) {
  const needP = won
    .ensureLoaded(needUri, { requesterWebId: needUri }) //ensure loaded does net seem to be necessary as it is called within getNeed also the requesterWebId is not necessary for need requests
    .then(() => won.getNeed(needUri));
  needP.then(need =>
    curriedDispatch(wellFormedPayload({ ownNeeds: { [needUri]: need } }))
  );
  return needP;
}

function fetchConnectionAndDispatch(
  cnctUri,
  curriedDispatch = () => undefined
) {
  const cnctP = won.getNode(cnctUri);
  cnctP.then(connection =>
    curriedDispatch(
      wellFormedPayload({ connections: { [cnctUri]: connection } })
    )
  );
  return cnctP;
}

function fetchTheirNeedAndDispatch(needUri, curriedDispatch = () => undefined) {
  const needP = won.getNeed(needUri);
  needP.then(need =>
    curriedDispatch(wellFormedPayload({ theirNeeds: { [needUri]: need } }))
  );
  return needP;
}
