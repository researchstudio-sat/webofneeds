import React, { useState, useEffect } from "react";
import PropTypes from "prop-types";
import { actionCreators } from "~/app/actions/actions";
import { useDispatch, useSelector } from "react-redux";
import { useHistory } from "react-router-dom";
import { get, generateLink } from "../utils.js";
import WonAtomHeaderBig from "./atom-header-big.jsx";
import WonAtomMenu from "./atom-menu.jsx";
import WonAtomActions from "./atom-actions.jsx";
import WonAtomContent from "./atom-content.jsx";
import * as generalSelectors from "../redux/selectors/general-selectors";
import * as atomUtils from "../redux/utils/atom-utils";
import * as processUtils from "../redux/utils/process-utils";
import * as connectionUtils from "../redux/utils/connection-utils";

import "~/style/_atom-info.scss";
import ico_loading_anim from "~/images/won-icons/ico_loading_anim.svg";
import ico16_indicator_error from "~/images/won-icons/ico16_indicator_error.svg";

export default function WonAtomInfo({
  atom,
  ownedConnection,
  className,
  initialTab = "DETAIL",
}) {
  const history = useHistory();
  const dispatch = useDispatch();
  const atomUri = get(atom, "uri");
  const connectionUri = get(ownedConnection, "uri");
  const processState = useSelector(generalSelectors.getProcessState);
  const storedAtoms = useSelector(generalSelectors.getAtoms);
  //TODO: IMPLEMENT VISIBILITY SENSOR TO FETCH ONLY WHEN IN VIEW
  const atomLoading =
    !atom || processUtils.isAtomLoading(processState, atomUri);
  const atomFailedToLoad =
    atom && processUtils.hasAtomFailedToLoad(processState, atomUri);
  const atomToLoad = !atom || processUtils.isAtomToLoad(processState, atomUri);
  const initialLoad = processUtils.isProcessingInitialLoad(processState);

  const [visibleTab, setVisibleTab] = useState(initialTab);
  const [showAddPicker, toggleAddPicker] = useState(false);
  const [showActions, toggleActions] = useState(
    !atomLoading &&
      !!ownedConnection &&
      (atomUtils.isInactive(atom) ||
        (ownedConnection && !connectionUtils.isConnected(ownedConnection)))
  );

  useEffect(
    () => {
      setVisibleTab(initialTab);
      toggleAddPicker(false);
    },
    [atomUri, initialTab]
  );

  useEffect(
    () => {
      toggleActions(
        !atomLoading &&
          (atomUtils.isInactive(atom) ||
            (!!ownedConnection &&
              !connectionUtils.isConnected(ownedConnection)))
      );

      if (atomUri && !initialLoad && ((atomToLoad && !atomLoading) || !atom)) {
        dispatch(actionCreators.atoms__fetchUnloadedAtom(atomUri));
      }
    },
    [atomUri, connectionUri, atomLoading, initialLoad]
  );

  const relevantConnectionsMap = useSelector(
    generalSelectors.getConnectionsOfAtomWithOwnedTargetConnections(atomUri)
  );

  /*FIXME: This is a quick(ish) workaround that adds the tabname
    to the route for all pages but /inventory & / -> that way navigating
    from a specific atom within a socket, will not result in
    browserBack showing the detail page but the tab that was previously selected */
  const changeTab =
    history.location.pathname === "/inventory" ||
    history.location.pathname === "/"
      ? setVisibleTab
      : tabName =>
          history.replace(generateLink(history.location, { tab: tabName }));

  function tryReload() {
    if (atomUri && atomFailedToLoad) {
      dispatch(actionCreators.atoms__fetchUnloadedAtom(atomUri));
    }
  }

  if (atomFailedToLoad) {
    return (
      <won-atom-info
        class={(className ? className : "") + " won-failed-to-load "}
      >
        <svg className="ai__failed__icon">
          <use xlinkHref={ico16_indicator_error} href={ico16_indicator_error} />
        </svg>
        <span className="ai__failed__label">
          Failed To Load - Atom might have been deleted
        </span>
        <div className="ai__failed__actions">
          <button
            className="ai__failed__actions__button red won-button--outlined thin"
            onClick={tryReload}
          >
            Try Reload
          </button>
        </div>
      </won-atom-info>
    );
  } else if (atomLoading || atomToLoad || !atom) {
    return (
      <won-atom-info class={(className ? className : "") + " won-is-loading "}>
        <svg className="ai__loading__spinner hspinner">
          <use xlinkHref={ico_loading_anim} href={ico_loading_anim} />
        </svg>
        <span className="ai__loading__label">Loading...</span>
      </won-atom-info>
    );
  } else {
    return (
      <won-atom-info class={className ? className : ""}>
        <WonAtomHeaderBig
          atom={atom}
          ownedConnection={ownedConnection}
          showActions={showActions}
          toggleActions={toggleActions}
        />
        {showActions ? (
          <WonAtomActions
            atom={atom}
            ownedConnection={ownedConnection}
            storedAtoms={storedAtoms}
          />
        ) : (
          undefined
        )}
        <WonAtomMenu
          atom={atom}
          visibleTab={visibleTab}
          setVisibleTab={changeTab}
          toggleAddPicker={toggleAddPicker}
          relevantConnectionsMap={relevantConnectionsMap}
        />
        <WonAtomContent
          atom={atom}
          visibleTab={visibleTab}
          relevantConnectionsMap={relevantConnectionsMap}
          toggleAddPicker={toggleAddPicker}
          showAddPicker={showAddPicker}
          setVisibleTab={changeTab}
          storedAtoms={storedAtoms}
        />
      </won-atom-info>
    );
  }
}
WonAtomInfo.propTypes = {
  atom: PropTypes.object,
  className: PropTypes.string,
  ownedConnection: PropTypes.object,
  initialTab: PropTypes.string,
};
