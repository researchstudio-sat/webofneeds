import { details, mergeInEmptyDraft } from "../detail-definitions.js";
import {
  vicinityScoreSubQuery,
  sparqlQuery,
} from "../../app/sparql-builder-utils.js";
import { getIn } from "../../app/utils.js";
import { Generator } from "sparqljs";
import won from "../../app/service/won.js";
import { findLatestIntervallEndInJsonLdOrNowAndAddMillis } from "../../app/won-utils.js";

window.SparqlGenerator4dbg = Generator;

export const lunchPlan = {
  identifier: "lunchPlan",
  label: "Plan Lunch!",
  icon: undefined,
  doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  draft: {
    ...mergeInEmptyDraft({
      content: {
        type: ["s:PlanAction"],
        title: "Let's go get lunch!",
        eventObject: "http://dbpedia.org/resource/Lunch",
        sockets: {
          "#groupSocket": won.GROUP.GroupSocketCompacted,
          "#holdableSocket": won.HOLD.HoldableSocketCompacted,
        },
        defaultSocket: { "#groupSocket": won.GROUP.GroupSocketCompacted },
      },
      seeks: {},
    }),
  },
  details: {
    title: { ...details.title },
    description: { ...details.description },
    location: { ...details.location, mandatory: true },
    fromDatetime: { ...details.fromDatetime },
  },
  seeksDetails: {},

  generateQuery: (draft, resultName) => {
    const vicinityScoreSQ = vicinityScoreSubQuery({
      resultName: resultName,
      bindScoreAs: "?location_geoScore",
      pathToGeoCoords: "s:location/s:geo",
      prefixesInPath: {
        s: won.defaultContext["s"],
        won: won.defaultContext["won"],
      },
      geoCoordinates: getIn(draft, ["content", "location"]),
    });

    const subQueries = [vicinityScoreSQ]
      .filter(sq => sq) // filter out non-existing details (the SQs should be `undefined` for them)
      .map(sq => ({
        query: sq,
        optional: true, // so counterparts without that detail don't get filtered out (just assigned a score of 0 via `coalesce`)
      }));

    const query = sparqlQuery({
      prefixes: {
        won: won.defaultContext["won"],
        rdf: won.defaultContext["rdf"],
        buddy: won.defaultContext["buddy"],
        hold: won.defaultContext["hold"],
        s: won.defaultContext["s"],
      },
      distinct: true,
      variables: [resultName, "?score"],
      subQueries: subQueries,
      where: [
        `${resultName} rdf:type won:Interest.`,
        `${resultName} won:seeks ?seeks .`,
        `?seeks rdf:type s:PlanAction.`,
        `?seeks s:object ?planObject.`,
        `?planObject s:about <http://dbpedia.org/resource/Lunch>.`,
        `?thisAtom hold:heldBy/buddy:buddy/^hold:holds ${resultName}.`,
        `BIND( ( 
          COALESCE(?location_geoScore, 0) 
        ) / 5  as ?score)`,
        // `FILTER(?score > 0)`, // not necessary atm to filter; there are parts of -postings we can't match yet (e.g. NLP on description). also content's sparse anyway.
      ],
      orderBy: [{ order: "DESC", variable: "?score" }],
    });

    return query;
  },
};
