import React from "react";
import { /*useDispatch,*/ useSelector } from "react-redux";
import { useHistory } from "react-router-dom";
import { get, getIn, generateLink } from "../utils.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as connectionUtils from "../redux/utils/connection-utils.js";
import WonConnectionSelectionItem from "./connection-selection-item.jsx";

import "~/style/_atom-content-chats.scss";

export default function AtomContentChats({ atomUri }) {
  //const dispatch = useDispatch();
  const history = useHistory();
  const atom = useSelector(state => getIn(state, ["atoms", atomUri]));
  const chatSocketUri = atomUtils.getChatSocket(atom);

  const chatConnections = get(atom, "connections").filter(
    conn => get(conn, "socketUri") === chatSocketUri
  );
  const chatConnectionsArray = chatConnections ? chatConnections.toArray() : [];

  if (chatConnectionsArray.length > 0) {
    return (
      <won-atom-content-chats>
        {chatConnectionsArray.map((conn, index) => {
          const connUri = get(conn, "uri");
          return (
            <div
              key={connUri + "-" + index}
              className={
                "acc__item " +
                (connectionUtils.isUnread(conn) ? " won-unread " : "")
              }
            >
              <WonConnectionSelectionItem
                connection={conn}
                toLink={generateLink(
                  history.location,
                  {
                    connectionUri: connUri,
                  },
                  "/connections",
                  false
                )}
              />
            </div>
          );
        })}
      </won-atom-content-chats>
    );
  } else {
    return (
      <won-atom-content-chats>
        <div className="acc__empty">No Chats present.</div>
      </won-atom-content-chats>
    );
  }
}
