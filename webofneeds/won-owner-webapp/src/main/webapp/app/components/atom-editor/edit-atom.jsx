/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React, { useState } from "react";
import { useHistory } from "react-router-dom";
import { useSelector, useDispatch } from "react-redux";
import PropTypes from "prop-types";
import { get, getIn, getUri } from "../../utils.js";

import * as generalSelectors from "../../redux/selectors/general-selectors.js";
import * as atomUtils from "../../redux/utils/atom-utils.js";
import * as useCaseUtils from "../../usecase-utils.js";
import * as accountUtils from "../../redux/utils/account-utils.js";

import WonBranchDetailInput from "./branch-detail-input.jsx";
import WonLabelledHr from "../labelled-hr.jsx";
import { actionCreators } from "../../actions/actions";

import "~/style/_create-atom.scss";
import "~/style/_responsiveness-utils.scss";
import * as wonLabelUtils from "~/app/won-label-utils";
import * as viewSelectors from "~/app/redux/selectors/view-selectors";
import WonAtomAclEditor from "~/app/components/atom-editor/atom-acl-editor";

export default function WonEditAtom({ fromAtom }) {
  const history = useHistory();
  const dispatch = useDispatch();

  const connectionHasBeenLost = !useSelector(
    generalSelectors.selectIsConnected
  );
  const accountState = useSelector(generalSelectors.getAccountState);

  const debugModeEnabled = useSelector(viewSelectors.isDebugModeEnabled);

  const useCaseImm = useCaseUtils.getUseCaseImmMergedWithAtom(
    atomUtils.getMatchedUseCaseIdentifier(fromAtom) || "customUseCase",
    fromAtom,
    true
  );

  const [draftObjectImm, setDraftObjectImm] = useState(
    get(useCaseImm, "draft")
  );

  const loggedIn = accountUtils.isLoggedIn(accountState);

  const isFromAtomOwned = accountUtils.isAtomOwned(
    accountState,
    getUri(fromAtom)
  );
  const isFromAtomEditable =
    useSelector(generalSelectors.isAtomEditable(getUri(fromAtom))) && loggedIn;

  function updateDraftSeeksImm(updatedDraftBranchImm) {
    updateDraftImm(updatedDraftBranchImm, "seeks");
  }
  function updateDraftContentImm(updatedDraftBranchImm) {
    updateDraftImm(updatedDraftBranchImm, "content");
  }
  function updateDraftAclImm(updatedDraftAclImm) {
    updateDraftImm(updatedDraftAclImm, "acl");
  }

  function updateDraftImm(updatedDraftImm, branch) {
    setDraftObjectImm(draftObjectImm.set(branch, updatedDraftImm));
  }

  function save() {
    if (loggedIn && isFromAtomOwned) {
      dispatch(
        actionCreators.atoms__edit(
          useCaseUtils
            .getSanitizedDraftObjectImm(draftObjectImm, useCaseImm)
            .toJS(),
          fromAtom,
          history.goBack
        )
      );
    }
  }

  if (useCaseImm) {
    const useCaseLabel = get(useCaseImm, "label");
    const useCaseIcon = get(useCaseImm, "icon");
    const useCaseIconJS = useCaseIcon && useCaseIcon.toJS();

    const headerIconElement = useCaseIconJS && (
      <svg className="cp__header__icon" title={useCaseLabel}>
        <use xlinkHref={useCaseIconJS} href={useCaseIconJS} />
      </svg>
    );
    let headerTitleElement = (
      <span className="cp__header__title">Edit Atom</span>
    );

    let contentDetailsImm = get(useCaseImm, "details");

    if (debugModeEnabled || useCaseUtils.hasDebugFlags(useCaseImm)) {
      const debugFlagsDetailImm = useCaseUtils.getDebugFlagsDetailImm();
      contentDetailsImm = contentDetailsImm.set(
        get(debugFlagsDetailImm, "identifier"),
        debugFlagsDetailImm
      );
    }

    const createContentFragment = contentDetailsImm &&
      contentDetailsImm.size > 0 && (
        <WonBranchDetailInput
          detailListImm={contentDetailsImm}
          initialDraftImm={getIn(useCaseImm, ["draft", "content"])}
          onUpdateImm={updateDraftContentImm}
        />
      );

    const seeksDetailsImm = get(useCaseImm, "seeksDetails");

    const createSeeksFragment = seeksDetailsImm &&
      seeksDetailsImm.size > 0 && (
        <React.Fragment>
          <div className="cp__content__branchheader">Looking For</div>
          <WonBranchDetailInput
            detailListImm={seeksDetailsImm}
            initialDraftImm={getIn(useCaseImm, ["draft", "seeks"])}
            onUpdateImm={updateDraftSeeksImm}
          />
        </React.Fragment>
      );

    const socketElements = [];

    // Show enabled sockets if debug mode is enabled FIXME: IMPLEMENT ABILITY TO (DE)SELECT AVAILABLE SOCKETS, REMOVE DEBUGVIEW
    if (debugModeEnabled) {
      const sockets = getIn(useCaseImm, ["draft", "content", "sockets"]);
      sockets.map(compactedSocketUri => {
        const helpText = wonLabelUtils.getSocketHelpText(compactedSocketUri);

        socketElements.push(
          <React.Fragment key={compactedSocketUri}>
            <div className="cp__content__sockets__label">
              {wonLabelUtils.getSocketLabel(compactedSocketUri)}
            </div>
            {helpText ? (
              <div className="cp__content__sockets__helptext">{helpText}</div>
            ) : (
              <div className="cp__content__sockets__helptext cp__content__sockets__helptext--notext" />
            )}
          </React.Fragment>
        );
      });
    }

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
          {socketElements.length > 0 && (
            <React.Fragment>
              <div className="cp__content__branchheader">Enabled Sockets</div>
              <div className="cp__content__sockets">{socketElements}</div>
            </React.Fragment>
          )}
          <WonAtomAclEditor
            initialDraftImm={getIn(useCaseImm, ["draft", "acl"])}
            onUpdateImm={updateDraftAclImm}
          />
        </div>
        <div className="cp__footer">
          <WonLabelledHr label="done?" className="cp__footer__labelledhr" />
          {isFromAtomEditable ? (
            <div className="cp__footer__edit">
              <button
                className="cp__footer__edit__save won-button--filled secondary"
                onClick={save}
                disabled={
                  connectionHasBeenLost ||
                  !useCaseUtils.isValidDraftImm(draftObjectImm, useCaseImm)
                }
              >
                Save
              </button>
              <button
                className="cp__footer__edit__cancel won-button--outlined thin secondary"
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
