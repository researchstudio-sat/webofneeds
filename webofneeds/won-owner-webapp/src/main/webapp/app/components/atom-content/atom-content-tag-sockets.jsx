import React from "react";
import PropTypes from "prop-types";
import vocab from "~/app/service/vocab.js";
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
  const tagViewSocketElements = [];
  let showsElements = false;

  relevantConnectionsMap.map((connections, socketType) => {
    const activeConnections = connections.filterNot(connectionUtils.isClosed);

    const showSocketElements =
      isOwned ||
      activeConnections.size > 0 ||
      !vocab.refuseAddToNonOwned[socketType];

    showsElements = showsElements || showSocketElements;

    if (showSocketElements) {
      const generateAtomItemCssClasses = (unread = false) => {
        const cssClassNames = ["actsockets__item__header"];

        unread && cssClassNames.push("actsockets__item__header--unread");

        return cssClassNames.join(" ");
      };

      const countLabel =
        activeConnections && activeConnections.size > 0
          ? "(" + activeConnections.size + ")"
          : undefined;

      const unread =
        activeConnections && !!activeConnections.find(connectionUtils.isUnread);

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
    }
  });

  // If there ar no Elements to show and no option to add sockets we will return an empty fragment
  return showsElements ? (
    <won-atom-content-tag-sockets>
      {tagViewSocketElements}
    </won-atom-content-tag-sockets>
  ) : (
    <React.Fragment />
  );
}
WonAtomContentTagSockets.propTypes = {
  atom: PropTypes.object.isRequired,
  relevantConnectionsMap: PropTypes.object.isRequired,
  storedAtoms: PropTypes.object.isRequired,
  isOwned: PropTypes.bool,
};
