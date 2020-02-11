/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import VisibilitySensor from "react-visibility-sensor";
import { get, getIn } from "../../utils.js";
import { actionCreators } from "../../actions/actions.js";
import { connect } from "react-redux";

import "~/style/_skeleton-card.scss";
import * as processUtils from "../../redux/utils/process-utils.js";
import WonAtomSuggestionsIndicator from "../atom-suggestions-indicator.jsx";
import PropTypes from "prop-types";

const mapStateToProps = (state, ownProps) => {
  const atom = getIn(state, ["atoms", ownProps.atomUri]);
  const process = get(state, "process");

  const atomInCreation = get(atom, "isBeingCreated");
  const atomLoaded =
    processUtils.isAtomLoaded(process, ownProps.atomUri) && !atomInCreation;
  const atomLoading = processUtils.isAtomLoading(process, ownProps.atomUri);
  const atomToLoad = processUtils.isAtomToLoad(process, ownProps.atomUri);
  const atomFailedToLoad = processUtils.hasAtomFailedToLoad(
    process,
    ownProps.atomUri
  );

  return {
    atomUri: ownProps.atomUri,
    atomLoaded,
    atomLoading,
    atomInCreation,
    atomToLoad,
    atomFailedToLoad,
  };
};

const mapDispatchToProps = dispatch => {
  return {
    fetchAtom: uri => {
      dispatch(actionCreators.atoms__fetchUnloadedAtom(uri));
    },
  };
};

class WonSkeletonCard extends React.Component {
  render() {
    const cardIconSkeleton = !this.props.atomLoaded ? (
      <VisibilitySensor
        onChange={isVisible => {
          this.onChange(isVisible);
        }}
        intervalDelay={200}
        partialVisibility={true}
        offset={{ top: -300, bottom: -300 }}
      >
        <div className="card__icon__skeleton" />
      </VisibilitySensor>
    ) : (
      undefined
    );

    const cardMainFailed = this.props.atomFailedToLoad ? (
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
    ) : (
      undefined
    );

    const cardMain =
      this.props.atomLoading ||
      this.props.atomToLoad ||
      this.props.atomInCreation ? (
        <div className="card__main">
          <div className="card__main__topline">
            <div className="card__main__topline__title" />
          </div>
          <div className="card__main__subtitle">
            <span className="card__main__subtitle__type" />
          </div>
        </div>
      ) : (
        undefined
      );

    const cardPersona =
      this.props.showHolder && !this.props.atomLoaded ? (
        <div className="card__nopersona" />
      ) : (
        undefined
      );

    const cardSuggestions = this.props.showSuggestions ? (
      <WonAtomSuggestionsIndicator atomUri={this.props.atomUri} />
    ) : (
      undefined
    );

    return (
      <won-skeleton-card
        class={
          (this.props.atomLoading || this.props.atomInCreation
            ? " won-is-loading "
            : "") + (this.props.atomToLoad ? "won-is-toload" : "")
        }
      >
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
      this.props &&
      this.props.atomUri &&
      !this.props.atomLoaded &&
      !this.props.atomLoading &&
      !this.props.atomInCreation &&
      this.props.atomToLoad
    ) {
      this.props.fetchAtom(this.props.atomUri);
    }
  }

  onChange(isVisible) {
    if (isVisible) {
      this.ensureAtomIsLoaded();
    }
  }
}
WonSkeletonCard.propTypes = {
  atomUri: PropTypes.string.isRequired,
  showHolder: PropTypes.bool,
  showSuggestions: PropTypes.bool,
  atomLoaded: PropTypes.bool,
  atomLoading: PropTypes.bool,
  atomInCreation: PropTypes.bool,
  atomToLoad: PropTypes.bool,
  atomFailedToLoad: PropTypes.bool,
  fetchAtom: PropTypes.func,
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(WonSkeletonCard);
