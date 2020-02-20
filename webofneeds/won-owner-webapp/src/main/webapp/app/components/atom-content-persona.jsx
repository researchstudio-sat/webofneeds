import React from "react";
import PropTypes from "prop-types";
import Immutable from "immutable";
import { actionCreators } from "../actions/actions.js";
import { connect } from "react-redux";
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

const mapStateToProps = (state, ownProps) => {
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
    get(connection, "targetAtomUri") == ownProps.holdsUri &&
    atomUtils.isHeld(ownAtom)
      ? connectionUri
      : null;

  const post = ownProps.holdsUri && getIn(state, ["atoms", ownProps.holdsUri]);
  const personaUri = atomUtils.getHeldByUri(post);
  const persona = post ? getIn(state, ["atoms", personaUri]) : undefined;

  const personaHasHolderSocket = atomUtils.hasHolderSocket(persona);
  const personaHolds = personaHasHolderSocket && get(persona, "holds");
  const personaVerified =
    personaHolds && personaHolds.includes(ownProps.holdsUri);

  const personaHasReviewSocket = atomUtils.hasReviewSocket(persona);
  const aggregateRating =
    personaHasReviewSocket && getIn(persona, ["rating", "aggregateRating"]);

  const personaHasBuddySocket = atomUtils.hasBuddySocket(persona);
  const personaBuddies = personaHasBuddySocket && get(persona, "buddies");

  const process = get(state, "process");

  return {
    holdsUri: ownProps.holdsUri,
    post,
    personaUri,
    postIsOwned: generalSelectors.isAtomOwned(state, ownProps.holdsUri),
    postHasHoldableSocket: atomUtils.hasHoldableSocket(post),
    personaLoading: !persona || processUtils.isAtomLoading(process, personaUri),
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
};

const mapDispatchToProps = dispatch => {
  return {
    routerGo: (path, props) => {
      dispatch(actionCreators.router__stateGo(path, props));
    },
    selectTab: (atomUri, tab) => {
      dispatch(
        actionCreators.atoms__selectTab(
          Immutable.fromJS({ atomUri: atomUri, selectTab: tab })
        )
      );
    },
    hideModalDialog: () => {
      dispatch(actionCreators.view__hideModalDialog());
    },
    showModalDialog: payload => {
      dispatch(actionCreators.view__showModalDialog(payload));
    },
    personaDisconnect: (holdsUri, holderUri) => {
      dispatch(actionCreators.personas__disconnect(holdsUri, holderUri));
    },
  };
};

class WonAtomContentPersona extends React.Component {
  render() {
    let personaNameElement;
    if (this.props.personaName) {
      const verificationElement = this.props.personaVerified ? (
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
            {this.props.personaName}
          </span>
          {verificationElement}
        </div>
      );
    }

    const websiteFragment = this.props.personaWebsite && (
      <React.Fragment>
        <div className="ac-persona__websitelabel">Website:</div>
        <a
          className="ac-persona__websitelink"
          target="_blank"
          rel="noopener noreferrer"
          href={this.props.personaWebsite}
        >
          {this.props.personaWebsite}
        </a>
      </React.Fragment>
    );

    return (
      <won-atom-content-persona>
        <div className="ac-persona__header">
          {!this.props.personaLoading && (
            <WonAtomIcon atomUri={this.props.personaUri} />
          )}
          {personaNameElement}
          {websiteFragment}
        </div>

        {this.props.personaHasReviewSocket && (
          <div className="ac-persona__rating">
            <div className="ac-persona__rating__label">
              <span className="ac-persona__rating__label__title">Rating</span>
              {this.props.aggregateRatingString && (
                <span className="ac-persona__rating__label__aggregate">
                  (★ {this.props.aggregateRatingString})
                </span>
              )}
            </div>
            <ElmReact
              src={Elm.RatingView}
              flags={{
                rating: this.props.aggregateRatingRounded,
                connectionUri: this.props.ratingConnectionUri,
              }}
            />
            {this.props.reviewCount && (
              <React.Fragment>
                <div className="ac-persona__rating__reviewcount">
                  {this.props.reviewCount} Reviews
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

        {this.props.personaHasHolderSocket && (
          <div className="ac-persona__holds">
            <div className="ac-persona__holds__label">
              {"Holder of " +
                this.props.personaHoldsSize +
                " Post" +
                (this.props.personaHoldsSize === 1 ? "" : "s")}
            </div>
            <button
              className="ac-persona__holds__view won-button--filled red"
              onClick={() => this.viewPersonaPosts()}
            >
              View
            </button>
          </div>
        )}

        {this.props.personaHasBuddySocket && (
          <div className="ac-persona__buddies">
            <div className="ac-persona__buddies__label">
              {"Buddy of " +
                this.props.personaBuddySize +
                " Persona" +
                (this.props.personaBuddySize === 1 ? "" : "s")}
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
          this.props.personaDescription && (
            <WonDescriptionViewer
              detail={details.description}
              content={this.props.personaDescription}
            />
          )}

        {this.props.postIsOwned && (
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
            this.props.personaDisconnect(
              this.props.holdsUri,
              this.props.personaUri
            );
            this.props.hideModalDialog();
          },
        },
        {
          caption: "No",
          callback: () => {
            this.props.hideModalDialog();
          },
        },
      ],
    };
    this.props.showModalDialog(payload);
  }

  viewPersonaPosts() {
    this.props.selectTab(this.props.personaUri, "HOLDS");
    this.props.routerGo("post", {
      postUri: this.props.personaUri,
    });
  }

  viewPersonaBuddies() {
    this.props.selectTab(this.props.personaUri, "BUDDIES");
    this.props.routerGo("post", {
      postUri: this.props.personaUri,
    });
  }

  viewPersonaReviews() {
    this.props.selectTab(this.props.personaUri, "REVIEWS");
    this.props.routerGo("post", {
      postUri: this.props.personaUri,
    });
  }
}
WonAtomContentPersona.propTypes = {
  holdsUri: PropTypes.string.isRequired,
  post: PropTypes.object,
  personaUri: PropTypes.string,
  postIsOwned: PropTypes.bool,
  postHasHoldableSocket: PropTypes.bool,
  personaLoading: PropTypes.bool,
  personaFailedToLoad: PropTypes.bool,
  personaName: PropTypes.string,
  personaDescription: PropTypes.string,
  personaWebsite: PropTypes.string,
  personaVerified: PropTypes.bool,
  personaHoldsSize: PropTypes.number,
  personaBuddySize: PropTypes.number,
  personaHasReviewSocket: PropTypes.bool,
  personaHasHolderSocket: PropTypes.bool,
  personaHasBuddySocket: PropTypes.bool,
  reviewCount: PropTypes.number,
  aggregateRatingString: PropTypes.string,
  aggregateRatingRounded: PropTypes.number,
  ratingConnectionUri: PropTypes.string,
  selectTab: PropTypes.func,
  routerGo: PropTypes.func,
  showModalDialog: PropTypes.func,
  hideModalDialog: PropTypes.func,
  personaDisconnect: PropTypes.func,
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(WonAtomContentPersona);
