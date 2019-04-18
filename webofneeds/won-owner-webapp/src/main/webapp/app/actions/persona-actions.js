import { getIn, get, generateIdString } from "../utils";
import won from "../won-es6";
import { getRandomWonId } from "../won-utils";
import { actionTypes } from "./actions";
import { getOwnedAtomByConnectionUri } from "../selectors/general-selectors";
import { getOwnedConnectionByUri } from "../selectors/connection-selectors";
import { buildConnectMessage, buildCloseMessage } from "../won-message-utils";

export function createPersona(persona, nodeUri) {
  return (dispatch, getState) => {
    const state = getState();
    if (!nodeUri) {
      nodeUri = getIn(state, ["config", "defaultNodeUri"]);
    }

    const publishedContentUri = nodeUri + "/atom/" + getRandomWonId();
    const msgUri = nodeUri + "/event/" + getRandomWonId();

    //FIXME: THIS SHOULD NOT USE ANY OF THE CODE BELOW BUT EXECUTE OUR ALREADY PRESENT ATOM-CREATION WITH A GIVEN DRAFT INSTEAD
    const graph = {
      "@id": publishedContentUri,
      "@type": ["won:Atom", "won:Persona"],
      "won:socket": [
        {
          "@id": "#holderSocket",
          "@type": "won:HolderSocket",
        },
        {
          "@id": "#reviewSocket",
          "@type": "won:ReviewSocket",
        },
      ],
      "won:flag": [
        { "@id": "won:NoHintForCounterpart" },
        { "@id": "won:NoHintForMe" },
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
  const getSocket = persona => {
    const reviewSocket = persona
      .getIn(["content", "sockets"])
      .filter(socketType => socketType == "won:ReviewSocket")
      .keySeq()
      .first();

    if (!reviewSocket) {
      throw new Error(
        `Persona ${persona.get("uri")} does not have a review socket`
      );
    }
    return reviewSocket;
  };

  const cnctMsg = buildConnectMessage({
    ownedAtomUri: ownPersona.get("uri"),
    theirAtomUri: foreignPersona.get("uri"),
    ownNodeUri: ownPersona.get("nodeUri"),
    theirNodeUri: foreignPersona.get("nodeUri"),
    connectMessage: connectMessage,
    optionalOwnConnectionUri: connectionUri,
    ownSocket: getSocket(ownPersona),
    theirSocket: getSocket(foreignPersona),
  });
  const optimisticEvent = await won.wonMessageFromJsonLd(cnctMsg.message);
  dispatch({
    type: actionTypes.atoms.connect,
    payload: {
      eventUri: cnctMsg.eventUri,
      message: cnctMsg.message,
      ownConnectionUri: connectionUri,
      optimisticEvent: optimisticEvent,
    },
  });
}

export function connectPersona(atomUri, personaUri) {
  return async dispatch => {
    const response = await fetch("rest/action/connect", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify([
        {
          pending: false,
          socket: `${personaUri}#holderSocket`,
        },
        {
          pending: false,
          socket: `${atomUri}#holdableSocket`,
        },
      ]),
      credentials: "include",
    });
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
  };
}

export function disconnectPersona(atomUri, personaUri) {
  return (dispatch, getState) => {
    const state = getState();
    const persona = state.getIn(["atoms", personaUri]);
    const atom = state.getIn(["atoms", atomUri]);

    const connectionUri = persona
      .get("connections")
      .filter(
        connection =>
          connection.get("targetAtomUri") == atom.get("uri") &&
          connection.get("socket") == won.WON.HolderSocketCompacted
      )
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
      return ownPersona
        .get("connections")
        .filter(
          connection =>
            connection.get("targetAtomUri") == foreignPersona.get("uri") &&
            connection.get("socket") == won.WON.ReviewSocket
        )
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
