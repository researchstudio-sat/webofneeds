import { deepFreeze } from "../app/utils.js";

export default deepFreeze({
  piwik: { baseUrl: "//matchat.org/piwik/" },
  ownerBaseUrl: "/owner/",
});

export const postTitleCharacterLimit = 140;
