/**
 * Created by ksinger on 22.01.2016.
 */

import { createSelector } from "reselect";

import { decodeUriComponentProperly, getIn, get } from "../utils.js";
import * as needUtils from "../need-utils.js";
import * as accountUtils from "../account-utils.js";
import Color from "color";

export const selectLastUpdateTime = state => state.get("lastUpdateTime");
export const getRouterParams = state =>
  getIn(state, ["router", "currentParams"]);

export const getNeeds = state => state.get("needs");
export const getOwnedNeeds = state => {
  const accountState = get(state, "account");
  return getNeeds(state).filter(need =>
    accountUtils.isNeedOwned(accountState, get(need, "uri"))
  );
};
export const getNonOwnedNeeds = state => {
  const accountState = get(state, "account");
  return getNeeds(state).filter(
    need => !accountUtils.isNeedOwned(accountState, get(need, "uri"))
  );
};

export function getPosts(state) {
  const needs = getNeeds(state);
  return needs.filter(need => {
    if (!need.getIn(["content", "type"])) return true;

    return needUtils.isNeed(need) && !needUtils.isPersona(need);
  });
}

export const getOwnedPosts = state => {
  const accountState = get(state, "account");
  return getPosts(state).filter(need =>
    accountUtils.isNeedOwned(accountState, get(need, "uri"))
  );
};

export function getOwnedOpenPosts(state) {
  const allOwnedNeeds = getOwnedPosts(state);
  return (
    allOwnedNeeds && allOwnedNeeds.filter(post => needUtils.isActive(post))
  );
}

export function getOpenPosts(state) {
  const allPosts = getPosts(state);
  return allPosts && allPosts.filter(post => needUtils.isActive(post));
}

export function getActiveNeeds(state) {
  const allNeeds = getNeeds(state);
  return allNeeds && allNeeds.filter(need => needUtils.isActive(need));
}

//TODO: METHOD NAME TO ACTUALLY REPRESENT WHAT THE SELECTOR DOES
export function getOwnedClosedPosts(state) {
  const allOwnedNeeds = getOwnedPosts(state);
  return (
    allOwnedNeeds && allOwnedNeeds.filter(post => needUtils.isInactive(post))
  );
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
  const personas = needs.toList().filter(need => needUtils.isPersona(need));
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
/**
 * Returns true if the need is owned by the user who is currently logged in
 * @param state FULL redux state, no substates allowed
 * @param needUri
 */
export function isNeedOwned(state, needUri) {
  if (needUri) {
    const accountState = get(state, "account");
    return accountUtils.isNeedOwned(accountState, needUri);
  }
  return false;
}

/**
 * This checks if the given needUri is allowed to be used as a template,
 * it is only allowed if the need exists is NOT owned, and if it has a matchedUseCase
 * @param needUri
 * @returns {*|boolean}
 */
export function isNeedUsableAsTemplate(state, needUri) {
  const need = getIn(state, ["needs", needUri]);

  return (
    !!need && !isNeedOwned(state, needUri) && needUtils.hasMatchedUseCase(need)
  );
}

/**
 * This checks if the given needUri is allowed to be edited,
 * it is only allowed if the need exists, and if it IS owned and has a matchedUseCase
 * @param need
 * @returns {*|boolean}
 */
export function isNeedEditable(state, needUri) {
  const need = getIn(state, ["needs", needUri]);

  return (
    !!need && isNeedOwned(state, needUri) && needUtils.hasMatchedUseCase(need)
  );
}
