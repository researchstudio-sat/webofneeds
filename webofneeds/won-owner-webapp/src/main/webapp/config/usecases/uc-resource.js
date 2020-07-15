import {
  details,
  mergeInEmptyDraft,
  defaultReactions,
} from "../detail-definitions.js";
import {
  name,
  accountingQuantity,
  onhandQuantity,
} from "../details/resource.js";
import vocab from "../../app/service/vocab.js";
import ico36_uc_transport_demand from "~/images/won-icons/ico36_uc_transport_demand.svg";

export const resource = {
  identifier: "resource",
  label: "Thing",
  icon: ico36_uc_transport_demand,
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: [vocab.VALUEFLOWS.EconomicResourceCompacted],
        sockets: {
          "#PrimaryAccountableSocket":
            vocab.WXVALUEFLOWS.PrimaryAccountableSocketCompacted,
          "#CustodianSocket": vocab.WXVALUEFLOWS.CustodianSocketCompacted,
          "#ResourceActivitySocket":
            vocab.WXVALUEFLOWS.ResourceActivitySocketCompacted,
        },
      },
      seeks: {},
    }),
  },
  reactions: {
    ...defaultReactions,
    [vocab.WXVALUEFLOWS.PrimaryAccountableSocketCompacted]: {
      [vocab.WXVALUEFLOWS.PrimaryAccountableOfSocketCompacted]: {
        useCaseIdentifiers: ["persona"],
      },
    },
    [vocab.WXVALUEFLOWS.CustodianSocketCompacted]: {
      [vocab.WXVALUEFLOWS.CustodianOfSocketCompacted]: {
        useCaseIdentifiers: ["persona"],
      },
    },
    [vocab.WXVALUEFLOWS.ResourceActivitySocketCompacted]: {
      [vocab.WXVALUEFLOWS.ResourceSocketCompacted]: {
        useCaseIdentifiers: ["activity"],
      },
    },
  },
  details: {
    name: { ...name },
    accountingQuantity: { ...accountingQuantity },
    onhandQuantity: { ...onhandQuantity },
    location: { ...details.location },
    classifiedAs: { ...details.classifiedAs },
    images: { ...details.images },
  },
  seeksDetails: {},
};
