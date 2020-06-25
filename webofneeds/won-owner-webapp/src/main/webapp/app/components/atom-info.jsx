import React, { useState, useEffect } from "react";
import PropTypes from "prop-types";
import { useSelector } from "react-redux";
import { get } from "../utils.js";
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
  atom,
  ownedConnection,
  className,
  initialTab = "DETAIL",
}) {
  const atomUri = get(atom, "uri");
  const connectionUri = get(ownedConnection, "uri");
  const processState = useSelector(generalSelectors.getProcessState);
  const storedAtoms = useSelector(generalSelectors.getAtoms);

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
    [atomUri, connectionUri, atomLoading]
  );

  const relevantConnectionsMap = useSelector(
    generalSelectors.getConnectionsOfAtomWithOwnedTargetConnections(atomUri)
  );

  return (
    <won-atom-info
      class={
        (className ? className : "") + (atomLoading ? " won-is-loading " : "")
      }
    >
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
        setVisibleTab={setVisibleTab}
        toggleAddPicker={toggleAddPicker}
        relevantConnectionsMap={relevantConnectionsMap}
      />
      <WonAtomContent
        atom={atom}
        visibleTab={visibleTab}
        relevantConnectionsMap={relevantConnectionsMap}
        toggleAddPicker={toggleAddPicker}
        showAddPicker={showAddPicker}
        setVisibleTab={setVisibleTab}
        storedAtoms={storedAtoms}
      />
    </won-atom-info>
  );
}
WonAtomInfo.propTypes = {
  atom: PropTypes.object,
  className: PropTypes.string,
  ownedConnection: PropTypes.object,
  initialTab: PropTypes.string,
};
