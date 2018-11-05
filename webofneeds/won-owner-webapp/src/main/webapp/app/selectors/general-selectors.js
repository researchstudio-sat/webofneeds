/**
 * Created by ksinger on 22.01.2016.
 */

import { createSelector } from "reselect";

import Immutable from "immutable";
import won from "../won-es6.js";
import { decodeUriComponentProperly, getIn } from "../utils.js";
import Color from "color";

export const selectLastUpdateTime = state => state.get("lastUpdateTime");
export const getRouterParams = state =>
  getIn(state, ["router", "currentParams"]);

export const getNeeds = state => state.get("needs");
export const getOwnedNeeds = state =>
  getNeeds(state).filter(need => need.get("ownNeed"));
export const getNonOwnedNeeds = state =>
  getNeeds(state).filter(need => !need.get("ownNeed"));

export function getPosts(state) {
  const needs = getNeeds(state);
  return needs.filter(need => {
    if (!need.get("types")) return true;

    return Immutable.is(need.get("types"), Immutable.Set(["won:Need"]));
  });
}

export const getOwnedPosts = state =>
  getPosts(state).filter(need => need.get("ownNeed"));

export function getOwnedOpenPosts(state) {
  const allOwnNeeds = getOwnedPosts(state);
  return (
    allOwnNeeds &&
    allOwnNeeds.filter(post => post.get("state") === won.WON.ActiveCompacted)
  );
}

export function getOpenPosts(state) {
  const allPosts = getPosts(state);
  return (
    allPosts &&
    allPosts.filter(post => post.get("state") === won.WON.ActiveCompacted)
  );
}

//TODO: METHOD NAME TO ACTUALLY REPRESENT WHAT THE SELECTOR DOES (e.g. ...WithoutWhatsX)
export function getOwnedClosedPosts(state) {
  const allOwnNeeds = getOwnedPosts(state);
  return (
    allOwnNeeds &&
    allOwnNeeds.filter(
      post =>
        post.get("state") === won.WON.InactiveCompacted &&
        !(post.get("isWhatsAround") || post.get("isWhatsNew"))
    )
  ); //Filter whatsAround and whatsNew needs automatically
}

export function getOwnedNeedsInCreation(state) {
  const allOwnNeeds = getOwnedNeeds(state);
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
export function getOwnedNeedByConnectionUri(state, connectionUri) {
  let needs = getOwnedNeeds(state); //we only check own needs as these are the only ones who have connections stored
  return needs
    .filter(need => need.getIn(["connections", connectionUri]))
    .first();
}

export const getConnectionUriFromRoute = createSelector(
  getRouterParams,
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

export const getPostUriFromRoute = createSelector(
  state => state,
  state => {
    const encodedPostUri = getIn(state, ["router", "currentParams", "postUri"]);
    return decodeUriComponentProperly(encodedPostUri);
  }
);

export function isPrivateUser(state) {
  return !!getIn(state, ["router", "currentParams", "privateId"]);
}

export function getOwnedPersonas(state) {
  const needs = getOwnedNeeds(state);
  const personas = needs
    .toList()
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

//TODO: move this method to a place that makes more sense, its not really a selector function
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
