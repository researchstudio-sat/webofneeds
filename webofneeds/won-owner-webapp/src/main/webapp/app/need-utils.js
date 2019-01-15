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
 */
export function generateNeedTypesLabel(needImm) {
  const matchingContexts = get(needImm, "matchingContexts");
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
