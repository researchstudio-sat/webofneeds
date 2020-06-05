/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React, { useState } from "react";
import { useSelector, useDispatch } from "react-redux";
import PropTypes from "prop-types";
import { get } from "../utils.js";

import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as processUtils from "../redux/utils/process-utils.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as useCaseUtils from "../usecase-utils.js";
import * as accountUtils from "../redux/utils/account-utils.js";

import "~/style/_create-atom.scss";
import "~/style/_responsiveness-utils.scss";

import Immutable from "immutable";
import WonCreateIsSeeks from "./create-isseeks.jsx";
import WonLabelledHr from "./labelled-hr.jsx";
import ElmReact from "./elm-react.jsx";
import { Elm } from "../../elm/PublishButton.elm";
import { actionCreators } from "../actions/actions";
import { useHistory } from "react-router-dom";

export default function WonCreateAtom({
  fromAtom,
  duplicate,
  connect,
  senderSocketType,
  targetSocketType,
  useCaseIdentifier,
  holderUri,
}) {
  const history = useHistory();
  const dispatch = useDispatch();
  const connectionHasBeenLost = !useSelector(
    generalSelectors.selectIsConnected
  );
  const processState = useSelector(generalSelectors.getProcessState);
  const accountState = useSelector(generalSelectors.getAccountState);

  const isHolderAtomValid = useSelector(
    state =>
      accountUtils.isAtomOwned(accountState, holderUri) &&
      atomUtils.hasHolderSocket(generalSelectors.getAtom(holderUri)(state))
  );

  let useCase;

  if (duplicate && fromAtom) {
    useCase = useCaseUtils.getUseCase(
      atomUtils.getMatchedUseCaseIdentifier(fromAtom) || "customUseCase"
    );

    const fromAtomContent = get(fromAtom, "content");
    const fromAtomSeeks = get(fromAtom, "seeks");
    const socketsReset = atomUtils.getSocketsWithKeysReset(fromAtom);
    const seeksSocketsReset = atomUtils.getSeeksSocketsWithKeysReset(fromAtom);

    if (fromAtomContent) {
      useCase.draft.content = fromAtomContent.toJS();
    }
    if (fromAtomSeeks) {
      useCase.draft.seeks = fromAtomSeeks.toJS();
    }

    if (socketsReset) {
      useCase.draft.content.sockets = socketsReset.toJS();
    }

    if (seeksSocketsReset) {
      useCase.draft.seeks.sockets = seeksSocketsReset.toJS();
    }
  } else {
    useCase = useCaseUtils.getUseCase(useCaseIdentifier);
  }

  const [draftObject, setDraftObject] = useState(
    JSON.parse(JSON.stringify(useCase.draft))
  );

  const defaultNodeUri = useSelector(generalSelectors.getDefaultNodeUri);
  const loggedIn = accountUtils.isLoggedIn(accountState);

  const isFromAtomUsableAsTemplate = useSelector(
    generalSelectors.isAtomUsableAsTemplate(get(fromAtom, "uri"))
  );
  const personas = useSelector(
    generalSelectors.getOwnedCondensedPersonaList
  ).toJS();

  function updateDraftSeeks(updatedDraftJson) {
    updateDraft(updatedDraftJson.draft, "seeks");
  }

  function updateDraftContent(updatedDraftJson) {
    updateDraft(updatedDraftJson.draft, "content");
  }

  function updateDraft(updatedDraft, branch) {
    const _draftObject = JSON.parse(JSON.stringify(draftObject));
    _draftObject[branch] = updatedDraft;

    setDraftObject(_draftObject);
  }

  function publish({ personaId }) {
    if (processUtils.isProcessingPublish(processState)) {
      console.debug("publish in process, do not take any action");
      return;
    }
    const tempDraft = useCaseUtils.getSanitizedDraftObject(
      draftObject,
      useCase
    );

    let executeFunction;

    if (connect) {
      executeFunction = () => {
        dispatch(
          actionCreators.connections__connectReactionAtom(
            get(fromAtom, "uri"),
            tempDraft,
            personaId,
            targetSocketType,
            senderSocketType,
            history
          )
        );
        history.push("/connections"); //TODO: PUSH TO PAGE DEPENDING ON CONNECTED SOCKETTYPES/ATOM
      };
    } else {
      executeFunction = () => {
        dispatch(
          actionCreators.atoms__create(tempDraft, personaId, defaultNodeUri)
        );
        history.push("/inventory");
      };
    }

    if (loggedIn) {
      executeFunction();
    } else {
      dispatch(
        actionCreators.view__showTermsDialog(
          Immutable.fromJS({
            acceptCallback: () => {
              dispatch(actionCreators.view__hideModalDialog());
              executeFunction();
            },
            cancelCallback: () => {
              dispatch(actionCreators.view__hideModalDialog());
            },
          })
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
    let headerTitleElement;
    if (duplicate && fromAtom) {
      headerTitleElement = (
        <span className="cp__header__title">
          {"Duplicate from '" + useCase.label + "'"}
        </span>
      );
    } else {
      headerTitleElement = (
        <span className="cp__header__title">{useCase.label}</span>
      );
    }

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

          <ElmReact
            src={Elm.PublishButton}
            flags={{
              buttonEnabled:
                !connectionHasBeenLost &&
                useCaseUtils.isValidDraft(draftObject, useCase),
              showPersonas: useCaseUtils.isHoldable(useCase) && loggedIn,
              personas: personas,
              presetHolderUri: isHolderAtomValid ? holderUri : undefined,
            }}
            onPublish={publish}
          />

          {duplicate &&
            !isFromAtomUsableAsTemplate && (
              <div className="cp__footer__error">
                {
                  "Can't use this atom as a template (atom is owned or doesn't have a matching usecase)"
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
WonCreateAtom.propTypes = {
  fromAtom: PropTypes.object,
  duplicate: PropTypes.bool,
  connect: PropTypes.bool,
  senderSocketType: PropTypes.string,
  targetSocketType: PropTypes.string,
  useCaseIdentifier: PropTypes.string,
  holderUri: PropTypes.string,
};
