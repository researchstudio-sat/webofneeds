/**
 * Created by fsuda on 25.04.2018.
 */
const READ_URIS = "wonReadUris";
const CLOSED_CONN_URIS = "wonClosedConnectionUris";
const DISCLAIMER_ACCEPTED = "disclaimerAccepted";

let readUrisCache;
let closedConnUrisCache;
let disclaimerAcceptedCache;

export function markUriAsRead(uri) {
  if (!isUriRead(uri)) {
    readUrisCache = getReadUris();
    readUrisCache.push(uri);
    localStorage.setItem(READ_URIS, JSON.stringify(readUrisCache));
  }
}

export function getReadUris() {
  if (!readUrisCache) {
    let readUrisString = localStorage.getItem(READ_URIS);

    if (readUrisString) {
      try {
        readUrisCache = JSON.parse(readUrisString);
      } catch (e) {
        localStorage.removeItem(READ_URIS);
        readUrisCache = [];
      }
    } else {
      readUrisCache = [];
    }
  }

  return readUrisCache;
}

export function isUriRead(uri) {
  for (const readUri of getReadUris()) {
    if (readUri === uri) {
      return true;
    }
  }

  return false;
}

export function clearReadUris() {
  readUrisCache = undefined;
  localStorage.removeItem(READ_URIS);
}

export function markConnUriAsClosed(uri) {
  if (!isConnUriClosed(uri)) {
    closedConnUrisCache = getClosedConnUris();
    closedConnUrisCache.push(uri);
    localStorage.setItem(CLOSED_CONN_URIS, JSON.stringify(closedConnUrisCache));
  }
}

export function getClosedConnUris() {
  if (!closedConnUrisCache) {
    let closedConnUrisString = localStorage.getItem(CLOSED_CONN_URIS);

    if (closedConnUrisString) {
      try {
        closedConnUrisCache = JSON.parse(closedConnUrisString);
      } catch (e) {
        localStorage.removeItem(CLOSED_CONN_URIS);
        closedConnUrisCache = [];
      }
    } else {
      closedConnUrisCache = [];
    }
  }

  return closedConnUrisCache;
}

export function isConnUriClosed(uri) {
  for (const closedUri of getClosedConnUris()) {
    if (closedUri === uri) {
      return true;
    }
  }

  return false;
}

export function clearClosedConnUris() {
  closedConnUrisCache = undefined;
  localStorage.removeItem(CLOSED_CONN_URIS);
}

export function isDisclaimerAccepted() {
  if (!disclaimerAcceptedCache) {
    disclaimerAcceptedCache = !!localStorage.getItem(DISCLAIMER_ACCEPTED);
  }
  return disclaimerAcceptedCache;
}

export function setDisclaimerAccepted() {
  disclaimerAcceptedCache = true;
  localStorage.setItem(DISCLAIMER_ACCEPTED, disclaimerAcceptedCache);
}

export function clearDisclaimerAccepted() {
  disclaimerAcceptedCache = undefined;
  localStorage.removeItem(DISCLAIMER_ACCEPTED);
}
