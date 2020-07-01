import React from "react";
import PropTypes from "prop-types";
import Immutable from "immutable";
import { actionCreators } from "../actions/actions.js";
import { useSelector, useDispatch } from "react-redux";
import {
  get,
  getQueryParams,
  extractAtomUriFromConnectionUri,
} from "../utils.js";

import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as processUtils from "../redux/utils/process-utils.js";
import * as connectionUtils from "../redux/utils/connection-utils.js";
import * as wonLabelUtils from "../won-label-utils.js";
import vocab from "../service/vocab.js";

import WonAtomContentChats from "./atom-content/atom-content-chats.jsx";
import WonAtomContentSocket from "./atom-content/atom-content-socket.jsx";
import WonAtomContentGeneral from "./atom-content/atom-content-general.jsx";
import WonSocketAddButton from "./socket-add-button.jsx";
import WonAtomConnectionsIndicator from "./atom-connections-indicator.jsx";
import WonAtomContentDetails from "./atom-content/atom-content-details.jsx";
import WonBuddyItem from "./socket-items/buddy-item.jsx";
import WonParticipantItem from "./socket-items/participant-item.jsx";
import WonHeldItem from "./socket-items/held-item.jsx";
import WonGenericItem from "./socket-items/generic-item.jsx";
import WonLabelledHr from "./labelled-hr.jsx";

import "~/style/_atom-content.scss";
import "~/style/_rdflink.scss";
import ico16_indicator_error from "~/images/won-icons/ico16_indicator_error.svg";
import rdf_logo_1 from "~/images/won-icons/rdf_logo_1.svg";
import ico_loading_anim from "~/images/won-icons/ico_loading_anim.svg";
import { useHistory } from "react-router-dom";

export default function WonAtomContent({
  atom,
  visibleTab,
  setVisibleTab,
  relevantConnectionsMap,
  storedAtoms,
  showAddPicker,
  toggleAddPicker,
}) {
  const history = useHistory();
  const dispatch = useDispatch();
  const atomUri = get(atom, "uri");
  const { connectionUri } = getQueryParams(history.location);
  const isOwned = useSelector(generalSelectors.isAtomOwned(atomUri));

  const process = useSelector(generalSelectors.getProcessState);

  const atomLoading = !atom || processUtils.isAtomLoading(process, atomUri);
  const atomFailedToLoad =
    atom && processUtils.hasAtomFailedToLoad(process, atomUri);
  const atomProcessingUpdate =
    atom && processUtils.isAtomProcessingUpdate(process, atomUri);

  function tryReload() {
    if (atomUri && atomFailedToLoad) {
      dispatch(actionCreators.atoms__fetchUnloadedAtom(atomUri));
    }
  }

  if (atomLoading) {
    return (
      <won-atom-content class="won-is-loading">
        <div className="atom-skeleton">
          <h2 className="atom-skeleton__heading" />
          <p className="atom-skeleton__details" />
          <h2 className="atom-skeleton__heading" />
          <p className="atom-skeleton__details" />
          <h2 className="atom-skeleton__heading" />
          <p className="atom-skeleton__details" />
          <p className="atom-skeleton__details" />
          <p className="atom-skeleton__details" />
          <p className="atom-skeleton__details" />
          <p className="atom-skeleton__details" />
          <h2 className="atom-skeleton__heading" />
          <div className="atom-skeleton__details" />
        </div>
      </won-atom-content>
    );
  } else if (atomFailedToLoad) {
    return (
      <won-atom-content>
        <div className="atom-failedtoload">
          <svg className="atom-failedtoload__icon">
            <use
              xlinkHref={ico16_indicator_error}
              href={ico16_indicator_error}
            />
          </svg>
          <span className="atom-failedtoload__label">
            Failed To Load - Atom might have been deleted
          </span>
          <div className="atom-failedtoload__actions">
            <button
              className="atom-failedtoload__actions__button red won-button--outlined thin"
              onClick={() => tryReload()}
            >
              Try Reload
            </button>
          </div>
        </div>
      </won-atom-content>
    );
  } else {
    const processingUpdateElement = atomProcessingUpdate && (
      <div className="atom-content__updateindicator">
        <svg className="hspinner atom-content__updateindicator__spinner">
          <use xlinkHref={ico_loading_anim} href={ico_loading_anim} />
        </svg>
        <span className="atom-content__updateindicator__label">
          Processing changes...
        </span>
      </div>
    );

    let visibleTabFragment;
    const relevantConnections =
      get(
        relevantConnectionsMap /*.filter(
          connectionUtils.filterSingleConnectedSocketCapacityFilter
        )*/,
        visibleTab
      ) || Immutable.Map();

    switch (visibleTab) {
      case "DETAIL": {
        const content = get(atom, "content");
        //TODO it will be possible to have more than one seeks
        const seeks = get(atom, "seeks");

        /**
         * This function checks if there is at least one detail present that is displayable
         */
        const hasVisibleDetails = contentBranchImm => {
          return !!(
            contentBranchImm &&
            contentBranchImm.find(
              (detailValue, detailKey) =>
                detailKey != "type" && detailKey != "sockets"
            )
          );
        };

        const hasContent = hasVisibleDetails(content);
        const hasSeeksBranch = hasVisibleDetails(seeks);

        const reactions = atomUtils.getReactions(atom);
        const relevantSingleConnectConnectionsMap = relevantConnectionsMap.filter(
          connectionUtils.filterNonSingleConnectedSocketCapacityFilter
        );

        // Filter out singleConnectSocketReactions
        const filteredReactions = reactions.filter(
          connectionUtils.filterSingleConnectedSocketCapacityFilter
        );

        visibleTabFragment = (
          <React.Fragment>
            <WonAtomContentGeneral atom={atom} />
            {atomUtils.isActive(atom) &&
              relevantSingleConnectConnectionsMap &&
              relevantSingleConnectConnectionsMap.size > 0 && (
                <WonAtomContentSingleConnectSockets
                  atom={atom}
                  isOwned={isOwned}
                  reactions={reactions}
                  relevantSingleConnectConnectionsMap={
                    relevantSingleConnectConnectionsMap
                  }
                  storedAtoms={storedAtoms}
                  toggleAddPicker={toggleAddPicker}
                  setVisibleTab={setVisibleTab}
                />
              )}

            {hasContent && (
              <WonAtomContentDetails atom={atom} branch="content" />
            )}
            {hasContent &&
              hasSeeksBranch && (
                <WonLabelledHr label="Search" className="cp__labelledhr" />
              )}
            {hasSeeksBranch && (
              <WonAtomContentDetails atom={atom} branch="seeks" />
            )}
            {atomUtils.isActive(atom) &&
              filteredReactions &&
              filteredReactions.size > 0 && (
                <WonAtomContentReactions
                  reactions={filteredReactions}
                  isOwned={isOwned}
                  setVisibleTab={setVisibleTab}
                  toggleAddPicker={toggleAddPicker}
                />
              )}
          </React.Fragment>
        );
        break;
      }

      case vocab.GROUP.GroupSocketCompacted: {
        visibleTabFragment = (
          <WonAtomContentSocket
            atom={atom}
            socketType={visibleTab}
            ItemComponent={WonParticipantItem}
            relevantConnections={relevantConnections}
            showAddPicker={showAddPicker}
            toggleAddPicker={toggleAddPicker}
            setVisibleTab={setVisibleTab}
          />
        );
        break;
      }

      case vocab.VALUEFLOWS.PrimaryAccountableSocketCompacted: {
        visibleTabFragment = (
          <WonAtomContentSocket
            atom={atom}
            socketType={visibleTab}
            ItemComponent={WonBuddyItem}
            relevantConnections={relevantConnections}
            showAddPicker={showAddPicker}
            toggleAddPicker={toggleAddPicker}
            setVisibleTab={setVisibleTab}
          />
        );
        break;
      }

      case vocab.BUDDY.BuddySocketCompacted: {
        visibleTabFragment = (
          <WonAtomContentSocket
            atom={atom}
            socketType={visibleTab}
            ItemComponent={WonBuddyItem}
            relevantConnections={relevantConnections}
            showAddPicker={showAddPicker}
            toggleAddPicker={toggleAddPicker}
            setVisibleTab={setVisibleTab}
          />
        );
        break;
      }

      case vocab.HOLD.HolderSocketCompacted:
        visibleTabFragment = (
          <WonAtomContentSocket
            atom={atom}
            socketType={visibleTab}
            ItemComponent={WonHeldItem}
            //Holdercontent should only display connected items
            relevantConnections={relevantConnections.filter(conn =>
              connectionUtils.isConnected(conn)
            )}
            showAddPicker={showAddPicker}
            toggleAddPicker={toggleAddPicker}
            setVisibleTab={setVisibleTab}
            addButtonClassName="won-socket-add-button--big"
            segmentContentClassName="acs__segment__content--fourcols"
          />
        );
        break;

      case vocab.CHAT.ChatSocketCompacted: {
        const socketUri = atomUtils.getSocketUri(
          atom,
          vocab.CHAT.ChatSocketCompacted
        );
        visibleTabFragment = (
          <WonAtomContentChats
            atom={atom}
            showAddPicker={showAddPicker}
            toggleAddPicker={toggleAddPicker}
            // We filter out every chat connection that is not owned, otherwise the count would show non owned chatconnections of non owned atoms
            // TODO: If isOwned is false, then we need to remove all the Group to Chat connections from the relevantConnections, otherwise we will
            // otherwise we allow the groupChat owner to send a direct message from the group to one single member
            relevantConnections={relevantConnections.filter(
              conn =>
                isOwned || connectionUtils.hasTargetSocketUri(conn, socketUri)
            )}
          />
        );
        break;
      }

      case "RDF":
        visibleTabFragment = (
          <div className="atom-info__content__rdf">
            <a
              className="rdflink clickable"
              target="_blank"
              rel="noopener noreferrer"
              href={atomUri}
            >
              <svg className="rdflink__small">
                <use xlinkHref={rdf_logo_1} href={rdf_logo_1} />
              </svg>
              <span className="rdflink__label">Atom</span>
            </a>
            {connectionUri && (
              <a
                className="rdflink clickable"
                target="_blank"
                rel="noopener noreferrer"
                href={connectionUri}
              >
                <svg className="rdflink__small">
                  <use xlinkHref={rdf_logo_1} href={rdf_logo_1} />
                </svg>
                <span className="rdflink__label">Connection</span>
              </a>
            )}
          </div>
        );
        break;

      default: {
        visibleTabFragment = (
          <WonAtomContentSocket
            atom={atom}
            socketType={visibleTab}
            ItemComponent={WonGenericItem}
            relevantConnections={relevantConnections}
            showAddPicker={showAddPicker}
            toggleAddPicker={toggleAddPicker}
            setVisibleTab={setVisibleTab}
          />
        );
        break;
      }
    }

    return (
      <won-atom-content>
        <div className="atom-content">
          {processingUpdateElement}
          {visibleTabFragment}
        </div>
      </won-atom-content>
    );
  }
}
WonAtomContent.propTypes = {
  atom: PropTypes.object.isRequired,
  relevantConnectionsMap: PropTypes.object.isRequired,
  visibleTab: PropTypes.string.isRequired,
  setVisibleTab: PropTypes.func.isRequired,
  showAddPicker: PropTypes.bool.isRequired,
  toggleAddPicker: PropTypes.func.isRequired,
};

function WonAtomContentReactions({
  reactions,
  isOwned,
  setVisibleTab,
  toggleAddPicker,
}) {
  const reactionElements = [];
  reactions.map((senderSocketReactions, targetSocketType) => {
    (isOwned || !vocab.refuseAddToNonOwned[targetSocketType]) &&
      reactionElements.push(
        <WonSocketAddButton
          senderReactions={senderSocketReactions}
          targetSocketType={targetSocketType}
          isAtomOwned={isOwned}
          key={targetSocketType}
          onClick={() => {
            setVisibleTab(targetSocketType);
            toggleAddPicker(true);
          }}
        />
      );
  });

  return reactionElements.length > 0 ? (
    <won-atom-content-reactions>{reactionElements}</won-atom-content-reactions>
  ) : (
    <div />
  );
}
WonAtomContentReactions.propTypes = {
  reactions: PropTypes.object.isRequired,
  isOwned: PropTypes.bool,
  setVisibleTab: PropTypes.func.isRequired,
  toggleAddPicker: PropTypes.func.isRequired,
};

function WonAtomContentSingleConnectSockets({
  atom,
  reactions,
  isOwned,
  setVisibleTab,
  toggleAddPicker,
  storedAtoms,
  relevantSingleConnectConnectionsMap,
}) {
  const reactionElements = [];
  relevantSingleConnectConnectionsMap &&
    relevantSingleConnectConnectionsMap.map((connections, targetSocketType) => {
      const connectedConnections = connections.filter(
        connectionUtils.isConnected
      );
      const requestSentConnections = connections.filter(
        connectionUtils.isRequestSent
      );

      const socketUri = atomUtils.getSocketUri(atom, targetSocketType);
      const contentElements = [];

      let displayConnections;
      if (connectedConnections.size > 0) {
        displayConnections = connectedConnections;
      } else if (requestSentConnections.size > 0) {
        displayConnections = requestSentConnections;
      }

      if (displayConnections) {
        displayConnections.map(conn => {
          switch (targetSocketType) {
            //If you want specific socket-items for a specific targetSocketType please include the items here
            default:
              contentElements.push(
                <WonGenericItem
                  key={get(conn, "uri")}
                  connection={conn}
                  atom={
                    isOwned
                      ? get(
                          storedAtoms,
                          extractAtomUriFromConnectionUri(get(conn, "uri"))
                        )
                      : atom
                  }
                  targetAtom={get(storedAtoms, get(conn, "targetAtomUri"))}
                  isOwned={isOwned}
                  flip={get(conn, "targetSocketUri") === socketUri}
                />
              );
              break;
          }
        });
      } else {
        const senderSocketReactions = get(reactions, targetSocketType);

        if (
          isOwned &&
          atomUtils
            .getConnections(atom, targetSocketType)
            .find(
              conn =>
                connectionUtils.isRequestReceived(conn) ||
                connectionUtils.isSuggested(conn)
            )
        ) {
          contentElements.push(
            <WonAtomConnectionsIndicator
              atom={atom}
              socketType={targetSocketType}
            />
          );
        } else {
          (isOwned || !vocab.refuseAddToNonOwned[targetSocketType]) &&
            contentElements.push(
              <WonSocketAddButton
                senderReactions={senderSocketReactions}
                targetSocketType={targetSocketType}
                isAtomOwned={isOwned}
                key={targetSocketType}
                onClick={() => {
                  setVisibleTab(targetSocketType);
                  toggleAddPicker(true);
                }}
              />
            );
        }
      }
      contentElements.length > 0 &&
        reactionElements.push(
          <div className="scs__item" key={targetSocketType}>
            <div className="scs__item__label">
              {wonLabelUtils.getSocketTabLabel(targetSocketType)}
            </div>
            <div className="scs__item__content">{contentElements}</div>
          </div>
        );
    });

  return reactionElements.length > 0 ? (
    <won-atom-content-single-connect-sockets>
      {reactionElements}
    </won-atom-content-single-connect-sockets>
  ) : (
    <div />
  );
}
WonAtomContentSingleConnectSockets.propTypes = {
  atom: PropTypes.object.isRequired,
  reactions: PropTypes.object.isRequired,
  relevantSingleConnectConnectionsMap: PropTypes.object.isRequired,
  storedAtoms: PropTypes.object.isRequired,
  isOwned: PropTypes.bool,
  setVisibleTab: PropTypes.func.isRequired,
  toggleAddPicker: PropTypes.func.isRequired,
};
