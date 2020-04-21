import { details, mergeInEmptyDraft } from "../detail-definitions.js";
import * as jsonLdUtils from "../../app/service/jsonld-utils.js";
import ico36_uc_custom from "../../images/won-icons/ico36_uc_custom.svg";

export const customUseCase = {
  identifier: "customUseCase",
  label: "New custom post",
  icon: ico36_uc_custom,
  doNotMatchAfter: jsonLdUtils.findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  draft: { ...mergeInEmptyDraft() },
  details: details,
  seeksDetails: details,
};
