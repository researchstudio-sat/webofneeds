import "babel-polyfill";

self.addEventListener("push", async event => {
  console.debug("push event");
  const payload = event.data.json();
  const clientWindows = await self.clients.matchAll({ type: "window" });
  const activeWindows = clientWindows.filter(client => {
    return client.focused;
  });

  if (activeWindows.length == 0) {
    switch (payload.type) {
      case "CONNECT":
        self.registration.showNotification("New Conversation!", {
          data: {
            atomUri: payload.atomUri,
            connectionUri: payload.connectionUri,
            type: payload.type,
          },
          tag: "won-connect",
          icon: payload.icon,
          body: payload.message,
        });
        break;
      case "MESSAGE":
        self.registration.showNotification("New message!", {
          data: {
            atomUri: payload.atomUri,
            connectionUri: payload.connectionUri,
            type: payload.type,
          },
          tag: "won-message",
          icon: payload.icon,
          body: payload.message,
        });
        break;
      case "HINT":
        self.registration.showNotification("New Match!", {
          data: {
            atomUri: payload.atomUri,
            connectionUri: payload.connectionUri,
            type: payload.type,
          },
          tag: "won-hint",
          icon: payload.icon,
        });
        break;
    }
  }
});

self.addEventListener("notificationclick", event => {
  event.waitUntil(openNotifiedPage(event)); //This is needed so that the browser allows us to open/focus a window
});

async function openNotifiedPage(event) {
  console.log(event.notification.data);
  const connectionUri = event.notification.data.connectionUri;
  const type = event.notification.data.type;
  const atomUri = event.notification.data.atomUri;
  const clientWindows = await self.clients.matchAll({ type: "window" });
  const urlWindows = clientWindows.filter(client => {
    const [, clientQuery] = client.url.split("?");
    const param = new URLSearchParams(clientQuery).get("connectionUri");
    console.log(param);
    return param == connectionUri;
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
      ? `${
          self.registration.scope
        }#!/post/?postUri=${atomUri}&viewConnUri=${connectionUri}`
      : `${
          self.registration.scope
        }#!/connections?connectionUri=${connectionUri}`;
  console.log(targetUri);

  if (urlWindows.length > 0) {
    const activeWindows = urlWindows.filter(client => client.focused);
    if (activeWindows.length == 0) {
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
