/**
 * Created by ksinger on 03.12.2015.
 */

import won from "./won-es6.js";

import { getRandomWonId } from "./won-utils.js";
import * as useCaseUtils from "./usecase-utils.js";

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
  ownedAtomUri,
  theirAtomUri,
  ownNodeUri,
  theirNodeUri,
  theirConnectionUri,
  rating
) {
  return new Promise(resolve => {
    const buildMessage = function(envelopeData) {
      //TODO: use event URI pattern specified by WoN node
      const eventUri =
        envelopeData[won.WONMSG.senderNode] + "/event/" + getRandomWonId();
      const message = new won.MessageBuilder(won.WONMSG.feedbackMessage) //TODO: Looks like a copy-paste-leftover from connect
        .eventURI(eventUri)
        .ownerDirection()
        .forEnvelopeData(envelopeData)
        .sentTimestamp(new Date().getTime().toString())
        .addRating(rating, msgToRateFor.connection.uri)
        .build();
      //const callback = createMessageCallbackForTargetAtomMessage(eventUri, won.EVENT.OPEN_SENT);
      return { eventUri: eventUri, message: message };
    };

    //fetch all datan needed
    won
      .getEnvelopeDataforConnection(
        msgToRateFor.connection.uri,
        ownedAtomUri,
        theirAtomUri,
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
  ownedAtomUri,
  theirAtomUri,
  ownNodeUri,
  theirNodeUri,
  theirConnectionUri
) {
  const buildMessage = function(envelopeData) {
    //TODO: use event URI pattern specified by WoN node
    const eventUri =
      envelopeData[won.WONMSG.senderNode] + "/event/" + getRandomWonId();
    const message = new won.MessageBuilder(won.WONMSG.closeMessage)
      .eventURI(eventUri)
      .forEnvelopeData(envelopeData)
      .ownerDirection()
      .sentTimestamp(new Date().getTime().toString())
      .build();
    //const callback = createMessageCallbackForTargetAtomMessage(eventUri, won.EVENT.CLOSE_SENT);
    return { eventUri: eventUri, message: message };
  };

  //fetch all datan needed
  return won
    .getEnvelopeDataforConnection(
      connectionUri,
      ownedAtomUri,
      theirAtomUri,
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
export function buildCloseAtomMessage(atomUri, wonNodeUri) {
  const buildMessage = function(envelopeData) {
    const eventUri =
      envelopeData[won.WONMSG.senderNode] + "/event/" + getRandomWonId();
    const message = new won.MessageBuilder(won.WONMSG.closeAtomMessage)
      .eventURI(eventUri)
      .recipientNode(wonNodeUri)
      .ownerDirection()
      .sentTimestamp(new Date().getTime().toString())
      .forEnvelopeData(envelopeData)
      .build();

    return { eventUri: eventUri, message: message };
  };

  return won
    .getEnvelopeDataForAtom(atomUri, wonNodeUri)
    .then(
      envelopeData => buildMessage(envelopeData),
      () => won.reportError("cannot close atom " + atomUri)
    );
}

export function buildDeleteAtomMessage(atomUri, wonNodeUri) {
  const buildMessage = function(envelopeData) {
    const eventUri =
      envelopeData[won.WONMSG.senderNode] + "/event/" + getRandomWonId();
    const message = new won.MessageBuilder(won.WONMSG.deleteAtomMessage)
      .eventURI(eventUri)
      .recipientNode(wonNodeUri)
      .ownerDirection()
      .sentTimestamp(new Date().getTime().toString())
      .forEnvelopeData(envelopeData)
      .build();

    return { eventUri: eventUri, message: message };
  };

  return won
    .getEnvelopeDataForAtom(atomUri, wonNodeUri)
    .then(
      envelopeData => buildMessage(envelopeData),
      () => won.reportError("cannot delete atom " + atomUri)
    );
}

export function buildOpenAtomMessage(atomUri, wonNodeUri) {
  const buildMessage = function(envelopeData) {
    const eventUri =
      envelopeData[won.WONMSG.senderNode] + "/event/" + getRandomWonId();
    const message = new won.MessageBuilder(won.WONMSG.activateAtomMessage)
      .eventURI(eventUri)
      .recipientNode(wonNodeUri)
      .ownerDirection()
      .sentTimestamp(new Date().getTime().toString())
      .forEnvelopeData(envelopeData)
      .build();

    return { eventUri: eventUri, message: message };
  };

  return won
    .getEnvelopeDataForAtom(atomUri, wonNodeUri)
    .then(
      envelopeData => buildMessage(envelopeData),
      () => won.reportError("cannot close atom " + atomUri)
    );
}

/**
 * Builds json-ld for a connect-message in reaction to an atom.
 * @param connectionUri
 * @param textMessage
 * @returns {{eventUri, message}|*}
 */
export function buildConnectMessage({
  ownedAtomUri,
  theirAtomUri,
  ownNodeUri,
  theirNodeUri,
  connectMessage,
  optionalOwnConnectionUri,
  socketUri,
  targetSocketUri,
}) {
  const envelopeData = won.getEnvelopeDataforNewConnection(
    ownedAtomUri,
    theirAtomUri,
    ownNodeUri,
    theirNodeUri
  );
  if (optionalOwnConnectionUri) {
    envelopeData[won.WONMSG.sender] = optionalOwnConnectionUri;
  }
  if (socketUri) {
    envelopeData[won.WONMSG.senderSocket] = socketUri;
  }
  if (targetSocketUri) {
    envelopeData[won.WONMSG.recipientSocket] = targetSocketUri;
  }
  //TODO: use event URI pattern specified by WoN node
  const eventUri =
    envelopeData[won.WONMSG.senderNode] + "/event/" + getRandomWonId();
  const messageBuilder = new won.MessageBuilder(won.WONMSG.connectMessage);
  messageBuilder.eventURI(eventUri);
  messageBuilder.forEnvelopeData(envelopeData);
  //do not set sockets: connect the default sockets with each other
  if (connectMessage && typeof connectMessage === "string") {
    messageBuilder.textMessage(connectMessage);
  } else if (connectMessage) {
    messageBuilder.mergeIntoContentGraph(connectMessage);
  }
  messageBuilder.ownerDirection();
  messageBuilder.sentTimestamp(new Date().getTime().toString());
  const message = messageBuilder.build();

  return { eventUri: eventUri, message: message };
}

export function buildChatMessage({
  chatMessage,
  additionalContent,
  referencedContentUris, //this is a map of corresponding uris to be e.g. proposes or retracted... (it already includes the correct uri -> remoteUri for received messages, and uri for sent messages)
  connectionUri,
  ownedAtomUri,
  theirAtomUri,
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
    ownedAtomUri,
    theirAtomUri,
    ownNodeUri,
    theirNodeUri,
    theirConnectionUri
  );

  const messageP = Promise.all([envelopeDataP, jsonldGraphPayloadP]).then(
    ([envelopeData, graphPayload]) => {
      const eventUri =
        envelopeData[won.WONMSG.senderNode] + "/event/" + getRandomWonId();

      /*
             * Build the json-ld message that's signed on the owner-server
             * and then send to the won-node.
             */
      const wonMessageBuilder = new won.MessageBuilder(
        won.WONMSG.connectionMessage
      )
        .forEnvelopeData(envelopeData)
        .ownerDirection()
        .sentTimestamp(new Date().getTime().toString());

      if (isTTL && graphPayload) {
        wonMessageBuilder.mergeIntoContentGraph(graphPayload);
      } else if (
        !isTTL &&
        (chatMessage || additionalContent || referencedContentUris)
      ) {
        //add the chatMessage as normal text message
        if (chatMessage) {
          wonMessageBuilder.addContentGraphData(won.WONCON.text, chatMessage);
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
                    "https://w3id.org/won/modification#retracts"
                  ] = uris;
                  break;
                case "rejects":
                  contentNode["https://w3id.org/won/agreement#rejects"] = uris;
                  break;
                case "proposes":
                  contentNode["https://w3id.org/won/agreement#proposes"] = uris;
                  break;
                case "claims":
                  contentNode["https://w3id.org/won/agreement#claims"] = uris;
                  break;
                case "proposesToCancel":
                  contentNode[
                    "https://w3id.org/won/agreement#proposesToCancel"
                  ] = uris;
                  break;
                case "accepts":
                  contentNode["https://w3id.org/won/agreement#accepts"] = uris;
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
  ownedAtomUri,
  theirAtomUri,
  ownNodeUri,
  theirNodeUri,
  theirConnectionUri,
  chatMessage
) {
  const messageP = won
    .getEnvelopeDataforConnection(
      connectionUri,
      ownedAtomUri,
      theirAtomUri,
      ownNodeUri,
      theirNodeUri,
      theirConnectionUri
    )
    .then(envelopeData => {
      //TODO: use event URI pattern specified by WoN node
      const eventUri =
        envelopeData[won.WONMSG.senderNode] + "/event/" + getRandomWonId();
      const message = new won.MessageBuilder(won.WONMSG.openMessage)
        .eventURI(eventUri)
        .forEnvelopeData(envelopeData)
        .textMessage(chatMessage)
        .ownerDirection()
        .sentTimestamp(new Date().getTime().toString())
        .build();

      return {
        eventUri,
        message,
      };
    });

  return messageP;
}

export async function buildEditMessage(editedAtomData, oldAtom, wonNodeUri) {
  const atomUriToEdit = oldAtom && oldAtom.get("uri");

  //if type  create -> use atomBuilder as well
  const prepareContentNodeData = async editedAtomData => ({
    // Adds all fields from atomDataIsOrSeeks:
    // title, description, tags, location,...
    ...editedAtomData,

    publishedContentUri: atomUriToEdit, //mandatory
    arbitraryJsonLd: editedAtomData.ttl
      ? await won.ttlToJsonLd(editedAtomData.ttl)
      : [],
  });

  let contentRdf = won.buildAtomRdf({
    content: editedAtomData.content
      ? await prepareContentNodeData(editedAtomData.content)
      : undefined,
    seeks: editedAtomData.seeks
      ? await prepareContentNodeData(editedAtomData.seeks)
      : undefined,
    useCase: editedAtomData.useCase ? editedAtomData.useCase : undefined, //only needed for atom building
    // FIXME: find a better way to include atom details that are not part of is or seeks
    socket: editedAtomData.socket,
  });

  const msgUri = wonNodeUri + "/event/" + getRandomWonId(); //mandatory
  const msgJson = won.buildMessageRdf(contentRdf, {
    recipientNode: wonNodeUri, //mandatory
    senderNode: wonNodeUri, //mandatory
    msgType: won.WONMSG.replaceMessage, //mandatory
    publishedContentUri: atomUriToEdit, //mandatory
    msgUri: msgUri,
  });
  //add the @base definition to the @context so we can use #fragments in the atom structure
  msgJson["@context"]["@base"] = atomUriToEdit;

  return {
    message: msgJson,
    eventUri: msgUri,
    atomUri: atomUriToEdit,
  };
}

/**
 *
 * @param atomData
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
 *    atomUri: string
 * }}
 */
export async function buildCreateMessage(atomData, wonNodeUri) {
  //Check for is and seeks
  /*
    if(!atomData.type || !atomData.title)
        throw new Error('Tried to create post without type or title. ', atomData);
    */

  const publishedContentUri = wonNodeUri + "/atom/" + getRandomWonId();

  //if type  create -> use atomBuilder as well
  const prepareContentNodeData = async atomData => ({
    // Adds all fields from atomDataIsOrSeeks:
    // title, description, tags, location,...
    ...atomData,

    publishedContentUri: publishedContentUri, //mandatory
    arbitraryJsonLd: atomData.ttl ? await won.ttlToJsonLd(atomData.ttl) : [],
  });

  let contentRdf = won.buildAtomRdf({
    content: atomData.content
      ? await prepareContentNodeData(atomData.content)
      : undefined,
    seeks: atomData.seeks
      ? await prepareContentNodeData(atomData.seeks)
      : undefined,
    useCase: atomData.useCase ? atomData.useCase : undefined, //only needed for atom building
    // FIXME: find a better way to include atom details that are not part of is or seeks
    socket: atomData.socket,
  });

  const msgUri = wonNodeUri + "/event/" + getRandomWonId(); //mandatory
  const msgJson = won.buildMessageRdf(contentRdf, {
    recipientNode: wonNodeUri, //mandatory
    senderNode: wonNodeUri, //mandatory
    msgType: won.WONMSG.createMessage, //mandatory
    publishedContentUri: publishedContentUri, //mandatory
    msgUri: msgUri,
  });
  //add the @base definition to the @context so we can use #fragments in the atom structure
  msgJson["@context"]["@base"] = publishedContentUri;
  return {
    message: msgJson,
    eventUri: msgUri,
    atomUri: publishedContentUri,
  };
}
