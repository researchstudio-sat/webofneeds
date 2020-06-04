import vocab from "../../app/service/vocab.js";

import { complain } from "./uc-complain.js";
import { handleComplaint } from "./uc-handle-complaint.js";
import { customUseCase } from "./uc-custom.js";
import { contactPaymentBot } from "./uc-contact-payment-bot.js";

import { details, mergeInEmptyDraft } from "../detail-definitions.js";

import ico36_plus from "../../images/won-icons/ico36_plus.svg";

export const otherGroup = {
  identifier: "othergroup",
  label: "More...",
  icon: ico36_plus,
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
            sockets: {
              "#groupSocket": vocab.GROUP.GroupSocketCompacted,
              "#holdableSocket": vocab.HOLD.HoldableSocketCompacted,
            },
          },
        }),
      },
      details: details,
      seeksDetails: details,
    },
    contactPaymentBot: contactPaymentBot,
  },
};
