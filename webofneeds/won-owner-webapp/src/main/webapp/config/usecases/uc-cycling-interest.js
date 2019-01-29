import { details, emptyDraft } from "../detail-definitions.js";
import {
  vicinityScoreSubQuery,
  sparqlQuery,
} from "../../app/sparql-builder-utils.js";
import won from "../../app/won-es6.js";
import { getIn } from "../../app/utils.js";
import { Generator } from "sparqljs";

window.SparqlGenerator4dbg = Generator;

export const cyclingInterest = {
  identifier: "cyclingInterest",
  label: "Add Cycling Interest",
  icon: undefined,
  draft: {
    ...emptyDraft,
    content: {
      type: "won:Interest",
      title: "I'm up for cycling!",
    },
    seeks: {
      sPlanAction: { "@id": "http://dbpedia.org/resource/Cycling" },
    },
  },
  details: {
    title: { ...details.title },
  },
  seeksDetails: {
    description: { ...details.description },
    location: { ...details.location },
    sPlanAction: { ...details.sPlanAction },
  },

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
        `${resultName} rdf:type won:Need.`,
        `${resultName} rdf:type s:PlanAction.`,
        `${resultName} s:object ?planObject.`,
        `?planObject s:about <http://dbpedia.org/resource/Cycling>`,
        // calculate average of scores; can be weighed if necessary
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
