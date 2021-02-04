/**
 * Created by quasarchimaere on 06.05.2020.
 */

import React, { useState } from "react";
import PropTypes from "prop-types";
import { useSelector } from "react-redux";
import vocab from "../../service/vocab.js";
import { get, extractAtomUriFromConnectionUri } from "../../utils.js";

import * as atomUtils from "../../redux/utils/atom-utils.js";
import * as connectionUtils from "../../redux/utils/connection-utils.js";
import * as wonLabelUtils from "../../won-label-utils.js";
import WonSocketAddAtom from "../socket-add-atom.jsx";
import WonSocketAddButton from "../socket-add-button.jsx";
import WonTagItem from "../socket-items/tag-item.jsx";

import "~/style/_atom-content-tag-socket.scss";
import * as generalSelectors from "../../redux/selectors/general-selectors";

export default function WonAtomContentTagSocket({
  atom,
  socketType,
  relevantConnections,
  segmentContentClassName,
  storedAtoms,
  isOwned,
}) {
  const [showAddPicker, toggleAddPicker] = useState(false);
  const accountState = useSelector(generalSelectors.getAccountState);
  const currentLocation = useSelector(generalSelectors.getCurrentLocation);

  const socketUri = atomUtils.getSocketUri(atom, socketType);
  const reactions = atomUtils.getReactions(atom, socketType);

  // If an atom is owned we display all connStates, if the atom is not owned we only display connected states
  const connections = isOwned
    ? relevantConnections
    : relevantConnections.filter(connectionUtils.isConnected);

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
            <WonTagItem
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
        connections.size > 0 || reactions ? (
          <div
            className={`actsocket__content ${segmentContentClassName || ""}`}
          >
            {generateConnectionItems(connections)}
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
                className="won-socket-add-button--tag"
              />
            ) : (
              undefined
            )}
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
  segmentContentClassName: PropTypes.string,
  storedAtoms: PropTypes.object.isRequired,
};
