import React, { useEffect } from "react";
import PropTypes from "prop-types";
import { useDispatch, useSelector } from "react-redux";
import { actionCreators } from "../actions/actions.js";

import "~/style/_menu.scss";
import * as viewSelectors from "../redux/selectors/view-selectors.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as accountUtils from "../redux/utils/account-utils.js";
import Immutable from "immutable";
import { Link, NavLink, useHistory } from "react-router-dom";
import ico36_person_anon from "~/images/won-icons/ico36_person_anon.svg";
import { get, generateLink } from "~/app/utils";
import ico32_buddy_add from "~/images/won-icons/ico32_buddy_add.svg";
import * as atomUtils from "~/app/redux/utils/atom-utils";
import WonAtomIcon from "~/app/components/atom-icon";
import WonShareDropdown from "~/app/components/share-dropdown";
import WonAtomContextDropdown from "~/app/components/atom-context-dropdown";
import WonAtomMenu from "~/app/components/atom-menu";
import WonFooter from "~/app/components/footer";
import VisibilitySensor from "react-visibility-sensor";
import * as processUtils from "~/app/redux/utils/process-utils";
import vocab from "~/app/service/vocab";

export default function WonMenu({ className }) {
  const dispatch = useDispatch();
  const history = useHistory();
  const hasSlideIns = useSelector(viewSelectors.hasSlideIns(history));
  const isMenuVisible = useSelector(viewSelectors.isMenuVisible);
  const accountState = useSelector(generalSelectors.getAccountState);

  // const email = accountUtils.getEmail(accountState); //TODO: E-Mail is not displayed for now, figure out what to do with it in the future
  const isAnonymous = accountUtils.isAnonymous(accountState);
  const isLocationAccessDenied = useSelector(
    generalSelectors.isLocationAccessDenied
  );
  const hasUnassignedUnpinnedAtomNonChatUnreads = useSelector(
    generalSelectors.hasUnassignedUnpinnedAtomNonChatUnreads
  );
  const hasUnassignedUnpinnedAtomChatUnreads = useSelector(
    generalSelectors.hasUnassignedUnpinnedAtomChatUnreads
  );

  const processState = useSelector(generalSelectors.getProcessState);

  const activePinnedAtomUri = useSelector(viewSelectors.getActivePinnedAtomUri);
  const activePinnedAtomTab = useSelector(viewSelectors.getActivePinnedAtomTab);

  const ownedPinnedAtoms = useSelector(state =>
    generalSelectors
      .getOwnedPinnedAtoms(state)
      .toOrderedMap()
      .sortBy(atom => atomUtils.getTitle(atom))
  );

  const ownedPinnedAtomUnreads = useSelector(
    generalSelectors.getOwnedPinnedAtomsUnreads
  );

  const activePinnedAtom = get(ownedPinnedAtoms, activePinnedAtomUri);

  const relevantActivePinnedAtomConnectionsMap = useSelector(
    generalSelectors.getConnectionsOfAtomWithOwnedTargetConnections(
      activePinnedAtomUri
    )
  );

  const pinnedAtomElements = [];
  ownedPinnedAtoms &&
    ownedPinnedAtoms.map((pinnedAtom, pinnedAtomUri) => {
      pinnedAtomElements.push(
        <div
          className={
            "pinnedatoms__pinnedatom " +
            (activePinnedAtomUri === pinnedAtomUri
              ? "pinnedatoms__pinnedatom--active"
              : "clickable")
          }
          onClick={() => {
            if (activePinnedAtomUri !== pinnedAtomUri) {
              hideMenuIfVisible();
              dispatch(
                actionCreators.view__setActivePinnedAtomUri(pinnedAtomUri)
              );
              if (
                !(
                  history.location.pathname === "/inventory" ||
                  history.location.pathname === "/"
                )
              ) {
                history.replace(
                  generateLink(history.location, {}, "/inventory")
                );
              }
            }
          }}
          title={atomUtils.getTitle(pinnedAtom)}
          key={pinnedAtomUri}
        >
          {get(ownedPinnedAtomUnreads, pinnedAtomUri) ? (
            <span className="pinnedatoms__pinnedatom__unreads" />
          ) : (
            undefined
          )}
          <VisibilitySensor
            onChange={isVisible => {
              if (isVisible) {
                const isAtomFetchNecessary = processUtils.isAtomFetchNecessary(
                  processState,
                  pinnedAtomUri,
                  pinnedAtom
                );
                if (isAtomFetchNecessary) {
                  console.debug("fetch pinnedAtomUri, ", pinnedAtomUri);
                  dispatch(
                    actionCreators.atoms__fetchUnloadedAtom(pinnedAtomUri)
                  );
                }
              }
            }}
            intervalDelay={200}
            partialVisibility={true}
            offset={{ top: -300, bottom: -300 }}
          >
            <WonAtomIcon
              className="pinnedatoms__pinnedatom__icon"
              atom={pinnedAtom}
            />
          </VisibilitySensor>
        </div>
      );
    });

  function generateTabClasses(inactive = false, unread = false) {
    const classes = ["menu__tab"];

    inactive && classes.push("menu__tab--inactive");
    unread && classes.push("menu__tab--unread");

    return classes.join(" ");
  }

  function generateRootClasses() {
    const classes = [];
    className && classes.push(className);
    hasSlideIns && classes.push("won-has-slideins");
    isMenuVisible && classes.push("won-menu--show-mobile");

    return classes.join(" ");
  }

  function hideMenuIfVisible() {
    if (isMenuVisible) {
      dispatch(actionCreators.view__hideMenu());
    }
  }

  function viewWhatsAround() {
    viewWhatsX(() => {
      hideMenuIfVisible();
    });
  }

  function logout() {
    const payload = {
      caption: "Sign out",
      text: "Do you really want to sign out?",
      buttons: [
        {
          caption: "Yes",
          callback: () => {
            dispatch(actionCreators.view__hideModalDialog());
            dispatch(actionCreators.account__logout(history));
            history.push("/inventory");
          },
        },
        {
          caption: "No",
          callback: () => {
            dispatch(actionCreators.view__hideModalDialog());
          },
        },
      ],
    };
    dispatch(actionCreators.view__showModalDialog(payload));
  }

  function viewWhatsNew() {
    viewWhatsX(() => {
      hideMenuIfVisible();
    });
  }

  function viewWhatsX(callback) {
    if (isLocationAccessDenied) {
      callback();
    } else if ("geolocation" in navigator) {
      navigator.geolocation.getCurrentPosition(
        currentLocation => {
          const lat = currentLocation.coords.latitude;
          const lng = currentLocation.coords.longitude;

          dispatch(
            actionCreators.view__updateCurrentLocation(
              Immutable.fromJS({ location: { lat, lng } })
            )
          );
          callback();
        },
        error => {
          //error handler
          console.error(
            "Could not retrieve geolocation due to error: ",
            error.code,
            ", continuing map initialization without currentLocation. fullerror:",
            error
          );
          dispatch(actionCreators.view__locationAccessDenied());
          callback();
        },
        {
          //options
          enableHighAccuracy: true,
          maximumAge: 30 * 60 * 1000, //use if cache is not older than 30min
        }
      );
    } else {
      console.error("location could not be retrieved");
      dispatch(actionCreators.view__locationAccessDenied());
      callback();
    }
  }

  let thisNode;
  useEffect(() => {
    function handleClick(e) {
      if (thisNode && !thisNode.contains(e.target) && isMenuVisible) {
        // TODO: Fix me.
        // Handler is closing menu before actual MenuAction is toggled
        // dispatch(actionCreators.view__hideMenu());
      }
    }

    document.addEventListener("mousedown", handleClick, false);

    return function cleanup() {
      document.removeEventListener("mousedown", handleClick, false);
    };
  });

  return (
    <won-menu class={generateRootClasses()} ref={node => (thisNode = node)}>
      <div className="pinnedatoms">
        {pinnedAtomElements}
        <div
          className={
            "pinnedatoms__pinnedatom pinnedatoms__pinnedatom--anon " +
            (!activePinnedAtomUri
              ? "pinnedatoms__pinnedatom--active"
              : "clickable")
          }
          onClick={() => {
            if (activePinnedAtomUri) {
              dispatch(actionCreators.view__setActivePinnedAtomUri());
              hideMenuIfVisible();
            }
          }}
        >
          {hasUnassignedUnpinnedAtomChatUnreads ||
          hasUnassignedUnpinnedAtomNonChatUnreads ? (
            <span className="pinnedatoms__pinnedatom__unreads" />
          ) : (
            undefined
          )}
          <svg className="pinnedatoms__pinnedatom__anonicon">
            <use xlinkHref={ico36_person_anon} href={ico36_person_anon} />
          </svg>
        </div>
        <Link
          className="pinnedatoms__create"
          onClick={hideMenuIfVisible}
          to={location =>
            generateLink(
              location,
              {
                useCase: "persona",
              },
              "/create",
              false
            )
          }
        >
          <svg
            className="pinnedatoms__create__icon"
            title="Create a new Persona"
          >
            <use xlinkHref={ico32_buddy_add} href={ico32_buddy_add} />
          </svg>
        </Link>
      </div>
      <div className="menu">
        <div className="menu__user">
          {activePinnedAtom ? (
            <div className="menu__user__pinnedatom">
              <span className="menu__user__caption">
                {atomUtils.getTitle(activePinnedAtom)}
              </span>
              <WonShareDropdown atom={activePinnedAtom} />
              <WonAtomContextDropdown atom={activePinnedAtom} />
            </div>
          ) : (
            <span className="menu__user__caption">Anonymous</span>
          )}
          <a className="menu__user__signout" onClick={logout}>
            Sign out
          </a>
        </div>
        {activePinnedAtom ? (
          <WonAtomMenu
            className="pinnedatom__menu"
            atom={activePinnedAtom}
            visibleTab={
              history.location.pathname === "/inventory" ||
              history.location.pathname === "/" ||
              history.location.pathname === "/connections"
                ? activePinnedAtomTab
                : "FIXME:NOTEXISTS"
            }
            toggleAddPicker={() => {}}
            setVisibleTab={tabName => {
              hideMenuIfVisible();
              dispatch(actionCreators.view__setActivePinnedAtomTab(tabName));
              if (tabName === vocab.CHAT.ChatSocketCompacted) {
                history.push(
                  generateLink(history.location, {}, "/connections", false)
                );
              } else if (
                !(
                  history.location.pathname === "/inventory" ||
                  history.location.pathname === "/"
                )
              ) {
                history.push(generateLink(history.location, {}, "/inventory"));
              }
            }}
            relevantConnectionsMap={relevantActivePinnedAtomConnectionsMap}
          />
        ) : (
          <React.Fragment>
            <NavLink
              className={generateTabClasses(
                false,
                hasUnassignedUnpinnedAtomNonChatUnreads
              )}
              activeClassName="menu__tab--selected"
              onClick={hideMenuIfVisible}
              to="/inventory"
            >
              <span className="menu__tab__unread" />
              <span className="menu__tab__label">Unassigned Atoms</span>
            </NavLink>
            <NavLink
              className={generateTabClasses(
                false,
                hasUnassignedUnpinnedAtomChatUnreads
              )}
              activeClassName="menu__tab--selected"
              onClick={hideMenuIfVisible}
              to="/connections"
            >
              <span className="menu__tab__unread" />
              <span className="menu__tab__label">Chats</span>
            </NavLink>
            <NavLink
              className={generateTabClasses()}
              activeClassName="menu__tab--selected"
              onClick={hideMenuIfVisible}
              to="/create"
            >
              <span className="menu__tab__label">Create</span>
            </NavLink>
          </React.Fragment>
        )}
        <NavLink
          className={generateTabClasses()}
          activeClassName="menu__tab--selected"
          onClick={viewWhatsNew}
          to="/overview"
        >
          <span className="menu__tab__label">{"What's New"}</span>
        </NavLink>
        <NavLink
          className={generateTabClasses()}
          activeClassName="menu__tab--selected"
          onClick={viewWhatsAround}
          to="/map"
        >
          <span className="menu__tab__label">{"What's Around"}</span>
        </NavLink>
        <NavLink
          className={generateTabClasses()}
          activeClassName="menu__tab--selected"
          onClick={hideMenuIfVisible}
          to="/settings"
        >
          <span className="menu__tab__label">Account Settings</span>
        </NavLink>
        {isAnonymous && (
          <NavLink
            className={generateTabClasses()}
            activeClassName="menu__tab--selected"
            onClick={hideMenuIfVisible}
            to="/signup"
          >
            <span>Sign up</span>
          </NavLink>
        )}
      </div>
      <WonFooter className="menu__footer" onClick={hideMenuIfVisible} />
    </won-menu>
  );
}
WonMenu.propTypes = {
  className: PropTypes.string,
};
