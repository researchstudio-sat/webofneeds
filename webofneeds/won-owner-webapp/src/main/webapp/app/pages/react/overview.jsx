import React, { useEffect, useState } from "react";
import { useSelector, useDispatch } from "react-redux";
import { actionCreators } from "../../actions/actions.js";
import * as generalSelectors from "../../redux/selectors/general-selectors.js";
import * as atomUtils from "../../redux/utils/atom-utils.js";
import { getIn, sortByDate } from "../../utils.js";
import * as processSelectors from "../../redux/selectors/process-selectors.js";
import * as accountUtils from "../../redux/utils/account-utils.js";
import * as useCaseUtils from "../../usecase-utils.js";
import * as wonLabelUtils from "../../won-label-utils.js";
import * as viewSelectors from "../../redux/selectors/view-selectors.js";

import WonTopnav from "../../components/topnav.jsx";
import WonModalDialog from "../../components/modal-dialog.jsx";
import WonMenu from "../../components/menu.jsx";
import WonToasts from "../../components/toasts.jsx";
import WonSlideIn from "../../components/slide-in.jsx";
import WonAtomCardGrid from "../../components/atom-card-grid";
import WonFooter from "../../components/footer";

import "~/style/_overview.scss";
import ico16_arrow_down from "~/images/won-icons/ico16_arrow_down.svg";
import { useHistory } from "react-router-dom";

export default function PageOverview() {
  const history = useHistory();
  const dispatch = useDispatch();
  const debugModeEnabled = useSelector(viewSelectors.isDebugModeEnabled);
  const [open, setOpen] = useState([]);

  const whatsNewAtomsUnfiltered = useSelector(
    generalSelectors.getWhatsNewAtoms
  );

  const accountState = useSelector(generalSelectors.getAccountState);
  const whatsNewAtoms = whatsNewAtomsUnfiltered
    .filter(metaAtom => atomUtils.isActive(metaAtom))
    .filter(
      metaAtom => debugModeEnabled || !atomUtils.isInvisibleAtom(metaAtom)
    )
    .filter(
      (metaAtom, metaAtomUri) =>
        !accountUtils.isAtomOwned(accountState, metaAtomUri)
    );

  const whatsNewUseCaseIdentifierArray = whatsNewAtoms
    .map(atom => getIn(atom, ["matchedUseCase", "identifier"]))
    .filter(identifier => !!identifier)
    .toSet()
    .toArray();

  const sortedVisibleAtoms = sortByDate(whatsNewAtoms, "creationDate");
  const lastAtomUrisUpdateDate = useSelector(state =>
    getIn(state, ["owner", "lastWhatsNewUpdateTime"])
  );

  const isOwnerAtomUrisLoading = useSelector(
    processSelectors.isProcessingWhatsNew
  );
  const isOwnerAtomUrisToLoad =
    !lastAtomUrisUpdateDate && !isOwnerAtomUrisLoading;

  const isLoggedIn = accountUtils.isLoggedIn(accountState);
  const currentLocation = useSelector(generalSelectors.getCurrentLocation);
  const globalLastUpdateTime = useSelector(
    generalSelectors.selectLastUpdateTime
  );
  const friendlyLastAtomUrisUpdateTimestamp =
    lastAtomUrisUpdateDate &&
    wonLabelUtils.relativeTime(globalLastUpdateTime, lastAtomUrisUpdateDate);
  const sortedVisibleAtomsSize = sortedVisibleAtoms
    ? sortedVisibleAtoms.length
    : 0;
  const hasVisibleAtoms = sortedVisibleAtomsSize > 0;
  const showSlideIns = useSelector(viewSelectors.showSlideIns(history));
  const showModalDialog = useSelector(viewSelectors.showModalDialog);

  useEffect(() => {
    if (isOwnerAtomUrisToLoad) {
      dispatch(actionCreators.atoms__fetchWhatsNew());
    }
  });

  function hasOtherAtoms() {
    return !!whatsNewAtoms.find(atom => !atomUtils.hasMatchedUseCase(atom));
  }
  function getOtherAtomsSize() {
    const useCaseAtoms = whatsNewAtoms.filter(
      atom => !atomUtils.hasMatchedUseCase(atom)
    );
    return useCaseAtoms ? useCaseAtoms.size : 0;
  }

  function getAtomsSizeByUseCase(ucIdentifier) {
    const useCaseAtoms = whatsNewAtoms.filter(
      atom => atomUtils.getMatchedUseCaseIdentifier(atom) === ucIdentifier
    );
    return useCaseAtoms ? useCaseAtoms.size : 0;
  }

  function getSortedVisibleAtomsByUseCase(ucIdentifier) {
    const useCaseAtoms = whatsNewAtoms.filter(
      atom => atomUtils.getMatchedUseCaseIdentifier(atom) === ucIdentifier
    );
    return sortByDate(useCaseAtoms, "creationDate") || [];
  }

  function getSortedVisibleOtherAtoms() {
    const useCaseAtoms = whatsNewAtoms.filter(
      atom => !atomUtils.hasMatchedUseCase(atom)
    );
    return sortByDate(useCaseAtoms, "creationDate") || [];
  }

  function isUseCaseExpanded(ucIdentifier) {
    return open.includes(ucIdentifier);
  }

  function toggleUseCase(ucIdentifier) {
    setOpen(
      isUseCaseExpanded(ucIdentifier)
        ? open.filter(element => ucIdentifier !== element)
        : open.concat(ucIdentifier)
    );
  }

  function reload() {
    if (!isOwnerAtomUrisLoading) {
      const modifiedAfterDate =
        (lastAtomUrisUpdateDate && new Date(lastAtomUrisUpdateDate)) ||
        new Date(Date.now() - 30 /*Days before*/ * 86400000);
      dispatch(actionCreators.atoms__fetchWhatsNew(modifiedAfterDate));
    }
  }

  return (
    <section className={!isLoggedIn ? "won-signed-out" : ""}>
      {showModalDialog && <WonModalDialog />}
      <WonTopnav pageTitle="What's New" />
      {isLoggedIn && <WonMenu />}
      <WonToasts />
      {showSlideIns && <WonSlideIn />}
      <main className="owneroverview">
        <div className="owneroverview__header">
          <div className="owneroverview__header__title">
            {"What's new? "}
            {!isOwnerAtomUrisLoading &&
              hasVisibleAtoms &&
              !whatsNewUseCaseIdentifierArray && (
                <span className="owneroverview__header__title__count">
                  {"(" + sortedVisibleAtomsSize + ")"}
                </span>
              )}
          </div>
          <div className="owneroverview__header__updated">
            {isOwnerAtomUrisLoading ? (
              <div className="owneroverview__header__updated__loading hide-in-responsive">
                Loading...
              </div>
            ) : (
              <div className="owneroverview__header__updated__time hide-in-responsive">
                {"Updated: " + friendlyLastAtomUrisUpdateTimestamp}
              </div>
            )}
            <button
              className="owneroverview__header__updated__reload won-button--filled secondary"
              onClick={reload}
              disabled={isOwnerAtomUrisLoading}
            >
              Reload
            </button>
          </div>
        </div>
        {hasVisibleAtoms ? (
          <div className="owneroverview__usecases">
            {whatsNewUseCaseIdentifierArray.map((ucIdentifier, index) => (
              <div
                className="owneroverview__usecases__usecase"
                key={ucIdentifier + "-" + index}
              >
                <div
                  className="owneroverview__usecases__usecase__header clickable"
                  onClick={() => toggleUseCase(ucIdentifier)}
                >
                  {useCaseUtils.getUseCaseIcon(ucIdentifier) && (
                    <svg className="owneroverview__usecases__usecase__header__icon">
                      <use
                        xlinkHref={useCaseUtils.getUseCaseIcon(ucIdentifier)}
                        href={useCaseUtils.getUseCaseIcon(ucIdentifier)}
                      />
                    </svg>
                  )}
                  <div className="owneroverview__usecases__usecase__header__title">
                    {useCaseUtils.getUseCaseLabel(ucIdentifier)}
                    <span className="owneroverview__usecases__usecase__header__title__count">
                      {"(" + getAtomsSizeByUseCase(ucIdentifier) + ")"}
                    </span>
                  </div>
                  <svg
                    className={
                      "owneroverview__usecases__usecase__header__carret " +
                      (isUseCaseExpanded(ucIdentifier)
                        ? " owneroverview__usecases__usecase__header__carret--expanded "
                        : " owneroverview__usecases__usecase__header__carret--collapsed ")
                    }
                  >
                    <use xlinkHref={ico16_arrow_down} href={ico16_arrow_down} />
                  </svg>
                </div>
                {isUseCaseExpanded(ucIdentifier) && (
                  <div className="owneroverview__usecases__usecase__atoms">
                    <WonAtomCardGrid
                      atoms={getSortedVisibleAtomsByUseCase(ucIdentifier)}
                      currentLocation={currentLocation}
                      showIndicators={false}
                      showHolder={true}
                      showCreate={false}
                    />
                  </div>
                )}
              </div>
            ))}
            {hasOtherAtoms() && (
              <div className="owneroverview__usecases__usecase">
                {whatsNewUseCaseIdentifierArray && (
                  <div
                    className="owneroverview__usecases__usecase__header clickable"
                    onClick={() => toggleUseCase(undefined)}
                  >
                    <div className="owneroverview__usecases__usecase__header__title">
                      Other
                      <span className="owneroverview__usecases__usecase__header__title__count">
                        {"(" + getOtherAtomsSize() + ")"}
                      </span>
                    </div>
                    <svg
                      className={
                        "owneroverview__usecases__usecase__header__carret " +
                        (isUseCaseExpanded(undefined)
                          ? " owneroverview__usecases__usecase__header__carret--expanded "
                          : " owneroverview__usecases__usecase__header__carret--collapsed ")
                      }
                    >
                      <use
                        xlinkHref={ico16_arrow_down}
                        href={ico16_arrow_down}
                      />
                    </svg>
                  </div>
                )}

                {isUseCaseExpanded(undefined) && (
                  <div className="owneroverview__usecases__usecase__atoms">
                    <WonAtomCardGrid
                      atoms={getSortedVisibleOtherAtoms()}
                      currentLocation={currentLocation}
                      showIndicators={false}
                      showHolder={true}
                      showCreate={false}
                    />
                  </div>
                )}
              </div>
            )}
          </div>
        ) : (
          <div className="owneroverview__noresults">
            <span className="owneroverview__noresults__label">
              Nothing new found.
            </span>
          </div>
        )}
      </main>
      <WonFooter />
    </section>
  );
}
