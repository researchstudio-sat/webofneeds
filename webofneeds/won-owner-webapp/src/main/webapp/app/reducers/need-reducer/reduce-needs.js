import { parseNeed } from "./parse-need.js";
import Immutable from "immutable";
import won from "../../won-es6.js";
import { addInactiveNeed, removeInactiveNeed } from "../../won-localstorage.js";

export function addNeed(needs, jsonldNeed, ownNeed) {
  const jsonldNeedImm = Immutable.fromJS(jsonldNeed);

  let newState;
  let parsedNeed = parseNeed(jsonldNeed, ownNeed);

  if (parsedNeed && parsedNeed.get("uri")) {
    if (ownNeed) {
      switch (parsedNeed.get("state")) {
        case won.WON.InactiveCompacted:
          addInactiveNeed(parsedNeed.get("uri"));
          break;
        case won.WON.ActiveCompacted:
          removeInactiveNeed(parsedNeed.get("uri"));
          break;
      }
    }
    let existingNeed = needs.get(parsedNeed.get("uri"));
    if (ownNeed && existingNeed) {
      // If need is already present and the
      // need is claimed as an own need we set
      // have to set it
      const isBeingCreated = existingNeed.get("isBeingCreated");
      if (isBeingCreated) {
        // replace it
        parsedNeed = parsedNeed.set(
          "connections",
          existingNeed.get("connections")
        );
        return needs.setIn([parsedNeed.get("uri")], parsedNeed);
      } else {
        // just be sure we mark it as own need
        return needs.setIn([parsedNeed.get("uri"), "ownNeed"], ownNeed);
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

export function addNeedInCreation(needs, needInCreation, needUri) {
  let newState;
  let need = Immutable.fromJS(needInCreation);

  if (need) {
    need = need.set("uri", needUri);
    need = need.set("ownNeed", true);
    need = need.set("isBeingCreated", true);
    need = need.set("connections", Immutable.Map());

    let type = undefined;
    let title = undefined;

    if (need.get("is")) {
      type = need.get("seeks")
        ? won.WON.BasicNeedTypeCombinedCompacted
        : won.WON.BasicNeedTypeSupplyCompacted;
      title = need.getIn(["is", "title"]);
      console.log("is title: ", title);
    }

    if (need.get("seeks")) {
      type = need.get("is")
        ? won.WON.BasicNeedTypeCombinedCompacted
        : won.WON.BasicNeedTypeDemandCompacted;
      title = need.getIn(["seeks", "title"]);
      console.log("seeks title: ", title);
    }

    need = need.set("type", type);
    need = need.set("title", title);

    let isWhatsAround = false;
    let isWhatsNew = false;

    if (
      need.getIn(["is", "whatsAround"]) ||
      need.getIn(["seeks", "whatsAround"])
    ) {
      isWhatsAround = true;
    }
    if (need.getIn(["is", "whatsNew"]) || need.getIn(["seeks", "whatsNew"])) {
      isWhatsNew = true;
    }

    need = need.set("isWhatsAround", isWhatsAround);
    need = need.set("isWhatsNew", isWhatsNew);

    newState = needs.setIn([needUri], need);
    console.log("need-reducer create new need: ", need.toJS());
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
    console.error("no need with needUri: <", needUri, ">");
    return state;
  }
  return state.setIn([needUri, "unread"], false);
}

export function changeNeedState(state, needUri, newState) {
  switch (newState) {
    case won.WON.InactiveCompacted:
      addInactiveNeed(needUri);
      break;
    case won.WON.ActiveCompacted:
      removeInactiveNeed(needUri);
      break;
  }
  return state.setIn([needUri, "state"], newState);
}
