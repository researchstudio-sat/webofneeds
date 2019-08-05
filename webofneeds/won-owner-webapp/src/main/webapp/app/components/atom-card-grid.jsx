/**
 * Created by fsuda on 21.08.2017.
 */

import React from "react";
import WonAtomCard from "./atom-card.jsx";

export default class WonAtomCardGrid extends React.Component {

  render() {
    if(this.props && this.props.atomUris && this.props.atomUris.length > 0) {
      const atomUris = this.props.atomUris;
      const showPersona = this.props.showPersona;
      const showSuggestions = this.props.showSuggestions;
      const currentLocation = this.props.currentLocation;
      const ngRedux = this.props && this.props.ngRedux;

      console.debug(
        "Render WonAtomCard-Grid for: \n",
        "atomUris: ", atomUris, "\n",
        "showPersona: ", showPersona, "\n",
        "showSuggestions: ", showSuggestions, "\n",
        "currentLocation: ", currentLocation
      );

      return atomUris.map(atomUri => {
        return (
          <WonAtomCard key={atomUri} atomUri={atomUri} showPersona={showPersona} showSuggestions={showSuggestions} currentLocation={currentLocation} ngRedux={ngRedux} />
        );
      });
    }
    return undefined;
  }
}