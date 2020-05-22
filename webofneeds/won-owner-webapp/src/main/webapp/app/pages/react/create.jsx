import React from "react";
import { useSelector } from "react-redux";
import { useHistory } from "react-router-dom";
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

export default function PageCreate() {
  const history = useHistory();
  const { useCase, useCaseGroup, fromAtomUri, mode } = getQueryParams(
    history.location
  );

  const accountState = useSelector(state => get(state, "account"));
  const isLoggedIn = accountUtils.isLoggedIn(accountState);
  const showModalDialog = useSelector(viewSelectors.showModalDialog);
  const showSlideIns = useSelector(
    state =>
      viewSelectors.hasSlideIns(state, history) &&
      viewSelectors.isSlideInsVisible(state)
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
    <section className={!isLoggedIn ? "won-signed-out" : ""}>
      {showModalDialog && <WonModalDialog />}
      <WonTopnav pageTitle="Create" />
      {isLoggedIn && <WonMenu />}
      <WonToasts />
      {showSlideIns && <WonSlideIn />}
      {/* RIGHT SIDE */}
      <main className="ownercreate">{contentElement}</main>
      <WonFooter />
    </section>
  );
}
