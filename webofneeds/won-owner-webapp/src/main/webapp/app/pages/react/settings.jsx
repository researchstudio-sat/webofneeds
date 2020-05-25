import React from "react";
import PropTypes from "prop-types";
import { connect } from "react-redux";
import { withRouter } from "react-router-dom";
import * as accountUtils from "../../redux/utils/account-utils.js";
import * as viewSelectors from "../../redux/selectors/view-selectors.js";
import WonModalDialog from "../../components/modal-dialog.jsx";
import WonTopnav from "../../components/topnav.jsx";
import WonMenu from "../../components/menu.jsx";
import WonToasts from "../../components/toasts.jsx";
import WonSlideIn from "../../components/slide-in.jsx";
import WonFooter from "../../components/footer.jsx";

import WonSettingsWrapper from "../../components/settings-wrapper";

import "~/style/_signup.scss";
import * as generalSelectors from "../../redux/selectors/general-selectors";

const mapStateToProps = (state, ownProps) => {
  const accountState = generalSelectors.getAccountState(state);

  return {
    isLoggedIn: accountUtils.isLoggedIn(accountState),
    showModalDialog: viewSelectors.showModalDialog(state),
    showSlideIns:
      viewSelectors.hasSlideIns(state, ownProps.history) &&
      viewSelectors.isSlideInsVisible(state),
  };
};

class PageSettings extends React.Component {
  render() {
    return (
      <section className={!this.props.isLoggedIn ? "won-signed-out" : ""}>
        {this.props.showModalDialog && <WonModalDialog />}
        <WonTopnav pageTitle="Settings" />
        {this.props.isLoggedIn && <WonMenu />}
        <WonToasts />
        {this.props.showSlideIns && <WonSlideIn />}
        <main className="settings">
          <WonSettingsWrapper />
        </main>
        <WonFooter />
      </section>
    );
  }
}
PageSettings.propTypes = {
  isLoggedIn: PropTypes.bool,
  showModalDialog: PropTypes.bool,
  showSlideIns: PropTypes.bool,
};

export default withRouter(connect(mapStateToProps)(PageSettings));
