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
            vocab.VALUEFLOWS.PrimaryAccountableInverseSocketCompacted,
          "#CustodianInverseSocket":
            vocab.VALUEFLOWS.CustodianInverseSocketCompacted,
        },
      },
      seeks: {},
    }),
  },
  reactions: {
    ...defaultReactions,
    [vocab.VALUEFLOWS.PrimaryAccountableInverseSocketCompacted]: {
      [vocab.VALUEFLOWS.PrimaryAccountableSocketCompacted]: {
        useCaseIdentifiers: ["*"],
        refuseOwned: true,
      },
    },
  },
  details: {
    title: { ...details.title },
    name: { ...name },
    accountingQuantity: { ...accountingQuantity },
    onhandQuantity: { ...onhandQuantity },
    location: { ...details.location, mandatory: false },
    //currentLocation: { ...currentLocation },
  },
  seeksDetails: {},
};
