import React from "react";
import { useSelector } from "react-redux";
import { useHistory } from "react-router-dom";
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

export default function PageSettings() {
  const history = useHistory();
  const accountState = useSelector(generalSelectors.getAccountState);
  const isLoggedIn = accountUtils.isLoggedIn(accountState);
  const showModalDialog = useSelector(viewSelectors.showModalDialog);
  const showSlideIns = useSelector(viewSelectors.showSlideIns(history));

  return (
    <section className={!isLoggedIn ? "won-signed-out" : ""}>
      {showModalDialog && <WonModalDialog />}
      <WonTopnav pageTitle="Settings" />
      {isLoggedIn && <WonMenu />}
      <WonToasts />
      {showSlideIns && <WonSlideIn />}
      <main className="settings">
        <WonSettingsWrapper />
      </main>
      <WonFooter />
    </section>
  );
}
