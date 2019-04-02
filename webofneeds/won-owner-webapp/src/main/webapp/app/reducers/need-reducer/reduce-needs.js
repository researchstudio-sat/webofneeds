import { parseNeed } from "./parse-need.js";
import Immutable from "immutable";
import won from "../../won-es6.js";

export function addNeed(needs, jsonldNeed, isOwned) {
  let newState;
  let parsedNeed = parseNeed(jsonldNeed);

  if (parsedNeed && parsedNeed.get("uri")) {
    let existingNeed = needs.get(parsedNeed.get("uri"));
    const isExistingOwnedNeed = existingNeed && existingNeed.get("isOwned");

    if (existingNeed) {
      if (isOwned || isExistingOwnedNeed) {
        parsedNeed = parsedNeed
          .set("connections", existingNeed.get("connections"))
          .set("isOwned", true);
      } else if (!isOwned) {
        parsedNeed = parsedNeed
          .set("connections", existingNeed.get("connections"))
          .set("isOwned", false);
      }

      const heldNeedUris = parsedNeed.get("holds");
      if (heldNeedUris.size > 0) {
        heldNeedUris.map(needUri => {
          if (!needs.get(needUri) || !needs.getIn([needUri, "isOwned"])) {
            needs = addTheirNeedToLoad(needs, needUri);
          }
        });
      }

      const groupMemberUris = parsedNeed.get("groupMembers");
      if (groupMemberUris.size > 0) {
        groupMemberUris.map(needUri => {
          if (!needs.get(needUri) || !needs.getIn([needUri, "isOwned"])) {
            needs = addTheirNeedToLoad(needs, needUri);
          }
        });
      }
    }

    return needs.set(parsedNeed.get("uri"), parsedNeed);
  } else {
    console.error("Tried to add invalid need-object: ", jsonldNeed);
    newState = needs;
  }

  return newState;
}

function addNeedInLoading(needs, needUri, state) {
  const oldNeed = needs.get(needUri);
  if (oldNeed) {
    return needs;
  } else {
    let need = Immutable.fromJS({
      uri: needUri,
      state: state,
      connections: Immutable.Map(),
    });
    return needs.setIn([needUri], need);
  }
}

function addTheirNeedInLoading(needs, needUri) {
  const oldNeed = needs.get(needUri);
  if (oldNeed) {
    return needs;
  } else {
    let need = Immutable.fromJS({
      uri: needUri,
      connections: Immutable.Map(),
    });
    return needs.setIn([needUri], need);
  }
}

export function addTheirNeedToLoad(needs, needUri) {
  const oldNeed = needs.get(needUri);
  if (oldNeed) {
    return needs;
  } else {
    let need = Immutable.fromJS({
      uri: needUri,
      connections: Immutable.Map(),
    });
    return needs.setIn([needUri], need);
  }
}

export function addOwnActiveNeedsInLoading(needs, needUris) {
  let newState = needs;
  needUris &&
    needUris.forEach(needUri => {
      newState = addNeedInLoading(
        newState,
        needUri,
        won.WON.ActiveCompacted,
        true
      );
    });
  return newState;
}

export function addOwnInactiveNeedsInLoading(needs, needUris) {
  let newState = needs;
  needUris &&
    needUris.forEach(needUri => {
      newState = addNeedInLoading(
        newState,
        needUri,
        won.WON.InactiveCompacted,
        true
      );
    });
  return newState;
}

export function addTheirNeedsInLoading(needs, needUris) {
  let newState = needs;
  needUris &&
    needUris.forEach(needUri => {
      newState = addTheirNeedInLoading(newState, needUri);
    });
  return newState;
}

export function addOwnInactiveNeedsToLoad(needs, needUris) {
  let newState = needs;
  needUris &&
    needUris.forEach(needUri => {
      newState = addNeedToLoad(
        newState,
        needUri,
        won.WON.InactiveCompacted,
        true
      );
    });
  return newState;
}

function addNeedToLoad(needs, needUri, state) {
  if (needs.get(needUri)) {
    return needs;
  } else {
    let need = Immutable.fromJS({
      uri: needUri,
      state: state,
      connections: Immutable.Map(),
    });
    return needs.setIn([needUri], need);
  }
}

export function addNeedInCreation(needs, needInCreation, needUri) {
  let newState;
  let need = Immutable.fromJS(needInCreation);

  if (need) {
    need = need.set("uri", needUri);
    need = need.set("isOwned", true);
    need = need.set("isBeingCreated", true);
    need = need.set("connections", Immutable.Map());

    let title = undefined;

    if (need.get("content")) {
      title = need.getIn(["content", "title"]);
    }

    if (need.get("seeks")) {
      title = need.getIn(["seeks", "title"]);
    }

    need = need.set("humanReadable", title);
    need = need.set("content", Immutable.Map());
    newState = needs.setIn([needUri], need);
  } else {
    console.error("Tried to add invalid need-object: ", needInCreation);
    newState = needs;
  }
  return newState;
}

export function markNeedAsRead(state, needUri) {
  const need = state.get(needUri);

  if (!need) {
    console.error("No need with needUri: <", needUri, ">");
    return state;
  }
  return state.setIn([needUri, "unread"], false);
}
