/**
 * Created by fsuda on 08.11.2018.
 */

import won from "./won-es6.js";
import { get, getIn, calculateDistance } from "./utils.js";
import { labels } from "./won-label-utils.js";
import * as connectionUtils from "./connection-utils.js";
import * as useCaseUtils from "./usecase-utils.js";

/**
 * Determines if a given atom is a Active
 * @param atom
 * @returns {*|boolean}
 */
export function isActive(atom) {
  return get(atom, "state") === won.WON.ActiveCompacted;
}

/**
 * Determines if a given atom is a Inactive
 * @param atom
 * @returns {*|boolean}
 */
export function isInactive(atom) {
  return get(atom, "state") === won.WON.InactiveCompacted;
}

export function getIdenticonSvg(atom) {
  return get(atom, "identiconSvg");
}

export function getMatchedUseCaseIcon(atom) {
  return getIn(atom, ["matchedUseCase", "icon"]);
}

export function getBackground(atom) {
  return get(atom, "background");
}

export function getMatchedUseCaseIdentifier(atom) {
  return getIn(atom, ["matchedUseCase", "identifier"]);
}

export function getReactionUseCases(atom) {
  return getIn(atom, ["matchedUseCase", "reactionUseCases"]);
}

export function hasReactionUseCases(atom) {
  const reactionUseCases = getReactionUseCases(atom);
  return !!reactionUseCases && reactionUseCases.size > 0;
}

export function getEnabledUseCases(atom) {
  return getIn(atom, ["matchedUseCase", "enabledUseCases"]);
}

export function hasEnabledUseCases(atom) {
  const enabledUseCases = getEnabledUseCases(atom);
  return !!enabledUseCases && enabledUseCases.size > 0;
}

export function hasMatchedUseCase(atom) {
  return !!getIn(atom, ["matchedUseCase", "identifier"]);
}

export function hasImages(atom) {
  return (
    !!getIn(atom, ["content", "images"]) || !!getIn(atom, ["seeks", "images"])
  );
}

export function hasLocation(atom) {
  return (
    !!getIn(atom, ["content", "jobLocation"]) ||
    !!getIn(atom, ["content", "location"]) ||
    !!getIn(atom, ["seeks", "jobLocation"]) ||
    !!getIn(atom, ["seeks", "location"])
  );
}

/**
 * Returns the first location found in the content of the atom
 * jobLocation has priority over "default" location
 * If no location is present undefined will be returned
 * @param atom
 * @returns {*}
 */
export function getLocation(atom) {
  if (hasLocation(atom)) {
    return (
      getIn(atom, ["content", "jobLocation"]) ||
      getIn(atom, ["content", "location"]) ||
      getIn(atom, ["seeks", "jobLocation"]) ||
      getIn(atom, ["seeks", "location"])
    );
  }
  return undefined;
}

/**
 * Get distance between the first found atomLocation and the location (parameter)
 * Distance in meters, undefined if atom has no location or location parameter is undefined
 * Priority of atomLocation is defined within getLocation
 * @param atom
 * @param location
 * @returns {*}
 */
export function getDistanceFrom(atom, location) {
  const atomLocation = getLocation(atom);

  return calculateDistance(atomLocation, location);
}

/**
 * Returns the "Default" Image (currently the content branch is checked before seeks) of an atom
 * if the atom does not have any images we return undefined
 * @param atom
 */
export function getDefaultImage(atom) {
  if (hasImages(atom)) {
    const contentImages = getIn(atom, ["content", "images"]);

    if (contentImages) {
      const defaultImage = contentImages.find(image => get(image, "default"));

      if (defaultImage) {
        return defaultImage;
      }
    }

    const seeksImages = getIn(atom, ["content", "images"]);

    if (seeksImages) {
      const defaultImage = seeksImages.find(image => get(image, "default"));

      if (defaultImage) {
        return defaultImage;
      } else {
        return seeksImages.first();
      }
    } else {
      return contentImages.first();
    }
  }
  return undefined;
}

/**
 * If an atom is active and has the chat or the group socket we can connect to it.
 * @param atom
 */
export function isConnectible(atom) {
  return isActive(atom) && (hasChatSocket(atom) || hasGroupSocket(atom));
}

/**
 * Determines if a given atom is a DirectResponse-Atom
 * @param atom
 * @returns {*|boolean}
 */
export function isDirectResponseAtom(atom) {
  return (
    getIn(atom, ["content", "flags"]) &&
    getIn(atom, ["content", "flags"]).contains("con:DirectResponse")
  );
}

/**
 * Determines if a given atom is Invisible (contains the no hint for counterpart flag)
 * @param atom
 * @returns {*|boolean}
 */
export function isInvisibleAtom(atom) {
  return (
    getIn(atom, ["content", "flags"]) &&
    getIn(atom, ["content", "flags"]).contains("match:NoHintForCounterpart")
  );
}

export function isPersona(atom) {
  return (
    getIn(atom, ["content", "type"]) &&
    getIn(atom, ["content", "type"]).has("won:Persona")
  );
}

export function isAtom(atom) {
  return (
    getIn(atom, ["content", "type"]) &&
    getIn(atom, ["content", "type"]).has("won:Atom")
  );
}

export function hasChatSocket(atom) {
  return hasSocket(atom, won.CHAT.ChatSocketCompacted);
}

export function hasGroupSocket(atom) {
  return hasSocket(atom, won.GROUP.GroupSocketCompacted);
}

export function hasHoldableSocket(atom) {
  return hasSocket(atom, won.HOLD.HoldableSocketCompacted);
}

export function hasHolderSocket(atom) {
  return hasSocket(atom, won.HOLD.HolderSocketCompacted);
}

export function hasReviewSocket(atom) {
  return hasSocket(atom, won.REVIEW.ReviewSocketCompacted);
}

export function hasSocket(atom, socket) {
  return (
    getIn(atom, ["content", "sockets"]) &&
    getIn(atom, ["content", "sockets"]).contains(socket)
  );
}

export function hasSuggestedConnections(atom) {
  return (
    get(atom, "connections") &&
    !!get(atom, "connections").find(conn => connectionUtils.isSuggested(conn))
  );
}

export function getSuggestedConnections(atom) {
  return (
    get(atom, "connections") &&
    get(atom, "connections").filter(conn => connectionUtils.isSuggested(conn))
  );
}

export function hasUnreadSuggestedConnections(atom) {
  return (
    get(atom, "connections") &&
    !!get(atom, "connections").find(
      conn => connectionUtils.isSuggested(conn) && get(conn, "unread")
    )
  );
}

/**
 * Determines if a given atom is a Search-Atom (see draft in create-search.js)
 * @param atom
 * @returns {*|boolean}
 */
export function isSearchAtom(atom) {
  return (
    getIn(atom, ["content", "type"]) &&
    getIn(atom, ["content", "type"]).has("demo:PureSearch")
  );
}

/**
 * Generates an array that contains all atom flags, using a human readable label if available.
 */
export function generateFullFlagLabels(atomImm) {
  const flags = atomImm && atomImm.getIn(["content", "flags"]);
  const flagsArray =
    flags &&
    flags
      .toArray()
      // use nicer socket labels if available
      // TODO: remove this to match RDF state?
      .map(flag => (labels.flags[flag] ? labels.flags[flag] : flag));
  return flagsArray;
}

/**
 * Generates an array that contains all atom sockets, using a human readable label if available.
 */
export function generateFullSocketLabels(atomImm) {
  const sockets = atomImm && atomImm.getIn(["content", "sockets"]);
  const socketsArray =
    sockets &&
    sockets
      .toArray()
      // use nicer socket labels if available
      // TODO: remove this to match RDF state?
      .map(
        socket => (labels.sockets[socket] ? labels.sockets[socket] : socket)
      );
  return socketsArray;
}

/**
 * Retrieves the Label of the used useCase as an atomType, if no usecase is specified we check if atom is a searchAtom or DirectResponseAtom
 * @param {*} atomImm the atom as saved in the state
 */
export function generateTypeLabel(atomImm) {
  const useCase = useCaseUtils.getUseCase(getMatchedUseCaseIdentifier(atomImm));

  if (useCase) {
    return useCase.label;
  } else {
    if (isSearchAtom(atomImm)) {
      return "Search";
    } else if (isDirectResponseAtom(atomImm)) {
      return "Direct Response";
    }

    return "";
  }
}

/**
 * Generates an array that contains some atom sockets, using a human readable label if possible.
 */
export function generateShortSocketLabels(atomImm) {
  const sockets = atomImm && atomImm.get(["content", "sockets"]);
  const socketsArray =
    sockets &&
    sockets
      .toArray()
      // rename sockets
      // TODO: check if this can be used anywhere or whether it should be Group Chat Enabled
      .map(socket => {
        if (socket === won.GROUP.GroupSocketCompacted) {
          return labels.sockets[socket] ? labels.sockets[socket] : socket;
        }
        return "";
      })
      .filter(socket => socket.length > 0);
  return socketsArray;
}

/**
 * Generates an array that contains some atom flags, using a human readable label if possible.
 */
export function generateShortFlagLabels(atomImm) {
  const flags = atomImm && atomImm.getIn(["content", "flags"]);
  const flagsArray =
    flags &&
    flags
      .toArray()
      // rename flags
      // TODO: flags should have explanatory hovertext
      .map(flag => {
        if (flag === won.WONMATCH.NoHintForCounterpartCompacted) {
          return labels.flags[flag] ? labels.flags[flag] : flag;
        }
        if (flag === won.WONMATCH.NoHintForMeCompacted) {
          return labels.flags[flag] ? labels.flags[flag] : flag;
        }
        return "";
      })
      .filter(flag => flag.length > 0);
  return flagsArray;
}

export function getSocketsWithKeysReset(atomImm) {
  const sockets = getIn(atomImm, ["content", "sockets"]);

  if (sockets) {
    return getSocketKeysReset(sockets);
  }
  return undefined;
}

export function getDefaultSocketWithKeyReset(atomImm) {
  const defaultSocket = getIn(atomImm, ["content", "defaultSocket"]);

  if (defaultSocket) {
    return getSocketKeysReset(defaultSocket);
  }
  return undefined;
}

export function getSeeksSocketsWithKeysReset(atomImm) {
  const sockets = getIn(atomImm, ["seeks", "sockets"]);

  if (sockets) {
    return getSocketKeysReset(sockets);
  }
  return undefined;
}

export function getSeeksDefaultSocketWithKeyReset(atomImm) {
  const defaultSocket = getIn(atomImm, ["seeks", "defaultSocket"]);

  if (defaultSocket) {
    return getSocketKeysReset(defaultSocket);
  }
  return undefined;
}

/**
 * Sorts the elements by distance from given location (default order is ascending)
 * @param elementsImm elements from state that need to be returned as a sorted array
 * @param location given location to calculate the distance from
 * @param order if "DESC" then the order will be descending, everything else resorts to the default sort of ascending order
 * @returns {*} sorted Elements array
 */
export function sortByDistanceFrom(atomsImm, location, order = "ASC") {
  let sortedAtoms = atomsImm && atomsImm.toArray();

  if (sortedAtoms) {
    sortedAtoms.sort(function(a, b) {
      const bDist = getDistanceFrom(b, location);
      const aDist = getDistanceFrom(a, location);

      if (order === "DESC") {
        return bDist - aDist;
      } else {
        return aDist - bDist;
      }
    });
  }

  return sortedAtoms;
}

function getSocketKeysReset(socketsImm) {
  return socketsImm.mapKeys((key, value) => {
    if (value === "chat:ChatSocket") {
      return "#chatSocket";
    }
    if (value === "group:GroupSocket") {
      return "#groupSocket";
    }
    if (value === "hold:HolderSocket") {
      return "#holderSocket";
    }
    if (value === "hold:HoldableSocket") {
      return "#holdableSocket";
    }
    if (value === "review:ReviewSocket") {
      return "#reviewSocket";
    }
    if (value === "buddy:BuddySocket") {
      return "#buddySocket";
    }
  });
}
