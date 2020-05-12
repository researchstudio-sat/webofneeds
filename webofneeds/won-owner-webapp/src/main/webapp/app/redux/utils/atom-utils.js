/**
 * Created by fsuda on 08.11.2018.
 */

import vocab from "../../service/vocab.js";
import { get, getIn } from "../../utils.js";
import { labels } from "../../won-label-utils.js";
import * as connectionUtils from "./connection-utils.js";
import * as useCaseUtils from "../../usecase-utils.js";
import Immutable from "immutable";

/**
 * Determines if a given atom is a Active
 * @param atom
 * @returns {*|boolean}
 */
export function isActive(atom) {
  return get(atom, "state") === vocab.WON.ActiveCompacted;
}

/**
 * Determines if a given atom is a Inactive
 * @param atom
 * @returns {*|boolean}
 */
export function isInactive(atom) {
  return get(atom, "state") === vocab.WON.InactiveCompacted;
}

export function getIdenticonSvg(atom) {
  return get(atom, "identiconSvg");
}

export function getMatchedUseCaseIcon(atom) {
  return getIn(atom, ["matchedUseCase", "icon"]);
}

export function matchesDefinitions(atom, useCaseDefinitions) {
  return (
    !!useCaseDefinitions &&
    !!useCaseDefinitions.find(useCaseDefinition =>
      matchesDefinition(atom, useCaseDefinition)
    )
  );
}

export function matchesDefinition(atom, useCaseDefinition) {
  return (
    !!useCaseDefinition &&
    get(useCaseDefinition, "identifier") ===
      getMatchedUseCaseIdentifier(atom) &&
    hasSocket(atom, get(useCaseDefinition, "senderSocketType"))
  );
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

  /**
   * Calculates distance between two locations in meters
   * If any of the locations or lat, lng of the location are undefined/null, return undefined
   * @param locationA json {lat, lng]
   * @param locationB json {lat, lng]
   * @returns {number} distance between these two coordinates in meters
   */
  const calculateDistance = (locationA, locationB) => {
    const locationAImm = locationA && Immutable.fromJS(locationA);
    const locationBImm = locationB && Immutable.fromJS(locationB);

    if (
      !locationAImm ||
      !locationAImm.get("lat") ||
      !locationAImm.get("lng") ||
      !locationBImm ||
      !locationBImm.get("lat") ||
      !locationBImm.get("lng")
    ) {
      return;
    }

    const earthRadius = 6371000; // earth radius in meters
    const dLat =
      ((locationBImm.get("lat") - locationAImm.get("lat")) * Math.PI) / 180;
    const dLon =
      ((locationBImm.get("lng") - locationAImm.get("lng")) * Math.PI) / 180;
    const a =
      Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos((locationAImm.get("lat") * Math.PI) / 180) *
        Math.cos((locationBImm.get("lat") * Math.PI) / 180) *
        Math.sin(dLon / 2) *
        Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    const d = earthRadius * c;

    return Math.round(d);
  };

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

    const seeksImages = getIn(atom, ["seeks", "images"]);

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
 * Returns the "Default" Image (currently the content branch is checked) of an atom which is a persona
 * if the atom does not have any images we return undefined
 * @param atom
 */
export function getDefaultPersonaImage(atom) {
  if (hasImages(atom) && isPersona(atom)) {
    const contentImages = getIn(atom, ["content", "images"]);

    if (contentImages) {
      const defaultImage = contentImages.find(image => get(image, "default"));

      if (defaultImage) {
        return defaultImage;
      }
    }
  }
  return undefined;
}

/**
 * Determines if a given atom is a DirectResponse-Atom
 * @param atom
 * @returns {*|boolean}
 */
export function isDirectResponseAtom(atom) {
  return (
    getIn(atom, ["content", "flags"]) &&
    getIn(atom, ["content", "flags"]).contains(
      vocab.WONCON.DirectResponseCompacted
    )
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
    getIn(atom, ["content", "flags"]).contains(
      vocab.WONMATCH.NoHintForCounterpartCompacted
    )
  );
}

export function isPersona(atom) {
  return (
    getIn(atom, ["content", "type"]) &&
    getIn(atom, ["content", "type"]).has(vocab.WON.PersonaCompacted)
  );
}

export function isServiceAtom(atom) {
  return (
    getIn(atom, ["content", "type"]) &&
    getIn(atom, ["content", "type"]).has(vocab.BOT.ServiceAtomCompacted)
  );
}

export function isAtom(atom) {
  return (
    getIn(atom, ["content", "type"]) &&
    getIn(atom, ["content", "type"]).has(vocab.WON.AtomCompacted)
  );
}

export function hasChatSocket(atom) {
  return hasSocket(atom, vocab.CHAT.ChatSocketCompacted);
}

export function hasGroupSocket(atom) {
  return hasSocket(atom, vocab.GROUP.GroupSocketCompacted);
}

export function hasHoldableSocket(atom) {
  return hasSocket(atom, vocab.HOLD.HoldableSocketCompacted);
}

export function hasHolderSocket(atom) {
  return hasSocket(atom, vocab.HOLD.HolderSocketCompacted);
}

export function hasReviewSocket(atom) {
  return hasSocket(atom, vocab.REVIEW.ReviewSocketCompacted);
}

export function hasBuddySocket(atom) {
  return hasSocket(atom, vocab.BUDDY.BuddySocketCompacted);
}

export function hasSocket(atom, socket) {
  const sockets = getSockets(atom);
  return sockets && sockets.contains(socket);
}

export function getChatSocket(atom) {
  return getSocketUri(atom, vocab.CHAT.ChatSocketCompacted);
}

export function getGroupSocket(atom) {
  return getSocketUri(atom, vocab.GROUP.GroupSocketCompacted);
}

export function hasSuggestedConnections(atom) {
  return (
    get(atom, "connections") &&
    !!get(atom, "connections").find(conn => connectionUtils.isSuggested(conn))
  );
}

export function getSuggestedConnections(atom) {
  return get(atom, "connections")
    ? get(atom, "connections").filter(conn => connectionUtils.isSuggested(conn))
    : Immutable.Map();
}

export function getRequestReceivedConnections(atom) {
  return get(atom, "connections")
    ? get(atom, "connections").filter(conn =>
        connectionUtils.isRequestReceived(conn)
      )
    : Immutable.Map();
}

export function getConnectedConnections(atom) {
  return get(atom, "connections")
    ? get(atom, "connections").filter(conn => connectionUtils.isConnected(conn))
    : Immutable.Map();
}

export function hasUnreadSuggestedConnections(atom) {
  return (
    get(atom, "connections") &&
    !!get(atom, "connections").find(
      conn =>
        connectionUtils.isSuggested(conn) && connectionUtils.isUnread(conn)
    )
  );
}

export function getConnectionBySocketUris(atom, socketUri, targetSocketUri) {
  const connections = get(atom, "connections");
  return (
    connections &&
    connections.find(
      conn =>
        get(conn, "socketUri") === socketUri &&
        get(conn, "targetSocketUri") === targetSocketUri
    )
  );
}

// to be used on personas
export function hasUnreadBuddyRequests(atom) {
  return (
    get(atom, "connections") &&
    !!get(atom, "connections").find(
      conn =>
        connectionUtils.isRequestReceived(conn) ||
        connectionUtils.isRequestSent(conn)
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
  const sockets = getSockets(atomImm);
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
  const useCaseLabel = useCaseUtils.getUseCaseLabel(
    getMatchedUseCaseIdentifier(atomImm)
  );

  if (useCaseLabel) {
    return useCaseLabel;
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
  const sockets = getSockets(atomImm);
  const socketsArray =
    sockets &&
    sockets
      .toArray()
      // rename sockets
      // TODO: check if this can be used anywhere or whether it should be Group Chat Enabled
      .map(socket => {
        if (socket === vocab.GROUP.GroupSocketCompacted) {
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
        if (flag === vocab.WONMATCH.NoHintForCounterpartCompacted) {
          return labels.flags[flag] ? labels.flags[flag] : flag;
        }
        if (flag === vocab.WONMATCH.NoHintForMeCompacted) {
          return labels.flags[flag] ? labels.flags[flag] : flag;
        }
        return "";
      })
      .filter(flag => flag.length > 0);
  return flagsArray;
}

export function getSockets(atomImm) {
  return getIn(atomImm, ["content", "sockets"]);
}

export function getSocketTypeArray(atomImm) {
  const sockets = getSockets(atomImm);
  return sockets ? sockets.valueSeq().toArray() : [];
}

export function getSocketsWithKeysReset(atomImm) {
  const sockets = getSockets(atomImm);

  return sockets ? getSocketKeysReset(sockets) : undefined;
}

export function getSocketUri(atomImm, socketType) {
  const sockets = getSockets(atomImm);

  return (
    sockets &&
    sockets
      .filter(type => type === socketType)
      .keySeq()
      .first()
  );
}

export function getSocketType(atomImm, socketUri) {
  return getIn(atomImm, ["content", "sockets", socketUri]);
}

export function getDefaultSocketUri(atomImm) {
  const defaultSocket = getIn(atomImm, ["content", "defaultSocket"]);
  return defaultSocket && defaultSocket.keySeq().first();
}

export function getHeldByUri(atomImm) {
  return hasHoldableSocket(atomImm) ? get(atomImm, "heldBy") : undefined;
}

export function isHeld(atomImm) {
  return !!getHeldByUri(atomImm);
}

export function getHeldAtomUris(atomImm) {
  return hasHolderSocket(atomImm) && get(atomImm, "holds");
}

export function hasHeldAtoms(atomImm) {
  const heldAtomUris = getHeldAtomUris(atomImm);
  return !!heldAtomUris && heldAtomUris.size > 0;
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
    switch (value) {
      case vocab.CHAT.ChatSocketCompacted:
        return "#chatSocket";
      case vocab.GROUP.GroupSocketCompacted:
        return "#groupSocket";
      case vocab.HOLD.HolderSocketCompacted:
        return "#holderSocket";
      case vocab.HOLD.HoldableSocketCompacted:
        return "#holdableSocket";
      case vocab.REVIEW.ReviewSocketCompacted:
        return "#reviewSocket";
      case vocab.BUDDY.BuddySocketCompacted:
        return "#buddySocket";
      default:
        console.warn("Trying to reset an unknown socket: ", value);
        return "#unknownSocket";
    }
  });
}

/**
 * Return all non-closed connections of the given atom
 * @param atomImm immutable atom that stores connections
 * @param socketType compactedSocketType Uri (senderSocket)
 */
export function getNonClosedConnectionsOfAtom(atomImm, socketType) {
  const socketUri = getSocketUri(atomImm, socketType);

  const connections = get(atomImm, "connections");

  return (
    connections &&
    connections
      .filter(conn => !connectionUtils.isClosed(conn))
      .filter(conn => connectionUtils.hasSocketUri(conn, socketUri))
  );
}

/**
 * Return all Active (non-closed) connections of the given atom
 * @param atomImm immutable atom that stores connections
 * @param socketType compactedSocketType Uri (senderSocket)
 */
export function getConnectedConnectionsOfAtom(atomImm, socketType) {
  const socketUri = getSocketUri(atomImm, socketType);

  const connections = get(atomImm, "connections");

  return (
    connections &&
    connections
      .filter(conn => connectionUtils.isConnected(conn))
      .filter(conn => connectionUtils.hasSocketUri(conn, socketUri))
  );
}

export function getAllNonClosedNonSuggestedChatConnections(atomImm) {
  return atomImm
    ? get(atomImm, "connections").filter(
        conn =>
          connectionUtils.hasSocketUri(conn, getChatSocket(atomImm)) &&
          !(connectionUtils.isClosed(conn) || connectionUtils.isSuggested(conn))
      )
    : Immutable.Map();
}

export function hasUnreadNonClosedNonSuggestedChatConnections(atom) {
  return getAllNonClosedNonSuggestedChatConnections(atom).find(conn =>
    connectionUtils.isUnread(conn)
  );
}
