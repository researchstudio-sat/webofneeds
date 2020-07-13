import React from "react";
import { useSelector } from "react-redux";
import { getQueryParams } from "../../utils.js";
import * as atomUtils from "../../redux/utils/atom-utils.js";
import * as accountUtils from "../../redux/utils/account-utils.js";
import * as viewSelectors from "../../redux/selectors/view-selectors.js";
import WonModalDialog from "../../components/modal-dialog.jsx";
import WonAtomInfo from "../../components/atom-info.jsx";
import WonTopnav from "../../components/topnav.jsx";
import WonMenu from "../../components/menu.jsx";
import WonToasts from "../../components/toasts.jsx";
import WonSlideIn from "../../components/slide-in.jsx";
import WonFooter from "../../components/footer.jsx";

import "~/style/_post.scss";
import { useHistory } from "react-router-dom";
import * as generalSelectors from "../../redux/selectors/general-selectors";

export default function PagePost() {
  const history = useHistory();
  const { postUri: atomUri, connectionUri, tab } = getQueryParams(
    history.location
  );
  const atom = useSelector(generalSelectors.getAtom(atomUri));
  const ownedConnection = useSelector(
    generalSelectors.getOwnedConnection(connectionUri)
  );

  const accountState = useSelector(generalSelectors.getAccountState);
  const externalDataState = useSelector(generalSelectors.getExternalDataState);

  const isLoggedIn = accountUtils.isLoggedIn(accountState);
  const atomTitle = atomUtils.getTitle(atom, externalDataState);
  const showSlideIns = useSelector(viewSelectors.showSlideIns(history));
  const showModalDialog = useSelector(viewSelectors.showModalDialog);

  return (
    <section className={!isLoggedIn ? "won-signed-out" : ""}>
      {showModalDialog && <WonModalDialog />}
      <WonTopnav pageTitle={atomTitle} />
      {isLoggedIn && <WonMenu />}
      <WonToasts />
      {showSlideIns && <WonSlideIn />}
      <main className="atomcontent">
        <WonAtomInfo
          atom={atom}
          ownedConnection={ownedConnection}
          initialTab={tab}
        />
      </main>
      <WonFooter />
    </section>
  );
}
