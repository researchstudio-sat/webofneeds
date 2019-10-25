import React from "react";
import PropTypes from "prop-types";
import { connect } from "react-redux";
import * as generalSelectors from "../../redux/selectors/general-selectors.js";
import { get } from "../../utils.js";
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

const mapStateToProps = state => {
  const useCase = generalSelectors.getUseCaseFromRoute(state);
  const useCaseGroup = generalSelectors.getUseCaseGroupFromRoute(state);

  const fromAtomUri = generalSelectors.getFromAtomUriFromRoute(state);
  const mode = generalSelectors.getModeFromRoute(state);

  const showCreateFromPost = !!(fromAtomUri && mode);

  const showUseCaseGroup = !useCase && !!useCaseGroup;
  const showCreatePost = showCreateFromPost || !!useCase;

  const accountState = get(state, "account");

  return {
    isLoggedIn: accountUtils.isLoggedIn(accountState),
    showModalDialog: viewSelectors.showModalDialog(state),
    showUseCasePicker: !(showUseCaseGroup || showCreatePost),
    showUseCaseGroup,
    showCreatePost,
    showSlideIns:
      viewSelectors.hasSlideIns(state) &&
      viewSelectors.isSlideInsVisible(state),
  };
};

class PageCreate extends React.Component {
  render() {
    let contentElement;

    if (this.props.showCreatePost) {
      contentElement = <WonCreateAtom />;
    } else if (this.props.showUseCaseGroup) {
      contentElement = <WonUseCaseGroup />;
    } else if (this.props.showUseCasePicker) {
      contentElement = <WonUseCasePicker />;
    }

    return (
      <section className={!this.props.isLoggedIn ? "won-signed-out" : ""}>
        {this.props.showModalDialog && <WonModalDialog />}
        <WonTopnav pageTitle="Create" />
        {this.props.isLoggedIn && <WonMenu />}
        <WonToasts />
        {this.props.showSlideIns && <WonSlideIn />}
        {/* RIGHT SIDE */}
        <main className="ownercreate">{contentElement}</main>
        <WonFooter />
      </section>
    );
  }
}
PageCreate.propTypes = {
  isLoggedIn: PropTypes.bool,
  showModalDialog: PropTypes.bool,
  showUseCasePicker: PropTypes.bool,
  showUseCaseGroup: PropTypes.bool,
  showCreatePost: PropTypes.bool,
  showSlideIns: PropTypes.bool,
};

export default connect(mapStateToProps)(PageCreate);
