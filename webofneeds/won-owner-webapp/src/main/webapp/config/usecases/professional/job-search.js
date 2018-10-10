import { details, emptyDraft } from "../../detail-definitions.js";
import { interestsDetail, skillsDetail } from "../../details/person.js";
import { findLatestIntervallEndInJsonLdOrNowAndAddMillis } from "../../../app/won-utils.js";
import {
  industryDetail,
  employmentTypesDetail,
  organizationNamesDetail,
} from "../../details/jobs.js";
import { jobLocation } from "../../details/location.js";
import { sparqlQuery } from "../../../app/sparql-builder-utils.js";

import won from "../../../app/won-es6.js";
import { getIn, is } from "../../../app/utils.js";

export const jobSearch = {
  identifier: "jobSearch",
  label: "Search a Job",
  // icon: "#ico36_uc_find_people", TODO
  doNotMatchAfter: findLatestIntervallEndInJsonLdOrNowAndAddMillis,
  draft: {
    ...emptyDraft,
    is: {
      "@type": "s:Person",
      tags: ["search-job"],
    },
    seeks: {
      "@type": "s:JobPosting",
    },
    searchString: ["offer-job"],
  },
  isDetails: {
    title: { ...details.title },
    description: { ...details.description },
    // location: { ...details.location }, // why would your current location of residency matter?
    // person: { ...details.person }, // has current company fields, that are weird for a search
    skills: { ...skillsDetail },
    interests: { ...interestsDetail },
  },
  seeksDetails: {
    description: { ...details.description },
    jobLocation: { ...jobLocation },
    industry: { ...industryDetail },
    employmentTypes: { ...employmentTypesDetail },
    organizationNames: { ...organizationNamesDetail },
  },

  /**
   *
   * e.g.: with just industries:
   * ```
   * # index for industries using binds
   * prefix s: <http://schema.org/>
   * prefix won:   <http://purl.org/webofneeds/model#>
   * select distinct * where {
   *   {
   *     select
   *       ${resultName}
   *       (sum(?var1) + sum(?var2) as ?targetOverlap)
   *       (count(${resultName}) as ?targetTotal)
   *     where {
   *       ${resultName} a won:Need;
   *             won:is ?is.
   *             ?is s:industry ?industry .
   *       bind(if(str(?industry) = "design",1,0) as ?var1)
   *       bind(if(str(?industry) = "computer science",1,0) as ?var2)
   *     } group by (${resultName})
   *   }
   *   bind (?targetOverlap / ( ?targetTotal + 2 - ?targetOverlap ) as ?jaccardIndex )
   * } order by desc(?jaccardIndex)
   * limit 100
   * ```
   */
  generateQuery: (draft, resultName) => {
    const pathInDraft = ["seeks", "industry"];
    const sparqlPredicatePath = "won:is/s:industry";
    const sparqlVarName = "?industry";

    const tagLikes = getIn(draft, pathInDraft);
    if (!is("Array", tagLikes)) {
      console.error("Expected array, got ", tagLikes);
      return;
    }

    // ----------------

    if (tagLikes.length == 0) {
      // TODO don't generate sub-query
    }
    const partialSums = Object.keys(tagLikes).map(idx => `sum(?var${idx})`);
    const targetOverlapSelect =
      "(" + partialSums.join(" + ") + " as ?targetOverlap)"; // TODO prefix/suffix variable to make it unique
    const targetTotalSelect = `(count(${resultName}) as ?targetTotal)`; // TODO prefix/suffix variable to make it unique

    const bindOps = Object.entries(tagLikes).map(
      ([idx, tagLike]) =>
        `bind(if(str(${sparqlVarName}) = "${tagLike}",1,0) as ?var${idx})` // TODO prefix/suffix variable to make it unique
    );

    const innerQuery = sparqlQuery({
      prefixes: {
        s: won.defaultContext["s"], // TODO needs to be moved to outermost query
        won: won.defaultContext["won"],
      },
      variables: [resultName, targetOverlapSelect, targetTotalSelect],
      //  ${resultName} (sum(?var1) + sum(?var2) as ?targetOverlap) (count(${resultName}) as ?targetTotal) {
      where: [
        `${resultName} a won:Need .`,
        `${resultName} ${sparqlPredicatePath} ${sparqlVarName} .`,
        ...bindOps,
      ],
      groupBy: resultName,
    });

    // const query = sparqlQuery({
    //   prefixes: { /* get prefixes from inner query */}
    // })
    /*
   *
   * e.g.: with just industries:
   * ```
   * # index for industries using binds
   * prefix s: <http://schema.org/>
   * prefix won:   <http://purl.org/webofneeds/model#>
   * select distinct * where {
   *   {
   *     select
   *       ?needUri 
   *       (sum(?var1) + sum(?var2) as ?targetOverlap)
   *       (count(?needUri as ?targetTotal)
   *     where {
   *       needUria won:Need;
   *             won:is ?is.
   *             ?is s:industry ?industry .
   *       bind(if(str(?industry) = "design",1,0) as ?var1)
   *       bind(if(str(?industry) = "computer science",1,0) as ?var2)
   *     } group by (?needUri)
   *   }
   *   bind (?targetOverlap / ( ?targetTotal + 2 - ?targetOverlap ) as ?jaccardIndex )
   * } order by desc(?jaccardIndex)
   * limit 100
   * ```
   */

    console.log(
      draft,
      resultName,
      innerQuery,
      innerQuery.prefixes,
      tagLikes,
      "deleteme"
    );
    //
    //
    //
    //     const seeksBranch = draft && draft.seeks;
    //     const rentRange = seeksBranch && seeksBranch.rentRange;
    //     const floorSizeRange = seeksBranch && seeksBranch.floorSizeRange;
    //     const numberOfRoomsRange =
    //       seeksBranch && seeksBranch.numberOfRoomsRange;
    //     const location = seeksBranch && seeksBranch.location;
    //     const filters = [
    //       {
    //         // to select is-branch
    //         prefixes: {
    //           won: won.defaultContext["won"],
    //         },
    //         operations: [
    //           `${resultName} a won:Need.`,
    //           `${resultName} won:is ?is.`,
    //           location && "?is won:hasLocation ?location.",
    //         ],
    //       },
    //       rentRange &&
    //         filterRentRange(
    //           "?is",
    //           rentRange.min,
    //           rentRange.max,
    //           rentRange.currency
    //         ),
    //       floorSizeRange &&
    //         filterFloorSizeRange("?is", floorSizeRange.min, floorSizeRange.max),
    //       numberOfRoomsRange &&
    //         filterNumOfRoomsRange(
    //           "?is",
    //           numberOfRoomsRange.min,
    //           numberOfRoomsRange.max
    //         ),
    //       filterInVicinity("?location", location),
    //     ];
    //     const concatenatedFilter = concatenateFilters(filters);
    //     return sparqlQuery({
    //       prefixes: concatenatedFilter.prefixes,
    //       selectDistinct: resultName,
    //       where: concatenatedFilter.operations,
    //       orderBy: [
    //         {
    //           order: "ASC",
    //           variable: "?location_geoDistance",
    //         },
    //       ],
    //     });
    //   },
    // },
  },
};
