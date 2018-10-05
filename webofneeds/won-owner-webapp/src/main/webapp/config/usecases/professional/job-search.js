import { details, emptyDraft } from "../../detail-definitions.js";
import { interestsDetail, skillsDetail } from "../../details/person.js";
import { findLatestIntervallEndInJsonLdOrNowAndAddMillis } from "../../../app/won-utils.js";
import {
  industryDetail,
  employmentTypesDetail,
  organizationNamesDetail,
} from "../../details/jobs.js";
import { jobLocation } from "../../details/location.js";

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

  generateQuery: (draft, resultName) => {
    console.log(draft, resultName, "deleteme");

    //
    // # index for industries using binds
    // prefix s: <http://schema.org/>
    // prefix won:   <http://purl.org/webofneeds/model#>
    // select distinct * where {
    //   {
    //     select ?need (sum(?var1) + sum(?var2) as ?targetOverlap) (count(?need) as ?targetTotal) where {
    //       ?need a won:Need;
    //             won:is ?is.
    //             ?is s:industry ?value .
    //       bind(if(str(?value) = "foo",1,0) as ?var1)
    //       bind(if(str(?value) = "bar",1,0) as ?var2)
    //     } group by (?need)
    //   }
    //   bind (?targetOverlap / ( ?targetTotal + 2 - ?targetOverlap ) as ?jaccardIndex )
    // } order by desc(?jaccardIndex)
    // limit 100
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
