import { ownerBaseUrl } from "../config/default";
import { base64UrlToUint8Array, compareArrayBuffers } from "./utils";
import { sendSubscriptionToServer } from "./api/owner-api";
function getServerKey() {
  return fetch(`${ownerBaseUrl}/appConfig/getWebPushKey`)
    .then(resp => resp.json())
    .then(base64UrlToUint8Array);
}

export async function runPushAgent() {
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
