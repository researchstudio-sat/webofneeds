/**
 * Created by fsuda on 21.08.2017.
 */

import React from "react";
import WonAtomCard from "./atom-card.jsx";
import { generateLink } from "../utils.js";
import PropTypes from "prop-types";

import ico36_plus from "~/images/won-icons/ico36_plus.svg";
import { Link } from "react-router-dom";

export default function WonAtomCardGrid({
  atoms,
  showHolder,
  showIndicators,
  showCreate,
  currentLocation,
}) {
  const atomCardElements = [];

  atoms &&
    atoms.map((atom, atomUri) => {
      atomCardElements.push(
        <WonAtomCard
          key={atomUri}
          atomUri={atomUri}
          atom={atom}
          showHolder={showHolder}
          showIndicators={showIndicators}
          currentLocation={currentLocation}
        />
      );
    });

  const createAtom = showCreate ? (
    <Link
      className="won-create-card"
      to={location => generateLink(location, {}, "/create", false)}
    >
      <svg className="createcard__icon" title="Create a new post">
        <use xlinkHref={ico36_plus} href={ico36_plus} />
      </svg>
      <span className="createcard__label">New</span>
    </Link>
  ) : (
    undefined
  );

  return (
    <React.Fragment>
      {createAtom}
      {atomCardElements}
    </React.Fragment>
  );
}
WonAtomCardGrid.propTypes = {
  atoms: PropTypes.object,
  showHolder: PropTypes.bool,
  showIndicators: PropTypes.bool,
  showCreate: PropTypes.bool,
  currentLocation: PropTypes.object,
};
