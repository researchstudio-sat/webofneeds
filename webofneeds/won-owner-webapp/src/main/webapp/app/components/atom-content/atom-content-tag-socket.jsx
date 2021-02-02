/**
 * Created by quasarchimaere on 06.05.2020.
 */

import React from "react";
import PropTypes from "prop-types";
import { useSelector } from "react-redux";
import vocab from "../../service/vocab.js";
import { get, extractAtomUriFromConnectionUri } from "../../utils.js";

import * as atomUtils from "../../redux/utils/atom-utils.js";
import * as connectionUtils from "../../redux/utils/connection-utils.js";
import * as wonLabelUtils from "../../won-label-utils.js";
import WonSocketAddAtom from "../socket-add-atom.jsx";
import WonSocketAddButton from "../socket-add-button.jsx";

import "~/style/_atom-content-tag-socket.scss";
import * as generalSelectors from "../../redux/selectors/general-selectors";
import WonGenericItem from "~/app/components/socket-items/generic-item";

export default function WonAtomContentTagSocket({
  atom,
  socketType,
  relevantConnections,
  showAddPicker,
  toggleAddPicker,
  addButtonClassName,
  segmentContentClassName,
  setVisibleTab,
  storedAtoms,
  isOwned,
}) {
  const accountState = useSelector(generalSelectors.getAccountState);
  const currentLocation = useSelector(generalSelectors.getCurrentLocation);

  const socketUri = atomUtils.getSocketUri(atom, socketType);
  const reactions = atomUtils.getReactions(atom, socketType);

  const connections = isOwned
    ? relevantConnections
    : relevantConnections.filter(connectionUtils.isConnected);

  // If an atom is owned we display all connStates, if the atom is not owned we only display connected states
  const activeConnections = connections.filter(conn =>
    connectionUtils.isConnected(conn)
  );

  function generateConnectionItems(connections) {
    const connectionElements = [];

    connections &&
      connections
        .toOrderedMap()
        .sortBy(conn => {
          const lastUpdateDate = connectionUtils.getLastUpdateDate(conn);
          return lastUpdateDate && lastUpdateDate.getTime();
        })
        .reverse()
        .map((conn, connUri) => {
          const flip = connectionUtils.hasTargetSocketUri(conn, socketUri);
          connectionElements.push(
            <WonGenericItem
              key={connUri}
              connection={conn}
              currentLocation={currentLocation}
              atom={
                flip
                  ? get(storedAtoms, extractAtomUriFromConnectionUri(connUri))
                  : atom
              }
              targetAtom={get(
                storedAtoms,
                connectionUtils.getTargetAtomUri(conn)
              )}
              isOwned={isOwned}
              flip={flip}
            />
          );
        });

    return connectionElements;
  }

  return (
    <won-atom-content-tag-socket>
      {!showAddPicker ? (
        activeConnections.size > 0 || reactions ? (
          <div
            className={`actsocket__content ${segmentContentClassName || ""}`}
          >
            {atomUtils.isActive(atom) &&
            reactions &&
            ((isOwned || !vocab.refuseAddToNonOwned[socketType]) &&
              (!vocab.socketCapacity[socketType] ||
                relevantConnections.filter(connectionUtils.isConnected).size <
                  vocab.socketCapacity[socketType])) ? (
              <WonSocketAddButton
                senderReactions={reactions}
                isAtomOwned={isOwned}
                onClick={() => toggleAddPicker(!showAddPicker)}
                targetSocketType={socketType}
                className={addButtonClassName}
              />
            ) : (
              undefined
            )}
            {generateConnectionItems(activeConnections)}
          </div>
        ) : (
          <div
            className={`actsocket__content ${segmentContentClassName || ""}`}
          >
            <div className="actsocket__empty">
              {`No ${wonLabelUtils.getSocketItemsLabel(socketType)}`}
            </div>
          </div>
        )
      ) : (
        undefined
      )}
      {showAddPicker ? (
        <WonSocketAddAtom
          addToAtom={atom}
          storedAtoms={storedAtoms}
          addToSocketType={socketType}
          reactions={reactions}
          accountState={accountState}
          onClose={() => {
            if (vocab.socketCapacity[socketType] === 1) {
              setVisibleTab("DETAIL");
            }
            toggleAddPicker(!showAddPicker);
          }}
        />
      ) : (
        undefined
      )}
    </won-atom-content-tag-socket>
  );
}

WonAtomContentTagSocket.propTypes = {
  atom: PropTypes.object.isRequired,
  isOwned: PropTypes.bool.isRequired,
  relevantConnections: PropTypes.object.isRequired,
  socketType: PropTypes.string.isRequired,
  showAddPicker: PropTypes.bool.isRequired,
  toggleAddPicker: PropTypes.func.isRequired,
  addButtonClassName: PropTypes.string,
  segmentContentClassName: PropTypes.string,
  setVisibleTab: PropTypes.func.isRequired,
  storedAtoms: PropTypes.object.isRequired,
};
