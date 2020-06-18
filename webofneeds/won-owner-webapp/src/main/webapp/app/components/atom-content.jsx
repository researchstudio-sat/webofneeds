import React from "react";
import PropTypes from "prop-types";
import Immutable from "immutable";
import { actionCreators } from "../actions/actions.js";
import { useSelector, useDispatch } from "react-redux";
import { get, getQueryParams } from "../utils.js";

import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as processUtils from "../redux/utils/process-utils.js";
import vocab from "../service/vocab.js";

import WonAtomContentHolds from "./atom-content/atom-content-holds.jsx";
import WonAtomContentChats from "./atom-content/atom-content-chats.jsx";
import WonAtomContentSocket from "./atom-content/atom-content-socket.jsx";
import WonAtomContentGeneral from "./atom-content/atom-content-general.jsx";
import WonAtomContentHolder from "./atom-content/atom-content-holder.jsx";
import WonAtomContentDetails from "./atom-content/atom-content-details.jsx";
import WonBuddyItem from "./socket-items/buddy-item.jsx";
import WonParticipantItem from "./socket-items/participant-item.jsx";
import WonGenericItem from "./socket-items/generic-item.jsx";
import WonLabelledHr from "./labelled-hr.jsx";
import ElmReact from "./elm-react.jsx";
import { Elm } from "../../elm/AddPersona.elm";

import "~/style/_atom-content.scss";
import "~/style/_rdflink.scss";
import ico16_indicator_error from "~/images/won-icons/ico16_indicator_error.svg";
import rdf_logo_1 from "~/images/won-icons/rdf_logo_1.svg";
import ico_loading_anim from "~/images/won-icons/ico_loading_anim.svg";
import { useHistory } from "react-router-dom";
import * as connectionUtils from "../redux/utils/connection-utils";

export default function WonAtomContent({
  atom,
  visibleTab,
  relevantConnectionsMap,
  showAddPicker,
  toggleAddPicker,
}) {
  const history = useHistory();
  const dispatch = useDispatch();
  const atomUri = get(atom, "uri");
  const { connectionUri } = getQueryParams(history.location);
  const openConnectionUri = connectionUri;
  const isOwned = useSelector(generalSelectors.isAtomOwned(atomUri));
  const isActive = atomUtils.isActive(atom);
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
  const process = useSelector(generalSelectors.getProcessState);

  const isHeld = atomUtils.isHeld(atom);
  const hasHoldableSocket = atomUtils.hasHoldableSocket(atom);
  const atomLoading = !atom || processUtils.isAtomLoading(process, atomUri);
  const atomFailedToLoad =
    atom && processUtils.hasAtomFailedToLoad(process, atomUri);
  const atomProcessingUpdate =
    atom && processUtils.isAtomProcessingUpdate(process, atomUri);

  const personas = useSelector(generalSelectors.getOwnedCondensedPersonaList);

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
      get(relevantConnectionsMap, visibleTab) || Immutable.Map();

    switch (visibleTab) {
      case "DETAIL":
        visibleTabFragment = (
          <React.Fragment>
            <WonAtomContentGeneral atom={atom} />

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
          </React.Fragment>
        );
        break;

      case vocab.HOLD.HoldableSocketCompacted:
        if (isHeld) {
          visibleTabFragment = <WonAtomContentHolder holdsUri={atomUri} />;
        } else if (isActive && hasHoldableSocket && isOwned) {
          visibleTabFragment = (
            <ElmReact
              src={Elm.AddPersona}
              flags={{
                post: atom.toJS(),
                personas: personas.toJS(),
              }}
            />
          );
        }
        break;

      case vocab.GROUP.GroupSocketCompacted: {
        visibleTabFragment = (
          <WonAtomContentSocket
            atom={atom}
            socketType={visibleTab}
            ItemComponent={WonParticipantItem}
            relevantConnections={relevantConnections}
            showAddPicker={showAddPicker}
            toggleAddPicker={toggleAddPicker}
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
          />
        );
        break;
      }

      case vocab.REVIEW.ReviewSocketCompacted:
        visibleTabFragment = (
          <div className="atom-content__reviews">
            <div className="atom-content__reviews__empty">
              No Reviews to display.
            </div>
          </div>
        );
        break;

      case vocab.HOLD.HolderSocketCompacted:
        visibleTabFragment = <WonAtomContentHolds atom={atom} />;
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
            {openConnectionUri && (
              <a
                className="rdflink clickable"
                target="_blank"
                rel="noopener noreferrer"
                href={openConnectionUri}
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
  showAddPicker: PropTypes.bool.isRequired,
  toggleAddPicker: PropTypes.func.isRequired,
};
