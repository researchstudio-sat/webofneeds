/**
 * Created by fsuda on 08.11.2018.
 */

import won from "./won-es6.js";
import { get, getIn } from "./utils.js";
import { labels } from "./won-label-utils.js";
import * as connectionUtils from "./connection-utils.js";

/**
 * Determines if a given need is a Active
 * @param need
 * @returns {*|boolean}
 */
export function isActive(need) {
  return get(need, "state") && get(need, "state") === won.WON.ActiveCompacted;
}

export function isOwned(need) {
  return get(need, "isOwned");
}

export function getIdenticonSvg(need) {
  return get(need, "identiconSvg");
}

export function getMatchedUseCaseIcon(need) {
  return getIn(need, ["matchedUseCase", "icon"]);
}

export function getMatchedUseCaseIconBackground(need) {
  return getIn(need, ["matchedUseCase", "iconBackground"]);
}

export function getMatchedUseCaseIdentifier(need) {
  return getIn(need, ["matchedUseCase", "identifier"]);
}

/**
 * Determines if a given need is a Inactive
 * @param need
 * @returns {*|boolean}
 */
export function isInactive(need) {
  return get(need, "state") && get(need, "state") === won.WON.InactiveCompacted;
}

/**
 * Determines if a given need is a WhatsAround-Need
 * @param need
 * @returns {*|boolean}
 */
export function isWhatsAroundNeed(need) {
  return (
    getIn(need, ["content", "flags"]) &&
    getIn(need, ["content", "flags"]).contains("won:WhatsAround")
  );
}

/**
 * This checks if the need is allowed to be used as a template,
 * it is only allowed if the need exists, and if it is not one of the following:
 * - DirectResponseNeed
 * - WhatsAroundNeed
 * - WhatsNewNeed
 * - SearchNeed
 * @param need
 * @returns {*|boolean}
 */
export function isUsableAsTemplate(need) {
  return (
    need &&
    !(
      isOwned(need) ||
      isDirectResponseNeed(need) ||
      isWhatsAroundNeed(need) ||
      isWhatsNewNeed(need) ||
      isSearchNeed(need)
    )
  );
}

/**
 * Determines if a given need is a DirectResponse-Need
 * @param need
 * @returns {*|boolean}
 */
export function isDirectResponseNeed(need) {
  return (
    getIn(need, ["content", "flags"]) &&
    getIn(need, ["content", "flags"]).contains("won:DirectResponse")
  );
}

export function isPersona(need) {
  return (
    getIn(need, ["content", "type"]) &&
    getIn(need, ["content", "type"]).has("won:Persona")
  );
}

export function isNeed(need) {
  return (
    getIn(need, ["content", "type"]) &&
    getIn(need, ["content", "type"]).has("won:Need")
  );
}

export function hasChatFacet(need) {
  return (
    getIn(need, ["content", "facets"]) &&
    getIn(need, ["content", "facets"]).contains(won.WON.ChatFacetCompacted)
  );
}

export function hasGroupFacet(need) {
  return (
    getIn(need, ["content", "facets"]) &&
    getIn(need, ["content", "facets"]).contains(won.WON.GroupFacetCompacted)
  );
}

export function hasHoldableFacet(need) {
  return (
    getIn(need, ["content", "facets"]) &&
    getIn(need, ["content", "facets"]).contains(won.WON.HoldableFacetCompacted)
  );
}

export function hasHolderFacet(need) {
  return (
    getIn(need, ["content", "facets"]) &&
    getIn(need, ["content", "facets"]).contains(won.WON.HolderFacetCompacted)
  );
}

export function hasReviewFacet(need) {
  return (
    getIn(need, ["content", "facets"]) &&
    getIn(need, ["content", "facets"]).contains(won.WON.ReviewFacetCompacted)
  );
}

export function hasSuggestedConnections(need) {
  return (
    get(need, "connections") &&
    !!get(need, "connections").find(conn => connectionUtils.isSuggested(conn))
  );
}

export function hasUnreadSuggestedConnections(need) {
  return (
    get(need, "connections") &&
    !!get(need, "connections").find(
      conn => connectionUtils.isSuggested(conn) && get(conn, "unread")
    )
  );
}

/**
 * Determines if a given need is a Search-Need (see draft in create-search.js)
 * @param need
 * @returns {*|boolean}
 */
export function isSearchNeed(need) {
  return (
    getIn(need, ["content", "type"]) &&
    getIn(need, ["content", "type"]).has("won:PureSearch")
  );
}

/**
 * Determines if a given need is a WhatsNew-Need
 * @param need
 * @returns {*|boolean}
 */
export function isWhatsNewNeed(need) {
  return (
    getIn(need, ["content", "flags"]) &&
    getIn(need, ["content", "flags"]).contains("won:WhatsNew")
  );
}

/**
 * Generates a string that can be used to add matching contexts to a label.
 */
export function generateNeedMatchingContext(needImm) {
  const matchingContexts = needImm && needImm.get("matchingContexts");
  if (matchingContexts && matchingContexts.size > 0) {
    return " posted in " + matchingContexts.join(", ");
  } else {
    return "";
  }
}

/**
 * Generates a string that can be used as a Types Label for any given need, includes the matchingContexts
 * TODO: We Do not store a single type anymore but a list of types... adapt accordingly
 */
export function generateFullNeedTypesLabel(needImm) {
  const types = getIn(needImm, ["content", "type"]);

  //TODO: GENERATE CORRECT LABEL
  //self.labels.type[self.need.getIn(['content','type'])]
  let label = "";

  if (types && types.size > 0) {
    label = types.join(", ");
  }

  return label + generateNeedMatchingContext(needImm);
}

/**
 * Generates an array that contains all need flags, using a human readable label if available.
 */
export function generateFullNeedFlags(needImm) {
  const flags = needImm && needImm.getIn(["content", "flags"]);
  const flagsArray =
    flags &&
    flags
      .toArray()
      // use nicer facet labels if available
      // TODO: remove this to match RDF state?
      .map(flag => (labels.flags[flag] ? labels.flags[flag] : flag));
  return flagsArray;
}

/**
 * Generates an array that contains all need facets, using a human readable label if available.
 */
export function generateFullNeedFacets(needImm) {
  const facets = needImm && needImm.getIn(["content", "facets"]);
  const facetsArray =
    facets &&
    facets
      .toArray()
      // use nicer facet labels if available
      // TODO: remove this to match RDF state?
      .map(facet => (labels.facets[facet] ? labels.facets[facet] : facet));
  return facetsArray;
}

/**
 * Generates a string that can be used as a simplified Types Label for non-debug views. Hides information.
 * @param {*} needImm the need as saved in the state
 */
export function generateShortNeedTypesLabel(needImm) {
  const needTypes = needImm && needImm.getIn(["content", "type"]);

  const getNeedLabel = type => {
    switch (type) {
      //Insert specific overrides here
      default: {
        const match = /[^:/]+$/.exec(type);
        if (match) {
          return match[0];
        } else {
          return "Unknown";
        }
      }
    }
  };

  let label = "";

  if (isWhatsAroundNeed(needImm) || isWhatsNewNeed(needImm)) {
    label = "";
  } else if (isSearchNeed(needImm)) {
    label = "Search";
  } else if (isDirectResponseNeed(needImm)) {
    label = "Direct Response";
    // TODO: groupchat label
  } else if (needTypes && needTypes.size > 0) {
    let types = new Array();
    for (let type of Array.from(needTypes)) {
      // hide won:Need
      if (type === "won:Need") {
        continue;
        // cut off everything before the first :
      } else {
        types.push(getNeedLabel(type));
      }
    }
    label += types.join(", ");
  }
  return label;
}

/**
 * Generates an array that contains some need facets, using a human readable label if possible.
 */
export function generateShortNeedFacets(needImm) {
  const facets = needImm && needImm.get(["content", "facets"]);
  const facetsArray =
    facets &&
    facets
      .toArray()
      // rename facets
      // TODO: check if this can be used anywhere or whether it should be Group Chat Enabled
      .map(facet => {
        if (facet === won.WON.GroupFacetCompacted) {
          return "Group Chat";
        } else {
          return "";
        }
      })
      .filter(facet => facet.length > 0);
  return facetsArray;
}

/**
 * Generates an array that contains some need flags, using a human readable label if possible.
 */
export function generateShortNeedFlags(needImm) {
  const flags = needImm && needImm.getIn(["content", "flags"]);
  const flagsArray =
    flags &&
    flags
      .toArray()
      // rename flags
      // TODO: flags should have explanatory hovertext
      .map(flag => {
        if (flag === won.WON.NoHintForCounterpartCompacted) {
          return "Invisible";
        }
        if (flag === won.WON.NoHintForMeCompacted) {
          return "Silent";
        } else {
          return "";
        }
      })
      .filter(flag => flag.length > 0);
  return flagsArray;
}

export function getFacetsWithKeysReset(needImm) {
  const facets = getIn(needImm, ["content", "facets"]);

  if (facets) {
    return getFacetKeysReset(facets);
  }
  return undefined;
}

export function getDefaultFacetWithKeyReset(needImm) {
  const defaultFacet = getIn(needImm, ["content", "defaultFacet"]);

  if (defaultFacet) {
    return getFacetKeysReset(defaultFacet);
  }
  return undefined;
}

export function getSeeksFacetsWithKeysReset(needImm) {
  const facets = getIn(needImm, ["seeks", "facets"]);

  if (facets) {
    return getFacetKeysReset(facets);
  }
  return undefined;
}

export function getSeeksDefaultFacetWithKeyReset(needImm) {
  const defaultFacet = getIn(needImm, ["seeks", "defaultFacet"]);

  if (defaultFacet) {
    return getFacetKeysReset(defaultFacet);
  }
  return undefined;
}

function getFacetKeysReset(facetsImm) {
  return facetsImm.mapKeys((key, value) => {
    if (value === "won:ChatFacet") {
      return "#chatFacet";
    }
    if (value === "won:GroupFacet") {
      return "#groupFacet";
    }
    if (value === "won:HolderFacet") {
      return "#holderFacet";
    }
    if (value === "won:HoldableFacet") {
      return "#holdableFacet";
    }
    if (value === "won:ReviewFacet") {
      return "#reviewFacet";
    }
  });
}
