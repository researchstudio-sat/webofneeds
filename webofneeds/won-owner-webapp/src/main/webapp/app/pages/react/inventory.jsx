import React from "react";
import { useSelector, useDispatch } from "react-redux";
import { actionCreators } from "../../actions/actions.js";
import * as generalSelectors from "../../redux/selectors/general-selectors.js";
import { get } from "../../utils.js";
import * as accountUtils from "../../redux/utils/account-utils.js";
import * as viewSelectors from "../../redux/selectors/view-selectors.js";
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

import ico16_arrow_down from "~/images/won-icons/ico16_arrow_down.svg";

import "~/style/_inventory.scss";
import { useHistory } from "react-router-dom";
import vocab from "../../service/vocab.js";

export default function PageInventory() {
  const history = useHistory();
  const dispatch = useDispatch();
  const viewState = useSelector(generalSelectors.getViewState);
  const theme = useSelector(generalSelectors.getTheme);
  const accountState = useSelector(generalSelectors.getAccountState);

  const ownedActivePersonas = useSelector(state =>
    generalSelectors
      .getOwnedPersonas(state)
      .filter(atomUtils.isActive)
      .toOrderedMap()
      .sortBy(persona => atomUtils.getTitle(persona))
  );

  const ownedUnassignedActivePosts = useSelector(state =>
    generalSelectors
      .getOwnedPosts(state)
      .filter(atomUtils.isActive)
      .filter(
        atom =>
          !atomUtils.isHeld(atom) ||
          !accountUtils.isAtomOwned(accountState, atomUtils.getHeldByUri(atom))
      )
      .toOrderedMap()
      .sortBy(atom => {
        const creationDate = atomUtils.getCreationDate(atom);
        return creationDate && creationDate.getTime();
      })
      .reverse()
  );

  const ownedInactiveAtoms = useSelector(state =>
    generalSelectors
      .getOwnedAtoms(state)
      .filter(atomUtils.isInactive)
      .toOrderedMap()
      .sortBy(atom => {
        const creationDate = atomUtils.getCreationDate(atom);
        return creationDate && creationDate.getTime();
      })
      .reverse()
  );

  const isLoggedIn = accountUtils.isLoggedIn(accountState);
  const welcomeTemplateHtml = get(theme, "welcomeTemplate");
  const additionalLogos =
    theme && get(theme, "additionalLogos")
      ? get(theme, "additionalLogos").toArray()
      : undefined;
  const currentLocation = useSelector(generalSelectors.getCurrentLocation);
  const unassignedAtomSize = ownedUnassignedActivePosts
    ? ownedUnassignedActivePosts.size
    : 0;
  const hasOwnedUnassignedAtomUris = unassignedAtomSize > 0;

  const inactiveAtomUriSize = ownedInactiveAtoms ? ownedInactiveAtoms.size : 0;
  const hasOwnedInactiveAtomUris = ownedInactiveAtoms > 0;

  const showSlideIns = useSelector(viewSelectors.showSlideIns(history));
  const showModalDialog = useSelector(viewSelectors.showModalDialog);
  const showClosedAtoms = viewUtils.showClosedAtoms(viewState);

  let additionalLogosElement;
  if (additionalLogos && additionalLogos.length > 0) {
    additionalLogosElement = additionalLogos.map(logo => (
      <svg className="ownerwelcome__logo__icon" key={logo}>
        <use xlinkHref={logo} href={logo} />
      </svg>
    ));
  }

  const personaElements = [];

  ownedActivePersonas &&
    ownedActivePersonas.map((persona, personaUri) => {
      personaElements.push(
        <WonAtomInfo
          key={personaUri}
          className="ownerinventory__personas__persona"
          atomUri={personaUri}
          atom={persona}
          initialTab={vocab.HOLD.HolderSocketCompacted}
        />
      );
    });

  return (
    <section className={!isLoggedIn ? "won-signed-out" : ""}>
      {showModalDialog && <WonModalDialog />}
      <WonTopnav pageTitle="Inventory" />
      {isLoggedIn && <WonMenu />}
      <WonToasts />
      {showSlideIns && <WonSlideIn />}

      {isLoggedIn ? (
        <main className="ownerinventory">
          {personaElements.length > 0 && (
            <div className="ownerinventory__personas">{personaElements}</div>
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
              atoms={ownedUnassignedActivePosts}
              currentLocation={currentLocation}
              showIndicators={true}
              showHolder={true}
              showCreate={true}
              showCreatePersona={true}
            />
          </div>
          {hasOwnedInactiveAtomUris && (
            <div
              className="ownerinventory__header clickable"
              onClick={() => dispatch(actionCreators.view__toggleClosedAtoms())}
            >
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
              >
                <use xlinkHref={ico16_arrow_down} href={ico16_arrow_down} />
              </svg>
            </div>
          )}
          {showClosedAtoms &&
            hasOwnedInactiveAtomUris && (
              <div className="ownerinventory__content">
                <WonAtomCardGrid
                  atoms={ownedInactiveAtoms}
                  currentLocation={currentLocation}
                  showIndicators={false}
                  showHolder={false}
                  showCreate={false}
                />
              </div>
            )}
        </main>
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
