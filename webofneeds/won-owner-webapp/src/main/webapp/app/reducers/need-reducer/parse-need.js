import Immutable from "immutable";
import won from "../../won-es6.js";
import { getAllDetails } from "../../won-utils.js";
import { isWhatsNewNeed, isWhatsAroundNeed } from "../../need-utils.js";

export function parseNeed(jsonldNeed, ownNeed) {
  const jsonldNeedImm = Immutable.fromJS(jsonldNeed);

  let parsedNeed = {
    uri: undefined,
    nodeUri: undefined,

    types: undefined,

    facets: undefined,
    flags: undefined,

    state: undefined,
    connections: Immutable.Map(),
    unread: false,
    ownNeed: !!ownNeed,

    isBeingCreated: false,
    toLoad: false,
    isLoading: false,

    matchingContexts: undefined,
    searchString: undefined,
    jsonld: jsonldNeed,

    heldBy: undefined,
    holds: undefined,

    content: undefined,
    seeks: undefined,

    creationDate: undefined,
    lastUpdateDate: undefined,
  };

  if (jsonldNeedImm) {
    parsedNeed.uri = jsonldNeedImm.get("@id");

    if (!parsedNeed.uri) {
      //If there was no uri we do not add the need
      return undefined;
    }

    parsedNeed.nodeUri = jsonldNeedImm.getIn(["won:hasWonNode", "@id"]);

    const wonHasMatchingContexts = jsonldNeedImm.get("won:hasMatchingContext");

    const creationDate =
      jsonldNeedImm.get("dct:created") ||
      jsonldNeedImm.get("http://purl.org/dc/terms/created");
    if (creationDate) {
      parsedNeed.creationDate = new Date(creationDate);
      parsedNeed.lastUpdateDate = parsedNeed.creationDate;
    }

    // we use to check for active
    // state and everything else
    // will be inactive
    parsedNeed.state =
      jsonldNeedImm.getIn([won.WON.isInStateCompacted, "@id"]) ===
      won.WON.ActiveCompacted
        ? won.WON.ActiveCompacted
        : won.WON.InactiveCompacted;

    parsedNeed.heldBy = jsonldNeedImm.getIn(["won:heldBy", "@id"]);

    const detailsToParse = getAllDetails();

    parsedNeed.holds =
      won.parseListFrom(jsonldNeedImm, ["won:holds"], "xsd:ID") ||
      Immutable.List();

    parsedNeed.heldBy = won.parseFrom(jsonldNeedImm, ["won:heldBy"], "xsd:ID");

    parsedNeed.types = (rawTypes => {
      if (Immutable.List.isList(rawTypes)) {
        return Immutable.Set(rawTypes);
      } else {
        return Immutable.Set([rawTypes]);
      }
    })(jsonldNeedImm.get("@type"));

    parsedNeed.searchString = jsonldNeedImm.get("won:hasSearchString");
    parsedNeed.content = generateContent(jsonldNeedImm, detailsToParse);
    parsedNeed.seeks = generateContent(
      jsonldNeedImm.get("won:seeks"),
      detailsToParse
    );

    parsedNeed.matchingContexts = wonHasMatchingContexts
      ? Immutable.List.isList(wonHasMatchingContexts)
        ? wonHasMatchingContexts
        : Immutable.List.of(wonHasMatchingContexts)
      : undefined;
    parsedNeed.flags = extractFlags(jsonldNeedImm.get("won:hasFlag"));
    parsedNeed.facets = extractFacets(jsonldNeedImm.get("won:hasFacet"));
    parsedNeed.humanReadable = getHumanReadableStringFromNeed(
      parsedNeed,
      detailsToParse
    );
  } else {
    console.error(
      "Cant parse need, data is an invalid need-object: ",
      jsonldNeedImm && jsonldNeedImm.toJS()
    );
    return undefined;
  }

  return Immutable.fromJS(parsedNeed);
}

/**
 * Tries to extract all the detailsToParse from the given contentJsonLd
 * uses the parseFromRdf function defined in the detail to extract the content
 * uses the detail identifier as the key of the contentDetail that is to be added
 * @param contentJsonLd
 * @param detailsToParse
 * @returns {title: *, tags: *}
 */
function generateContent(contentJsonLd, detailsToParse) {
  let content = {};
  if (contentJsonLd && contentJsonLd.size > 0 && detailsToParse) {
    for (const detailKey in detailsToParse) {
      const detailToParse = detailsToParse[detailKey];
      const detailIdentifier = detailToParse && detailToParse.identifier;
      const detailValue =
        detailToParse && detailToParse.parseFromRDF(contentJsonLd);

      if (detailIdentifier && detailValue) {
        content[detailIdentifier] = detailValue;
      }
    }
  }

  return content;
}

function extractFlags(wonHasFlags) {
  let flags = Immutable.List();

  wonHasFlags &&
    wonHasFlags.map(function(flag) {
      if (flag instanceof Immutable.Map) {
        flags = flags.push(flag.get("@id"));
      } else {
        flags = flags.push(flag);
      }
    });

  return flags;
}

function extractFacets(wonHasFacets) {
  let facets = Immutable.Map();

  if (wonHasFacets) {
    if (Immutable.List.isList(wonHasFacets)) {
      wonHasFacets.map(facet => {
        facets = facets.set(facet.get("@id"), facet.get("@type"));
      });
      return facets;
    } else {
      return facets.set(wonHasFacets.get("@id"), wonHasFacets.get("@type"));
    }
  }
  return facets;
}

function getHumanReadableStringFromNeed(need, detailsToParse) {
  if (need && detailsToParse) {
    const needContent = need.content;
    const seeksBranch = need.seeks;

    const title = needContent && needContent.title;
    const seeksTitle = seeksBranch && seeksBranch.title;

    if (isWhatsNewNeed(Immutable.fromJS(need))) {
      return "What's New";
    }

    if (isWhatsAroundNeed(Immutable.fromJS(need))) {
      let location = needContent["location"] || seeksBranch["location"];

      const locationJS =
        location && Immutable.Iterable.isIterable(location)
          ? location.toJS()
          : location;

      return (
        "What's Around " +
        detailsToParse["location"].generateHumanReadable({
          value: locationJS,
          includeLabel: false,
        })
      );
    }

    if (title && seeksTitle) {
      return title + " - " + seeksTitle;
    } else if (seeksTitle) {
      return seeksTitle;
    } else if (title) {
      return title;
    } else {
      const searchString = need && need.searchString;

      if (searchString) {
        return "Search: " + searchString;
      }
    }

    let humanReadableDetails = generateHumanReadableArray(
      needContent,
      detailsToParse
    );
    let humanReadableSeeksDetails = generateHumanReadableArray(
      seeksBranch,
      detailsToParse
    );

    if (
      humanReadableDetails.length > 0 &&
      humanReadableSeeksDetails.length > 0
    ) {
      return (
        humanReadableDetails.join(" ") +
        " Seeks: " +
        humanReadableSeeksDetails.join(", ")
      );
    } else if (humanReadableDetails.length > 0) {
      return humanReadableDetails.join(", ");
    } else if (humanReadableSeeksDetails.length > 0) {
      return humanReadableSeeksDetails.join(", ");
    }
  }
  return undefined;
}

function generateHumanReadableArray(presentDetails, detailsToParse) {
  let humanReadableArray = [];
  if (presentDetails) {
    for (const key in presentDetails) {
      const detailToParse = detailsToParse[key];
      if (detailToParse) {
        const detailValue = presentDetails[key];
        const detailValueJS =
          detailValue && Immutable.Iterable.isIterable(detailValue)
            ? detailValue.toJS()
            : detailValue;

        if (detailValueJS) {
          humanReadableArray.push(
            detailToParse.generateHumanReadable({
              value: detailValueJS,
              includeLabel: true,
            })
          );
        }
      }
    }
  }
  return humanReadableArray;
}
