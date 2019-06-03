import { mergeInEmptyDraft } from "../detail-definitions.js";
import { findLatestIntervallEndInJsonLdOrNowAndAddMillis } from "../../app/won-utils.js";
import { sparqlQuery } from "../../app/sparql-builder-utils.js";
import won from "../../app/service/won.js";

export const contactPaymentBot = {
  identifier: "contactPaymentBot",
  label: "Contact Payment Bot",
  icon: "#ico36_uc_custom",
  doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  // TODO: check if this can work like whatsX -> one click for atom creation, because nothing can be changed
  draft: {
    ...mergeInEmptyDraft({
      content: {
        title: "Payment Service Contact Request",
        type: ["won:ServiceContactRequest", "demo:PaymentServiceRequest"],
      },
      // TODO: hardcoded title & description for user
      seeks: {
        // should only look for type
        // future extension possibility: add payment type dropdown
        // alternatively: extend for other bots (might require more extensive RDF stuff)
        type: ["con:ServiceBot"],
      },
    }),
  },
  generateQuery: (draft, resultName) => {
    if (draft) {
      // do nothing, draft is not needed here
    }
    const query = sparqlQuery({
      prefixes: {
        won: won.defaultContext["won"],
        rdf: won.defaultContext["rdf"],
        con: won.defaultContext["con"],
      },
      distinct: true,
      variables: [resultName, "?score"],
      where: [`${resultName} rdf:type con:ServiceBot`, `BIND(1 as ?score)`],
    });

    return query;
  },
};
