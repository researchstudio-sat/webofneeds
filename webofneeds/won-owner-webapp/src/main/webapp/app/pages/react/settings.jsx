import React from "react";
import PropTypes from "prop-types";
import { connect } from "react-redux";
import { get, getIn } from "../../utils.js";
import * as accountUtils from "../../redux/utils/account-utils.js";
import * as viewSelectors from "../../redux/selectors/view-selectors.js";
import WonModalDialog from "../../components/modal-dialog.jsx";
import WonTopnav from "../../components/topnav.jsx";
import WonMenu from "../../components/menu.jsx";
import WonToasts from "../../components/toasts.jsx";
import WonSlideIn from "../../components/slide-in.jsx";
import WonFooter from "../../components/footer.jsx";

import "~/style/_signup.scss";

const mapStateToProps = state => {
  const accountState = get(state, "account");

  return {
    isLoggedIn: accountUtils.isLoggedIn(accountState),
    showModalDialog: getIn(state, ["view", "showModalDialog"]),
    showSlideIns:
      viewSelectors.hasSlideIns(state) &&
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
        <main className="settings">{/*<won-settings-wrapper />*/}</main>
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

export default connect(mapStateToProps)(PageSettings);
