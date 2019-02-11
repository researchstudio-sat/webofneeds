import won from "../../app/service/won.js";

import { complain } from "./uc-complain.js";
import { handleComplaint } from "./uc-handle-complaint.js";
import { customUseCase } from "./uc-custom.js";

import { details, mergeInEmptyDraft } from "../detail-definitions.js";

export const otherGroup = {
  identifier: "othergroup",
  label: "More...",
  icon: undefined,
  subItems: {
    complain: complain,
    handleComplaint: handleComplaint,
    customUseCase: customUseCase,
    groupChat: {
      identifier: "groupChat",
      label: "New Groupchat Post",
      draft: {
        ...mergeInEmptyDraft({
          content: {
            facets: {
              "#groupFacet": won.WON.GroupFacetCompacted,
              "#holdableFacet": won.WON.HoldableFacetCompacted,
            },
            defaultFacet: { "#groupFacet": won.WON.GroupFacetCompacted },
          },
        }),
      },
      details: details,
      seeksDetails: details,
    },
  },
};
