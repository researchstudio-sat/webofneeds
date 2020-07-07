import React, { useEffect } from "react";
import { useSelector, useDispatch } from "react-redux";
import { actionCreators } from "../../actions/actions.js";
import { getQueryParams } from "../../utils.js";
import * as atomUtils from "../../redux/utils/atom-utils.js";
import * as accountUtils from "../../redux/utils/account-utils.js";
import * as viewSelectors from "../../redux/selectors/view-selectors.js";
import * as processUtils from "../../redux/utils/process-utils.js";
import WonModalDialog from "../../components/modal-dialog.jsx";
import WonAtomInfo from "../../components/atom-info.jsx";
import WonTopnav from "../../components/topnav.jsx";
import WonMenu from "../../components/menu.jsx";
import WonToasts from "../../components/toasts.jsx";
import WonSlideIn from "../../components/slide-in.jsx";
import WonFooter from "../../components/footer.jsx";

import "~/style/_post.scss";
import ico_loading_anim from "~/images/won-icons/ico_loading_anim.svg";
import ico16_indicator_error from "~/images/won-icons/ico16_indicator_error.svg";
import { useHistory } from "react-router-dom";
import * as generalSelectors from "../../redux/selectors/general-selectors";

export default function PagePost() {
  const history = useHistory();
  const dispatch = useDispatch();
  const { postUri, connectionUri, tab } = getQueryParams(history.location);
  const atomUri = postUri;
  const atom = useSelector(generalSelectors.getAtom(atomUri));
  const ownedConnection = useSelector(
    generalSelectors.getOwnedConnection(connectionUri)
  );

  const processState = useSelector(generalSelectors.getProcessState);
  const accountState = useSelector(generalSelectors.getAccountState);
  const externalDataState = useSelector(generalSelectors.getExternalDataState);

  const isLoggedIn = accountUtils.isLoggedIn(accountState);
  const atomTitle = atomUtils.getTitle(atom, externalDataState);
  const showSlideIns = useSelector(viewSelectors.showSlideIns(history));
  const showModalDialog = useSelector(viewSelectors.showModalDialog);
  const atomLoading =
    !atom || processUtils.isAtomLoading(processState, atomUri);
  const atomToLoad = !atom || processUtils.isAtomToLoad(processState, atomUri);
  const atomFailedToLoad =
    atom && processUtils.hasAtomFailedToLoad(processState, atomUri);

  function tryReload() {
    if (atomUri && atomFailedToLoad) {
      dispatch(actionCreators.atoms__fetchUnloadedAtom(atomUri));
    }
  }

  useEffect(() => {
    //THIS IS THE OLD APPROACH I AM NOT SURE IF THEY ARE EQUAL
    /* componentDidMount() {
      if ((!atom || (atomToLoad && !atomLoading))) {
        dispatch(actionCreators.atoms__fetchUnloadedAtom(atomUri));
      }
    }

    componentDidUpdate(prevProps) {
      // Invoke possible fetch if:
      //    - atomUri is present AND
      //        - atomUri has changed OR
      //        - loginStatus has changed to loggedOut
      if (
        (atomUri !== prevProps.atomUri || (!isLoggedIn && prevProps.isLoggedIn))
      ) {
        if (!atom || (atomToLoad && !atomLoading)) {
          dispatch(actionCreators.atoms__fetchUnloadedAtom(atomUri));
        }
      }
    } */

    //COMBINED:
    if (atomUri && ((atomToLoad && !atomLoading) || !atom)) {
      dispatch(actionCreators.atoms__fetchUnloadedAtom(atomUri));
    }
  });

  let atomContentElement;

  if (atomLoading) {
    atomContentElement = (
      <div className="pc__loading">
        <svg className="pc__loading__spinner hspinner">
          <use xlinkHref={ico_loading_anim} href={ico_loading_anim} />
        </svg>
        <span className="pc__loading__label">Loading...</span>
      </div>
    );
  } else if (atomFailedToLoad) {
    atomContentElement = (
      <div className="pc__failed">
        <svg className="pc__failed__icon">
          <use xlinkHref={ico16_indicator_error} href={ico16_indicator_error} />
        </svg>
        <span className="pc__failed__label">
          Failed To Load - Atom might have been deleted
        </span>
        <div className="pc__failed__actions">
          <button
            className="pc__failed__actions__button red won-button--outlined thin"
            onClick={tryReload}
          >
            Try Reload
          </button>
        </div>
      </div>
    );
  } else if (atom) {
    atomContentElement = (
      <WonAtomInfo
        atom={atom}
        ownedConnection={ownedConnection}
        initialTab={tab}
      />
    );
  }

  return (
    <section className={!isLoggedIn ? "won-signed-out" : ""}>
      {showModalDialog && <WonModalDialog />}
      <WonTopnav pageTitle={atomTitle} />
      {isLoggedIn && <WonMenu />}
      <WonToasts />
      {showSlideIns && <WonSlideIn />}
      <main className="atomcontent">{atomContentElement}</main>
      <WonFooter />
    </section>
  );
}
