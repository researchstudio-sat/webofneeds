/**
 * Created by quasarchimaere on 21.01.2019.
 */
import { get } from "../utils.js";
import * as viewUtils from "../view-utils.js";

/**
 * Check if showSlideIns is true
 * @param state (full redux-state)
 * @returns {*}
 */
export function showSlideIns(state) {
  return viewUtils.showSlideIns(get(state, "view"));
}
