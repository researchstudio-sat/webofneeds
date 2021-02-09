import React from "react";
import { useSelector } from "react-redux";

import WonModalDialog from "~/app/components/modal-dialog";
import WonTopnav from "~/app/components/topnav";
import WonMenu from "~/app/components/menu";
import WonToasts from "~/app/components/toasts";
import WonSlideIn from "~/app/components/slide-in";
import WonFooter from "~/app/components/footer";
import PropTypes from "prop-types";

import * as accountUtils from "~/app/redux/utils/account-utils";
import * as generalSelectors from "~/app/redux/selectors/general-selectors";
import * as viewSelectors from "~/app/redux/selectors/view-selectors";
import { useHistory } from "react-router-dom";

export default function WonGenericPage({ pageTitle, children }) {
  const history = useHistory();
  const showModalDialog = useSelector(viewSelectors.showModalDialog);
  const showSlideIns = useSelector(viewSelectors.showSlideIns(history));

  const accountState = useSelector(generalSelectors.getAccountState);
  const isLoggedIn = accountUtils.isLoggedIn(accountState);

  return (
    <section className={!isLoggedIn ? "won-signed-out" : ""}>
      {showModalDialog && <WonModalDialog />}
      <WonTopnav pageTitle={pageTitle} />
      {isLoggedIn && <WonMenu />}
      <WonToasts />
      {showSlideIns && <WonSlideIn />}
      {children}
      {!isLoggedIn && <WonFooter />}
    </section>
  );
}
WonGenericPage.propTypes = {
  pageTitle: PropTypes.string,
  children: PropTypes.any,
};
