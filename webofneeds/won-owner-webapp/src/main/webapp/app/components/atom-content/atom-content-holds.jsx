/**
 * Created by quasarchimaere on 05.08.2019.
 */
import React, { useState } from "react";
import {
  get,
  generateLink,
  filterConnectionsBySearchValue,
  sortByDate,
} from "../../utils.js";
import { useSelector } from "react-redux";
import { Link } from "react-router-dom";
import * as atomUtils from "../../redux/utils/atom-utils.js";
import * as generalSelectors from "../../redux/selectors/general-selectors.js";
import WonHeldItem from "../socket-items/held-item.jsx";
import WonTitlePicker from "../details/picker/title-picker.jsx";

import "~/style/_atom-content-holds.scss";
import ico36_plus from "~/images/won-icons/ico36_plus.svg";
import PropTypes from "prop-types";
import vocab from "../../service/vocab";

export default function WonAtomContentHolds({ atom }) {
  const atomUri = get(atom, "uri");
  const isOwned = useSelector(generalSelectors.isAtomOwned(atomUri));
  const currentLocation = useSelector(generalSelectors.getCurrentLocation);

  const [searchText, setSearchText] = useState({ value: "" });

  const storedAtoms = useSelector(generalSelectors.getAtoms);

  const connections = filterConnectionsBySearchValue(
    atomUtils.getConnectedConnections(atom, vocab.HOLD.HolderSocketCompacted),
    storedAtoms,
    searchText
  );
  const connectionsArray = sortByDate(connections) || [];

  const atomCards = connectionsArray.map(conn => {
    return (
      <WonHeldItem
        key={get(conn, "uri")}
        atom={atom}
        connection={conn}
        targetAtom={get(storedAtoms, get(conn, "targetAtomUri"))}
        isOwned={isOwned}
        currentLocation={currentLocation}
      />
    );
  });

  return (
    <won-atom-content-holds>
      {atomCards || searchText.value.trim().length > 0 ? (
        <div className="ach__search">
          <WonTitlePicker
            onUpdate={setSearchText}
            initialValue={searchText.value}
            detail={{ placeholder: "Filter Atoms" }}
          />
        </div>
      ) : (
        undefined
      )}
      {atomCards}
      {isOwned && atomUtils.isActive(atom) ? (
        <Link
          className="ach__createatom"
          to={location =>
            generateLink(
              location,
              {
                senderSocketType: vocab.HOLD.HoldableSocketCompacted,
                targetSocketType: vocab.HOLD.HolderSocketCompacted,
                fromAtomUri: atomUri,
                mode: "CONNECT",
              },
              "/create",
              false
            )
          }
        >
          <svg className="ach__createatom__icon" title="Create a new post">
            <use xlinkHref={ico36_plus} href={ico36_plus} />
          </svg>
          <span className="ach__createatom__label">New</span>
        </Link>
      ) : connectionsArray.length === 0 ? (
        <div className="ach__empty">
          {searchText.value.trim().length > 0
            ? "No Results"
            : "Not one single Atom present."}
        </div>
      ) : (
        undefined
      )}
    </won-atom-content-holds>
  );
}

WonAtomContentHolds.propTypes = {
  atom: PropTypes.object.isRequired,
};
