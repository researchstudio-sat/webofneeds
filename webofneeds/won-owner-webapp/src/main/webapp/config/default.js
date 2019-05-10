/* global process */
export const piwik = Object.freeze({
  baseUrl:
    process.env.WON_DEPLOY_NODE_ENV == "live" ? "//matchat.org/piwik/" : "",
});
export const ownerBaseUrl = "/owner/";
