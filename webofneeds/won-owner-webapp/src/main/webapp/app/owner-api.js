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
