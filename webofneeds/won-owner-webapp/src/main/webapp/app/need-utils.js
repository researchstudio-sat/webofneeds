/**
 * Created by fsuda on 08.11.2018.
 */

import won from "./won-es6.js";
import { get, getIn } from "./utils.js";

/**
 * Determines if a given need is a WhatsAround-Need
 * @param msg
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
 * @param msg
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
    get(need, "facets") &&
    get(need, "facets").contains(won.WON.ChatFacetCompacted)
  );
}

export function hasGroupFacet(need) {
  return (
    get(need, "facets") &&
    get(need, "facets").contains(won.WON.GroupFacetCompacted)
  );
}

/**
 * Determines if a given need is a Search-Need (see draft in create-search.js)
 * @param msg
 * @returns {*|boolean}
 */
export function isSearchNeed(need) {
  return get(need, "types") && get(need, "types").has("won:PureSearch");
}

/**
 * Determines if a given need is a WhatsNew-Need
 * @param msg
 * @returns {*|boolean}
 */
export function isWhatsNewNeed(need) {
  return (
    getIn(need, ["content", "flags"]) &&
    getIn(need, ["content", "flags"]).contains("won:WhatsNew")
  );
}

/**
 * Generates a string that can be used as a Types Label for any given need, includes the matchingContexts
 * TODO: We Do not store a single type anymore but a list of types... adapt accordingly
 */
export function generateFullNeedTypesLabel(needImm) {
  const matchingContexts = needImm && needImm.get("matchingContexts");
  const types = get(needImm, "types");

  //TODO: GENERATE CORRECT LABEL
  //self.labels.type[self.need.get('type')]
  let label = "";

  if (types && types.size > 0) {
    label = types.join(", ");
  }
  if (matchingContexts && matchingContexts.size > 0) {
    if (label.length > 0) {
      label += " ";
    }
    label += "in " + matchingContexts.join(", ");
  }

  return label;
}

/**
 * Generates a string that can be used as a simplified Types Label for non-debug views. Hides information.
 * @param {*} needImm the need as saved in the state
 */
export function generateShortNeedTypesLabel(needImm) {
  const needTypes = needImm && needImm.get("types");
  const matchingContexts = needImm && needImm.get("matchingContexts");

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

  // add matching contexts
  if (matchingContexts && matchingContexts.size > 0) {
    if (label.length === 0) {
      label += "Posted in " + matchingContexts.join(", ");
    }
    label += " in " + matchingContexts.join(", ");
  }

  return label;
}
