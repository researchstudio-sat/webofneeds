/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import Immutable from "immutable";
import PropTypes from "prop-types";
import { connect } from "react-redux";
import { actionCreators } from "../actions/actions.js";
import { getIn, generateLink } from "../utils.js";

import * as atomUtils from "../redux/utils/atom-utils.js";
import "~/style/_atom-suggestions-indicator.scss";
import ico36_match from "~/images/won-icons/ico36_match.svg";
import { withRouter } from "react-router-dom";
import vocab from "../service/vocab";

const mapStateToProps = (state, ownProps) => {
  const atom = getIn(state, ["atoms", ownProps.atomUri]);
  const suggestedConnections = atomUtils.getSuggestedConnections(atom);

  const suggestionsCount = suggestedConnections ? suggestedConnections.size : 0;
  const unreadSuggestions =
    suggestedConnections &&
    suggestedConnections.filter(conn => conn.get("unread"));
  const unreadSuggestionsCount = unreadSuggestions ? unreadSuggestions.size : 0;

  return {
    atomUri: ownProps.atomUri,
    suggestionsCount,
    unreadSuggestionsCount,
  };
};

const mapDispatchToProps = dispatch => {
  return {
    selectAtomTab: (atomUri, selectTab) => {
      dispatch(
        actionCreators.atoms__selectTab(
          Immutable.fromJS({
            atomUri: atomUri,
            selectTab: selectTab,
          })
        )
      );
    },
  };
};

class WonAtomSuggestionsIndicator extends React.Component {
  constructor(props) {
    super(props);
    this.showAtomSuggestions = this.showAtomSuggestions.bind(this);
  }
  showAtomSuggestions() {
    this.props.selectAtomTab(
      this.props.atomUri,
      vocab.CHAT.ChatSocketCompacted
    ); //TODO: The suggestions indicator should link to the latest type of connection (since the atomTab SUGGESTIONS is not available any longer)
    this.props.history.push(
      generateLink(
        this.props.history.location,
        { postUri: this.props.atomUri },
        "/post"
      )
    );
  }

  render() {
    return (
      <won-atom-suggestions-indicator
        class={!this.props.suggestionsCount > 0 ? "won-no-suggestions" : ""}
        onClick={this.showAtomSuggestions}
      >
        <svg
          className={
            "asi__icon " +
            (this.props.unreadSuggestionsCount > 0
              ? "asi__icon--unreads"
              : "asi__icon--reads")
          }
        >
          <use xlinkHref={ico36_match} href={ico36_match} />
        </svg>
        <div className="asi__right">
          <div className="asi__right__topline">
            <div className="asi__right__topline__title">Suggestions</div>
          </div>
          <div className="asi__right__subtitle">
            <div className="asi__right__subtitle__label">
              <span>{this.props.suggestionsCount + " Suggestions"}</span>
              {this.props.unreadSuggestionsCount > 0 ? (
                <span>{", " + this.props.unreadSuggestionsCount + " new"}</span>
              ) : null}
            </div>
          </div>
        </div>
      </won-atom-suggestions-indicator>
    );
  }
}
WonAtomSuggestionsIndicator.propTypes = {
  atomUri: PropTypes.string.isRequired,
  suggestionsCount: PropTypes.number,
  unreadSuggestionsCount: PropTypes.number,
  selectAtomTab: PropTypes.func,
  history: PropTypes.object,
};

export default withRouter(
  connect(
    mapStateToProps,
    mapDispatchToProps
  )(WonAtomSuggestionsIndicator)
);
