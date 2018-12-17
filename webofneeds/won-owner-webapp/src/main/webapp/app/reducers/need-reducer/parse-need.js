import Immutable from "immutable";
import won from "../../won-es6.js";
import { getAllDetails } from "../../won-utils.js";
import {
  isWhatsNewNeed,
  isWhatsAroundNeed,
  isSearchNeed,
} from "../../need-utils.js";

export function parseNeed(jsonldNeed, isOwned) {
  const jsonldNeedImm = Immutable.fromJS(jsonldNeed);

  if (jsonldNeedImm && jsonldNeedImm.get("@id")) {
    const detailsToParse = getAllDetails();

    let parsedNeed = {
      uri: jsonldNeedImm.get("@id"),
      nodeUri: jsonldNeedImm.getIn(["won:hasWonNode", "@id"]),
      types: extractTypes(jsonldNeedImm),
      facets: extractFacets(jsonldNeedImm.get("won:hasFacet")),
      state: extractState(jsonldNeedImm),
      matchingContexts: extractMatchingContext(jsonldNeedImm),
      heldBy: won.parseFrom(jsonldNeedImm, ["won:heldBy"], "xsd:ID"),
      holds:
        won.parseListFrom(jsonldNeedImm, ["won:holds"], "xsd:ID") ||
        Immutable.List(),
      content: generateContent(jsonldNeedImm, detailsToParse),
      seeks: generateContent(jsonldNeedImm.get("won:seeks"), detailsToParse),
      creationDate: extractCreationDate(jsonldNeedImm),
      lastUpdateDate: extractCreationDate(jsonldNeedImm),
      humanReadable: undefined, //can only be determined after we generated The Content
      unread: false,
      isOwned: !!isOwned,
      isBeingCreated: false,
      jsonld: jsonldNeed,
      connections: Immutable.Map(),
    };

    if (!parsedNeed.creationDate || !parsedNeed.lastUpdateDate) {
      console.error(
        "Cant parse need, creationDate or lastUpdateDate not set",
        jsonldNeedImm && jsonldNeedImm.toJS()
      );
      return undefined;
    }

    parsedNeed.humanReadable = getHumanReadableStringFromNeed(
      parsedNeed,
      detailsToParse
    );

    return Immutable.fromJS(parsedNeed);
  } else {
    console.error(
      "Cant parse need, data is an invalid need-object: ",
      jsonldNeedImm && jsonldNeedImm.toJS()
    );
    return undefined;
  }
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

function extractState(needJsonLd) {
  // we use to check for active
  // state and everything else
  // will be inactive
  return needJsonLd.getIn([won.WON.isInStateCompacted, "@id"]) ===
    won.WON.ActiveCompacted
    ? won.WON.ActiveCompacted
    : won.WON.InactiveCompacted;
}

function extractMatchingContext(needJsonLd) {
  const wonHasMatchingContexts = needJsonLd.get("won:hasMatchingContext");
  return wonHasMatchingContexts
    ? Immutable.List.isList(wonHasMatchingContexts)
      ? wonHasMatchingContexts
      : Immutable.List.of(wonHasMatchingContexts)
    : undefined;
}

function extractTypes(needJsonLd) {
  const types = (rawTypes => {
    if (Immutable.List.isList(rawTypes)) {
      return Immutable.Set(rawTypes);
    } else {
      return Immutable.Set([rawTypes]);
    }
  })(needJsonLd.get("@type"));

  return types;
}

function extractCreationDate(needJsonLd) {
  const creationDate =
    needJsonLd.get("dct:created") ||
    needJsonLd.get("http://purl.org/dc/terms/created");
  if (creationDate) {
    return new Date(creationDate);
  }
  return undefined;
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

    const immNeed = Immutable.fromJS(need);

    if (isWhatsNewNeed(immNeed)) {
      return "What's New";
    } else if (isWhatsAroundNeed(immNeed)) {
      let location =
        (needContent && needContent["location"]) ||
        (seeksBranch && seeksBranch["location"]);

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
    } else if (isSearchNeed(immNeed)) {
      const searchString = need && need.content && need.content.searchString;

      if (searchString) {
        return "Search: " + searchString;
      }
    }

    if (title && seeksTitle) {
      return title + " - " + seeksTitle;
    } else if (seeksTitle) {
      return seeksTitle;
    } else if (title) {
      return title;
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
