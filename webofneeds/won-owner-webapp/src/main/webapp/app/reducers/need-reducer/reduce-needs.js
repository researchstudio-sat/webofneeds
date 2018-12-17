import { parseNeed } from "./parse-need.js";
import Immutable from "immutable";
import won from "../../won-es6.js";

export function addNeed(needs, jsonldNeed, isOwned) {
  const jsonldNeedImm = Immutable.fromJS(jsonldNeed);

  let newState;
  let parsedNeed = parseNeed(jsonldNeed, isOwned);

  if (parsedNeed && parsedNeed.get("uri")) {
    let existingNeed = needs.get(parsedNeed.get("uri"));
    const isExistingOwnedNeed = existingNeed && existingNeed.get("isOwned");

    if ((isOwned || isExistingOwnedNeed) && existingNeed) {
      // If need is already present and the
      // need is claimed as an own need we set
      // have to set it
      const isBeingCreated = existingNeed.get("isBeingCreated");
      const isLoading = existingNeed.get("isLoading");
      const toLoad = existingNeed.get("toLoad");

      if (isBeingCreated || isLoading || toLoad) {
        // replace it
        parsedNeed = parsedNeed
          .set("connections", existingNeed.get("connections"))
          .set("isOwned", true);
        return needs.setIn([parsedNeed.get("uri")], parsedNeed);
      } else {
        // just be sure we mark it as own need
        return needs.setIn([parsedNeed.get("uri"), "isOwned"], true);
      }
    } else if (!isOwned && existingNeed) {
      // If need is already present and the
      // need is claimed as a non own need
      const isLoading = existingNeed.get("isLoading");
      const toLoad = existingNeed.get("toLoad");

      if (isLoading || toLoad) {
        // replace it
        parsedNeed = parsedNeed
          .set("connections", existingNeed.get("connections"))
          .set("isOwned", false);
        return needs.setIn([parsedNeed.get("uri")], parsedNeed);
      } else {
        // just be sure we mark it as non own need
        return needs.setIn([parsedNeed.get("uri"), "isOwned"], false);
      }
    } else {
      return setIfNew(needs, parsedNeed.get("uri"), parsedNeed);
    }
  } else {
    console.error("Tried to add invalid need-object: ", jsonldNeedImm.toJS());
    newState = needs;
  }

  return newState;
}

export function addNeedInLoading(needs, needUri, state, isOwned) {
  console.debug("addNeedInLoading: ", needUri);
  const oldNeed = needs.get(needUri);
  if (oldNeed && !oldNeed.get("isLoading")) {
    return needs;
  } else {
    let need = Immutable.fromJS({
      uri: needUri,
      toLoad: false,
      isLoading: true,
      failedToLoad: false,
      isOwned: isOwned,
      state: state,
      connections: Immutable.Map(),
    });
    return needs.setIn([needUri], need);
  }
}

export function addTheirNeedInLoading(needs, needUri) {
  const oldNeed = needs.get(needUri);
  if (oldNeed && (oldNeed.get("isOwned") || !oldNeed.get("isLoading"))) {
    return needs;
  } else {
    let need = Immutable.fromJS({
      uri: needUri,
      toLoad: false,
      isLoading: true,
      failedToLoad: false,
      isOwned: false,
      connections: Immutable.Map(),
    });
    return needs.setIn([needUri], need);
  }
}

export function addOwnActiveNeedsInLoading(needs, needUris) {
  needUris &&
    needUris.size > 0 &&
    console.debug("addOwnActiveNeedsInLoading: ", needUris);
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
  needUris &&
    needUris.size > 0 &&
    console.debug("addOwnInactiveNeedsInLoading: ", needUris);
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
  needUris &&
    needUris.size > 0 &&
    console.debug("addOwnInactiveNeedsInLoading: ", needUris);
  let newState = needs;
  needUris &&
    needUris.forEach(needUri => {
      newState = addTheirNeedInLoading(newState, needUri);
    });
  return newState;
}

export function addOwnInactiveNeedsToLoad(needs, needUris) {
  needUris &&
    needUris.size > 0 &&
    console.debug("addOwnInactiveNeedsToLoad: ", needUris);
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

export function addNeedToLoad(needs, needUri, state, isOwned) {
  console.debug("addNeedToLoad: ", needUri);
  if (needs.get(needUri)) {
    return needs;
  } else {
    let need = Immutable.fromJS({
      uri: needUri,
      toLoad: true,
      isLoading: false,
      failedToLoad: false,
      isOwned: isOwned,
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

    newState = needs.setIn([needUri], need);
    console.debug("need-reducer create new need: ", need.toJS());
  } else {
    console.error("Tried to add invalid need-object: ", needInCreation);
    newState = needs;
  }
  return newState;
}

function setIfNew(state, path, obj) {
  return state.update(
    path,
    val =>
      val
        ? // we've seen this need before, no need to overwrite it
          val
        : // it's the first time we see this need -> add it
          Immutable.fromJS(obj)
  );
}

export function markNeedAsRead(state, needUri) {
  const need = state.get(needUri);

  if (!need) {
    console.error("No need with needUri: <", needUri, ">");
    return state;
  }
  return state.setIn([needUri, "unread"], false);
}

export function changeNeedState(state, needUri, newState) {
  return state.setIn([needUri, "state"], newState);
}
