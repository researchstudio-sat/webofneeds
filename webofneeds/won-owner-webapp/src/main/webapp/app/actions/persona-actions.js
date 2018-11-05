import { getIn, get } from "../utils";
import won from "../won-es6";
import { getRandomWonId } from "../won-utils";
import { actionTypes } from "./actions";
import { getOwnedNeedByConnectionUri } from "../selectors/general-selectors";
import { getOwnedConnectionByUri } from "../selectors/connection-selectors";

export function createPersona(persona, nodeUri) {
  return (dispatch, getState) => {
    const state = getState();
    if (!nodeUri) {
      nodeUri = getIn(state, ["config", "defaultNodeUri"]);
    }

    const publishedContentUri = nodeUri + "/need/" + getRandomWonId();
    const msgUri = nodeUri + "/event/" + getRandomWonId();

    const graph = {
      "@id": publishedContentUri,
      "@type": ["won:Need", "won:Persona"],
      "won:hasFacet": [
        {
          "@id": "#holderFacet",
          "@type": "won:HolderFacet",
        },
        {
          "@id": "#reviewFacet",
          "@type": "won:ReviewFacet",
        },
      ],
      "won:hasFlag": [
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
      receiverNode: nodeUri, //mandatory
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
        needUri: publishedContentUri,
        persona: graph,
      },
    });
  };
}

export function reviewPersona(reviewableConnectionUri, review) {
  return (dispatch, getState) => {
    const state = getState();
    const connection = getOwnedConnectionByUri(state, reviewableConnectionUri);

    const ownNeed = getOwnedNeedByConnectionUri(state, reviewableConnectionUri);
    const foreignNeedUri = get(connection, "remoteNeedUri");
    const foreignNeed = getIn(state, ["needs", foreignNeedUri]);

    const ownPersonaUri = get(ownNeed, "heldBy");
    const foreignPersonaUri = get(foreignNeed, "heldBy");

    if (!ownPersonaUri) {
      throw new Error(
        `No own persona found for rating when looking at ${reviewableConnectionUri}`
      );
    }

    if (!foreignPersonaUri) {
      throw new Error(
        `No foreign persona found for rating when looking at ${reviewableConnectionUri}`
      );
    }

    console.info(
      `${ownPersonaUri} reviewing ${foreignPersonaUri} with value ${
        review.value
      } and message "${review.message}"`
    );
  };
}
