import React from "react";
import PropTypes from "prop-types";
import { connect } from "react-redux";
import * as generalSelectors from "../../redux/selectors/general-selectors.js";
import { get, getIn, getQueryParams } from "../../utils.js";
import * as accountUtils from "../../redux/utils/account-utils.js";
import * as viewSelectors from "../../redux/selectors/view-selectors.js";
import * as connectionSelectors from "../../redux/selectors/connection-selectors.js";
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
import { withRouter } from "react-router-dom";

const mapStateToProps = (state, ownProps) => {
  const { connectionUri } = getQueryParams(ownProps.location);

  const selectedConnectionUri = connectionUri;

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

  const hasChatConnections = generalSelectors.hasChatConnections(state);

  const accountState = get(state, "account");

  return {
    isLoggedIn: accountUtils.isLoggedIn(accountState),
    showModalDialog: viewSelectors.showModalDialog(state),
    showListSide: hasChatConnections,
    showNoSelectionSide: hasChatConnections && !selectedConnection,
    showContentSide: hasChatConnections && selectedConnection,
    isSelectedConnectionGroupChat,
    showSlideIns:
      viewSelectors.hasSlideIns(state, ownProps.history) &&
      viewSelectors.isSlideInsVisible(state),
    hideListSideInResponsive: !hasChatConnections || !!selectedConnection,
    hideNoSelectionInResponsive: hasChatConnections,
    hideFooterInResponsive: !!selectedConnection,
  };
};

class PageConnections extends React.Component {
  render() {
    return (
      <section className={!this.props.isLoggedIn ? "won-signed-out" : ""}>
        {this.props.showModalDialog && <WonModalDialog />}
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
            {this.props.isSelectedConnectionGroupChat ? (
              <WonGroupAtomMessages />
            ) : (
              <WonAtomMessages />
            )}
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
  isSelectedConnectionGroupChat: PropTypes.bool,
  showSlideIns: PropTypes.bool,
  hideListSideInResponsive: PropTypes.bool,
  hideNoSelectionInResponsive: PropTypes.bool,
  hideFooterInResponsive: PropTypes.bool,
  history: PropTypes.object,
};

export default withRouter(connect(mapStateToProps)(PageConnections));
