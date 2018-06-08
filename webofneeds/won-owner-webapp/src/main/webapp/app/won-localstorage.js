/**
 * Created by fsuda on 25.04.2018.
 */
const READ_URIS = "wonReadUris";
const CLOSED_CONN_URIS = "wonClosedConnectionUris";
const INACTIVE_NEED_URIS = "inactiveNeedUris";

export function markUriAsRead(uri) {
  //TODO: BETTER IMPL
  if (!isUriRead(uri)) {
    let readUrisString = localStorage.getItem(READ_URIS);
    if (!readUrisString) {
      readUrisString = JSON.stringify([uri]);
    } else {
      try {
        let readUriList = JSON.parse(readUrisString);
        readUriList.push(uri);
        readUrisString = JSON.stringify(readUriList);
      } catch (e) {
        clearReadUris();
        readUrisString = JSON.stringify([uri]);
      }
    }

    localStorage.setItem(READ_URIS, readUrisString);
  }
}

export function isUriRead(uri) {
  //TODO: BETTER IMPL
  let readUrisString = localStorage.getItem(READ_URIS);

  if (readUrisString) {
    let readUriList = JSON.parse(readUrisString);

    for (const readUri of readUriList) {
      if (readUri === uri) {
        return true;
      }
    }
  }
  return false;
}

export function clearReadUris() {
  localStorage.removeItem(READ_URIS);
}

export function clearPrivateId() {
  localStorage.removeItem("privateId");
}

export function savePrivateId(privateId) {
  localStorage.setItem("privateId", privateId);
}

export function markConnUriAsClosed(uri) {
  //TODO: BETTER IMPL
  if (!isConnUriClosed(uri)) {
    let closedConnUrisString = localStorage.getItem(CLOSED_CONN_URIS);
    if (!closedConnUrisString) {
      closedConnUrisString = JSON.stringify([uri]);
    } else {
      try {
        let closedConnUriList = JSON.parse(closedConnUrisString);
        closedConnUriList.push(uri);
        closedConnUrisString = JSON.stringify(closedConnUriList);
      } catch (e) {
        clearClosedConnUris();
        closedConnUrisString = JSON.stringify([uri]);
      }
    }

    localStorage.setItem(CLOSED_CONN_URIS, closedConnUrisString);
  }
}

export function getClosedConnUris() {
  let closedConnUrisString = localStorage.getItem(CLOSED_CONN_URIS);

  if (!closedConnUrisString) {
    return [];
  } else {
    return JSON.parse(closedConnUrisString);
  }
}

export function isConnUriClosed(uri) {
  //TODO: BETTER IMPL
  let closedConnUrisString = localStorage.getItem(CLOSED_CONN_URIS);

  if (closedConnUrisString) {
    let closedConnUriList = JSON.parse(closedConnUrisString);

    for (const closedUri of closedConnUriList) {
      if (closedUri === uri) {
        return true;
      }
    }
  }
  return false;
}

export function clearClosedConnUris() {
  localStorage.removeItem(CLOSED_CONN_URIS);
}

export function addInactiveNeed(needUri) {
  const inactiveNeeds = getInactiveNeedUris();
  if (inactiveNeeds.includes(needUri)) {
    return false;
  } else {
    localStorage.setItem(
      INACTIVE_NEED_URIS,
      JSON.stringify(inactiveNeeds.concat([needUri]))
    );
    return true;
  }
}

export function removeInactiveNeed(needUri) {
  localStorage.setItem(
    INACTIVE_NEED_URIS,
    JSON.stringify(getInactiveNeedUris().filter(uri => uri != needUri))
  );
}

export function getInactiveNeedUris() {
  try {
    const needUris = JSON.parse(localStorage.getItem(INACTIVE_NEED_URIS)) || [];
    if (Array.isArray(needUris)) {
      return needUris;
    } else {
      return [];
    }
  } catch (e) {
    console.warn(e);
    clearInactiveNeedUris();
    return [];
  }
}

export function clearInactiveNeedUris() {
  localStorage.removeItem(INACTIVE_NEED_URIS);
}
