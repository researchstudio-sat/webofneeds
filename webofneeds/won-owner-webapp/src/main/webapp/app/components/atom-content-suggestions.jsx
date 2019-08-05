/**
 * Created by quasarchimaere on 05.08.2019.
 */
import React from "react";
import {actionCreators} from "../actions/actions.js";
import WonAtomCard from "./atom-card.jsx";

import "~/style/_atom-content-suggestions.scss";
import * as connectionSelectors from "../redux/selectors/connection-selectors.js";
import VisibilitySensor from "react-visibility-sensor";
import {get} from "../utils";
import won from "../won-es6";

export default class WonAtomContentSuggestions extends React.Component {
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
    const suggestions = connectionSelectors.getSuggestedConnectionsByAtomUri(
      state,
      this.atomUri
    );

    return {
      hasSuggestions: suggestions && suggestions.size > 0,
      suggestionsArray: suggestions && suggestions.toArray(),
    };
  }

  markAsRead(conn) {
    if (conn && conn.get("unread")) {
      const payload = {
        connectionUri: conn.get("uri"),
        atomUri: this.atomUri,
      };

      this.props.ngRedux.dispatch(actionCreators.connections__markAsRead(payload));
    }
  }

  viewSuggestion(conn) {
    if (!conn) {
      return;
    }

    const connUri = conn.get("uri");

    if (conn.get("unread")) {
      this.props.ngRedux.dispatch(actionCreators.connections__markAsRead({
        connectionUri: connUri,
        atomUri: this.atomUri,
      }));
    }

    this.props.ngRedux.dispatch(actionCreators.router__stateGoCurrent({
      viewConnUri: connUri,
    }));
  }

  closeConnection(conn, rateBad = false) {
    if (!conn) {
      return;
    }

    const connUri = conn.get("uri");

    if (rateBad) {
      this.props.ngRedux.dispatch(actionCreators.connections__rate(connUri, won.WONCON.binaryRatingBad));
    }

    if (conn.get("unread")) {
      this.props.ngRedux.dispatch(actionCreators.connections__markAsRead({
        connectionUri: connUri,
        atomUri: this.atomUri,
      }));
    }

    this.props.ngRedux.dispatch(actionCreators.connections__close(connUri));
  }

  sendRequest(conn, message = "") {
    if (!conn) {
      return;
    }

    const connUri = get(conn, "uri");
    const targetAtomUri = get(conn, "targetAtomUri");

    if (conn.get("unread")) {
      this.props.ngRedux.dispatch(actionCreators.connections__markAsRead({
        connectionUri: connUri,
        atomUri: this.atomUri,
      }));
    }

    this.props.ngRedux.dispatch(actionCreators.connections__rate(connUri, won.WONCON.binaryRatingGood));
    this.props.ngRedux.dispatch(actionCreators.atoms__connect(this.atomUri, connUri, targetAtomUri, message));
    this.props.ngRedux.dispatch(actionCreators.router__stateGo("connections", {
      connectionUri: connUri,
      viewConnUri: undefined,
    }));
  }

  render() {
    if (!this.state) {
      console.debug("render with null state");
      return <div/>;
    }

    if (this.state.hasSuggestions) {
      const atomCards = this.state.suggestionsArray.map(suggestion => {
        return (
          <VisibilitySensor key={suggestion.get("uri")} onChange={(isVisible) => { isVisible && suggestion.get("unread") && this.markAsRead(suggestion) }} intervalDelay={2000}>
            <div className={"acs__atom " + (suggestion.get("unread") ? "won-unread" : "")}>
              <WonAtomCard
                atomUri={suggestion.get("targetAtomUri")}
                currentLocation={this.state.currentLocation}
                showSuggestions={false}
                showPersona={true}
                ngRedux={this.props.ngRedux}
                onAtomClick={() => {this.viewSuggestion(suggestion)}}/>
              <div className="acs__atom__actions">
                <div
                  className="acs__atom__actions__button red won-button--filled"
                  onClick={() => {this.sendRequest(suggestion)}}>
                  Request
                </div>
                <div
                  className="acs__atom__actions__button red won-button--outlined thin"
                  onClick={() => {this.closeConnection(suggestion)}}>
                  Remove
                </div>
              </div>
            </div>
          </VisibilitySensor>
        );
      });

      return (
        <won-atom-content-suggestions>
          {atomCards}
        </won-atom-content-suggestions>
      );
    } else {
      <won-atom-content-suggestions>
        <div className="acs__empty">
          No Suggestions for this Atom.
        </div>
      </won-atom-content-suggestions>
    }
  }
}