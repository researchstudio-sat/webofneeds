import {
  details,
  mergeInEmptyDraft,
  defaultReactions,
} from "../detail-definitions.js";
import {
  name,
  accountingQuantity,
  onhandQuantity,
  effortQuantity,
} from "../details/resource.js";
import vocab from "../../app/service/vocab.js";

export const resource = {
  identifier: "resource",
  label: "EconomicResource",
  icon: undefined, //No Icon For Persona UseCase (uses identicon)
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: ["vf:EconomicResource"],
        sockets: {
          "#PrimaryAccountableInverseSocket":
            vocab.WXVALUEFLOWS.PrimaryAccountableInverseSocketCompacted,
          "#CustodianInverseSocket":
            vocab.WXVALUEFLOWS.CustodianInverseSocketCompacted,
        },
      },
      seeks: {},
    }),
  },
  reactions: {
    ...defaultReactions,
    [vocab.WXVALUEFLOWS.PrimaryAccountableInverseSocketCompacted]: {
      [vocab.WXVALUEFLOWS.PrimaryAccountableSocketCompacted]: {
        useCaseIdentifiers: ["persona"],
      },
    },
    [vocab.WXVALUEFLOWS.CustodianInverseSocketCompacted]: {
      [vocab.WXVALUEFLOWS.CustodianSocketCompacted]: {
        useCaseIdentifiers: ["persona"],
      },
    },
  },
  details: {
    title: { ...details.title },
    name: { ...name },
    accountingQuantity: { ...accountingQuantity },
    onhandQuantity: { ...onhandQuantity },
    effortQuantity: { ...effortQuantity },
    location: { ...details.location, mandatory: false },
    classifiedAs: { ...details.classifiedAs },
  },
  seeksDetails: {},
};
