/**
 * Created by fsuda on 08.11.2018.
 */

import won from "./won-es6.js";
import { get, getIn } from "./utils.js";
import { labels } from "./won-label-utils.js";
import * as connectionUtils from "./connection-utils.js";
import * as useCaseUtils from "./usecase-utils.js";

/**
 * Determines if a given need is a Active
 * @param need
 * @returns {*|boolean}
 */
export function isActive(need) {
  return get(need, "state") && get(need, "state") === won.WON.ActiveCompacted;
}

export function getIdenticonSvg(need) {
  return get(need, "identiconSvg");
}

export function getMatchedUseCaseIcon(need) {
  return getIn(need, ["matchedUseCase", "icon"]);
}

export function getBackground(need) {
  return get(need, "background");
}

export function getMatchedUseCaseIdentifier(need) {
  return getIn(need, ["matchedUseCase", "identifier"]);
}

export function getReactionUseCases(need) {
  return getIn(need, ["matchedUseCase", "reactionUseCases"]);
}

export function getEnabledUseCases(need) {
  return getIn(need, ["matchedUseCase", "enabledUseCases"]);
}

export function hasMatchedUseCase(need) {
  return !!getIn(need, ["matchedUseCase", "identifier"]);
}

export function hasImages(need) {
  return (
    !!getIn(need, ["content", "images"]) || !!getIn(need, ["seeks", "images"])
  );
}

/**
 * Returns the "Default" Image (currently the first one, branch content is checked before seeks) of a need
 * if the need does not have any images we return undefined
 * @param need
 */
export function getDefaultImage(need) {
  if (hasImages(need)) {
    console.debug("NEED HAS IMAGES");
    const contentImages = getIn(need, ["content", "images"]);

    if (contentImages) {
      return contentImages.first();
    }

    const seeksImages = getIn(need, ["content", "images"]);

    if (seeksImages) {
      return seeksImages.first();
    }
  }
  return undefined;
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
  return hasFacet(need, won.WON.ChatFacetCompacted);
}

export function hasGroupFacet(need) {
  return hasFacet(need, won.WON.GroupFacetCompacted);
}

export function hasHoldableFacet(need) {
  return hasFacet(need, won.WON.HoldableFacetCompacted);
}

export function hasHolderFacet(need) {
  return hasFacet(need, won.WON.HolderFacetCompacted);
}

export function hasReviewFacet(need) {
  return hasFacet(need, won.WON.ReviewFacetCompacted);
}

export function hasFacet(need, facet) {
  return (
    getIn(need, ["content", "facets"]) &&
    getIn(need, ["content", "facets"]).contains(facet)
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
 * Retrieves the Label of the used useCase as a needType, if no usecase is specified we check if need is a searchNeed or DirectResponseNeed
 * @param {*} needImm the need as saved in the state
 */
export function generateNeedTypeLabel(needImm) {
  const useCase = useCaseUtils.getUseCase(getMatchedUseCaseIdentifier(needImm));

  if (useCase) {
    return useCase.label;
  } else {
    if (isSearchNeed(needImm)) {
      return "Search";
    } else if (isDirectResponseNeed(needImm)) {
      return "Direct Response";
    }

    return "";
  }
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
