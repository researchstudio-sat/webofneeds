import { getIn, get, checkHttpStatus, clone } from "../utils.js";
import { actionTypes } from "./actions";

import loadCSS from "loadcss";
/**
 * Anything that is load-once, read-only, global app-config
 * should be initialized in this action. Ideally all of this
 * should be baked-in/prerendered when shipping the code, in
 * future versions => TODO
 */
export function configInit() {
  return (dispatch, getState) => {
    const defaultTheme = getIn(getState(), ["config", "theme"]);
    const defaultThemeName = get(defaultTheme, "name");
    Promise.all([loadDefaultNodeUri(), loadSkinConfig(defaultThemeName)]).then(
      ([defaultNodeUri, defaultThemeConfig]) =>
        dispatch({
          type: actionTypes.config.init,
          payload: {
            defaultNodeUri,
            theme: defaultTheme.mergeDeep(defaultThemeConfig).toJS(),
          },
        })
    );
  };
}

export function update(patch) {
  return (dispatch, getState) => {
    const currentTheme = getIn(getState(), ["config", "theme", "name"]);
    const newTheme = getIn(patch, ["theme", "name"]);
    if (currentTheme && newTheme && currentTheme != newTheme) {
      // theme has changed (or was initialized), load the updated theme config first

      //TODO fetch config. also do that in init!. note that this can fail if an invalid theme has been set.
      loadSkinConfig(newTheme).then(themeConfig => {
        const patch_ = clone(patch);
        Object.assign(patch_.theme, themeConfig);
        dispatch({
          type: actionTypes.config.update,
          payload: patch_,
        });
      });
    } else {
      dispatch({
        type: actionTypes.config.update,
        payload: patch,
      });
    }
  };
}
async function loadDefaultNodeUri() {
  /* this allows the owner-app-server to dynamically switch default nodes. */
  return fetch(/*relativePathToConfig=*/ "appConfig/getDefaultWonNodeUri")
    .then(checkHttpStatus)
    .then(resp => resp.json())
    .catch((/*err*/) => {
      const defaultNodeUri = `${location.protocol}://${
        location.host
      }/won/resource`;
      console.warn(
        "Failed to fetch default node uri at the relative path `",
        "appConfig/getDefaultWonNodeUri",
        "` (is the API endpoint there up and reachable?) -> falling back to the default ",
        defaultNodeUri
      );
      return defaultNodeUri;
    });
}

let _themeCssElement;
async function loadSkinConfig(themeName) {
  if (!themeName) {
    return Promise.resolve({});
  } else {
    // add config.css link to body
    const cssPath = `skin/${themeName}/config.css`;
    if (!_themeCssElement) {
      _themeCssElement = loadCSS(cssPath)[0];
    } else {
      _themeCssElement.href = cssPath;
    }

    // load config.json
    return fetch(`skin/${themeName}/config.json`)
      .then(resp => resp.json())
      .then(config => {
        document.title = config.title;
        return config;
      });
  }
}
