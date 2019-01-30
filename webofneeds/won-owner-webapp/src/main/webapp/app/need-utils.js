/**
 * Created by fsuda on 08.11.2018.
 */

import won from "./won-es6.js";
import { get, getIn } from "./utils.js";
import { labels } from "./won-label-utils.js";

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
  return get(need, "types") && get(need, "types").has("won:Persona");
}

export function isNeed(need) {
  return get(need, "types") && get(need, "types").has("won:Need");
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

/**
 * Determines if a given need is a Search-Need (see draft in create-search.js)
 * @param need
 * @returns {*|boolean}
 */
export function isSearchNeed(need) {
  return get(need, "types") && get(need, "types").has("won:PureSearch");
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
  const types = get(needImm, "types");

  //TODO: GENERATE CORRECT LABEL
  //self.labels.type[self.need.get('type')]
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
  const needTypes = needImm && needImm.get("types");

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
        types.push(type.substring(type.indexOf(":") + 1));
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
