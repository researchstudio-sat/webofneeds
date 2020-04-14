/**
 * Created by quasarchimaere on 05.08.2019.
 */
import React from "react";
import { getIn, generateQueryString } from "../utils";
import { connect } from "react-redux";
import * as atomUtils from "../redux/utils/atom-utils";
import * as generalSelectors from "../redux/selectors/general-selectors";
import WonAtomCard from "./atom-card.jsx";

import "~/style/_atom-content-holds.scss";
import ico36_plus from "~/images/won-icons/ico36_plus.svg";
import PropTypes from "prop-types";
import { withRouter } from "react-router-dom";

const mapStateToProps = (state, ownProps) => {
  const atom = getIn(state, ["atoms", ownProps.atomUri]);
  const heldAtomUris = atomUtils.getHeldAtomUris(atom);

  return {
    atomUri: ownProps.atomUri,
    isOwned: generalSelectors.isAtomOwned(state, ownProps.atomUri),
    hasHeldAtoms: atomUtils.hasHeldAtoms(atom),
    heldAtomUrisArray: heldAtomUris && heldAtomUris.toArray(),
    currentLocation: generalSelectors.getCurrentLocation(state),
  };
};

class WonAtomContentHolds extends React.Component {
  createAtom() {
    this.props.history.push(
      generateQueryString("/create", { holderUri: this.props.atomUri })
    );
  }

  render() {
    if (this.props.isOwned || this.props.hasHeldAtoms) {
      const atomCards = this.props.hasHeldAtoms
        ? this.props.heldAtomUrisArray.map(atomUri => {
            return (
              <WonAtomCard
                key={atomUri}
                atomUri={atomUri}
                currentLocation={this.props.currentLocation}
                showSuggestions={this.props.isOwned}
                showHolder={false}
              />
            );
          })
        : undefined;

      const createAtom = this.props.isOwned ? (
        <div
          className="ach__createatom"
          onClick={() => {
            this.createAtom();
          }}
        >
          <svg className="ach__createatom__icon" title="Create a new post">
            <use xlinkHref={ico36_plus} href={ico36_plus} />
          </svg>
          <span className="ach__createatom__label">New</span>
        </div>
      ) : (
        undefined
      );

      return (
        <won-atom-content-holds>
          {atomCards}
          {createAtom}
        </won-atom-content-holds>
      );
    } else {
      return (
        <won-atom-content-holds>
          <div className="ach__empty">Not one single Atom present.</div>
        </won-atom-content-holds>
      );
    }
  }
}
WonAtomContentHolds.propTypes = {
  atomUri: PropTypes.string.isRequired,
  isOwned: PropTypes.bool,
  hasHeldAtoms: PropTypes.bool,
  heldAtomUrisArray: PropTypes.arrayOf(PropTypes.string),
  currentLocation: PropTypes.object,
  history: PropTypes.object,
};

export default withRouter(connect(mapStateToProps)(WonAtomContentHolds));
