import React from "react";
import { useSelector } from "react-redux";
import * as generalSelectors from "../../redux/selectors/general-selectors.js";
import { get, getIn, getQueryParams } from "../../utils.js";
import * as accountUtils from "../../redux/utils/account-utils.js";
import * as viewSelectors from "../../redux/selectors/view-selectors.js";
import WonModalDialog from "../../components/modal-dialog.jsx";
import WonTopnav from "../../components/topnav.jsx";
import WonMenu from "../../components/menu.jsx";
import WonToasts from "../../components/toasts.jsx";
import WonSlideIn from "../../components/slide-in.jsx";
import WonFooter from "../../components/footer.jsx";
import WonAtomMessages from "../../components/atom-messages.jsx";
import WonGroupAtomMessages from "../../components/group-atom-messages.jsx";
import WonConnectionsOverview from "../../components/connections-overview.jsx";

import "~/style/_connections.scss";
import "~/style/_responsiveness-utils.scss";
import ico36_message from "~/images/won-icons/ico36_message.svg";
import { useHistory } from "react-router-dom";
import * as atomUtils from "../../redux/utils/atom-utils";

export default function PageConnections() {
  const history = useHistory();

  const { connectionUri, postUri } = getQueryParams(history.location);

  const selectedConnectionUri = connectionUri;

  const atom = useSelector(
    state =>
      selectedConnectionUri &&
      generalSelectors.getOwnedAtomByConnectionUri(state, selectedConnectionUri)
  );

  const selectedConnection = getIn(atom, [
    "connections",
    selectedConnectionUri,
  ]);
  const selectedTargetAtom = useSelector(state =>
    getIn(state, "atoms", get(selectedConnection, "targetAtomUri"))
  );

  const isSelectedConnectionGroupChat =
    selectedConnection &&
    atomUtils.getGroupSocket(selectedTargetAtom) ===
      get(selectedConnection, "targetSocketUri");
  const hasChatConnections = useSelector(state =>
    generalSelectors.hasChatConnections(state)
  );
  const isLoggedIn = useSelector(state =>
    accountUtils.isLoggedIn(get(state, "account"))
  );
  const showModalDialog = useSelector(state =>
    viewSelectors.showModalDialog(state)
  );
  const showSlideIns = useSelector(
    state =>
      viewSelectors.hasSlideIns(state, history) &&
      viewSelectors.isSlideInsVisible(state)
  );

  let contentElements;
  if (selectedConnection && postUri) {
    contentElements = (
      <main className="overview__justconnection">
        {isSelectedConnectionGroupChat ? (
          <WonGroupAtomMessages />
        ) : (
          <WonAtomMessages />
        )}
      </main>
    );
  } else if (hasChatConnections) {
    contentElements = (
      <React.Fragment>
        <aside
          className={
            "overview__left " + (selectedConnection ? "hide-in-responsive" : "")
          }
        >
          <WonConnectionsOverview />
        </aside>
        {selectedConnection ? (
          <main className="overview__right">
            {isSelectedConnectionGroupChat ? (
              <WonGroupAtomMessages />
            ) : (
              <WonAtomMessages />
            )}
          </main>
        ) : (
          <main className="overview__rightempty hide-in-responsive">
            <div className="overview__rightempty__noselection">
              <svg
                className="overview__rightempty__noselection__icon"
                title="Messages"
              >
                <use xlinkHref={ico36_message} href={ico36_message} />
              </svg>
              <div className="overview__rightempty__noselection__text">
                No Chat selected
              </div>
              <div className="overview__rightempty__noselection__subtext">
                Click on a Chat on the left to open
              </div>
            </div>
          </main>
        )}
      </React.Fragment>
    );
  } else {
    contentElements = (
      <main className="overview__nochats">
        <div className="overview__nochats__empty">
          <svg className="overview__nochats__empty__icon" title="Messages">
            <use xlinkHref={ico36_message} href={ico36_message} />
          </svg>
          <div className="overview__nochats__empty__text">
            No Open Chats available
          </div>
        </div>
      </main>
    );
  }

  return (
    <section className={!isLoggedIn ? "won-signed-out" : ""}>
      {showModalDialog && <WonModalDialog />}
      <WonTopnav pageTitle="Chats" />
      {isLoggedIn && <WonMenu />}
      <WonToasts />
      {showSlideIns && <WonSlideIn />}

      {contentElements}

      {/* Connection view does not show the footer in responsive mode as there should not be two scrollable areas imho */}
      <WonFooter
        className={selectedConnection ? "hide-in-responsive" : undefined}
      />
    </section>
  );
}
