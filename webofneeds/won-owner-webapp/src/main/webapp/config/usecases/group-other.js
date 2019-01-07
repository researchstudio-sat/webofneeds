import won from "../../app/service/won.js";

import { complain } from "./uc-complain.js";
import { handleComplaint } from "./uc-handle-complaint.js";
import { customUseCase } from "./uc-custom.js";

import { details, emptyDraft } from "../detail-definitions.js";

export const otherGroup = {
  identifier: "othergroup",
  label: "More...",
  icon: undefined,
  useCases: {
    complain: complain,
    handleComplaint: handleComplaint,
    customUseCase: customUseCase,
    groupChat: {
      identifier: "groupChat",
      label: "New Groupchat Post",
      draft: {
        ...emptyDraft,
        facet: { "@id": "#groupFacet", "@type": won.WON.GroupFacet },
      },
      details: details,
      seeksDetails: details,
    },
  },
};
