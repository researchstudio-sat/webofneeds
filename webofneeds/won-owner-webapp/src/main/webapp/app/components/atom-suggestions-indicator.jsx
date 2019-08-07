/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import {getIn} from "../utils.js";
import {actionCreators} from "../actions/actions.js";

import "~/style/_atom-suggestions-indicator.scss";
import Immutable from "immutable";
import * as atomUtils from "../redux/utils/atom-utils";

export default class WonAtomSuggestionsIndicator extends React.Component {
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
    const suggestedConnections = atomUtils.getSuggestedConnections(atom);

    const suggestionsCount = suggestedConnections
      ? suggestedConnections.size
      : 0;
    const unreadSuggestions =
      suggestedConnections &&
      suggestedConnections.filter(conn => conn.get("unread"));
    const unreadSuggestionsCount = unreadSuggestions
      ? unreadSuggestions.size
      : 0;

    return {
      suggestionsCount,
      unreadSuggestionsCount,
      hasSuggestions: suggestionsCount > 0,
      hasUnreadSuggestions: unreadSuggestionsCount > 0,
    };
  }

  showAtomSuggestions() {
    this.props.ngRedux.dispatch(actionCreators.atoms__selectTab(
      Immutable.fromJS({ atomUri: this.atomUri, selectTab: "SUGGESTIONS" })
    ));
    this.props.ngRedux.dispatch(actionCreators.router__stateGo("post", { postUri: this.atomUri }));
  }

  render() {
    if (!this.state) {
      console.debug("render with null state");
      return <div/>;
    }
    return (
      <won-atom-suggestions-indicator class={(!this.state.hasSuggestions ? "won-no-suggestions" : "")} onClick={() => this.showAtomSuggestions()}>
        <svg className={"asi__icon " + (this.state.hasUnreadSuggestions ? "asi__icon--unreads" : "asi__icon--reads")}>
          <use xlinkHref="#ico36_match" href="#ico36_match"></use>
        </svg>
        <div className="asi__right">
          <div className="asi__right__topline">
            <div className="asi__right__topline__title">
              Suggestions
            </div>
          </div>
          <div className="asi__right__subtitle">
            <div className="asi__right__subtitle__label">
              <span>{this.state.suggestionsCount + " Suggestions"}</span>
              { this.state.hasUnreadSuggestions
                  ? <span>{", " + this.state.unreadSuggestionsCount + " new"}</span>
                  : null
              }
            </div>
          </div>
        </div>
      </won-atom-suggestions-indicator>
    );
  }
}