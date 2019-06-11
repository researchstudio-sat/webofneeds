/**
 * Created by quasarchimaere on 11.06.2019.
 */

export function serverSideConnect(socket1, socket2) {
  return fetch("rest/action/connect", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify([socket1, socket2]),
    credentials: "include",
  });
}
