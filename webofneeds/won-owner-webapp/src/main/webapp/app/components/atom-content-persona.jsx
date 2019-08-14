import React from "react";
import PropTypes from "prop-types";
import Immutable from "immutable";
import { actionCreators } from "../actions/actions.js";
import { get, getIn } from "../utils.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as processUtils from "../redux/utils/process-utils.js";
import * as connectionSelectors from "../redux/selectors/connection-selectors.js";
import { Elm } from "../../elm/RatingView.elm";

import WonAtomIcon from "./atom-icon.jsx";
import WonDescriptionViewer from "./details/viewer/description-viewer.jsx";
import { details } from "../../config/detail-definitions.js";
import "~/style/_atom-content-persona.scss";
import ElmReact from "./elm-react";

export default class WonAtomContentPersona extends React.Component {
  componentDidMount() {
    this.holdsUri = this.props.holdsUri;
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
    this.holdsUri = nextProps.holdsUri;
    this.setState(this.selectFromState(this.props.ngRedux.getState()));
  }

  selectFromState(state) {
    const connectionUri = generalSelectors.getConnectionUriFromRoute(state);
    const connection = connectionSelectors.getOwnedConnectionByUri(
      state,
      connectionUri
    );
    const ownAtom = generalSelectors.getOwnedAtomByConnectionUri(
      state,
      connectionUri
    );

    const ratingConnectionUri =
      get(connection, "targetAtomUri") == this.holdsUri &&
      atomUtils.isHeld(ownAtom)
        ? connectionUri
        : null;

    const post = this.holdsUri && getIn(state, ["atoms", this.holdsUri]);
    const personaUri = atomUtils.getHeldByUri(post);
    const persona = post ? getIn(state, ["atoms", personaUri]) : undefined;

    const personaHasHolderSocket = atomUtils.hasHolderSocket(persona);
    const personaHolds = personaHasHolderSocket && get(persona, "holds");
    const personaVerified =
      personaHolds && personaHolds.includes(this.holdsUri);

    const personaHasReviewSocket = atomUtils.hasReviewSocket(persona);
    const aggregateRating =
      personaHasReviewSocket && getIn(persona, ["rating", "aggregateRating"]);

    const personaHasBuddySocket = atomUtils.hasBuddySocket(persona);
    const personaBuddies = personaHasBuddySocket && get(persona, "buddies");

    const process = get(state, "process");

    return {
      post,
      personaUri,
      postIsOwned: generalSelectors.isAtomOwned(state, this.holdsUri),
      postHasHoldableSocket: atomUtils.hasHoldableSocket(post),
      personaLoading:
        !persona || processUtils.isAtomLoading(process, personaUri),
      personaFailedToLoad:
        persona && processUtils.hasAtomFailedToLoad(process, personaUri),
      personaName: getIn(persona, ["content", "personaName"]),
      personaDescription: getIn(persona, ["content", "description"]),
      personaWebsite: getIn(persona, ["content", "website"]),
      personaVerified,
      personaHoldsSize: personaHolds ? personaHolds.size : 0,
      personaBuddySize: personaBuddies ? personaBuddies.size : 0,
      personaHasReviewSocket,
      personaHasHolderSocket,
      personaHasBuddySocket,
      reviewCount:
        personaHasReviewSocket && getIn(persona, ["rating", "reviewCount"]),
      aggregateRatingString: aggregateRating && aggregateRating.toFixed(1),
      aggregateRatingRounded: aggregateRating ? Math.round(aggregateRating) : 0,
      ratingConnectionUri: ratingConnectionUri,
    };
  }

  render() {
    if (!this.state) {
      console.debug("render with null state");
      return <div />;
    }

    //TODO ELM STUFF

    let personaNameElement;
    if (this.state.personaName) {
      const verificationElement = this.state.personaVerified ? (
        <span
          className="ac-persona__header__name__verification ac-persona__header__name__verification--verified"
          title="The Persona-Relation of this Post is verified by the Persona"
        >
          Verified
        </span>
      ) : (
        <span
          className="ac-persona__header__name__verification ac-persona__header__name__verification--unverified"
          title="The Persona-Relation of this Post is NOT verified by the Persona"
        >
          Unverified!
        </span>
      );

      personaNameElement = (
        <div className="ac-persona__header__name">
          <span className="ac-persona__header__name__label">
            {this.state.personaName}
          </span>
          {verificationElement}
        </div>
      );
    }

    const websiteFragment = this.state.personaWebsite && (
      <React.Fragment>
        <div className="ac-persona__websitelabel">Website:</div>
        <a
          className="ac-persona__websitelink"
          target="_blank"
          rel="noopener noreferrer"
          href="{this.state.personaWebsite}"
        >
          {this.state.personaWebsite}
        </a>
      </React.Fragment>
    );

    return (
      <won-atom-content-persona>
        <div className="ac-persona__header">
          {!this.state.personaLoading && (
            <WonAtomIcon
              atomUri={this.state.personaUri}
              ngRedux={this.props.ngRedux}
            />
          )}
          {personaNameElement}
          {websiteFragment}
        </div>

        {this.state.personaHasReviewSocket && (
          <div className="ac-persona__rating">
            <div className="ac-persona__rating__label">
              <span className="ac-persona__rating__label__title">Rating</span>
              {this.state.aggregateRatingString && (
                <span className="ac-persona__rating__label__aggregate">
                  (★ {this.state.aggregateRatingString})
                </span>
              )}
            </div>
            <ElmReact
              src={Elm.RatingView}
              ngRedux={this.props.ngRedux}
              flags={{
                rating: this.state.aggregateRatingRounded,
                connectionUri: this.state.ratingConnectionUri,
              }}
            />
            {this.state.reviewCount && (
              <React.Fragment>
                <div className="ac-persona__rating__reviewcount">
                  {this.state.reviewCount} Reviews
                </div>
                <button
                  className="ac-persona__rating__view won-button--filled red"
                  onClick={() => this.viewPersonaReviews()}
                >
                  View
                </button>
              </React.Fragment>
            )}
          </div>
        )}

        {this.state.personaHasHolderSocket && (
          <div className="ac-persona__holds">
            <div className="ac-persona__holds__label">
              {"Holder of " +
                this.state.personaHoldsSize +
                " Post" +
                (this.state.personaHoldsSize != 0 && "s")}
            </div>
            <button
              className="ac-persona__holds__view won-button--filled red"
              onClick={() => this.viewPersonaPosts()}
            >
              View
            </button>
          </div>
        )}

        {this.state.personaHasBuddySocket && (
          <div className="ac-persona__buddies">
            <div className="ac-persona__buddies__label">
              {"Buddy of " +
                this.state.personaBuddySize +
                " Persona" +
                (this.state.personaBuddySize != 0 && "s")}
            </div>
            <button
              className="ac-persona__buddies__view won-button--filled red"
              onClick={() => this.viewPersonaBuddies()}
            >
              View
            </button>
          </div>
        )}

        {details.description &&
          this.state.personaDescription && (
            <WonDescriptionViewer
              detail={details.description}
              content={this.state.personaDescription}
            />
          )}

        {this.state.postIsOwned && (
          <button
            className="won-button--filled red"
            onClick={() => this.removePersona()}
          >
            Remove Persona
          </button>
        )}
      </won-atom-content-persona>
    );
  }

  removePersona() {
    const payload = {
      caption: "Attention!",
      text: "Do you want to remove the Persona from this Atom?",
      buttons: [
        {
          caption: "Yes",
          callback: () => {
            this.props.ngRedux.dispatch(
              actionCreators.personas__disconnect(
                this.holdsUri,
                this.state.personaUri
              )
            );
            this.props.ngRedux.dispatch(actionCreators.view__hideModalDialog());
          },
        },
        {
          caption: "No",
          callback: () => {
            this.props.ngRedux.dispatch(actionCreators.view__hideModalDialog());
          },
        },
      ],
    };
    this.props.ngRedux.dispatch(actionCreators.view__showModalDialog(payload));
  }

  viewPersonaPosts() {
    this.props.ngRedux.dispatch(
      actionCreators.atoms__selectTab(
        Immutable.fromJS({ atomUri: this.state.personaUri, selectTab: "HOLDS" })
      )
    );
    this.props.ngRedux.dispatch(
      actionCreators.router__stateGo("post", {
        postUri: this.state.personaUri,
      })
    );
  }

  viewPersonaBuddies() {
    this.props.ngRedux.dispatch(
      actionCreators.atoms__selectTab(
        Immutable.fromJS({
          atomUri: this.state.personaUri,
          selectTab: "BUDDIES",
        })
      )
    );
    this.props.ngRedux.dispatch(
      actionCreators.router__stateGo("post", {
        postUri: this.state.personaUri,
      })
    );
  }

  viewPersonaReviews() {
    this.props.ngRedux.dispatch(
      actionCreators.atoms__selectTab(
        Immutable.fromJS({
          atomUri: this.state.personaUri,
          selectTab: "REVIEWS",
        })
      )
    );
    this.props.ngRedux.dispatch(
      actionCreators.router__stateGo("post", {
        postUri: this.state.personaUri,
      })
    );
  }
}
WonAtomContentPersona.propTypes = {
  holdsUri: PropTypes.string.isRequired,
  ngRedux: PropTypes.object.isRequired,
};
