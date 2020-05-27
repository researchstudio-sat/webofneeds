/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React, { useState } from "react";
import { useSelector, useDispatch } from "react-redux";
import PropTypes from "prop-types";
import { get } from "../utils.js";

import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as useCaseUtils from "../usecase-utils.js";
import * as accountUtils from "../redux/utils/account-utils.js";

import "~/style/_create-atom.scss";
import "~/style/_responsiveness-utils.scss";

import WonCreateIsSeeks from "./create-isseeks.jsx";
import WonLabelledHr from "./labelled-hr.jsx";
import { actionCreators } from "../actions/actions";
import { useHistory } from "react-router-dom";

export default function WonEditAtom({ fromAtom }) {
  const history = useHistory();
  const dispatch = useDispatch();

  const connectionHasBeenLost = !useSelector(
    generalSelectors.selectIsConnected
  );
  const accountState = useSelector(generalSelectors.getAccountState);

  const useCase = useCaseUtils.getUseCase(
    atomUtils.getMatchedUseCaseIdentifier(fromAtom) || "customUseCase"
  );

  const fromAtomContent = get(fromAtom, "content");
  const fromAtomSeeks = get(fromAtom, "seeks");

  if (fromAtomContent) {
    useCase.draft.content = fromAtomContent.toJS();
  }
  if (fromAtomSeeks) {
    useCase.draft.seeks = fromAtomSeeks.toJS();
  }

  const [draftObject, setDraftObject] = useState(
    JSON.parse(JSON.stringify(useCase.draft))
  );

  const loggedIn = accountUtils.isLoggedIn(accountState);

  const isFromAtomOwned = accountUtils.isAtomOwned(
    accountState,
    get(fromAtom, "uri")
  );
  const isFromAtomEditable = useSelector(
    state =>
      loggedIn && generalSelectors.isAtomEditable(state, get(fromAtom, "uri"))
  );

  function updateDraftSeeks(updatedDraftJson) {
    updateDraft(updatedDraftJson.draft, "seeks");
  }

  function updateDraftContent(updatedDraftJson) {
    updateDraft(updatedDraftJson.draft, "content");
  }

  function updateDraft(updatedDraft, branch) {
    const _draftObject = draftObject;
    _draftObject[branch] = updatedDraft;

    setDraftObject(_draftObject);
  }

  function save() {
    if (loggedIn && isFromAtomOwned) {
      dispatch(
        actionCreators.atoms__edit(
          useCaseUtils.getSanitizedDraftObject(draftObject, useCase),
          fromAtom,
          history.goBack
        )
      );
    }
  }

  if (useCase) {
    const headerIconElement = useCase.icon && (
      <svg className="cp__header__icon" title={useCase.label}>
        <use xlinkHref={useCase.icon} href={useCase.icon} />
      </svg>
    );
    let headerTitleElement = (
      <span className="cp__header__title">Edit Atom</span>
    );

    const createContentFragment = useCase.details &&
      Object.keys(useCase.details).length > 0 && (
        <React.Fragment>
          <div className="cp__content__branchheader">
            Your offer or self description
          </div>
          <WonCreateIsSeeks
            detailList={useCase.details}
            initialDraft={useCase.draft.content}
            onUpdate={updateDraftContent}
          />
        </React.Fragment>
      );

    const createSeeksFragment = useCase.seeksDetails &&
      Object.keys(useCase.seeksDetails).length > 0 && (
        <React.Fragment>
          <div className="cp__content__branchheader">Looking For</div>
          <WonCreateIsSeeks
            detailList={useCase.seeksDetails}
            initialDraft={useCase.draft.seeks}
            onUpdate={updateDraftSeeks}
          />
        </React.Fragment>
      );

    return (
      <won-create-atom>
        <div className="cp__header">
          {headerIconElement}
          {headerTitleElement}
        </div>
        <div className="cp__content">
          {/*ADD TITLE AND DETAILS*/}
          {createContentFragment}
          {createSeeksFragment}
        </div>
        <div className="cp__footer">
          <WonLabelledHr label="done?" className="cp__footer__labelledhr" />
          {isFromAtomEditable ? (
            <div className="cp__footer__edit">
              <button
                className="cp__footer__edit__save won-button--filled red"
                onClick={save}
                disabled={
                  connectionHasBeenLost ||
                  !useCaseUtils.isValidDraft(draftObject, useCase)
                }
              >
                Save
              </button>
              <button
                className="cp__footer__edit__cancel won-button--outlined thin red"
                onClick={history.goBack}
              >
                Cancel
              </button>
            </div>
          ) : (
            <div className="cp__footer__error">
              {
                "Can't edit this atom (atom not owned or doesn't have a matching usecase or you are not logged in anymore)"
              }
            </div>
          )}
        </div>
      </won-create-atom>
    );
  } else {
    console.debug("no usecase specified, return empty div");
    return <div />;
  }
}
WonEditAtom.propTypes = {
  fromAtom: PropTypes.object.isRequired,
};
