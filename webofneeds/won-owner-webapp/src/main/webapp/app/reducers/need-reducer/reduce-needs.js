import { parseNeed } from "./parse-need.js";
import Immutable from "immutable";
import { get } from "../../utils.js";

export function addNeed(needs, jsonldNeed) {
  let newState;
  let parsedNeed = parseNeed(jsonldNeed);
  const parsedNeedUri = get(parsedNeed, "uri");

  if (parsedNeedUri) {
    let existingNeed = get(needs, parsedNeedUri);

    if (existingNeed) {
      parsedNeed = parsedNeed.set(
        "connections",
        get(existingNeed, "connections")
      );

      const heldNeedUris = get(parsedNeed, "holds");
      if (heldNeedUris.size > 0) {
        heldNeedUris.map(needUri => {
          if (!get(needs, needUri)) {
            needs = addNeedStub(needs, needUri);
          }
        });
      }

      const groupMemberUris = get(parsedNeed, "groupMembers");
      if (groupMemberUris.size > 0) {
        groupMemberUris.map(needUri => {
          if (!get(needs, needUri)) {
            needs = addNeedStub(needs, needUri);
          }
        });
      }
    }

    return needs.set(parsedNeedUri, parsedNeed);
  } else {
    console.error("Tried to add invalid need-object: ", jsonldNeed);
    newState = needs;
  }

  return newState;
}

/**
 * Adds a need-stub into the need-redux-state, needed to get Posts that are not loaded/loading to show up as skeletons
 * Checks if stub/need already exists, if so do nothing
 * @param needs redux need state
 * @param needUri stub accessible under uri
 * @param state not mandatory will be set undefined if not set, otherwise the needState is stored (e.g. Active/Inactive)
 * @returns {*}
 */
export function addNeedStub(needs, needUri, state) {
  if (get(needs, needUri)) {
    return needs;
  } else {
    return needs.setIn(
      [needUri],
      Immutable.fromJS({
        uri: needUri,
        state: state,
        connections: Immutable.Map(),
      })
    );
  }
}

/**
 * Adds need-stubs into the need-redux-state, needed to get Posts that are not loaded/loading to show up as skeletons
 * Checks if stub/need already exists, if so do nothing
 * @param needs redux need state
 * @param needUris stub accessible under uris
 * @param state not mandatory will be set undefined if not set, otherwise the needState is stored (e.g. Active/Inactive)
 * @returns {*}
 */
export function addNeedStubs(needs, needUris, state) {
  let newState = needs;
  needUris &&
    needUris.forEach(needUri => {
      newState = addNeedStub(newState, needUri, state);
    });
  return newState;
}

export function addNeedInCreation(needs, needInCreation, needUri) {
  let newState;
  let need = Immutable.fromJS(needInCreation);

  if (need) {
    need = need.set("uri", needUri);
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
