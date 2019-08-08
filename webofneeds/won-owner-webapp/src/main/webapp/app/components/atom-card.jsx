/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import { get, getIn } from "../utils.js";
import { actionCreators } from "../actions/actions.js";

import "~/style/_atom-card.scss";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as processUtils from "../redux/utils/process-utils.js";
import WonOtherCard from "./cards/other-card.jsx";
import WonSkeletonCard from "./cards/skeleton-card.jsx";
import WonPersonaCard from "./cards/persona-card.jsx";
import PropTypes from "prop-types";

export default class WonAtomCard extends React.Component {
  static propTypes = {
    atomUri: PropTypes.string.isRequired,
    showPersona: PropTypes.bool,
    showSuggestions: PropTypes.bool,
    currentLocation: PropTypes.object,
    onAtomClick: PropTypes.func,
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
    const isPersona = atomUtils.isPersona(atom);
    const process = get(state, "process");
    const isSkeleton =
      !(
        processUtils.isAtomLoaded(process, this.atomUri) &&
        !get(atom, "isBeingCreated")
      ) ||
      get(atom, "isBeingCreated") ||
      processUtils.hasAtomFailedToLoad(process, this.atomUri) ||
      processUtils.isAtomLoading(process, this.atomUri) ||
      processUtils.isAtomToLoad(process, this.atomUri);

    if (isSkeleton) {
      return {
        isPersona: false,
        isOtherAtom: false,
        isSkeleton: true,
      };
    } else if (isPersona) {
      return {
        isPersona: true,
        isOtherAtom: false,
        isSkeleton: false,
      };
    } else {
      return {
        isPersona: false,
        isOtherAtom: true,
        isSkeleton: false,
      };
    }
  }

  render() {
    if (!this.state) {
      console.debug("render with null state");
      return <div />;
    }

    if (this.state.isSkeleton) {
      return (
        <won-atom-card>
          <WonSkeletonCard
            atomUri={this.atomUri}
            showSuggestions={this.props.showSuggestions}
            showPersona={this.props.showPersona}
            ngRedux={this.props.ngRedux}
          />
        </won-atom-card>
      );
    } else if (this.state.isPersona) {
      return (
        <won-atom-card>
          <WonPersonaCard
            atomUri={this.atomUri}
            onAtomClick={this.props.onAtomClick}
            ngRedux={this.props.ngRedux}
          />
        </won-atom-card>
      );
    } else {
      return (
        <won-atom-card>
          <WonOtherCard
            atomUri={this.atomUri}
            showSuggestions={this.props.showSuggestions}
            showPersona={this.props.showPersona}
            onAtomClick={this.props.onAtomClick}
            currentLocation={this.props.currentLocation}
            ngRedux={this.props.ngRedux}
          />
        </won-atom-card>
      );
    }
  }
}
