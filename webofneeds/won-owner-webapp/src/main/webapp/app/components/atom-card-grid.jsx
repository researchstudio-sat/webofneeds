/**
 * Created by fsuda on 21.08.2017.
 */

import React from "react";
import WonAtomCard from "./atom-card.jsx";
import { get } from "../utils.js";
import PropTypes from "prop-types";

import ico32_buddy_add from "~/images/won-icons/ico32_buddy_add.svg";
import ico36_plus from "~/images/won-icons/ico36_plus.svg";
import { Link } from "react-router-dom";

export default function WonAtomCardGrid({
  atoms,
  showHolder,
  showIndicators,
  showCreate,
  showCreatePersona,
  currentLocation,
}) {
  const atomCards =
    atoms &&
    atoms.map(atom => {
      return (
        <WonAtomCard
          key={get(atom, "uri")}
          atom={atom}
          showHolder={showHolder}
          showIndicators={showIndicators}
          currentLocation={currentLocation}
        />
      );
    });

  const createAtom = showCreate ? (
    <Link className="won-create-card" to="/create">
      <svg className="createcard__icon" title="Create a new post">
        <use xlinkHref={ico36_plus} href={ico36_plus} />
      </svg>
      <span className="createcard__label">New</span>
    </Link>
  ) : (
    undefined
  );

  const createPersonaAtom = showCreatePersona ? (
    <Link className="won-create-card" to="/create?useCase=persona">
      <svg className="createcard__icon" title="Create a new post">
        <use xlinkHref={ico32_buddy_add} href={ico32_buddy_add} />
      </svg>
      <span className="createcard__label">New Persona</span>
    </Link>
  ) : (
    undefined
  );

  return (
    <React.Fragment>
      {atomCards}
      {createAtom}
      {createPersonaAtom}
    </React.Fragment>
  );
}
WonAtomCardGrid.propTypes = {
  atoms: PropTypes.arrayOf(PropTypes.object).isRequired,
  showHolder: PropTypes.bool,
  showIndicators: PropTypes.bool,
  showCreate: PropTypes.bool,
  showCreatePersona: PropTypes.bool,
  currentLocation: PropTypes.object,
};
