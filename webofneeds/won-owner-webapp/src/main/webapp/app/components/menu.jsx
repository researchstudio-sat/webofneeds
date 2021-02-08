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
import VisibilitySensor from "react-visibility-sensor";
import * as processUtils from "~/app/redux/utils/process-utils";

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
  const hasUnreadSuggestedConnections = useSelector(
    generalSelectors.hasUnreadSuggestedConnections
  );
  const hasUnreadBuddyConnections = useSelector(
    generalSelectors.hasUnreadBuddyConnections(true, false)
  );
  const hasUnreadChatConnections = useSelector(
    generalSelectors.hasUnreadChatConnections
  );

  const processState = useSelector(generalSelectors.getProcessState);

  const activePersonaUri = useSelector(viewSelectors.getActivePersonaUri);
  const activePersonaTab = useSelector(viewSelectors.getActivePersonaTab);

  const ownedPersonas = useSelector(state =>
    generalSelectors
      .getOwnedPersonas(state)
      .toOrderedMap()
      .sortBy(persona => atomUtils.getTitle(persona))
  );

  const activePersona = get(ownedPersonas, activePersonaUri);

  const relevantActivePersonaConnectionsMap = useSelector(
    generalSelectors.getConnectionsOfAtomWithOwnedTargetConnections(
      activePersonaUri
    )
  );

  const personaElements = [];
  ownedPersonas &&
    ownedPersonas.map((persona, personaUri) => {
      personaElements.push(
        <div
          className={
            "personas__persona " +
            (activePersonaUri === personaUri
              ? "personas__persona--active"
              : "clickable")
          }
          onClick={() =>
            activePersonaUri !== personaUri &&
            dispatch(actionCreators.view__setActivePersonaUri(personaUri))
          }
          title={atomUtils.getTitle(persona)}
          key={personaUri}
        >
          <VisibilitySensor
            onChange={isVisible => {
              if (isVisible) {
                const isAtomFetchNecessary = processUtils.isAtomFetchNecessary(
                  processState,
                  personaUri,
                  persona
                );
                if (isAtomFetchNecessary) {
                  console.debug("fetch personaUri, ", personaUri);
                  dispatch(actionCreators.atoms__fetchUnloadedAtom(personaUri));
                }
              }
            }}
            intervalDelay={200}
            partialVisibility={true}
            offset={{ top: -300, bottom: -300 }}
          >
            <WonAtomIcon className="personas__persona__icon" atom={persona} />
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
    dispatch(actionCreators.account__logout(history));
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
      <div className="personas">
        {personaElements}
        <div
          className={
            "personas__persona personas__persona--anon " +
            (!activePersonaUri ? "personas__persona--active" : "clickable")
          }
          onClick={() =>
            !!activePersonaUri &&
            dispatch(actionCreators.view__setActivePersonaUri())
          }
        >
          <svg className="personas__persona__anonicon">
            <use xlinkHref={ico36_person_anon} href={ico36_person_anon} />
          </svg>
        </div>
        <Link
          className="personas__create"
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
          <svg className="personas__create__icon" title="Create a new Persona">
            <use xlinkHref={ico32_buddy_add} href={ico32_buddy_add} />
          </svg>
        </Link>
      </div>
      <div className="menu">
        <div className="menu__user">
          {activePersona ? (
            <div className="menu__user__persona">
              <span className="menu__user__caption">
                {atomUtils.getTitle(activePersona)}
              </span>
              <WonShareDropdown atom={activePersona} />
              <WonAtomContextDropdown atom={activePersona} />
            </div>
          ) : (
            <span className="menu__user__caption">Anonymous</span>
          )}
          <a className="menu__user__signout" onClick={logout}>
            Sign out
          </a>
        </div>
        {activePersona ? (
          <WonAtomMenu
            className="persona__menu"
            atom={activePersona}
            visibleTab={activePersonaTab}
            setVisibleTab={tabName => {
              hideMenuIfVisible();
              dispatch(actionCreators.view__setActivePersonaTab(tabName));
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
            }}
            relevantConnectionsMap={relevantActivePersonaConnectionsMap}
          />
        ) : (
          <NavLink
            className={generateTabClasses(
              false,
              hasUnreadSuggestedConnections || hasUnreadBuddyConnections
            )}
            activeClassName="menu__tab--selected"
            onClick={hideMenuIfVisible}
            to="/inventory"
          >
            <span className="menu__tab__unread" />
            <span className="menu__tab__label">Inventory</span>
          </NavLink>
        )}
        <NavLink
          className={generateTabClasses(false, hasUnreadChatConnections)}
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
    </won-menu>
  );
}
WonMenu.propTypes = {
  className: PropTypes.string,
};
