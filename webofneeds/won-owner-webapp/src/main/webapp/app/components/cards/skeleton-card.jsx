/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import VisibilitySensor from "react-visibility-sensor";
import {get, getIn} from "../../utils.js";
import {actionCreators} from "../../actions/actions.js";

import "~/style/_skeleton-card.scss";
import * as processUtils from "../../redux/utils/process-utils.js";
import WonAtomSuggestionsIndicator from "../atom-suggestions-indicator.jsx";

export default class WonSkeletonCard extends React.Component {
  // TODO: Implement fetch if in view
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
    const process = get(state, "process");

    const atomInCreation = get(atom, "isBeingCreated");
    const atomLoaded =
      processUtils.isAtomLoaded(process, this.atomUri) && !atomInCreation;
    const atomLoading = processUtils.isAtomLoading(process, this.atomUri);
    const atomToLoad = processUtils.isAtomToLoad(process, this.atomUri);
    const atomFailedToLoad = processUtils.hasAtomFailedToLoad(
      process,
      this.atomUri
    );

    return {
      atomLoaded,
      atomLoading,
      atomInCreation,
      atomToLoad,
      atomFailedToLoad,
    };
  }

  render() {
    if (!this.state) {
      console.debug("render with null state");
      return <div/>;
    }

    const showSuggestions = !!(this.props && this.props.showSuggestions);
    const showPersona = !!(this.props && this.props.showPersona);

    const cardIconSkeleton = !this.state.atomLoaded
      ? <VisibilitySensor onChange={(isVisible) => { this.onChange(isVisible) }}><div className="card__icon__skeleton"/></VisibilitySensor>
      : undefined;

    const cardMainFailed = this.state.atomFailedToLoad
      ? (
        <div className="card__main">
          <div className="card__main__topline">
            <div className="card__main__topline__notitle">
              Atom Loading failed
            </div>
          </div>
          <div className="card__main__subtitle">
            <span className="card__main__subtitle__type">
              Atom might have been deleted.
            </span>
          </div>
        </div>
      )
      : undefined;

    const cardMain = (this.state.atomLoading || this.state.atomToLoad || this.state.atomInCreation)
      ? (
        <div className="card__main">
          <div className="card__main__topline">
            <div className="card__main__topline__title"></div>
          </div>
          <div className="card__main__subtitle">
            <span className="card__main__subtitle__type"></span>
          </div>
        </div>
    )
    : undefined;

    const cardPersona = (this.props.showPersona && !this.state.atomLoaded)
      ? <div className="card__nopersona"/>
      : undefined;

    const cardSuggestions = (this.props.showSuggestions)
      ? <WonAtomSuggestionsIndicator atomUri={this.atomUri} ngRedux={this.props.ngRedux}/>
      : undefined;

    return (
      <won-skeleton-card class={(this.state.atomLoading || this.state.atomInCreation ? " won-is-loading " : "") + (this.state.atomToLoad ? "won-is-toload" : "")}>
        {cardIconSkeleton}
        {cardMainFailed}
        {cardMain}
        {cardPersona}
        {cardSuggestions}
      </won-skeleton-card>
    );
  }

  ensureAtomIsLoaded() {
    if (
      this.state &&
      this.atomUri &&
      !this.state.atomLoaded &&
      !this.state.atomLoading &&
      !this.state.atomInCreation &&
      this.state.atomToLoad
    ) {
      this.props.ngRedux.dispatch(actionCreators.atoms__fetchUnloadedAtom(this.atomUri));
    }
  }

  onChange(isVisible) {
    if (isVisible) {
      this.ensureAtomIsLoaded();
    }
  }
}