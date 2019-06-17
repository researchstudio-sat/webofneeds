import { getIn, get, generateIdString } from "../utils";
import won from "../won-es6";
import * as wonUtils from "../won-utils.js";
import { actionTypes } from "./actions";
import { getOwnedAtomByConnectionUri } from "../selectors/general-selectors";
import { getOwnedConnectionByUri } from "../selectors/connection-selectors";
import { buildConnectMessage, buildCloseMessage } from "../won-message-utils";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as ownerApi from "../owner-api.js";

export function createPersona(persona, nodeUri) {
  return (dispatch, getState) => {
    const state = getState();
    if (!nodeUri) {
      nodeUri = getIn(state, ["config", "defaultNodeUri"]);
    }

    const publishedContentUri = nodeUri + "/atom/" + wonUtils.getRandomWonId();
    const msgUri = nodeUri + "/event/" + wonUtils.getRandomWonId();

    //FIXME: THIS SHOULD NOT USE ANY OF THE CODE BELOW BUT EXECUTE OUR ALREADY PRESENT ATOM-CREATION WITH A GIVEN DRAFT INSTEAD
    const graph = {
      "@id": publishedContentUri,
      "@type": ["won:Atom", "won:Persona"],
      "won:socket": [
        {
          "@id": "#holderSocket",
          "won:socketDefinition": { "@id": "hold:HolderSocket" },
        },
        {
          "@id": "#reviewSocket",
          "won:socketDefinition": { "@id": "review:ReviewSocket" },
        },
        {
          "@id": "#buddySocket",
          "won:socketDefinition": { "@id": "buddy:BuddySocket" },
        },
      ],
      "match:flag": [
        { "@id": "match:NoHintForCounterpart" },
        { "@id": "match:NoHintForMe" },
      ],
      "s:name": persona.displayName,
      "s:description": persona.aboutMe || undefined,
      "s:url": persona.website || undefined,
    };
    const graphEnvelope = {
      "@graph": [graph],
    };

    const msg = won.buildMessageRdf(graphEnvelope, {
      recipientNode: nodeUri, //mandatory
      senderNode: nodeUri, //mandatory
      msgType: won.WONMSG.createMessage, //mandatory
      publishedContentUri: publishedContentUri, //mandatory
      msgUri: msgUri,
    });

    msg["@context"]["@base"] = publishedContentUri;

    dispatch({
      type: actionTypes.personas.create,
      payload: {
        eventUri: msgUri,
        message: msg,
        atomUri: publishedContentUri,
        persona: graph,
      },
    });
  };
}

async function connectReview(
  dispatch,
  ownPersona,
  foreignPersona,
  connectMessage,
  connectionUri = undefined
) {
  const socketUri = atomUtils.getSocketUri(
    ownPersona,
    won.REVIEW.ReviewSocketCompacted
  );
  const targetSocketUri = atomUtils.getSocketUri(
    foreignPersona,
    won.REVIEW.ReviewSocketCompacted
  );

  if (!socketUri) {
    throw new Error(
      `Persona ${ownPersona.get("uri")} does not have a review socket`
    );
  }

  if (!targetSocketUri) {
    throw new Error(
      `Persona ${foreignPersona.get("uri")} does not have a review socket`
    );
  }

  const cnctMsg = buildConnectMessage({
    ownedAtomUri: ownPersona.get("uri"),
    theirAtomUri: foreignPersona.get("uri"),
    ownNodeUri: ownPersona.get("nodeUri"),
    theirNodeUri: foreignPersona.get("nodeUri"),
    connectMessage: connectMessage,
    optionalOwnConnectionUri: connectionUri,
    socketUri: socketUri,
    targetSocketUri: targetSocketUri,
  });
  const optimisticEvent = await won.wonMessageFromJsonLd(cnctMsg.message);
  dispatch({
    type: actionTypes.atoms.connect,
    payload: {
      eventUri: cnctMsg.eventUri,
      message: cnctMsg.message,
      ownConnectionUri: connectionUri,
      optimisticEvent: optimisticEvent,
      socketUri: socketUri,
      targetSocketUri: targetSocketUri,
    },
  });
}

export function connectPersona(atomUri, personaUri) {
  return dispatch => {
    return ownerApi
      .serverSideConnect(
        `${personaUri}#holderSocket`,
        `${atomUri}#holdableSocket`
      )
      .then(async response => {
        if (!response.ok) {
          const errorMsg = await response.text();
          throw new Error(`Could not connect identity: ${errorMsg}`);
        }
        dispatch({
          type: actionTypes.personas.connect,
          payload: {
            atomUri: atomUri,
            personaUri: personaUri,
          },
        });
      });
  };
}

export function disconnectPersona(atomUri, personaUri) {
  return (dispatch, getState) => {
    const state = getState();
    const persona = getIn(state, ["atoms", personaUri]);
    const atom = getIn(state, ["atoms", atomUri]);

    const connectionUri = get(persona, "connections")
      .filter(connection => {
        const socketUri = get(connection, "socketUri");
        const socketType = getIn(atom, ["content", "sockets", socketUri]);
        return (
          get(connection, "targetAtomUri") === atomUri &&
          socketType === won.HOLD.HolderSocketCompacted
        );
      })
      .keySeq()
      .first();

    const connection = getOwnedConnectionByUri(state, connectionUri);

    buildCloseMessage(
      connectionUri,
      personaUri,
      atomUri,
      persona.get("nodeUri"),
      atom.get("nodeUri"),
      connection.get("targetConnectionUri")
    ).then(({ eventUri, message }) => {
      dispatch({
        type: actionTypes.connections.close,
        payload: {
          connectionUri,
          eventUri,
          message,
        },
      });
    });
  };
}

export function reviewPersona(reviewableConnectionUri, review) {
  return (dispatch, getState) => {
    const state = getState();
    const connection = getOwnedConnectionByUri(state, reviewableConnectionUri);

    const ownAtom = getOwnedAtomByConnectionUri(state, reviewableConnectionUri);
    const foreignAtomUri = get(connection, "targetAtomUri");
    const foreignAtom = getIn(state, ["atoms", foreignAtomUri]);

    const getPersona = atom => {
      const personaUri = get(atom, "heldBy");
      const persona = state.getIn(["atoms", personaUri]);

      return persona;
    };

    const getConnection = (ownPersona, foreignPersona) => {
      return get(ownPersona, "connections")
        .filter(connection => {
          const socketUri = get(connection, "socketUri");
          const socketType = getIn(ownPersona, [
            "content",
            "sockets",
            socketUri,
          ]);
          return (
            get(connection, "targetAtomUri") === get(foreignPersona, "uri") &&
            socketType === won.REVIEW.ReviewSocketCompacted
          );
        })
        .keySeq()
        .first();
    };

    const ownPersona = getPersona(ownAtom);
    const foreignPersona = getPersona(foreignAtom);
    const identifier = "review";
    const reviewRdf = {
      "s:review": {
        "@type": "s:Review",
        "@id":
          reviewableConnectionUri && identifier
            ? reviewableConnectionUri +
              "/" +
              identifier +
              "/" +
              generateIdString(10)
            : undefined,

        "s:about": foreignPersona.get("uri"),
        "s:author": ownPersona.get("uri"),
        "s:reviewRating": {
          "@type": "s:Rating",
          "@id":
            reviewableConnectionUri && identifier
              ? reviewableConnectionUri +
                "/" +
                identifier +
                "/" +
                generateIdString(10)
              : undefined,
          "s:bestRating": { "@value": 5, "@type": "xsd:int" }, //not necessary but possible
          "s:ratingValue": { "@value": review.value, "@type": "xsd:int" },
          "s:worstRating": { "@value": 1, "@type": "xsd:int" }, //not necessary but possible
        },
        "s:description": review.message,
      },
    };

    connectReview(
      dispatch,
      ownPersona,
      foreignPersona,
      reviewRdf,
      getConnection(ownPersona, foreignPersona)
    );
  };
}
