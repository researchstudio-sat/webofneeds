import Immutable from "immutable";
import won from "../../won-es6.js";
import * as useCaseUtils from "../../usecase-utils.js";
import { isSearchAtom, isPersona } from "../../atom-utils.js";
import { generateHexColor, generateRgbColorArray, getIn } from "../../utils.js";
import shajs from "sha.js";
import Identicon from "identicon.js";

export function parseAtom(jsonldAtom) {
  const jsonldAtomImm = Immutable.fromJS(jsonldAtom);

  if (jsonldAtomImm && jsonldAtomImm.get("@id")) {
    const detailsToParse = useCaseUtils.getAllDetails();

    let parsedAtom = {
      uri: jsonldAtomImm.get("@id"),
      identiconSvg: generateIdenticon(jsonldAtomImm),
      nodeUri: jsonldAtomImm.getIn(["won:wonNode", "@id"]),
      state: extractState(jsonldAtomImm),
      heldBy: won.parseFrom(jsonldAtomImm, ["won:heldBy"], "xsd:ID"),
      holds:
        won.parseListFrom(jsonldAtomImm, ["won:holds"], "xsd:ID") ||
        Immutable.List(),
      rating: extractRating(jsonldAtomImm),
      groupMembers:
        won.parseListFrom(jsonldAtomImm, ["won:groupMember"], "xsd:ID") ||
        Immutable.List(),
      content: generateContent(jsonldAtomImm, detailsToParse),
      seeks: generateContent(jsonldAtomImm.get("won:seeks"), detailsToParse),
      creationDate: extractCreationDate(jsonldAtomImm),
      lastUpdateDate: extractCreationDate(jsonldAtomImm), //Used for sorting/updates (e.g. if connection comes in etc...)
      modifiedDate: extractLastModifiedDate(jsonldAtomImm), //Used as a flag if the atom itself has changed (e.g. atom edit)
      humanReadable: undefined, //can only be determined after we generated The Content
      matchedUseCase: {
        identifier: undefined,
        icon: undefined,
        enabledUseCases: undefined,
        reactionUseCases: undefined,
      },
      background: generateBackground(jsonldAtomImm),
      unread: false,
      isBeingCreated: false,
      jsonld: jsonldAtom,
      connections: Immutable.Map(),
    };

    if (!parsedAtom.creationDate || !parsedAtom.lastUpdateDate) {
      console.error(
        "Cant parse atom, creationDate or lastUpdateDate not set",
        jsonldAtomImm && jsonldAtomImm.toJS()
      );
      return undefined;
    }

    parsedAtom.humanReadable = getHumanReadableStringFromAtom(
      parsedAtom,
      detailsToParse
    );

    let parsedAtomImm = Immutable.fromJS(parsedAtom);

    if (!isPersona(parsedAtomImm)) {
      const matchingUseCase = useCaseUtils.findUseCaseByAtom(parsedAtomImm);

      if (matchingUseCase) {
        parsedAtomImm = parsedAtomImm
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

    return parsedAtomImm;
  } else {
    console.error(
      "Cant parse atom, data is an invalid atom-object: ",
      jsonldAtomImm && jsonldAtomImm.toJS()
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

function extractState(atomJsonLd) {
  // we use to check for active
  // state and everything else
  // will be inactive
  return atomJsonLd.getIn([won.WON.atomStateCompacted, "@id"]) ===
    won.WON.ActiveCompacted
    ? won.WON.ActiveCompacted
    : won.WON.InactiveCompacted;
}

function generateIdenticon(atomJsonLd) {
  const atomUri = atomJsonLd.get("@id");

  if (!atomUri) {
    return;
  }
  // quick extra hash here as identicon.js only uses first 15
  // chars (which aren't very unique for our uris due to the base-url):
  const hash = new shajs.sha512().update(atomUri).digest("hex");
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

function generateBackground(atomJsonLd) {
  const atomUri = atomJsonLd.get("@id");

  if (!atomUri) {
    return;
  }

  const hash = new shajs.sha512().update(atomUri).digest("hex");
  return generateHexColor(hash);
}

function extractCreationDate(atomJsonLd) {
  const creationDate =
    atomJsonLd.get("dct:created") ||
    atomJsonLd.get("http://purl.org/dc/terms/created");
  if (creationDate) {
    return new Date(creationDate);
  }
  return undefined;
}

function extractLastModifiedDate(atomJsonLd) {
  const lastModifiedDate =
    atomJsonLd.get("dct:modified") ||
    atomJsonLd.get("http://purl.org/dc/terms/modified");
  if (lastModifiedDate) {
    return new Date(lastModifiedDate);
  }
  return undefined;
}

function extractRating(atomJsonLd) {
  const reviews = atomJsonLd.get("won:reviews");
  const reviewedConnection = atomJsonLd.get("won:reviewedConnection");

  const rating = {
    aggregateRating:
      atomJsonLd.get("s:aggregateRating") &&
      parseFloat(atomJsonLd.get("s:aggregateRating")),
    reviewCount:
      atomJsonLd.get("s:reviewCount") &&
      parseInt(atomJsonLd.get("s:reviewCount")),
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

function getHumanReadableStringFromAtom(atom, detailsToParse) {
  if (atom && detailsToParse) {
    const atomContent = atom.content;
    const seeksBranch = atom.seeks;

    const title = atomContent && atomContent.title;
    const seeksTitle = seeksBranch && seeksBranch.title;

    const immAtom = Immutable.fromJS(atom);

    if (isPersona(atom)) {
      return getIn(atom, ["content", "personaName"]);
    } else if (isSearchAtom(immAtom)) {
      const searchString = atom && atom.content && atom.content.searchString;

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
      atomContent,
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
      if (!(key === "sockets" || key === "type" || key === "defaultSocket")) {
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
