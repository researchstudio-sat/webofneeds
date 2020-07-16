import React, { useState, useEffect } from "react";
import PropTypes from "prop-types";
import { useSelector } from "react-redux";
import { useHistory } from "react-router-dom";
import { generateLink } from "../utils.js";
import WonAtomHeaderBig from "./atom-header-big.jsx";
import WonAtomMenu from "./atom-menu.jsx";
import WonAtomActions from "./atom-actions.jsx";
import WonAtomContent from "./atom-content.jsx";
import * as generalSelectors from "../redux/selectors/general-selectors";
import * as atomUtils from "../redux/utils/atom-utils";
import * as processUtils from "../redux/utils/process-utils";
import * as connectionUtils from "../redux/utils/connection-utils";

import "~/style/_atom-info.scss";

export default function WonAtomInfo({
  atomUri,
  atom,
  ownedConnectionUri,
  ownedConnection,
  className,
  initialTab = "DETAIL",
}) {
  const history = useHistory();
  const processState = useSelector(generalSelectors.getProcessState);
  const storedAtoms = useSelector(generalSelectors.getAtoms);
  //TODO: IMPLEMENT VISIBILITY SENSOR TO FETCH ONLY WHEN IN VIEW
  const atomLoading =
    !atom || processUtils.isAtomLoading(processState, atomUri);

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
    },
    [atomUri, ownedConnectionUri, atomLoading]
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

  return (
    <won-atom-info class={className ? className : ""}>
      <WonAtomHeaderBig
        atomUri={atomUri}
        atom={atom}
        ownedConnectionUri={ownedConnectionUri}
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
WonAtomInfo.propTypes = {
  atomUri: PropTypes.string.isRequired,
  atom: PropTypes.object,
  className: PropTypes.string,
  ownedConnectionUri: PropTypes.string,
  ownedConnection: PropTypes.object,
  initialTab: PropTypes.string,
};
