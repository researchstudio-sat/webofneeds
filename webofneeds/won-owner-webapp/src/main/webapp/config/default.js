/* global process */
export const piwik = Object.freeze({
  baseUrl: process.env.WON_PIWIK_URL || "",
});
export const ownerBaseUrl = process.env.WON_OWNER_BASE_URL || "/";
export const nodeEnv = process.env.NODE_ENV;
export const enableNotifications = true; //process.env.ENABLE_NOTIFICATIONS;
