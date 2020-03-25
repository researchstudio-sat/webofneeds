import React from "react";
import PropTypes from "prop-types";
import { connect } from "react-redux";
import * as generalSelectors from "../../redux/selectors/general-selectors.js";
import { get, getIn } from "../../utils.js";
import * as accountUtils from "../../redux/utils/account-utils.js";
import * as viewSelectors from "../../redux/selectors/view-selectors.js";
import * as connectionSelectors from "../../redux/selectors/connection-selectors.js";
import * as connectionUtils from "../../redux/utils/connection-utils.js";
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
import "~/style/_connection-overlay.scss";
import ico36_message from "~/images/won-icons/ico36_message.svg";

const mapStateToProps = state => {
  const viewConnUri = generalSelectors.getViewConnectionUriFromRoute(state);

  const selectedConnectionUri = generalSelectors.getConnectionUriFromRoute(
    state
  );

  const atom =
    selectedConnectionUri &&
    generalSelectors.getOwnedAtomByConnectionUri(state, selectedConnectionUri);
  const selectedConnection = getIn(atom, [
    "connections",
    selectedConnectionUri,
  ]);
  const isSelectedConnectionGroupChat = connectionSelectors.isChatToGroupConnection(
    get(state, "atoms"),
    selectedConnection
  );

  const chatAtoms = generalSelectors.getChatAtoms(state);

  const hasChatAtoms = chatAtoms && chatAtoms.size > 0;

  const accountState = get(state, "account");

  return {
    isLoggedIn: accountUtils.isLoggedIn(accountState),
    showModalDialog: viewSelectors.showModalDialog(state),
    showListSide: hasChatAtoms,
    showNoSelectionSide:
      (hasChatAtoms && !selectedConnection) ||
      connectionUtils.isClosed(selectedConnection),
    showContentSide:
      hasChatAtoms &&
      selectedConnection &&
      !connectionUtils.isClosed(selectedConnection),
    showPostMessages:
      !isSelectedConnectionGroupChat &&
      (connectionUtils.isConnected(selectedConnection) ||
        connectionUtils.isRequestReceived(selectedConnection) ||
        connectionUtils.isRequestSent(selectedConnection) ||
        connectionUtils.isSuggested(selectedConnection)),
    showGroupPostMessages:
      isSelectedConnectionGroupChat &&
      (connectionUtils.isConnected(selectedConnection) ||
        connectionUtils.isRequestReceived(selectedConnection) ||
        connectionUtils.isRequestSent(selectedConnection) ||
        connectionUtils.isSuggested(selectedConnection)),
    showSlideIns:
      viewSelectors.hasSlideIns(state) &&
      viewSelectors.isSlideInsVisible(state),
    showConnectionOverlay: !!viewConnUri,
    viewConnUri,
    hideListSideInResponsive: !hasChatAtoms || !!selectedConnection,
    hideNoSelectionInResponsive: hasChatAtoms,
    hideFooterInResponsive: !!selectedConnection,
  };
};

class PageConnections extends React.Component {
  render() {
    return (
      <section className={!this.props.isLoggedIn ? "won-signed-out" : ""}>
        {this.props.showModalDialog && <WonModalDialog />}
        {this.props.showConnectionOverlay && (
          <div className="won-modal-connectionview">
            <WonAtomMessages connectionUri={this.props.viewConnUri} />
          </div>
        )}
        <WonTopnav pageTitle="Chats" />
        {this.props.isLoggedIn && <WonMenu />}
        <WonToasts />
        {this.props.showSlideIns && <WonSlideIn />}
        {this.props.showListSide && (
          <aside
            className={
              "overview__left " +
              (this.props.hideListSideInResponsive ? "hide-in-responsive" : "")
            }
          >
            <WonConnectionsOverview />
          </aside>
        )}
        {/* RIGHT SIDE */}
        {this.props.showNoSelectionSide && (
          <main
            className={
              "overview__rightempty " +
              (this.props.hideNoSelectionInResponsive
                ? "hide-in-responsive"
                : "")
            }
          >
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
        {this.props.showContentSide && (
          <main className="overview__right">
            {this.props.showPostMessages && <WonAtomMessages />}
            {this.props.showGroupPostMessages && <WonGroupAtomMessages />}
          </main>
        )}
        {!this.props.showListSide && (
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
        )}

        {/* Connection view does not show the footer in responsive mode as there should not be two scrollable areas imho */}
        <WonFooter
          className={
            this.props.hideFooterInResponsive ? "hide-in-responsive" : undefined
          }
        />
      </section>
    );
  }
}
PageConnections.propTypes = {
  isLoggedIn: PropTypes.bool,
  showModalDialog: PropTypes.bool,
  showListSide: PropTypes.bool,
  showNoSelectionSide: PropTypes.bool,
  showContentSide: PropTypes.bool,
  showPostMessages: PropTypes.bool,
  showGroupPostMessages: PropTypes.bool,
  showSlideIns: PropTypes.bool,
  showConnectionOverlay: PropTypes.bool,
  viewConnUri: PropTypes.string,
  hideListSideInResponsive: PropTypes.bool,
  hideNoSelectionInResponsive: PropTypes.bool,
  hideFooterInResponsive: PropTypes.bool,
};

export default connect(mapStateToProps)(PageConnections);
