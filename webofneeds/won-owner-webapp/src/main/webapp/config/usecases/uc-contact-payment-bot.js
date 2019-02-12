import { details, mergeInEmptyDraft } from "../detail-definitions.js";
import { findLatestIntervallEndInJsonLdOrNowAndAddMillis } from "../../app/won-utils.js";

export const contactPaymentBot = {
  identifier: "contactPaymentBot",
  label: "Contact Payment Bot",
  icon: "#ico36_uc_custom",
  doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  draft: {
    ...mergeInEmptyDraft({
      seeks: {
        title: "Contacting Payment Bot",
        description: "Contacting the Paypal Payment Bot...",
        tags: ["Paypal"],
      },
    }),
  },
  seeksDetails: {
    title: { ...details.title },
    description: { ...details.description },
    tags: { ...details.tags },
  },
};
