import "babel-polyfill";

self.addEventListener("activate", async () => {
  // This will be called only once when the service worker is activated.
  console.debug("ServiceWorker for PushNotifications active");
});

self.addEventListener("push", async event => {
  console.debug("PushEvent");
  event.waitUntil(pushInfoPromise(event.data.json()));
});

self.addEventListener(
  "notificationclick",
  async event => {
    console.debug("Notification Click");
    event.notification.close();
    event.waitUntil(openNotifiedPage(event)); //This is needed so that the browser allows us to open/focus a window
  },
  true
);

self.addEventListener("notificationclose", async () => {
  console.debug("Notification Close");
});

async function pushInfoPromise(payload) {
  const clientWindows = await self.clients.matchAll({
    includeUncontrolled: true,
    type: "window",
  });
  console.debug("clientWindows", clientWindows);
  const activeWindows = clientWindows.filter(
    client => client.visibilityState !== "hidden"
  );

  console.debug("visibleWindows: ", activeWindows.length);
  console.debug("payload", payload);
  if (activeWindows.length === 0) {
    switch (payload.type) {
      case "CONNECT":
        return self.registration.showNotification("New Conversation!", {
          data: {
            atomUri: payload.atomUri,
            connectionUri: payload.connectionUri,
            type: payload.type,
          },
          tag: "won-connect",
          icon: payload.icon,
          body: payload.message,
        });
      case "MESSAGE":
        return self.registration.showNotification("New message!", {
          data: {
            atomUri: payload.atomUri,
            connectionUri: payload.connectionUri,
            type: payload.type,
          },
          tag: "won-message",
          icon: payload.icon,
          body: payload.message,
        });
      case "HINT":
        return self.registration.showNotification("New Match!", {
          data: {
            atomUri: payload.atomUri,
            connectionUri: payload.connectionUri,
            type: payload.type,
          },
          tag: "won-hint",
          icon: payload.icon,
        });
    }
  }
}

async function openNotifiedPage(event) {
  console.debug("NotificationData: ", event.notification.data);
  const connectionUri = event.notification.data.connectionUri;
  const type = event.notification.data.type;
  const atomUri = event.notification.data.atomUri;
  const clientWindows = await self.clients.matchAll({
    includeUncontrolled: true,
    type: "window",
  });
  const urlWindows = clientWindows.filter(client => {
    const [, clientQuery] = client.url.split("?");
    const param = new URLSearchParams(clientQuery).get("connectionUri");
    console.log(param);
    return param === connectionUri;
  });
  console.log(clientWindows.map(client => client.url));
  const inventoryWindows = clientWindows.filter(client => {
    const genericUris = [
      `${self.registration.scope}#!/inventory`,
      `${self.registration.scope}#!/connections`,
    ];
    return genericUris.find(uri => client.url.startsWith(uri)) !== undefined;
  });
  const targetUri =
    type === "HINT"
      ? `${self.registration.scope}#!/post?postUri=${atomUri}&requireLogin=true` // when deeplink works, change this to `/connections?connectionUri=${connectionUri}` see https://github.com/researchstudio-sat/webofneeds/issues/2985
      : `${
          self.registration.scope
        }#!/connections?postUri=${atomUri}&connectionUri=${connectionUri}&requireLogin=true`;
  console.log(targetUri);

  if (urlWindows.length > 0) {
    const activeWindows = urlWindows.filter(client => client.focused);
    if (activeWindows.length === 0) {
      urlWindows[0].focus();
    }
  } else if (inventoryWindows.length > 0) {
    const activeWindows = inventoryWindows.filter(client => client.focused);
    let targetWindow;
    if (activeWindows.length > 0) {
      targetWindow = activeWindows[0];
    } else {
      targetWindow = inventoryWindows[0];
      targetWindow.focus();
    }
    targetWindow.navigate(targetUri);
  } else {
    self.clients.openWindow(targetUri);
  }
}
