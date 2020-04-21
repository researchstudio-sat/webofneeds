import React from "react";
import PropTypes from "prop-types";
import { connect } from "react-redux";
import { withRouter } from "react-router-dom";
import { get, getQueryParams } from "../../utils.js";
import * as accountUtils from "../../redux/utils/account-utils.js";
import * as viewSelectors from "../../redux/selectors/view-selectors.js";
import WonModalDialog from "../../components/modal-dialog.jsx";
import WonTopnav from "../../components/topnav.jsx";
import WonMenu from "../../components/menu.jsx";
import WonToasts from "../../components/toasts.jsx";
import WonSlideIn from "../../components/slide-in.jsx";
import WonFooter from "../../components/footer.jsx";
import WonCreateAtom from "../../components/create-atom.jsx";
import WonUseCaseGroup from "../../components/usecase-group.jsx";
import WonUseCasePicker from "../../components/usecase-picker.jsx";

import "~/style/_create.scss";
import "~/style/_responsiveness-utils.scss";

const mapStateToProps = (state, ownProps) => {
  const accountState = get(state, "account");

  return {
    isLoggedIn: accountUtils.isLoggedIn(accountState),
    showModalDialog: viewSelectors.showModalDialog(state),
    showSlideIns:
      viewSelectors.hasSlideIns(state, ownProps.history) &&
      viewSelectors.isSlideInsVisible(state),
  };
};

function PageCreate(props) {
  let { useCase, useCaseGroup, fromAtomUri, mode } = getQueryParams(
    props.location
  );
  let contentElement;

  let showCreateFromPost = !!(fromAtomUri && mode);
  let showUseCaseGroup = !useCase && !!useCaseGroup;

  let showCreatePost = showCreateFromPost || !!useCase;

  let showUseCasePicker = !(showUseCaseGroup || showCreatePost);

  if (showCreatePost) {
    contentElement = <WonCreateAtom />;
  } else if (showUseCaseGroup) {
    contentElement = <WonUseCaseGroup />;
  } else if (showUseCasePicker) {
    contentElement = <WonUseCasePicker />;
  }

  return (
    <section className={!props.isLoggedIn ? "won-signed-out" : ""}>
      {props.showModalDialog && <WonModalDialog />}
      <WonTopnav pageTitle="Create" />
      {props.isLoggedIn && <WonMenu />}
      <WonToasts />
      {props.showSlideIns && <WonSlideIn />}
      {/* RIGHT SIDE */}
      <main className="ownercreate">{contentElement}</main>
      <WonFooter />
    </section>
  );
}

PageCreate.propTypes = {
  location: PropTypes.object,
  isLoggedIn: PropTypes.bool,
  showModalDialog: PropTypes.bool,
  showSlideIns: PropTypes.bool,
  history: PropTypes.object,
};

export default withRouter(connect(mapStateToProps)(PageCreate));
