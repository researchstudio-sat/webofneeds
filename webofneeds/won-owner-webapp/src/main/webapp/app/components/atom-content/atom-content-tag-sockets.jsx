import React from "react";
import PropTypes from "prop-types";
import * as connectionUtils from "~/app/redux/utils/connection-utils";
import * as wonLabelUtils from "~/app/won-label-utils";
import WonAtomContentTagSocket from "~/app/components/atom-content/atom-content-tag-socket";

import "~/style/_atom-content-tag-sockets.scss";

export default function WonAtomContentTagSockets({
  atom,
  relevantConnectionsMap,
  storedAtoms,
  isOwned,
}) {
  console.debug(
    "relevantTagViewSocketsConnectionsMap: ",
    relevantConnectionsMap
  );

  const tagViewSocketElements = [];

  relevantConnectionsMap.map((connections, socketType) => {
    const activeConnections = connections.filter(
      conn => !connectionUtils.isClosed(conn)
    );

    const countLabel =
      activeConnections && activeConnections.size > 0
        ? "(" + activeConnections.size + ")"
        : undefined;

    const unread =
      activeConnections &&
      !!activeConnections.find(conn => connectionUtils.isUnread(conn));

    function generateAtomItemCssClasses(unread = false) {
      const cssClassNames = ["actsockets__item__header"];

      unread && cssClassNames.push("actsockets__item__header--unread");

      return cssClassNames.join(" ");
    }

    tagViewSocketElements.push(
      <div key={socketType} className="actsockets__item">
        <div className={generateAtomItemCssClasses(unread)}>
          <span className="actsockets__item__header__unread" />
          <span className="actsockets__item__header__label">
            {wonLabelUtils.getSocketTabLabel(socketType)}
          </span>
          {countLabel ? (
            <span className="actsockets__item__header__count">
              {countLabel}
            </span>
          ) : (
            undefined
          )}
        </div>
        <WonAtomContentTagSocket
          atom={atom}
          isOwned={isOwned}
          socketType={socketType}
          relevantConnections={connections}
          storedAtoms={storedAtoms}
        />
      </div>
    );
  });

  return (
    <won-atom-content-tag-sockets>
      {tagViewSocketElements}
    </won-atom-content-tag-sockets>
  );
}
WonAtomContentTagSockets.propTypes = {
  atom: PropTypes.object.isRequired,
  relevantConnectionsMap: PropTypes.object.isRequired,
  storedAtoms: PropTypes.object.isRequired,
  isOwned: PropTypes.bool,
};
