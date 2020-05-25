import React from "react";
import { useSelector, useDispatch } from "react-redux";
import { actionCreators } from "../../actions/actions.js";
import * as generalSelectors from "../../redux/selectors/general-selectors.js";
import { get, sortByDate } from "../../utils.js";
import * as accountUtils from "../../redux/utils/account-utils.js";
import * as viewSelectors from "../../redux/selectors/view-selectors.js";
import * as processUtils from "../../redux/utils/process-utils.js";
import * as atomUtils from "../../redux/utils/atom-utils.js";
import * as viewUtils from "../../redux/utils/view-utils.js";
import WonModalDialog from "../../components/modal-dialog.jsx";
import WonAtomInfo from "../../components/atom-info.jsx";
import WonTopnav from "../../components/topnav.jsx";
import WonMenu from "../../components/menu.jsx";
import WonToasts from "../../components/toasts.jsx";
import WonSlideIn from "../../components/slide-in.jsx";
import WonFooter from "../../components/footer.jsx";
import WonHowTo from "../../components/howto.jsx";
import WonAtomCardGrid from "../../components/atom-card-grid.jsx";

import ico_loading_anim from "~/images/won-icons/ico_loading_anim.svg";
import ico16_arrow_down from "~/images/won-icons/ico16_arrow_down.svg";

import "~/style/_inventory.scss";
import { useHistory } from "react-router-dom";
import vocab from "../../service/vocab.js";

export default function PageInventory() {
  const history = useHistory();
  const dispatch = useDispatch();
  const ownedActivePersonas = useSelector(state =>
    generalSelectors
      .getOwnedPersonas(state)
      .filter(atom => atomUtils.isActive(atom))
  );
  const ownedUnassignedActivePosts = useSelector(state =>
    generalSelectors
      .getOwnedPosts(state)
      .filter(atom => atomUtils.isActive(atom))
      .filter(atom => !atomUtils.isHeld(atom))
  );
  const ownedInactiveAtoms = useSelector(state =>
    generalSelectors
      .getOwnedAtoms(state)
      .filter(atom => atomUtils.isInactive(atom))
  );

  const sortedOwnedUnassignedActivePosts =
    sortByDate(ownedUnassignedActivePosts, "creationDate") || [];

  const sortedOwnedInactiveAtoms =
    sortByDate(ownedInactiveAtoms, "creationDate") || [];
  const sortedOwnedActivePersonas =
    sortByDate(ownedActivePersonas, "modifiedDate") || [];

  const viewState = useSelector(generalSelectors.getViewState);

  const theme = useSelector(generalSelectors.getTheme);
  const accountState = useSelector(generalSelectors.getAccountState);
  const process = useSelector(generalSelectors.getProcessState);

  const isInitialLoadInProgress = processUtils.isProcessingInitialLoad(process);
  const isLoggedIn = accountUtils.isLoggedIn(accountState);
  const welcomeTemplateHtml = get(theme, "welcomeTemplate");
  const additionalLogos =
    theme && get(theme, "additionalLogos")
      ? get(theme, "additionalLogos").toArray()
      : undefined;
  const currentLocation = useSelector(generalSelectors.getCurrentLocation);
  const hasOwnedUnassignedAtomUris =
    sortedOwnedUnassignedActivePosts.length > 0;

  const hasOwnedInactiveAtomUris = sortedOwnedInactiveAtoms.length > 0;
  const hasOwnedActivePersonas = sortedOwnedActivePersonas.length > 0;
  const unassignedAtomSize = sortedOwnedUnassignedActivePosts.length;
  const inactiveAtomUriSize = sortedOwnedInactiveAtoms.length;

  const showSlideIns = useSelector(
    state =>
      viewSelectors.hasSlideIns(state, history) &&
      viewSelectors.isSlideInsVisible(state)
  );
  const showModalDialog = useSelector(state =>
    viewSelectors.showModalDialog(state)
  );
  const showClosedAtoms = viewUtils.showClosedAtoms(viewState);

  let additionalLogosElement;
  if (additionalLogos && additionalLogos.length > 0) {
    additionalLogosElement = additionalLogos.map(logo => (
      <svg className="ownerwelcome__logo__icon" key={logo}>
        <use xlinkHref={logo} href={logo} />
      </svg>
    ));
  }
  return (
    <section className={!isLoggedIn ? "won-signed-out" : ""}>
      {showModalDialog && <WonModalDialog />}
      <WonTopnav pageTitle="Inventory" />
      {isLoggedIn && <WonMenu />}
      <WonToasts />
      {showSlideIns && <WonSlideIn />}

      {isLoggedIn ? (
        isInitialLoadInProgress ? (
          <main className="ownerloading">
            <svg className="ownerloading__spinner hspinner">
              <use xlinkHref={ico_loading_anim} href={ico_loading_anim} />
            </svg>
            <span className="ownerloading__label">Gathering your Atoms...</span>
          </main>
        ) : (
          <main className="ownerinventory">
            {hasOwnedActivePersonas && (
              <div className="ownerinventory__personas">
                {sortedOwnedActivePersonas.map((persona, index) => (
                  <WonAtomInfo
                    key={get(persona, "uri") + "-" + index}
                    className="ownerinventory__personas__persona"
                    atom={persona}
                    defaultTab={vocab.HOLD.HolderSocketCompacted}
                  />
                ))}
              </div>
            )}
            <div className="ownerinventory__header">
              <div className="ownerinventory__header__title">
                Unassigned
                {hasOwnedUnassignedAtomUris && (
                  <span className="ownerinventory__header__title__count">
                    {"(" + unassignedAtomSize + ")"}
                  </span>
                )}
              </div>
            </div>
            <div className="ownerinventory__content">
              <WonAtomCardGrid
                atoms={sortedOwnedUnassignedActivePosts}
                currentLocation={currentLocation}
                showSuggestions={true}
                showHolder={true}
                showCreate={true}
                showCreatePersona={true}
              />
            </div>
            {hasOwnedInactiveAtomUris && (
              <div className="ownerinventory__header">
                <div className="ownerinventory__header__title">
                  Archived
                  <span className="ownerinventory__header__title__count">
                    {"(" + inactiveAtomUriSize + ")"}
                  </span>
                </div>
                <svg
                  className={
                    "ownerinventory__header__carret " +
                    (showClosedAtoms
                      ? "ownerinventory__header__carret--expanded"
                      : "ownerinventory__header__carret--collapsed")
                  }
                  onClick={() =>
                    dispatch(actionCreators.view__toggleClosedAtoms())
                  }
                >
                  <use xlinkHref={ico16_arrow_down} href={ico16_arrow_down} />
                </svg>
              </div>
            )}
            {showClosedAtoms &&
              hasOwnedInactiveAtomUris && (
                <div className="ownerinventory__content">
                  <WonAtomCardGrid
                    atoms={sortedOwnedInactiveAtoms}
                    currentLocation={currentLocation}
                    showSuggestions={false}
                    showHolder={false}
                    showCreate={false}
                  />
                </div>
              )}
          </main>
        )
      ) : (
        <main className="ownerwelcome">
          <div
            className="ownerwelcome__text"
            dangerouslySetInnerHTML={{
              __html: welcomeTemplateHtml,
            }}
          />
          <div className="ownerwelcome__logo">{additionalLogosElement}</div>
          <WonHowTo />
        </main>
      )}

      <WonFooter />
    </section>
  );
}
