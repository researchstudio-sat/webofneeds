import Immutable from "immutable";
import won from "../../won-es6.js";
import * as useCaseUtils from "../../usecase-utils.js";
import {
  isWhatsAroundNeed,
  isSearchNeed,
  isPersona,
} from "../../need-utils.js";
import { generateHexColor, generateRgbColorArray, getIn } from "../../utils.js";
import shajs from "sha.js";
import Identicon from "identicon.js";

export function parseNeed(jsonldNeed) {
  const jsonldNeedImm = Immutable.fromJS(jsonldNeed);

  if (jsonldNeedImm && jsonldNeedImm.get("@id")) {
    const detailsToParse = useCaseUtils.getAllDetails();

    let parsedNeed = {
      uri: jsonldNeedImm.get("@id"),
      identiconSvg: generateIdenticon(jsonldNeedImm),
      nodeUri: jsonldNeedImm.getIn(["won:hasWonNode", "@id"]),
      state: extractState(jsonldNeedImm),
      heldBy: won.parseFrom(jsonldNeedImm, ["won:heldBy"], "xsd:ID"),
      holds:
        won.parseListFrom(jsonldNeedImm, ["won:holds"], "xsd:ID") ||
        Immutable.List(),
      rating: extractRating(jsonldNeedImm),
      groupMembers:
        won.parseListFrom(jsonldNeedImm, ["won:hasGroupMember"], "xsd:ID") ||
        Immutable.List(),
      content: generateContent(jsonldNeedImm, detailsToParse),
      seeks: generateContent(jsonldNeedImm.get("won:seeks"), detailsToParse),
      creationDate: extractCreationDate(jsonldNeedImm),
      lastUpdateDate: extractCreationDate(jsonldNeedImm), //Used for sorting/updates (e.g. if connection comes in etc...)
      modifiedDate: extractLastModifiedDate(jsonldNeedImm), //Used as a flag if the need itself has changed (e.g. need edit)
      humanReadable: undefined, //can only be determined after we generated The Content
      matchedUseCase: {
        identifier: undefined,
        icon: undefined,
        enabledUseCases: undefined,
        reactionUseCases: undefined,
      },
      background: generateBackground(jsonldNeedImm),
      unread: false,
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

    let parsedNeedImm = Immutable.fromJS(parsedNeed);

    if (!isPersona(parsedNeedImm)) {
      const matchingUseCase = useCaseUtils.findUseCaseByNeed(parsedNeedImm);

      if (matchingUseCase) {
        parsedNeedImm = parsedNeedImm
          .setIn(["matchedUseCase", "identifier"], matchingUseCase.identifier)
          .setIn(["matchedUseCase", "icon"], matchingUseCase.icon)
          .setIn(
            ["matchedUseCase", "enabledUseCases"],
            matchingUseCase.enabledUseCases
              ? Immutable.fromJS(matchingUseCase.enabledUseCases)
              : Immutable.List()
          )
          .setIn(
            ["matchedUseCase", "reactionUseCases"],
            matchingUseCase.reactionUseCases
              ? Immutable.fromJS(matchingUseCase.reactionUseCases)
              : Immutable.List()
          );
      }
    }

    return parsedNeedImm;
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

function generateIdenticon(needJsonLd) {
  const needUri = needJsonLd.get("@id");

  if (!needUri) {
    return;
  }
  // quick extra hash here as identicon.js only uses first 15
  // chars (which aren't very unique for our uris due to the base-url):
  const hash = new shajs.sha512().update(needUri).digest("hex");
  const rgbColorArray = generateRgbColorArray(hash);
  const idc = new Identicon(hash, {
    size: 100,
    foreground: [255, 255, 255, 255], // rgba white
    background: [...rgbColorArray, 255], // rgba
    margin: 0.2,
    format: "svg",
  });
  return idc.toString();
}

function generateBackground(needJsonLd) {
  const needUri = needJsonLd.get("@id");

  if (!needUri) {
    return;
  }

  const hash = new shajs.sha512().update(needUri).digest("hex");
  return generateHexColor(hash);
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

function extractLastModifiedDate(needJsonLd) {
  const lastModifiedDate =
    needJsonLd.get("dct:modified") ||
    needJsonLd.get("http://purl.org/dc/terms/modified");
  if (lastModifiedDate) {
    return new Date(lastModifiedDate);
  }
  return undefined;
}

function extractRating(needJsonLd) {
  const reviews = needJsonLd.get("won:reviews");
  const reviewedConnection = needJsonLd.get("won:reviewedConnection");

  const rating = {
    aggregateRating:
      needJsonLd.get("s:aggregateRating") &&
      parseFloat(needJsonLd.get("s:aggregateRating")),
    reviewCount:
      needJsonLd.get("s:reviewCount") &&
      parseInt(needJsonLd.get("s:reviewCount")),
    reviews: reviews
      ? Immutable.List.isList(reviews)
        ? reviews
        : Immutable.List.of(reviews)
      : undefined,
    reviewedConnection: reviewedConnection
      ? Immutable.List.isList(reviewedConnection)
        ? reviewedConnection
        : Immutable.List.of(reviewedConnection)
      : undefined,
  };
  if (rating.aggregateRating && rating.reviewCount) {
    return rating;
  } else {
    return undefined;
  }
}

function getHumanReadableStringFromNeed(need, detailsToParse) {
  if (need && detailsToParse) {
    const needContent = need.content;
    const seeksBranch = need.seeks;

    const title = needContent && needContent.title;
    const seeksTitle = seeksBranch && seeksBranch.title;

    const immNeed = Immutable.fromJS(need);

    if (isPersona(need)) {
      return getIn(need, ["content", "personaName"]);
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
      if (!(key === "facets" || key === "type" || key === "defaultFacet")) {
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
  }
  return humanReadableArray;
}
