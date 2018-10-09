import { getIn } from "../utils";
import won from "../won-es6";
import { getRandomWonId } from "../won-utils";
import { actionTypes } from "./actions";

export function createIdentity(identity, nodeUri) {
  return (dispatch, getState) => {
    const state = getState();
    if (!nodeUri) {
      nodeUri = getIn(state, ["config", "defaultNodeUri"]);
    }

    const publishedContentUri = nodeUri + "/need/" + getRandomWonId();
    const msgUri = nodeUri + "/event/" + getRandomWonId();

    const graph = {
      "@graph": [
        {
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
          "s:name": identity.displayName,
          "s:description": identity.aboutMe || undefined,
          "s:url": identity.website || undefined,
        },
      ],
    };

    const msg = won.buildMessageRdf(graph, {
      receiverNode: nodeUri, //mandatory
      senderNode: nodeUri, //mandatory
      msgType: won.WONMSG.createMessage, //mandatory
      publishedContentUri: publishedContentUri, //mandatory
      msgUri: msgUri,
    });

    msg["@context"]["@base"] = publishedContentUri;

    dispatch({
      type: actionTypes.identities.create,
      payload: { eventUri: msgUri, message: msg, needUri: publishedContentUri },
    });
  };
}
