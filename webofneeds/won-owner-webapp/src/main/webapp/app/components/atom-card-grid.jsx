/**
 * Created by fsuda on 21.08.2017.
 */

import React from "react";
import WonAtomCard from "./atom-card.jsx";
import { actionCreators } from "../actions/actions.js";
import { connect } from "react-redux";
import PropTypes from "prop-types";

const mapDispatchToProps = dispatch => {
  return {
    routerGo: (path, props) => {
      dispatch(actionCreators.router__stateGo(path, props));
    },
  };
};

class WonAtomCardGrid extends React.Component {
  render() {
    const atomUris = this.props.atomUris;
    const showPersona = this.props.showPersona;
    const showSuggestions = this.props.showSuggestions;
    const showCreate = this.props.showCreate;
    const showCreatePersona = this.props.showCreatePersona;
    const currentLocation = this.props.currentLocation;

    let atomCards = undefined;

    if (atomUris && atomUris.length > 0) {
      atomCards = atomUris.map(atomUri => {
        return (
          <WonAtomCard
            key={atomUri}
            atomUri={atomUri}
            showPersona={showPersona}
            showSuggestions={showSuggestions}
            currentLocation={currentLocation}
          />
        );
      });
    }

    const createAtom = showCreate ? (
      <won-create-card onClick={() => this.props.routerGo("create")}>
        <svg className="createcard__icon" title="Create a new post">
          <use xlinkHref="#ico36_plus" href="#ico36_plus" />
        </svg>
        <span className="createcard__label">New</span>
      </won-create-card>
    ) : (
      undefined
    );

    const createPersonaAtom = showCreatePersona ? (
      <won-create-card
        onClick={() => this.props.routerGo("create", { useCase: "persona" })}
      >
        <svg className="createcard__icon" title="Create a new post">
          <use xlinkHref="#ico32_buddy_add" href="#ico32_buddy_add" />
        </svg>
        <span className="createcard__label">New Persona</span>
      </won-create-card>
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
}

WonAtomCardGrid.propTypes = {
  atomUris: PropTypes.arrayOf(PropTypes.string).isRequired,
  showPersona: PropTypes.bool,
  showSuggestions: PropTypes.bool,
  showCreate: PropTypes.bool,
  showCreatePersona: PropTypes.bool,
  currentLocation: PropTypes.object,
  routerGo: PropTypes.func,
};

export default connect(
  undefined,
  mapDispatchToProps
)(WonAtomCardGrid);
