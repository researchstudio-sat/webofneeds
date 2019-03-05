/**
 * Created by ksinger on 22.01.2016.
 */

import { createSelector } from "reselect";

import { decodeUriComponentProperly, getIn, get } from "../utils.js";
import { isPersona, isNeed, isActive, isInactive } from "../need-utils.js";
import Color from "color";

export const selectLastUpdateTime = state => state.get("lastUpdateTime");
export const getRouterParams = state =>
  getIn(state, ["router", "currentParams"]);

export const getNeeds = state => state.get("needs");
export const getOwnedNeeds = state =>
  getNeeds(state).filter(need => need.get("isOwned"));
export const getNonOwnedNeeds = state =>
  getNeeds(state).filter(need => !need.get("isOwned"));

export function getPosts(state) {
  const needs = getNeeds(state);
  return needs.filter(need => {
    if (!need.getIn(["content", "type"])) return true;

    return isNeed(need) && !isPersona(need);
  });
}

export const getOwnedPosts = state =>
  getPosts(state).filter(need => need.get("isOwned"));

export function getOwnedOpenPosts(state) {
  const allOwnedNeeds = getOwnedPosts(state);
  return allOwnedNeeds && allOwnedNeeds.filter(post => isActive(post));
}

export function getOpenPosts(state) {
  const allPosts = getPosts(state);
  return allPosts && allPosts.filter(post => isActive(post));
}

export function getActiveNeeds(state) {
  const allNeeds = getNeeds(state);
  return allNeeds && allNeeds.filter(need => isActive(need));
}

//TODO: METHOD NAME TO ACTUALLY REPRESENT WHAT THE SELECTOR DOES
export function getOwnedClosedPosts(state) {
  const allOwnedNeeds = getOwnedPosts(state);
  return allOwnedNeeds && allOwnedNeeds.filter(post => isInactive(post));
}

export function getOwnedNeedsInCreation(state) {
  const allOwnedNeeds = getOwnedNeeds(state);
  // needs that have been created but are not confirmed by the server yet
  return (
    allOwnedNeeds && allOwnedNeeds.filter(post => post.get("isBeingCreated"))
  );
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
  return needs.find(need => need.getIn(["connections", connectionUri]));
}

export const getCurrentParamsFromRoute = createSelector(
  state => state,
  state => {
    return getIn(state, ["router", "currentParams"]);
  }
);

export const getViewNeedUriFromRoute = createSelector(
  state => state,
  state => {
    const encodedNeedUri = getIn(state, [
      "router",
      "currentParams",
      "viewNeedUri",
    ]);
    return decodeUriComponentProperly(encodedNeedUri);
  }
);

export const getViewConnectionUriFromRoute = createSelector(
  state => state,
  state => {
    const encodedConnUri = getIn(state, [
      "router",
      "currentParams",
      "viewConnUri",
    ]);
    return decodeUriComponentProperly(encodedConnUri);
  }
);

export const getUseCaseFromRoute = createSelector(
  state => state,
  state => {
    return getIn(state, ["router", "currentParams", "useCase"]);
  }
);

export const getUseCaseGroupFromRoute = createSelector(
  state => state,
  state => {
    return getIn(state, ["router", "currentParams", "useCaseGroup"]);
  }
);

export const getPrivateIdFromRoute = createSelector(
  state => state,
  state => {
    return getIn(state, ["router", "currentParams", "privateId"]);
  }
);

export const getVerificationTokenFromRoute = createSelector(
  state => state,
  state => {
    return getIn(state, ["router", "currentParams", "token"]);
  }
);

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

export const getGroupPostAdminUriFromRoute = createSelector(
  state => state,
  state => {
    const encodedPostUri = getIn(state, [
      "router",
      "currentParams",
      "groupPostAdminUri",
    ]);
    return decodeUriComponentProperly(encodedPostUri);
  }
);

export const getFromNeedUriFromRoute = createSelector(
  state => state,
  state => {
    const encodedNeedUri = getIn(state, [
      "router",
      "currentParams",
      "fromNeedUri",
    ]);
    return decodeUriComponentProperly(encodedNeedUri);
  }
);

export const getModeFromRoute = createSelector(
  state => state,
  state => {
    const mode = getIn(state, ["router", "currentParams", "mode"]);

    if (mode) {
      if (mode === "CONNECT") {
        return "CONNECT";
      } else if (mode === "EDIT") {
        return "EDIT";
      }
      return "DUPLICATE";
    }
    return undefined;
  }
);

export const getPostUriFromRoute = createSelector(
  state => state,
  state => {
    const encodedPostUri = getIn(state, ["router", "currentParams", "postUri"]);
    return decodeUriComponentProperly(encodedPostUri);
  }
);

export const getAboutSectionFromRoute = createSelector(
  state => state,
  state => {
    return getIn(state, ["router", "currentParams", "aboutSection"]);
  }
);

export function getOwnedPersonas(state) {
  const needs = getOwnedNeeds(state);
  const personas = needs.toList().filter(need => isPersona(need));
  return personas.map(persona => {
    return {
      displayName: getIn(persona, ["content", "personaName"]),
      website: getIn(persona, ["content", "website"]),
      aboutMe: getIn(persona, ["content", "description"]),
      url: get(persona, "uri"),
      saved: !get(persona, "isBeingCreated"),
      timestamp: get(persona, "creationDate").toISOString(),
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
