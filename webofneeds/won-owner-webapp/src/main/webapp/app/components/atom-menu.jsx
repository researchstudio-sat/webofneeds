/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import { useSelector } from "react-redux";
import PropTypes from "prop-types";
import { getUri } from "../utils.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as viewSelectors from "../redux/selectors/view-selectors.js";
import * as connectionUtils from "../redux/utils/connection-utils.js";
import * as processUtils from "../redux/utils/process-utils.js";
import * as viewUtils from "../redux/utils/view-utils.js";
import * as wonLabelUtils from "../won-label-utils.js";
import vocab from "../service/vocab.js";

import "~/style/_atom-menu.scss";

export default function WonAtomMenu({
  atom,
  className,
  visibleTab,
  setVisibleTab,
  toggleAddPicker,
  relevantConnectionsMap,
}) {
  const atomUri = getUri(atom);
  const isOwned = useSelector(generalSelectors.isAtomOwned(atomUri));

  const viewState = useSelector(generalSelectors.getViewState);
  const process = useSelector(generalSelectors.getProcessState);
  const activePinnedAtomUri = useSelector(viewSelectors.getActivePinnedAtomUri);
  const chatConnections = useSelector(
    activePinnedAtomUri
      ? generalSelectors.getChatConnectionsOfActivePinnedAtom(
          activePinnedAtomUri
        )
      : generalSelectors.emptyMapSelector
  );

  const atomLoading = !atom || processUtils.isAtomLoading(process, atomUri);
  const atomFailedToLoad =
    atom && processUtils.hasAtomFailedToLoad(process, atomUri);
  const connectionContainerFailedToLoad =
    atom &&
    processUtils.areConnectionContainerRequestsFailedOnly(process, atomUri);
  const connectionContainerLoading =
    atom && processUtils.isConnectionContainerLoading(process, atomUri);

  function generateParentCssClasses() {
    const cssClassNames = [className];
    (atomLoading || connectionContainerLoading) &&
      cssClassNames.push("won-is-loading");
    (atomFailedToLoad || connectionContainerFailedToLoad) &&
      cssClassNames.push("won-failed-to-load");

    return cssClassNames.join(" ");
  }

  function generateAtomItemCssClasses(
    selected = false,
    inactive = false,
    unread = false
  ) {
    const cssClassNames = ["atom-menu__item"];

    selected && cssClassNames.push("atom-menu__item--selected");
    inactive && cssClassNames.push("atom-menu__item--inactive");
    unread && cssClassNames.push("atom-menu__item--unread");

    return cssClassNames.join(" ");
  }

  const buttons = [];

  buttons.push(
    <div
      key="detail"
      className={generateAtomItemCssClasses(visibleTab === "DETAIL")}
      onClick={() => {
        setVisibleTab("DETAIL");
        toggleAddPicker(false);
      }}
    >
      <span className="atom-menu__item__label">Detail</span>
    </div>
  );

  // Add generic Tabs based on available Sockets, only when the atom has not failed to load
  !atomFailedToLoad &&
    relevantConnectionsMap
      .filter(connectionUtils.filterSingleConnectedSocketCapacityFilter)
      .filter(connectionUtils.filterTagViewSockets)
      .toOrderedMap()
      .sortBy((socketTypeConnections, socketType) => {
        switch (socketType) {
          case vocab.HOLD.HolderSocketCompacted:
            return "1";
          case vocab.WXPERSONA.InterestSocketCompacted:
            return "2";
          case vocab.WXPERSONA.ExpertiseSocketCompacted:
            return "3";
          case vocab.CHAT.ChatSocketCompacted:
            return "4";
          case vocab.GROUP.GroupSocketCompacted:
            return "5";
          default:
            return wonLabelUtils.getSocketTabLabel(socketType);
        }
      })
      .map((socketTypeConnections, socketType) => {
        let label = wonLabelUtils.getSocketTabLabel(socketType);
        let countLabel;

        const selected = visibleTab === socketType;
        let inactive = false; //TODO: Implement inactive based on connectionsCount and possibleReactions to socket
        let unread = false;

        switch (socketType) {
          case vocab.CHAT.ChatSocketCompacted: {
            //IF AN ATOM IS PINNED WE DISPLAY THE CHILDCHATS ALSO

            const socketUri = atomUtils.getSocketUri(atom, socketType);
            const activeConnections =
              activePinnedAtomUri === atomUri
                ? chatConnections.filterNot(connectionUtils.isClosed)
                : socketTypeConnections
                    .filter(
                      conn =>
                        // We filter out every chat connection that is not owned, otherwise the count would show non owned chatconnections of non owned atoms
                        isOwned ||
                        connectionUtils.hasTargetSocketUri(conn, socketUri)
                    )
                    .filterNot(connectionUtils.isClosed);
            countLabel =
              activeConnections && activeConnections.size > 0
                ? "(" + activeConnections.size + ")"
                : undefined;
            unread =
              activeConnections &&
              !!activeConnections.find(connectionUtils.isUnread);
            break;
          }

          case vocab.HOLD.HolderSocketCompacted: {
            //Holdertab should always just display the amount of connected items
            const activeConnections = socketTypeConnections.filter(
              connectionUtils.isConnected
            );

            countLabel =
              activeConnections && activeConnections.size > 0
                ? "(" + activeConnections.size + ")"
                : undefined;
            unread =
              activeConnections &&
              !!activeConnections.find(connectionUtils.isUnread);
            break;
          }

          default: {
            const activeConnections = socketTypeConnections.filterNot(
              connectionUtils.isClosed
            );

            countLabel =
              activeConnections && activeConnections.size > 0
                ? "(" + activeConnections.size + ")"
                : undefined;
            unread =
              activeConnections &&
              !!activeConnections.find(connectionUtils.isUnread);
            break;
          }
        }

        if (label) {
          buttons.push(
            <div
              key={socketType}
              className={generateAtomItemCssClasses(selected, inactive, unread)}
              onClick={() => {
                setVisibleTab(socketType);
                toggleAddPicker(false);
              }}
            >
              <span className="atom-menu__item__unread" />
              <span className="atom-menu__item__label">{label}</span>
              {countLabel ? (
                <span className="atom-menu__item__count">{countLabel}</span>
              ) : (
                undefined
              )}
            </div>
          );
        }
      });

  viewUtils.showRdf(viewState) &&
    buttons.push(
      <div
        key="rdf"
        className={generateAtomItemCssClasses(visibleTab === "RDF")}
        onClick={() => {
          setVisibleTab("RDF");
          toggleAddPicker(false);
        }}
      >
        <span className="atom-menu__item__label">RDF</span>
      </div>
    ) &&
    buttons.push(
      <div
        key="acl"
        className={generateAtomItemCssClasses(visibleTab === "ACL")}
        onClick={() => {
          setVisibleTab("ACL");
          toggleAddPicker(false);
        }}
      >
        <span className="atom-menu__item__label">ACL</span>
      </div>
    );

  viewUtils.isDebugModeEnabled(viewState) &&
    buttons.push(
      <div
        key="rdf"
        className={generateAtomItemCssClasses(visibleTab === "REQUESTSTATUS")}
        onClick={() => {
          setVisibleTab("REQUESTSTATUS");
          toggleAddPicker(false);
        }}
      >
        <span className="atom-menu__item__label">Request Status</span>
      </div>
    );

  return (
    <won-atom-menu class={generateParentCssClasses()}>{buttons}</won-atom-menu>
  );
}

WonAtomMenu.propTypes = {
  atom: PropTypes.object.isRequired,
  className: PropTypes.string,
  relevantConnectionsMap: PropTypes.object.isRequired,
  visibleTab: PropTypes.string.isRequired,
  setVisibleTab: PropTypes.func.isRequired,
  toggleAddPicker: PropTypes.func.isRequired,
};
