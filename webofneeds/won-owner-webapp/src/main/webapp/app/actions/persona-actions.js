import { generateIdString, get, getIn } from "../utils";
import vocab from "../service/vocab.js";
import { actionTypes } from "./actions";
import { getOwnedAtomByConnectionUri } from "../redux/selectors/general-selectors";
import { getOwnedConnectionByUri } from "../redux/selectors/connection-selectors";
import { buildCloseMessage, buildConnectMessage } from "../won-message-utils";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as ownerApi from "../api/owner-api.js";

async function connectReview(
  dispatch,
  ownPersona,
  foreignPersona,
  connectMessage,
  connectionUri = undefined
) {
  const socketUri = atomUtils.getSocketUri(
    ownPersona,
    vocab.REVIEW.ReviewSocketCompacted
  );
  const targetSocketUri = atomUtils.getSocketUri(
    foreignPersona,
    vocab.REVIEW.ReviewSocketCompacted
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
    connectMessage: connectMessage,
    socketUri: socketUri,
    targetSocketUri: targetSocketUri,
  });

  ownerApi.sendMessage(cnctMsg.message).then(jsonResp => {
    dispatch({
      type: actionTypes.atoms.connect,
      payload: {
        eventUri: jsonResp.messageUri,
        message: jsonResp.message,
        ownConnectionUri: connectionUri,
        atomUri: get(ownPersona, "uri"),
        targetAtomUri: get(foreignPersona, "uri"),
        socketUri: socketUri,
        targetSocketUri: targetSocketUri,
      },
    });
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

    const connection = get(persona, "connections").find(conn => {
      const socketUri = get(conn, "targetSocketUri");
      const socketType = getIn(atom, ["content", "sockets", socketUri]);
      return (
        get(conn, "targetAtomUri") === atomUri &&
        socketType === vocab.HOLD.HoldableSocketCompacted
      );
    });

    const socketUri = get(connection, "socketUri");
    const targetSocketUri = get(connection, "targetSocketUri");
    const connectionUri = get(connection, "uri");

    buildCloseMessage(socketUri, targetSocketUri)
      .then(({ message }) => ownerApi.sendMessage(message))
      .then(jsonResp => {
        dispatch({
          type: actionTypes.connections.close,
          payload: {
            connectionUri: connectionUri,
            eventUri: jsonResp.messageUri,
            message: jsonResp.message,
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
      const personaUri = atomUtils.getHeldByUri(atom);
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
            socketType === vocab.REVIEW.ReviewSocketCompacted
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
