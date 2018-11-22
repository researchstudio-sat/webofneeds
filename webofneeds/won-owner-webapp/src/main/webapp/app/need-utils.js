/**
 * Created by fsuda on 08.11.2018.
 */

/**
 * Determines if a given need is a WhatsAround-Need
 * @param msg
 * @returns {*|boolean}
 */
export function isWhatsAroundNeed(need) {
  return (
    need &&
    need.getIn(["content", "flags"]) &&
    need.getIn(["content", "flags"]).contains("won:WhatsAround")
  );
}

/**
 * Determines if a given need is a DirectResponse-Need
 * @param msg
 * @returns {*|boolean}
 */
export function isDirectResponseNeed(need) {
  return (
    need &&
    need.getIn(["content", "flags"]) &&
    need.getIn(["content", "flags"]).contains("won:DirectResponse")
  );
}

/**
 * Determines if a given need is a Search-Need (see draft in create-search.js)
 * @param msg
 * @returns {*|boolean}
 */
export function isSearchNeed(need) {
  return need && need.get("types") && need.get("types").has("won:PureSearch");
}

/**
 * Determines if a given need is a WhatsNew-Need
 * @param msg
 * @returns {*|boolean}
 */
export function isWhatsNewNeed(need) {
  return (
    need &&
    need.getIn(["content", "flags"]) &&
    need.getIn(["content", "flags"]).contains("won:WhatsNew")
  );
}

/**
 * Generates a string that can be used as a Types Label for any given need, includes the matchingContexts
 */
export function generateNeedTypesLabel(needImm) {
  const matchingContexts = needImm && needImm.get("matchingContexts");
  const types = needImm && needImm.get("types");

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
