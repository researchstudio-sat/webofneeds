/**
 * Created by fsuda on 29.01.2021.
 */
import {
  details,
  mergeInEmptyDraft,
  defaultReactions,
} from "../detail-definitions.js";
import vocab from "../../app/service/vocab.js";
import ico36_uc_consortium_offer from "../../images/won-icons/ico36_uc_consortium-offer.svg";

export const role = {
  identifier: "role",
  label: "Role",
  icon: ico36_uc_consortium_offer, //TODO: Find better Icon
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: ["s:Role"],
        sockets: {
          "#memberSocket": vocab.WXSCHEMA.MemberSocketCompacted,
          "#orgRoleOfSocket": vocab.WXSCHEMA.OrganizationRoleOfSocketCompacted,
        },
      },
    }),
  },
  reactions: {
    ...defaultReactions,
    [vocab.WXSCHEMA.MemberSocketCompacted]: {
      [vocab.WXSCHEMA.MemberOfSocketCompacted]: {
        useCaseIdentifiers: ["persona"],
      },
    },
    //TODO: CHANGE SOCKET TO ORG ROLE SOCKET IF POSSIBLE
    [vocab.WXSCHEMA.OrganizationRoleOfSocketCompacted]: {
      [vocab.WXSCHEMA.MemberSocketCompacted]: {
        useCaseIdentifiers: ["organization"],
        refuseNonOwned: true,
      },
    },
  },
  details: {
    title: { ...details.title },
    description: { ...details.description },
  },
  seeksDetails: undefined,
};
