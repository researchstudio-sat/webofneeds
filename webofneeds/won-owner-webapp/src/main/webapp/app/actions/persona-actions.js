import { getIn } from "../utils";
import won from "../won-es6";
import { getRandomWonId } from "../won-utils";
import { actionTypes } from "./actions";

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
      "won:hasFacet": {
        "@id": "#holderFacet",
        "@type": "won:HolderFacet",
      },
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

export function reviewPersona(ownedPersonaUri, foreignPersonaUri, review) {
  return (dispatch, getState) => {
    review;
    getState;
  };
}
