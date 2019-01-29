import { details, emptyDraft } from "../detail-definitions.js";
import {
  vicinityScoreSubQuery,
  sparqlQuery,
} from "../../app/sparql-builder-utils.js";
import { getIn } from "../../app/utils.js";
import { Generator } from "sparqljs";
import won from "../../app/service/won.js";

window.SparqlGenerator4dbg = Generator;

export const cyclingPlan = {
  identifier: "cyclingPlan",
  label: "Plan a Ride!",
  icon: undefined,
  draft: {
    ...emptyDraft,
    content: {
      type: "won:Plan",
      title: "Let's go cycling!",
      sPlanAction: "http://dbpedia.org/resource/Cycling",
    },
    seeks: {},
    facet: { "@id": "#groupFacet", "@type": won.WON.GroupFacet },
  },
  details: {
    title: { ...details.title },
    description: { ...details.description },
    location: { ...details.location },
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
      geoCoordinates: getIn(draft, ["seeks", "location"]),
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
        `?planObject s:about <http://dbpedia.org/resource/Cycling>`,
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
