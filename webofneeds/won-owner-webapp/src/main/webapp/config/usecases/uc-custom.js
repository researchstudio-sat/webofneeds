import { details, mergeInEmptyDraft } from "../detail-definitions.js";
import { findLatestIntervallEndInJsonLdOrNowAndAddMillis } from "../../app/won-utils.js";

export const customUseCase = {
  identifier: "customUseCase",
  label: "New custom post",
  icon: "#ico36_uc_custom",
  doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  draft: { ...mergeInEmptyDraft() },
  details: details,
  seeksDetails: details,
};
