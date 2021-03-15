import React, { useState } from "react";
import { useSelector, useDispatch } from "react-redux";
import { actionCreators } from "../../actions/actions.js";
import * as generalSelectors from "../../redux/selectors/general-selectors.js";
import { get } from "../../utils.js";
import * as accountUtils from "../../redux/utils/account-utils.js";
import * as viewSelectors from "../../redux/selectors/view-selectors.js";
import * as atomUtils from "../../redux/utils/atom-utils.js";
import * as viewUtils from "../../redux/utils/view-utils.js";
import WonAtomContent from "~/app/components/atom-content";
import WonGenericPage from "~/app/pages/genericPage";
import WonHowTo from "../../components/howto.jsx";
import WonAtomCardGrid from "../../components/atom-card-grid.jsx";

import ico16_arrow_down from "~/images/won-icons/ico16_arrow_down.svg";

import "~/style/_inventory.scss";

export default function PageInventory() {
  const dispatch = useDispatch();
  const viewState = useSelector(generalSelectors.getViewState);
  const theme = useSelector(generalSelectors.getTheme);
  const accountState = useSelector(generalSelectors.getAccountState);

  const storedAtoms = useSelector(generalSelectors.getAtoms);

  const unpinnedActiveAtoms = useSelector(state =>
    generalSelectors
      .getUnassignedUnpinnedAtoms(state)
      .filter(atomUtils.isActive)
      .toOrderedMap()
      .sortBy(atom => {
        const creationDate = atomUtils.getCreationDate(atom);
        return creationDate && creationDate.getTime();
      })
      .reverse()
  );

  const unpinnedInactiveAtoms = useSelector(state =>
    generalSelectors
      .getUnassignedUnpinnedAtoms(state)
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
  const unassignedAtomSize = unpinnedActiveAtoms ? unpinnedActiveAtoms.size : 0;
  const hasOwnedUnassignedAtomUris = unassignedAtomSize > 0;

  const inactiveAtomUriSize = unpinnedInactiveAtoms
    ? unpinnedInactiveAtoms.size
    : 0;
  const hasOwnedInactiveAtomUris = unpinnedInactiveAtoms > 0;

  const showClosedAtoms = viewUtils.showClosedAtoms(viewState);

  let additionalLogosElement;
  if (additionalLogos && additionalLogos.length > 0) {
    additionalLogosElement = additionalLogos.map(logo => (
      <svg className="ownerwelcome__logo__icon" key={logo}>
        <use xlinkHref={logo} href={logo} />
      </svg>
    ));
  }

  const activePinnedAtomUri = useSelector(viewSelectors.getActivePinnedAtomUri);
  const activePinnedAtomTab = useSelector(viewSelectors.getActivePinnedAtomTab);
  const activePinnedAtom = useSelector(
    generalSelectors.getActivePinnedAtom(activePinnedAtomUri)
  );

  const relevantActivePinnedAtomConnectionsMap = useSelector(
    generalSelectors.getConnectionsOfAtomWithOwnedTargetConnections(
      activePinnedAtomUri
    )
  );

  const [showAddPicker, toggleAddPicker] = useState(false);

  return (
    <WonGenericPage pageTitle="Inventory">
      {isLoggedIn ? (
        <main className="ownerinventory">
          {activePinnedAtom ? (
            <div className="ownerinventory__activepersona">
              <WonAtomContent
                atomUri={activePinnedAtomUri}
                atom={activePinnedAtom}
                visibleTab={activePinnedAtomTab}
                relevantConnectionsMap={relevantActivePinnedAtomConnectionsMap}
                toggleAddPicker={toggleAddPicker}
                showAddPicker={showAddPicker}
                setVisibleTab={tabName =>
                  dispatch(actionCreators.view__setActivePinnedAtomTab(tabName))
                }
                storedAtoms={storedAtoms}
              />
            </div>
          ) : (
            <React.Fragment>
              <div className="ownerinventory__header">
                <div className="ownerinventory__header__title">
                  Unassigned Atoms
                  {hasOwnedUnassignedAtomUris && (
                    <span className="ownerinventory__header__title__count">
                      {"(" + unassignedAtomSize + ")"}
                    </span>
                  )}
                </div>
              </div>
              <div className="ownerinventory__content">
                <WonAtomCardGrid
                  atoms={unpinnedActiveAtoms}
                  currentLocation={currentLocation}
                  showIndicators={true}
                  showHolder={true}
                  showCreate={true}
                />
              </div>
              {hasOwnedInactiveAtomUris && (
                <div
                  className="ownerinventory__header clickable"
                  onClick={() =>
                    dispatch(actionCreators.view__toggleClosedAtoms())
                  }
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
                      atoms={unpinnedInactiveAtoms}
                      currentLocation={currentLocation}
                      showIndicators={false}
                      showHolder={false}
                      showCreate={false}
                    />
                  </div>
                )}
            </React.Fragment>
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
    </WonGenericPage>
  );
}
