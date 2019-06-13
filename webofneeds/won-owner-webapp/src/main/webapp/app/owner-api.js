import urljoin from "url-join";
import { checkHttpStatus } from "./utils";
import { ownerBaseUrl } from "~/config/default.js";

/**
 * Created by quasarchimaere on 11.06.2019.
 */

export function serverSideConnect(
  socketUri1,
  socketUri2,
  pending1 = false,
  pending2 = false
) {
  return fetch("rest/action/connect", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify([
      {
        pending: pending1,
        socket: socketUri1,
      },
      {
        pending: pending2,
        socket: socketUri2,
      },
    ]),
    credentials: "include",
  });
}

/**
 * Returns all stored Atoms including MetaData (e.g. type, creationDate, location, state) as a Map
 * @param state either "ACTIVE" or "INACTIVE"
 * @returns {*}
 */
export function getOwnedMetaAtoms(state) {
  let paramString = "";
  if (state === "ACTIVE" || state === "INACTIVE") {
    paramString = "?state=" + state;
  }

  return fetch(urljoin(ownerBaseUrl, "/rest/atoms" + paramString), {
    method: "get",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
    },
    credentials: "include",
  })
    .then(checkHttpStatus)
    .then(response => response.json());
}

export function getMessage(atomUri, eventUri) {
  return fetch(
    urljoin(
      ownerBaseUrl,
      "/rest/linked-data/",
      `?requester=${encodeURI(atomUri)}`,
      `&uri=${encodeURI(eventUri)}`
    ),
    {
      method: "get",
      headers: {
        Accept: "application/ld+json",
        "Content-Type": "application/ld+json",
      },
      credentials: "include",
    }
  )
    .then(checkHttpStatus)
    .then(response => response.json());
}

export function getAllMetaAtoms(
  modifiedAfterDate,
  state = "ACTIVE",
  limit = 200
) {
  return fetch(
    urljoin(
      ownerBaseUrl,
      "/rest/atoms/all?state=" +
        state +
        (modifiedAfterDate
          ? "&modifiedafter=" + modifiedAfterDate.toISOString()
          : "") +
        "&limit=" +
        limit
    ),
    {
      method: "get",
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json",
      },
      credentials: "include",
    }
  )
    .then(checkHttpStatus)
    .then(response => response.json());
}

export function getAllMetaAtomsNear(
  modifiedAfterDate,
  location,
  maxDistance = 5000,
  limit = 200,
  state = "ACTIVE"
) {
  if (location && location.lat && location.lng) {
    return fetch(
      urljoin(
        ownerBaseUrl,
        "/rest/atoms/all?state=" +
          state +
          "&limit=" +
          limit +
          "&latitude=" +
          location.lat +
          "&longitude=" +
          location.lng +
          "&maxDistance" +
          maxDistance +
          (modifiedAfterDate
            ? "&modifiedafter=" + modifiedAfterDate.toISOString()
            : "")
      ),
      {
        method: "get",
        headers: {
          Accept: "application/json",
          "Content-Type": "application/json",
        },
        body: JSON.stringify(),
        credentials: "include",
      }
    )
      .then(checkHttpStatus)
      .then(response => response.json());
  } else {
    return Promise.reject();
  }
}

export function getMessageEffects(connectionUri, messageUri) {
  const url = urljoin(
    ownerBaseUrl,
    "/rest/agreement/getMessageEffects",
    `?connectionUri=${connectionUri}`,
    `&messageUri=${messageUri}`
  );

  return fetch(url, {
    method: "get",
    headers: {
      Accept: "application/ld+json",
      "Content-Type": "application/ld+json",
    },
    credentials: "include",
  })
    .then(checkHttpStatus)
    .then(response => response.json());
}

export function getAgreementProtocolUris(connectionUri) {
  const url = urljoin(
    ownerBaseUrl,
    "/rest/agreement/getAgreementProtocolUris",
    `?connectionUri=${connectionUri}`
  );

  return fetch(url, {
    method: "get",
    headers: {
      Accept: "application/ld+json",
      "Content-Type": "application/ld+json",
    },
    credentials: "include",
  })
    .then(checkHttpStatus)
    .then(response => response.json());
}

export function getPetriNetUris(connectionUri) {
  const url = urljoin(
    ownerBaseUrl,
    "/rest/petrinet/getPetriNetUris",
    `?connectionUri=${connectionUri}`
  );

  return fetch(url, {
    method: "get",
    headers: {
      Accept: "application/ld+json",
      "Content-Type": "application/ld+json",
    },
    credentials: "include",
  })
    .then(checkHttpStatus)
    .then(response => response.json());
}
