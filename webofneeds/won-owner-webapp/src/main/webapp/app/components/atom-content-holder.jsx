import React from "react";
import PropTypes from "prop-types";
import Immutable from "immutable";
import { actionCreators } from "../actions/actions.js";
import { useSelector, useDispatch } from "react-redux";
import { get, getIn, getQueryParams, generateLink } from "../utils.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as processUtils from "../redux/utils/process-utils.js";
import { Elm } from "../../elm/RatingView.elm";

import WonAtomIcon from "./atom-icon.jsx";
import WonDescriptionViewer from "./details/viewer/description-viewer.jsx";
import { details } from "../../config/detail-definitions.js";
import "~/style/_atom-content-holder.scss";
import ElmReact from "./elm-react";
import { useHistory } from "react-router-dom";
import vocab from "../service/vocab.js";

export default function WonAtomContentHolder({ holdsUri }) {
  const history = useHistory();
  const dispatch = useDispatch();
  const { connectionUri } = getQueryParams(history.location);
  const connection = useSelector(state =>
    generalSelectors.getOwnedConnectionByUri(state, connectionUri)
  );
  const ownAtom = useSelector(state =>
    generalSelectors.getOwnedAtomByConnectionUri(state, connectionUri)
  );

  const ratingConnectionUri =
    get(connection, "targetAtomUri") == holdsUri && atomUtils.isHeld(ownAtom)
      ? connectionUri
      : null;

  const heldAtom = useSelector(
    state => holdsUri && getIn(state, ["atoms", holdsUri])
  );
  const holderUri = atomUtils.getHeldByUri(heldAtom);
  const holderAtom = useSelector(
    state => (heldAtom ? getIn(state, ["atoms", holderUri]) : undefined)
  );

  const holderHasHolderSocket = atomUtils.hasHolderSocket(holderAtom);
  const holderHolds = holderHasHolderSocket && get(holderAtom, "holds");
  const holderVerified = holderHolds && holderHolds.includes(holdsUri);

  const holderHasReviewSocket = atomUtils.hasReviewSocket(holderAtom);
  const aggregateRating =
    holderHasReviewSocket && getIn(holderAtom, ["rating", "aggregateRating"]);

  const holderHasBuddySocket = atomUtils.hasBuddySocket(holderAtom);
  const holderBuddies = holderHasBuddySocket && get(holderAtom, "buddies");

  const process = useSelector(generalSelectors.getProcessState);
  const postIsOwned = useSelector(state =>
    generalSelectors.isAtomOwned(state, holdsUri)
  );
  const holderLoading =
    !holderAtom || processUtils.isAtomLoading(process, holderUri);
  const holderName = getIn(holderAtom, ["content", "personaName"]);
  const holderDescription = getIn(holderAtom, ["content", "description"]);
  const holderWebsite = getIn(holderAtom, ["content", "website"]);
  const holderHoldsSize = holderHolds ? holderHolds.size : 0;
  const holderBuddySize = holderBuddies ? holderBuddies.size : 0;
  const reviewCount =
    holderHasReviewSocket && getIn(holderAtom, ["rating", "reviewCount"]);
  const aggregateRatingString = aggregateRating && aggregateRating.toFixed(1);
  const aggregateRatingRounded = aggregateRating
    ? Math.round(aggregateRating)
    : 0;

  function removePersona() {
    const payload = {
      caption: "Attention!",
      text: "Do you want to remove the Persona from this Atom?",
      buttons: [
        {
          caption: "Yes",
          callback: () => {
            dispatch(actionCreators.personas__disconnect(holdsUri, holderUri));
            dispatch(actionCreators.view__hideModalDialog());
          },
        },
        {
          caption: "No",
          callback: () => {
            dispatch(actionCreators.view__hideModalDialog());
          },
        },
      ],
    };
    dispatch(actionCreators.view__showModalDialog(payload));
  }

  function viewPersonaPosts() {
    dispatch(
      actionCreators.atoms__selectTab(
        Immutable.fromJS({
          atomUri: holderUri,
          selectTab: vocab.HOLD.HolderSocketCompacted,
        })
      )
    );
    history.push(
      generateLink(
        history.location,
        {
          postUri: holderUri,
        },
        "/post"
      )
    );
  }

  function viewPersonaBuddies() {
    dispatch(
      actionCreators.atoms__selectTab(
        Immutable.fromJS({
          atomUri: holderUri,
          selectTab: vocab.BUDDY.BuddySocketCompacted,
        })
      )
    );
    history.push(
      generateLink(
        history.location,
        {
          postUri: holderUri,
        },
        "/post"
      )
    );
  }

  function viewPersonaReviews() {
    dispatch(
      actionCreators.atoms__selectTab(
        Immutable.fromJS({
          atomUri: holderUri,
          selectTab: vocab.REVIEW.ReviewSocketCompacted,
        })
      )
    );
    history.push(
      generateLink(
        history.location,
        {
          postUri: holderUri,
        },
        "/post"
      )
    );
  }

  let holderNameElement;
  if (holderName) {
    const verificationElement = holderVerified ? (
      <span
        className="ac-holder__header__name__verification ac-holder__header__name__verification--verified"
        title="The Persona-Relation of this Post is verified by the Persona"
      >
        Verified
      </span>
    ) : (
      <span
        className="ac-holder__header__name__verification ac-holder__header__name__verification--unverified"
        title="The Persona-Relation of this Post is NOT verified by the Persona"
      >
        Unverified!
      </span>
    );

    holderNameElement = (
      <div className="ac-holder__header__name">
        <span className="ac-holder__header__name__label">{holderName}</span>
        {verificationElement}
      </div>
    );
  }

  const websiteFragment = holderWebsite && (
    <React.Fragment>
      <div className="ac-holder__websitelabel">Website:</div>
      <a
        className="ac-holder__websitelink"
        target="_blank"
        rel="noopener noreferrer"
        href={holderWebsite}
      >
        {holderWebsite}
      </a>
    </React.Fragment>
  );

  return (
    <won-atom-content-holder>
      <div className="ac-holder__header">
        {!holderLoading && <WonAtomIcon atom={holderAtom} />}
        {holderNameElement}
        {websiteFragment}
      </div>

      {holderHasReviewSocket && (
        <div className="ac-holder__rating">
          <div className="ac-holder__rating__label">
            <span className="ac-holder__rating__label__title">Rating</span>
            {aggregateRatingString && (
              <span className="ac-holder__rating__label__aggregate">
                (★ {aggregateRatingString})
              </span>
            )}
          </div>
          <ElmReact
            src={Elm.RatingView}
            flags={{
              rating: aggregateRatingRounded,
              connectionUri: ratingConnectionUri,
            }}
          />
          {reviewCount && (
            <React.Fragment>
              <div className="ac-holder__rating__reviewcount">
                {reviewCount} Reviews
              </div>
              <button
                className="ac-holder__rating__view won-button--filled red"
                onClick={() => viewPersonaReviews()}
              >
                View
              </button>
            </React.Fragment>
          )}
        </div>
      )}

      {holderHasHolderSocket && (
        <div className="ac-holder__holds">
          <div className="ac-holder__holds__label">
            {"Holder of " +
              holderHoldsSize +
              " Post" +
              (holderHoldsSize === 1 ? "" : "s")}
          </div>
          <button
            className="ac-holder__holds__view won-button--filled red"
            onClick={() => viewPersonaPosts()}
          >
            View
          </button>
        </div>
      )}

      {holderHasBuddySocket && (
        <div className="ac-holder__buddies">
          <div className="ac-holder__buddies__label">
            {"Buddy of " +
              holderBuddySize +
              " Persona" +
              (holderBuddySize === 1 ? "" : "s")}
          </div>
          <button
            className="ac-holder__buddies__view won-button--filled red"
            onClick={() => viewPersonaBuddies()}
          >
            View
          </button>
        </div>
      )}

      {details.description &&
        holderDescription && (
          <WonDescriptionViewer
            detail={details.description}
            content={holderDescription}
          />
        )}

      {postIsOwned && (
        <button
          className="won-button--filled red"
          onClick={() => removePersona()}
        >
          Remove Persona
        </button>
      )}
    </won-atom-content-holder>
  );
}
WonAtomContentHolder.propTypes = {
  holdsUri: PropTypes.string.isRequired,
};
