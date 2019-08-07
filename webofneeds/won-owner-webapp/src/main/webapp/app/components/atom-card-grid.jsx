/**
 * Created by fsuda on 21.08.2017.
 */

import React from "react";
import WonAtomCard from "./atom-card.jsx";
import {actionCreators} from "../actions/actions.js";

export default class WonAtomCardGrid extends React.Component {

  render() {
    if(this.props && this.props.atomUris && this.props.atomUris.length > 0) {
      const atomUris = this.props.atomUris;
      const showPersona = this.props.showPersona;
      const showSuggestions = this.props.showSuggestions;
      const showCreate = this.props.showCreate;
      const currentLocation = this.props.currentLocation;
      const ngRedux = this.props && this.props.ngRedux;

      console.debug(
        "Render WonAtomCard-Grid for: \n",
        "atomUris: ", atomUris, "\n",
        "showPersona: ", showPersona, "\n",
        "showSuggestions: ", showSuggestions, "\n",
        "showCreate: ", showCreate, "\n",
        "currentLocation: ", currentLocation
      );
      const atomCards = atomUris.map(atomUri => {
        return (
          <WonAtomCard key={atomUri} atomUri={atomUri} showPersona={showPersona} showSuggestions={showSuggestions} currentLocation={currentLocation} ngRedux={ngRedux} />
        );
      });

      const createAtom = showCreate
        ? (
          <won-create-card onClick={() => this.props.ngRedux.dispatch(actionCreators.router__stateGo('create'))}>
            <svg className="createcard__icon" title="Create a new post">
              <use xlinkHref="#ico36_plus" href="#ico36_plus" />
            </svg>
            <span className="createcard__label">New</span>
          </won-create-card>
        )
        : undefined;

      return (
        <React.Fragment>
          {atomCards}
          {createAtom}
        </React.Fragment>
      );
    }
    return undefined;
  }
}