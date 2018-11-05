/**
 * Created by ksinger on 22.01.2016.
 */

import { createSelector } from "reselect";

import Immutable from "immutable";
import won from "../won-es6.js";
import { decodeUriComponentProperly, getIn } from "../utils.js";
import Color from "color";

export const selectLastUpdateTime = state => state.get("lastUpdateTime");
export const selectRouterParams = state =>
  getIn(state, ["router", "currentParams"]);

export const selectAllNeeds = state => state.get("needs");
export const selectAllOwnNeeds = state =>
  selectAllNeeds(state).filter(need => need.get("ownNeed"));
export const selectAllTheirNeeds = state =>
  selectAllNeeds(state).filter(need => !need.get("ownNeed"));

export function selectAllPosts(state) {
  const needs = selectAllNeeds(state);
  return needs.filter(need => {
    if (!need.get("types")) return true;

    return Immutable.is(need.get("types"), Immutable.Set(["won:Need"]));
  });
}

export const selectAllOwnPosts = state =>
  selectAllPosts(state).filter(need => need.get("ownNeed"));

export function selectOpenNeeds(state) {
  const allOwnNeeds = selectAllOwnNeeds(state);
  return (
    allOwnNeeds &&
    allOwnNeeds.filter(post => post.get("state") === won.WON.ActiveCompacted)
  );
}

export function selectOpenPosts(state) {
  const allOwnNeeds = selectAllOwnPosts(state);
  return (
    allOwnNeeds &&
    allOwnNeeds.filter(post => post.get("state") === won.WON.ActiveCompacted)
  );
}

export function selectAllOpenPosts(state) {
  const allPosts = selectAllPosts(state);
  return (
    allPosts &&
    allPosts.filter(post => post.get("state") === won.WON.ActiveCompacted)
  );
}

export function selectClosedNeeds(state) {
  const allOwnNeeds = selectAllOwnNeeds(state);
  return (
    allOwnNeeds &&
    allOwnNeeds.filter(
      post =>
        post.get("state") === won.WON.InactiveCompacted &&
        !(post.get("isWhatsAround") || post.get("isWhatsNew"))
    )
  ); //Filter whatsAround and whatsNew needs automatically
}

export function selectClosedPosts(state) {
  const allOwnNeeds = selectAllOwnPosts(state);
  return (
    allOwnNeeds &&
    allOwnNeeds.filter(
      post =>
        post.get("state") === won.WON.InactiveCompacted &&
        !(post.get("isWhatsAround") || post.get("isWhatsNew"))
    )
  ); //Filter whatsAround and whatsNew needs automatically
}

export function selectNeedsInCreationProcess(state) {
  const allOwnNeeds = selectAllOwnNeeds(state);
  // needs that have been created but are not confirmed by the server yet
  return allOwnNeeds && allOwnNeeds.filter(post => post.get("isBeingCreated"));
}

export const selectIsConnected = state =>
  !state.getIn(["messages", "reconnecting"]) &&
  !state.getIn(["messages", "lostConnection"]);

/**
 * Get the need for a given connectionUri
 * @param state to retrieve data from
 * @param connectionUri to find corresponding need for
 */
export function selectNeedByConnectionUri(state, connectionUri) {
  let needs = selectAllOwnNeeds(state); //we only check own needs as these are the only ones who have connections stored
  return needs
    .filter(need => need.getIn(["connections", connectionUri]))
    .first();
}

export const selectOpenConnectionUri = createSelector(
  selectRouterParams,
  routerParams => {
    //de-escaping is lost in transpiling if not done in two steps :|
    const openConnectionUri = decodeUriComponentProperly(
      routerParams["connectionUri"] || routerParams["openConversation"]
    );

    if (openConnectionUri) {
      return openConnectionUri;
    } else {
      return undefined;
    }
  }
);

export const selectOpenPostUri = createSelector(
  state => state,
  state => {
    const encodedPostUri = getIn(state, ["router", "currentParams", "postUri"]);
    return decodeUriComponentProperly(encodedPostUri);
  }
);

export function isPrivateUser(state) {
  return !!getIn(state, ["router", "currentParams", "privateId"]);
}

export function getPersonas(needs) {
  const personas = needs
    .toList()
    .filter(need => need.get("ownNeed"))
    .filter(need => need.get("types") && need.get("types").has("won:Persona"));
  return personas.map(persona => {
    const graph = persona.get("jsonld");
    return {
      displayName: graph.get("s:name"),
      website: graph.get("s:url"),
      aboutMe: graph.get("s:description"),
      url: persona.get("uri"),
      saved: !persona.get("isBeingCreated"),
      timestamp: persona.get("creationDate").toISOString(),
    };
  });
}

export function currentSkin() {
  const style = getComputedStyle(document.body);
  const getColor = name => {
    const color = Color(style.getPropertyValue(name).trim());
    return color.rgb().object();
  };
  return {
    primaryColor: getColor("--won-primary-color"),
    lightGray: getColor("--won-light-gray"),
    lineGray: getColor("--won-line-gray"),
    subtitleGray: getColor("--won-subtitle-gray"),
  };
}
