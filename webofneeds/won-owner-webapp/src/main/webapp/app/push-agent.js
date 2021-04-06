import urljoin from "url-join";
import { ownerBaseUrl } from "../config/default";
import { compareArrayBuffers } from "./utils";

export async function runPushAgent(store) {
  if (!("serviceWorker" in navigator)) {
    return;
  }

  navigator.serviceWorker.register(`${ownerBaseUrl}/sw.js`);
  const serviceWorker = await navigator.serviceWorker.ready;
  const serverKey = await getServerKey();
  const permissionState = await serviceWorker.pushManager.permissionState({
    userVisibleOnly: true,
    applicationServerKey: serverKey,
  });

  let initialized = false;
  store.subscribe(async () => {
    const state = store.getState();
    const ownedAtomUris = state.getIn(["account", "ownedAtomUris"]);
    if (!initialized && ownedAtomUris && ownedAtomUris.size > 0) {
      initialized = true;
      console.debug("numAtoms: ", ownedAtomUris.size);
      console.debug("PermissionState: ", permissionState);
      switch (permissionState) {
        case "granted": {
          let subscription = await serviceWorker.pushManager.getSubscription();
          if (
            !subscription ||
            !compareArrayBuffers(
              subscription.options.applicationServerKey,
              serverKey
            )
          ) {
            console.debug("Subscription is stale, trying to generate new one");
            if (subscription) {
              await subscription.unsubscribe();
            }
            subscription = await serviceWorker.pushManager.subscribe({
              userVisibleOnly: true,
              applicationServerKey: serverKey,
            });
            await sendSubscriptionToServer(subscription.toJSON());
          }
          break;
        }
        case "prompt": {
          await serviceWorker.pushManager
            .subscribe({
              userVisibleOnly: true,
              applicationServerKey: serverKey,
            })
            .then(
              subscription => {
                return sendSubscriptionToServer(subscription.toJSON());
              },
              () => {
                console.info("Push subscription denied");
              }
            );
          break;
        }
        case "denied":
          console.debug("Push subscription denied");
          break;
      }
    }
  });
}

function getServerKey() {
  const url = urljoin(ownerBaseUrl, "/appConfig/getWebPushKey");
  return fetch(url)
    .then(resp => resp.json())
    .then(base64UrlToUint8Array);
}

function sendSubscriptionToServer(subscription) {
  const url = urljoin(ownerBaseUrl, "/rest/users/subscribeNotifications");
  return fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    credentials: "include",
    body: JSON.stringify(subscription),
  }).then(checkHttpStatus());
}

/**
 * Throws an error if this isn't a good http-response
 * @param response
 * @returns {*}
 */
const checkHttpStatus = (uri, params = {}) => response => {
  if (
    (response.status >= 200 && response.status < 300) ||
    response.status === 304
  ) {
    return response;
  } else {
    return response
      .json()
      .then(jsonResponse => ({
        response: jsonResponse,
        status: response.status,
        params: params,
        message: `ERROR for request: ${response.status} - ${
          response.statusText
        } for request ${uri}`,
      }))
      .catch(err => {
        console.debug(
          "checkHttpStatus response, does not have json content",
          response.status,
          err
        );

        return {
          response: {},
          status: response.status,
          params: params,
          message: `ERROR for request: ${response.status} - ${
            response.statusText
          } for request ${uri}`,
        };
      })
      .then(errorPayload => {
        throw new Error(JSON.stringify(errorPayload));
      });
  }
};

function base64UrlToUint8Array(base64UrlData) {
  const padding = "=".repeat((4 - (base64UrlData.length % 4)) % 4);
  const base64 = (base64UrlData + padding)
    .replace(/-/g, "+")
    .replace(/_/g, "/");

  const rawData = atob(base64);
  const buffer = new Uint8Array(rawData.length);

  for (const i of buffer.keys()) {
    buffer[i] = rawData.charCodeAt(i);
  }
  return buffer.buffer;
}
