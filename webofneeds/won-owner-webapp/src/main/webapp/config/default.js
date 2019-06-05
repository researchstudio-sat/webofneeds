/* global process */
export const piwik = Object.freeze({
  baseUrl: process.env.WON_PIWIK_URL || "",
});
export const ownerBaseUrl = process.env.WON_OWNER_BASE_URL || "/";
