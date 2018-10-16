import { details, emptyDraft } from "../../detail-definitions.js";
import { interestsDetail, skillsDetail } from "../../details/person.js";
import { findLatestIntervallEndInJsonLdOrNowAndAddMillis } from "../../../app/won-utils.js";
import {
  industryDetail,
  employmentTypesDetail,
  organizationNamesDetail,
} from "../../details/jobs.js";
import { jobLocation } from "../../details/location.js";
import {
  sparqlQuery,
  vicinityScoreSubQuery,
} from "../../../app/sparql-builder-utils.js";

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
    // skills
    // const pathInDraft = ["is", "skills"];
    // const sparqlPredicatePath = "won:seeks/s:knowsAbout";
    // const sparqlVarName = "?skills";

    // hiringOrganizationName
    // const pathInDraft = ["seeks", "organizationNames"];
    // const sparqlPredicatePath = "won:is/s:hiringOrganization/s:name";
    // const sparqlVarName = "?organizationName";

    // employmentType
    // const pathInDraft = ["seeks", "employmentTypes"];
    // const sparqlPredicatePath = "won:is/s:employmentType";
    // const sparqlVarName = "?employmentType";

    // industry:
    const fieldName = "industry";
    const pathInDraft = ["seeks", "industry"];
    const sparqlPredicatePath = "won:is/s:industry";
    const sparqlVarName = "?" + fieldName;
    const targetTotalVar = `?${fieldName}_targetTotal`;
    const targetOverlapVar = `?${fieldName}_targetOverlap`;
    const jaccardIndexVar = `?${fieldName}_jaccardIndex`;

    const tagLikes = getIn(draft, pathInDraft);
    if (!is("Array", tagLikes)) {
      console.error("Expected array, got ", tagLikes);
      return;
    }

    // ----------------

    if (tagLikes.length == 0) {
      // TODO don't generate sub-query
    }

    // ?varX is 1 if the tag-like occurs in the match
    const bindOps = Object.entries(tagLikes).map(
      ([idx, tagLike]) =>
        `bind(if(str(${sparqlVarName}) = "${tagLike}",1,0) as ?var${idx})` // TODO prefix/suffix variable to make it unique
    );

    // operations to sum up to cardinality/size of intersection
    const partialSums = Object.keys(tagLikes).map(idx => `sum(?var${idx})`);
    const targetOverlapSelect = `( ${partialSums.join(
      " + "
    )} as ${targetOverlapVar} )`; // TODO prefix/suffix variable to make it unique

    // operations to sum up to cardinality/size of union
    const targetTotalSelect = `(count(${resultName}) as ${targetTotalVar})`; // TODO prefix/suffix variable to make it unique

    // sub-query that actually calculates cardinality of union and intersection
    const subQuery = sparqlQuery({
      prefixes: {
        s: won.defaultContext["s"],
        won: won.defaultContext["won"],
      },
      //  ?result (sum(?var1) + sum(?var2) as ?targetOverlap) (count(${resultName}) as ?targetTotal) {
      variables: [resultName, targetOverlapSelect, targetTotalSelect],
      where: [
        `${resultName} a won:Need .`,
        `${resultName} ${sparqlPredicatePath} ${sparqlVarName} .`,
        ...bindOps,
      ],
      groupBy: resultName,
    });

    const jobLocation = getIn(draft, ["seeks", "jobLocation"]); // TODO move to better place

    // outer query that calculates jaccard-index (see https://en.wikipedia.org/wiki/Jaccard_index)
    const query = sparqlQuery({
      prefixes: {
        s: won.defaultContext["s"],
        won: won.defaultContext["won"],
      },
      variables: [resultName],
      distinct: true,
      where: [
        `bind (${targetOverlapVar} / ( ${targetTotalVar} + ${
          tagLikes.length
        } - ${targetOverlapVar} ) as ${jaccardIndexVar} )`, // intersection over union, see https://en.wikipedia.org/wiki/Jaccard_index
        `filter(${jaccardIndexVar} > 0)`, // filter out posts without any common tag-likes
        jobLocation && `${resultName} won:is/s:jobLocation ?jobLocation.`,
      ],
      subQueries: [subQuery],
      orderBy: {
        order: "DESC",
        variable: jaccardIndexVar, // TODO combined sort
      },
    });

    const vicinity = vicinityScoreSubQuery(
      resultName,
      "?jobLocation_geoScore",
      "won:is/s:jobLocation",
      jobLocation
    );

    console.log("job location filter deleteme 2: ", vicinity);

    // console.log(draft, resultName, subQuery, tagLikes, query, "deleteme");

    return query;
  },
};
