import { generateIdString, get, getIn, delay } from "../utils";
import vocab from "../service/vocab.js";
import { actionTypes } from "./actions";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import {
  buildCloseMessage,
  buildConnectMessage,
} from "../won-message-utils.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as ownerApi from "../api/owner-api.js";
import won from "../won-es6";

function connectReview(dispatch, ownPersona, foreignPersona, connectMessage) {
  const senderSocketUri = atomUtils.getSocketUri(
    ownPersona,
    vocab.REVIEW.ReviewSocketCompacted
  );
  const targetSocketUri = atomUtils.getSocketUri(
    foreignPersona,
    vocab.REVIEW.ReviewSocketCompacted
  );

  if (!senderSocketUri) {
    throw new Error(
      `Persona ${get(ownPersona, "uri")} does not have a review socket`
    );
  }

  if (!targetSocketUri) {
    throw new Error(
      `Persona ${get(foreignPersona, "uri")} does not have a review socket`
    );
  }

  const cnctMsg = buildConnectMessage({
    connectMessage: connectMessage,
    socketUri: senderSocketUri,
    targetSocketUri: targetSocketUri,
  });

  //TODO: DELAY WORKAROUND TO FIX CONNECT ISSUES
  return delay(2000)
    .then(() => ownerApi.sendMessage(cnctMsg))
    .then(jsonResp =>
      won
        .wonMessageFromJsonLd(
          jsonResp.message,
          vocab.WONMSG.uriPlaceholder.event
        )
        .then(wonMessage =>
          dispatch({
            type: actionTypes.atoms.connectSockets,
            payload: {
              eventUri: jsonResp.messageUri,
              message: jsonResp.message,
              optimisticEvent: wonMessage,
              senderSocketUri: senderSocketUri,
              targetSocketUri: targetSocketUri,
            },
          })
        )
    );
}

export function connectPersona(atomUri, personaUri) {
  //TODO: ADDED BUT LET'S REPLACE THIS WITH OUR GENERIC CONNECT LOGIC

  return () => {
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
      });
  };
}

export function disconnectPersona(atomUri, personaUri) {
  return (dispatch, getState) => {
    const state = getState();
    const persona = generalSelectors.getAtom(personaUri)(state);
    const atom = generalSelectors.getAtom(atomUri)(state);

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
      .then(message => ownerApi.sendMessage(message))
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
    const ownAtom = generalSelectors.getOwnedAtomByConnectionUri(
      reviewableConnectionUri
    )(state);
    const connection =
      reviewableConnectionUri &&
      getIn(ownAtom, ["connections", reviewableConnectionUri]);

    const foreignAtomUri = get(connection, "targetAtomUri");
    const foreignAtom = generalSelectors.getAtom(foreignAtomUri)(state);

    const getPersona = atom => {
      const personaUri = atomUtils.getHeldByUri(atom);
      const persona = generalSelectors.getAtom(personaUri)(state);

      return persona;
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

    return connectReview(dispatch, ownPersona, foreignPersona, reviewRdf);
  };
}
