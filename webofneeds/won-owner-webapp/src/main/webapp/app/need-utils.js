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
    need && need.get("flags") && need.get("flags").contains("won:WhatsAround")
  );
}

/**
 * Determines if a given need is a WhatsNew-Need
 * @param msg
 * @returns {*|boolean}
 */
export function isWhatsNewNeed(need) {
  return (
    need && need.get("flags") && need.get("flags").contains("won:WhatsNew")
  );
}
