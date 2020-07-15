import React, { useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useHistory } from "react-router-dom";
import { getQueryParams } from "../../utils.js";
import { actionCreators } from "../../actions/actions.js";
import * as accountUtils from "../../redux/utils/account-utils.js";
import * as processUtils from "../../redux/utils/process-utils.js";
import * as viewSelectors from "../../redux/selectors/view-selectors.js";
import * as generalSelectors from "../../redux/selectors/general-selectors.js";
import WonModalDialog from "../../components/modal-dialog.jsx";
import WonTopnav from "../../components/topnav.jsx";
import WonMenu from "../../components/menu.jsx";
import WonToasts from "../../components/toasts.jsx";
import WonSlideIn from "../../components/slide-in.jsx";
import WonFooter from "../../components/footer.jsx";
import WonCreateAtom from "../../components/create-atom.jsx";
import WonEditAtom from "../../components/edit-atom.jsx";
import WonUseCaseGroup from "../../components/usecase-group.jsx";
import WonUseCasePicker from "../../components/usecase-picker.jsx";

import "~/style/_create.scss";
import "~/style/_responsiveness-utils.scss";

import ico_loading_anim from "~/images/won-icons/ico_loading_anim.svg";
import ico16_indicator_error from "~/images/won-icons/ico16_indicator_error.svg";

export default function PageCreate() {
  const history = useHistory();
  const dispatch = useDispatch();
  const {
    useCase,
    useCaseGroup,
    fromAtomUri,
    mode,
    senderSocketType,
    targetSocketType,
    holderUri,
  } = getQueryParams(history.location);
  const processState = useSelector(generalSelectors.getProcessState);
  const fromAtom = useSelector(generalSelectors.getAtom(fromAtomUri));
  const accountState = useSelector(generalSelectors.getAccountState);
  const isLoggedIn = accountUtils.isLoggedIn(accountState);
  const showModalDialog = useSelector(viewSelectors.showModalDialog);
  const showSlideIns = useSelector(viewSelectors.showSlideIns(history));

  const visibleUseCasesByConfig = useSelector(
    generalSelectors.getVisibleUseCasesByConfig
  );

  const isFromAtomLoading = fromAtomUri
    ? processUtils.isAtomLoading(processState, fromAtomUri)
    : false;
  const isFromAtomToLoad = fromAtomUri
    ? processUtils.isAtomToLoad(processState, fromAtomUri)
    : false;
  const hasFromAtomFailedToLoad = fromAtomUri
    ? processUtils.hasAtomFailedToLoad(processState, fromAtomUri)
    : false;

  const isFromAtomFetchNecessary = processUtils.isAtomFetchNecessary(
    processState,
    fromAtomUri,
    fromAtom
  );

  useEffect(
    () => {
      if (isFromAtomFetchNecessary) {
        console.debug("fetch fromAtomUri, ", fromAtomUri);
        dispatch(actionCreators.atoms__fetchUnloadedAtom(fromAtomUri));
      }
    },
    [fromAtomUri, fromAtom, isFromAtomFetchNecessary]
  );

  let contentElement;
  let showUseCaseGroup = !useCase && !!useCaseGroup;

  let showCreatePostFromAtom = !!fromAtomUri && !!mode && !!useCase;
  let showCreatePostFromUseCase = !showCreatePostFromAtom && !!useCase;

  let showUseCasePicker = !(
    showUseCaseGroup ||
    showCreatePostFromAtom ||
    showCreatePostFromUseCase
  );

  if (showUseCaseGroup) {
    contentElement = (
      <WonUseCaseGroup
        visibleUseCasesByConfig={visibleUseCasesByConfig}
        filterBySocketType={senderSocketType}
      />
    );
  } else if (showUseCasePicker) {
    contentElement = (
      <WonUseCasePicker
        visibleUseCasesByConfig={visibleUseCasesByConfig}
        filterBySocketType={senderSocketType}
      />
    );
  } else if (showCreatePostFromAtom) {
    if (
      fromAtom &&
      !isFromAtomToLoad &&
      !isFromAtomLoading &&
      !hasFromAtomFailedToLoad
    ) {
      switch (mode) {
        case "EDIT": {
          contentElement = <WonEditAtom fromAtom={fromAtom} />;
          break;
        }

        case "CONNECT": {
          contentElement = (
            <WonCreateAtom
              useCaseIdentifier={useCase}
              connect={true}
              fromAtom={fromAtom}
              senderSocketType={senderSocketType}
              targetSocketType={targetSocketType}
              holderUri={holderUri}
            />
          );
          break;
        }

        case "DUPLICATE": {
          contentElement = (
            <WonCreateAtom
              useCaseIdentifier={useCase}
              fromAtom={fromAtom}
              duplicate={true}
              holderUri={holderUri}
            />
          );
          break;
        }

        default: {
          console.error("Returning empty div, mode is not valid: ", mode);
          contentElement = <div />;
          break;
        }
      }
    } else {
      contentElement = (
        <won-create-atom>
          <div className="cp__content">
            {hasFromAtomFailedToLoad ? (
              <div className="cp__content__failed">
                <svg className="cp__content__failed__icon">
                  <use
                    xlinkHref={ico16_indicator_error}
                    href={ico16_indicator_error}
                  />
                </svg>
                <span className="cp__content__failed__label">
                  Failed To Load - Post might have been deleted
                </span>
                <div className="cp__content__failed__actions">
                  <button
                    className="cp__content__failed__actions__button red won-button--outlined thin"
                    onClick={() =>
                      dispatch(
                        actionCreators.atoms__fetchUnloadedAtom(fromAtomUri)
                      )
                    }
                  >
                    Try Reload
                  </button>
                </div>
              </div>
            ) : (
              <div className="cp__content__loading">
                <svg className="cp__content__loading__spinner hspinner">
                  <use xlinkHref={ico_loading_anim} href={ico_loading_anim} />
                </svg>
                <span className="cp__content__loading__label">Loading...</span>
              </div>
            )}
          </div>
        </won-create-atom>
      );
    }
  } else if (showCreatePostFromUseCase) {
    contentElement = (
      <WonCreateAtom useCaseIdentifier={useCase} holderUri={holderUri} />
    );
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
