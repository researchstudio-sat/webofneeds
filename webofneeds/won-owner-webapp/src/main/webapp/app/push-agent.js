import { ownerBaseUrl } from "../config/default";
import { base64UrlToUint8Array, compareArrayBuffers } from "./utils";

function getServerKey() {
  return fetch(`${ownerBaseUrl}/appConfig/getWebPushKey`)
    .then(resp => resp.json())
    .then(base64UrlToUint8Array);
}

function sendSubscriptionToServer(subscription) {
  return fetch("/owner/rest/users/subscribeNotifications", {
    body: JSON.stringify(subscription.toJSON()),
    method: "POST",
    headers: new Headers({
      "Content-Type": "application/json",
    }),
    credentials: "include",
  });
}

export function runPushAgent(store) {
  if (!("serviceWorker" in navigator)) {
    return;
  }
  navigator.serviceWorker.register("~/sw.js").then(async swReg => {
    const [serverKey, pushReg] = await Promise.all([
      getServerKey(),
      swReg.pushManager.getSubscription(),
    ]);

    if (pushReg && pushReg.options.applicationServerKey === serverKey) {
      await sendSubscriptionToServer(pushReg);
    }
  });

  let initialized = false;

  store.subscribe(() => {
    const state = store.getState();
    const ownedAtomUris = state.getIn(["account", "ownedAtomUris"]);
    return { numAtoms: ownedAtomUris ? ownedAtomUris.size : 0 };
  })(({ numAtoms }) => {
    if (!initialized && numAtoms > 0) {
      initialized = true;
      navigator.serviceWorker.ready.then(async swReg => {
        const serverKey = await getServerKey();
        switch (
          await swReg.pushManager.permissionState({
            userVisibleOnly: true,
            applicationServerKey: serverKey,
          })
        ) {
          case "granted": {
            let pushSubscription = await swReg.pushManager.getSubscription();
            if (
              !pushSubscription ||
              !compareArrayBuffers(
                pushSubscription.options.applicationServerKey,
                serverKey
              )
            ) {
              console.debug(
                "Subscription is stale, trying to generate new one"
              );
              if (pushSubscription) {
                await pushSubscription.unsubscribe();
              }
              pushSubscription = await swReg.pushManager.subscribe({
                userVisibleOnly: true,
                applicationServerKey: serverKey,
              });
              await sendSubscriptionToServer(pushSubscription);
            }
            break;
          }
          case "prompt": {
            await swReg.pushManager
              .subscribe({
                userVisibleOnly: true,
                applicationServerKey: serverKey,
              })
              .then(
                subscription => {
                  return sendSubscriptionToServer(subscription);
                },
                () => {
                  console.info("Push subscription denied");
                }
              );
            break;
          }
          case "denied":
            break;
        }
      });
    }
  });
}

// if ("serviceWorker" in navigator) {
//     (async function() {
//       console.log("starting push registration");
//       const reg = await navigator.serviceWorker.ready;
//       const pushKeyResponse = await fetch("/owner/appConfig/getWebPushKey");
//       const pushKey = base64UrlToUint8Array(await pushKeyResponse.json());
//       let pushReg = await reg.pushManager.getSubscription();
//       if (pushReg && pushReg.options.applicationServerKey != pushKey) {
//         pushReg.unsubscribe();
//         pushReg = null;
//       }
//       if (pushReg === null) {
//         const permissionState = await reg.pushManager.permissionState({
//           userVisibleOnly: true,
//           applicationServerKey: pushKey,
//         });
//         if (permissionState === "denied") {
//           return;
//         }
//         pushReg = await reg.pushManager.subscribe({
//           userVisibleOnly: true,
//           applicationServerKey: pushKey,
//         });
//       }
//       await fetch("/owner/rest/users/subscribeNotifications", {
//         body: JSON.stringify(pushReg.toJSON()),
//         method: "POST",
//         headers: new Headers({
//           "Content-Type": "application/json",
//         }),
//         credentials: "include",
//       });
//       console.log(pushReg);
//     })().then(() => {
//       console.log("done");
//     });
//   }
