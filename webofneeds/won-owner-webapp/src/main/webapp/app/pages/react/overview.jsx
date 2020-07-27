import React, { useEffect } from "react";
import { useSelector, useDispatch } from "react-redux";
import { actionCreators } from "../../actions/actions.js";
import * as generalSelectors from "../../redux/selectors/general-selectors.js";
import * as atomUtils from "../../redux/utils/atom-utils.js";
import {
  get,
  getIn,
  sortByDate,
  generateLink,
  getQueryParams,
} from "../../utils.js";
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
import { useHistory, Link } from "react-router-dom";

const OTHERIDENTIFIER = "_other";

export default function PageOverview() {
  const history = useHistory();
  const { useCase } = getQueryParams(history.location);
  const dispatch = useDispatch();
  const debugModeEnabled = useSelector(viewSelectors.isDebugModeEnabled);
  const accountState = useSelector(generalSelectors.getAccountState);
  const currentLocation = useSelector(generalSelectors.getCurrentLocation);
  const showSlideIns = useSelector(viewSelectors.showSlideIns(history));
  const showModalDialog = useSelector(viewSelectors.showModalDialog);
  const isOwnerAtomUrisLoading = useSelector(
    processSelectors.isProcessingWhatsNew
  );
  const globalLastUpdateTime = useSelector(
    generalSelectors.selectLastUpdateTime
  );

  const whatsNewAtomsUnfiltered = useSelector(
    generalSelectors.getWhatsNewAtoms
  );

  const whatsNewAtoms = whatsNewAtomsUnfiltered
    .filter(metaAtom => atomUtils.isActive(metaAtom))
    .filter(
      metaAtom => debugModeEnabled || !atomUtils.isInvisibleAtom(metaAtom)
    )
    .filter(
      (metaAtom, metaAtomUri) =>
        !accountUtils.isAtomOwned(accountState, metaAtomUri)
    );

  const whatsNewAtomsGroupedByUseCaseIdentifier =
    whatsNewAtoms &&
    whatsNewAtoms
      .groupBy(atom => atomUtils.getMatchedUseCaseIdentifier(atom))
      .toOrderedMap()
      .sortBy(
        (_, ucIdentifier) => useCaseUtils.getUseCaseLabel(ucIdentifier) || "zzz"
      );

  const lastAtomUrisUpdateDate = useSelector(state =>
    getIn(state, ["owner", "lastWhatsNewUpdateTime"])
  );

  const isOwnerAtomUrisToLoad =
    !lastAtomUrisUpdateDate && !isOwnerAtomUrisLoading;

  const isLoggedIn = accountUtils.isLoggedIn(accountState);
  const friendlyLastAtomUrisUpdateTimestamp =
    lastAtomUrisUpdateDate &&
    wonLabelUtils.relativeTime(globalLastUpdateTime, lastAtomUrisUpdateDate);

  useEffect(() => {
    if (isOwnerAtomUrisToLoad) {
      dispatch(actionCreators.atoms__fetchWhatsNew());
    }
  });

  function reload() {
    if (!isOwnerAtomUrisLoading) {
      const modifiedAfterDate =
        (lastAtomUrisUpdateDate && new Date(lastAtomUrisUpdateDate)) ||
        new Date(Date.now() - 30 /*Days before*/ * 86400000);
      dispatch(actionCreators.atoms__fetchWhatsNew(modifiedAfterDate));
    }
  }

  let overviewContentElement;
  if (useCase) {
    const useCaseIcon = useCaseUtils.getUseCaseIcon(useCase);
    const visibleAtoms =
      useCase === OTHERIDENTIFIER
        ? get(whatsNewAtomsGroupedByUseCaseIdentifier, undefined)
        : get(whatsNewAtomsGroupedByUseCaseIdentifier, useCase);
    const visibleAtomsSize = visibleAtoms ? visibleAtoms.size : 0;

    overviewContentElement = (
      <main className="owneroverview">
        <div
          className={
            "owneroverview__header " +
            (useCaseIcon ? " owneroverview__header--withIcon " : "")
          }
        >
          {useCaseIcon ? (
            <svg className="owneroverview__header__icon">
              <use xlinkHref={useCaseIcon} href={useCaseIcon} />
            </svg>
          ) : (
            undefined
          )}
          <div className="owneroverview__header__title">
            {useCaseUtils.getUseCaseLabel(useCase)}
            <span className="owneroverview__header__title__count">
              {`(${visibleAtomsSize})`}
            </span>
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
        {visibleAtomsSize > 0 ? (
          <div className="owneroverview__usecases">
            <div className="owneroverview__usecases__usecase">
              <div className="owneroverview__usecases__usecase__atoms">
                <WonAtomCardGrid
                  atoms={sortByDate(visibleAtoms)}
                  currentLocation={currentLocation}
                  showIndicators={false}
                  showHolder={true}
                  showCreate={false}
                />
              </div>
            </div>
          </div>
        ) : (
          <div className="owneroverview__noresults">
            <span className="owneroverview__noresults__label">
              Nothing new found.
            </span>
          </div>
        )}
      </main>
    );
  } else {
    const useCaseIdentifierElements = [];
    whatsNewAtomsGroupedByUseCaseIdentifier &&
      whatsNewAtomsGroupedByUseCaseIdentifier.map((atoms, ucIdentifier) => {
        const useCaseIcon = useCaseUtils.getUseCaseIcon(ucIdentifier);

        useCaseIdentifierElements.push(
          <div
            className="owneroverview__usecases__usecase"
            key={ucIdentifier || OTHERIDENTIFIER}
          >
            <Link
              className="owneroverview__usecases__usecase__header clickable"
              to={location =>
                generateLink(
                  location,
                  { useCase: ucIdentifier || OTHERIDENTIFIER },
                  "/overview",
                  false
                )
              }
            >
              {useCaseIcon && (
                <svg className="owneroverview__usecases__usecase__header__icon">
                  <use xlinkHref={useCaseIcon} href={useCaseIcon} />
                </svg>
              )}
              <div className="owneroverview__usecases__usecase__header__title">
                {useCaseUtils.getUseCaseLabel(ucIdentifier) || "Other"}
                <span className="owneroverview__usecases__usecase__header__title__count">
                  {`(${atoms ? atoms.size : 0})`}
                </span>
              </div>
              <svg className="owneroverview__usecases__usecase__header__carret">
                <use xlinkHref={ico16_arrow_down} href={ico16_arrow_down} />
              </svg>
            </Link>
          </div>
        );
      });

    overviewContentElement = (
      <main className="owneroverview">
        <div className="owneroverview__header">
          <div className="owneroverview__header__title">{"What's new?"}</div>
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
        {useCaseIdentifierElements.length > 0 ? (
          <div className="owneroverview__usecases">
            {useCaseIdentifierElements}
          </div>
        ) : (
          <div className="owneroverview__noresults">
            <span className="owneroverview__noresults__label">
              Nothing new found.
            </span>
          </div>
        )}
      </main>
    );
  }

  return (
    <section className={!isLoggedIn ? "won-signed-out" : ""}>
      {showModalDialog && <WonModalDialog />}
      <WonTopnav pageTitle="What's New" />
      {isLoggedIn && <WonMenu />}
      <WonToasts />
      {showSlideIns && <WonSlideIn />}
      {overviewContentElement}
      <WonFooter />
    </section>
  );
}
