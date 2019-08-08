/**
 * Created by quasarchimaere on 05.08.2019.
 */
import React from "react";
import { getIn } from "../utils";
import { actionCreators } from "../actions/actions.js";
import * as atomUtils from "../redux/utils/atom-utils";
import * as generalSelectors from "../redux/selectors/general-selectors";
import WonAtomCard from "./atom-card.jsx";

import "~/style/_atom-content-holds.scss";
import PropTypes from "prop-types";

export default class WonAtomContentHolds extends React.Component {
  static propTypes = {
    atomUri: PropTypes.string.isRequired,
    ngRedux: PropTypes.object.isRequired,
  };

  componentDidMount() {
    this.atomUri = this.props.atomUri;
    this.disconnect = this.props.ngRedux.connect(
      this.selectFromState.bind(this),
      actionCreators
    )(state => {
      this.setState(state);
    });
  }

  componentWillUnmount() {
    this.disconnect();
  }

  UNSAFE_componentWillReceiveProps(nextProps) {
    this.atomUri = nextProps.atomUri;
    this.setState(this.selectFromState(this.props.ngRedux.getState()));
  }

  selectFromState(state) {
    const atom = getIn(state, ["atoms", this.atomUri]);
    const heldAtomUris = atomUtils.getHeldAtomUris(atom);

    return {
      isOwned: generalSelectors.isAtomOwned(state, this.atomUri),
      hasHeldAtoms: atomUtils.hasHeldAtoms(atom),
      heldAtomUrisArray: heldAtomUris && heldAtomUris.toArray(),
    };
  }

  createAtom() {
    this.props.ngRedux.dispatch(
      actionCreators.router__stateGo("create", { holderUri: this.atomUri })
    );
  }

  render() {
    if (!this.state) {
      console.debug("render with null state");
      return <div />;
    }

    if (this.state.isOwned || this.state.hasHeldAtoms) {
      const atomCards = this.state.hasHeldAtoms
        ? this.state.heldAtomUrisArray.map(atomUri => {
            return (
              <WonAtomCard
                key={atomUri}
                atomUri={atomUri}
                currentLocation={this.state.currentLocation}
                showSuggestions={this.state.isOwned}
                showPersona={false}
                ngRedux={this.props.ngRedux}
              />
            );
          })
        : undefined;

      const createAtom = this.state.isOwned ? (
        <div
          className="ach__createatom"
          onClick={() => {
            this.createAtom();
          }}
        >
          <svg className="ach__createatom__icon" title="Create a new post">
            <use xlinkHref="#ico36_plus" href="#ico36_plus" />
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
