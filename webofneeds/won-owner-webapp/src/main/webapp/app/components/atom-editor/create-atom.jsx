/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React, { useEffect, useState } from "react";
import { useSelector, useDispatch } from "react-redux";
import { useHistory } from "react-router-dom";
import PropTypes from "prop-types";
import { get, getIn, getUri, generateLink } from "../../utils.js";
import { isTagViewSocket } from "../../won-utils.js";
import vocab from "../../service/vocab";

import * as generalSelectors from "../../redux/selectors/general-selectors.js";
import * as viewSelectors from "../../redux/selectors/view-selectors.js";
import * as processUtils from "../../redux/utils/process-utils.js";
import * as atomUtils from "../../redux/utils/atom-utils.js";
import * as useCaseUtils from "../../usecase-utils.js";
import * as accountUtils from "../../redux/utils/account-utils.js";

import WonBranchDetailInput from "./branch-detail-input.jsx";
import WonLabelledHr from "../labelled-hr.jsx";
import WonPublishButton from "~/app/components/publish-button";
import { actionCreators } from "../../actions/actions";

import "~/style/_create-atom.scss";
import "~/style/_responsiveness-utils.scss";
import WonAtomHeaderBig from "~/app/components/atom-header-big";
import * as wonLabelUtils from "~/app/won-label-utils";

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

  const activePinnedAtomUri = useSelector(viewSelectors.getActivePinnedAtomUri);

  const isHolderAtomValid = useSelector(
    state =>
      accountUtils.isAtomOwned(accountState, holderUri) &&
      atomUtils.hasHolderSocket(generalSelectors.getAtom(holderUri)(state))
  );

  const fromAtomUri = getUri(fromAtom);
  const isFromAtomOwned = accountUtils.isAtomOwned(accountState, fromAtomUri);
  const fromAtomUseCase = atomUtils.getMatchedUseCaseIdentifier(fromAtom);

  let useCaseImm;

  if (duplicate && fromAtom) {
    useCaseImm = useCaseUtils.getUseCaseImm(
      atomUtils.getMatchedUseCaseIdentifier(fromAtom) || "customUseCase"
    );

    const fromAtomContent = get(fromAtom, "content");
    const fromAtomSeeks = get(fromAtom, "seeks");
    const socketsReset = atomUtils.getSocketsWithKeysReset(fromAtom);
    const seeksSocketsReset = atomUtils.getSeeksSocketsWithKeysReset(fromAtom);

    if (fromAtomContent) {
      useCaseImm = useCaseImm.setIn(["draft", "content"], fromAtomContent);
    }
    if (fromAtomSeeks) {
      useCaseImm = useCaseImm.setIn(["draft", "seeks"], fromAtomSeeks);
    }

    if (socketsReset) {
      useCaseImm = useCaseImm.setIn(
        ["draft", "seeks", "content", "sockets"],
        socketsReset
      );
    }

    if (seeksSocketsReset) {
      useCaseImm = useCaseImm.setIn(
        ["draft", "seeks", "seeks", "sockets"],
        socketsReset
      );
    }
  } else {
    useCaseImm = useCaseUtils.getUseCaseImm(useCaseIdentifier);
  }

  if (connect && fromAtom) {
    // For some special create cases we move some content of the fromAtom to the createAtomDraft

    const contentTypes =
      getIn(fromAtom, ["content", "type"]) &&
      getIn(fromAtom, ["content", "type"])
        .toSet()
        .remove(vocab.WON.AtomCompacted);

    const useCaseContentTypesImm = getIn(useCaseImm, [
      "draft",
      "content",
      "type",
    ]);

    if (
      useCaseContentTypesImm &&
      (useCaseContentTypesImm.includes("s:PlanAction") ||
        useCaseContentTypesImm.includes(vocab.WXPERSONA.InterestCompacted) ||
        useCaseContentTypesImm.includes(vocab.WXPERSONA.ExpertiseCompacted)) &&
      (contentTypes.includes("s:PlanAction") ||
        contentTypes.includes(vocab.WXPERSONA.InterestCompacted) ||
        contentTypes.includes(vocab.WXPERSONA.ExpertiseCompacted))
    ) {
      const eventObjectAboutUris = getIn(fromAtom, [
        "content",
        "eventObjectAboutUris",
      ]);
      if (eventObjectAboutUris) {
        useCaseImm = useCaseImm.setIn(
          ["draft", "content", "eventObjectAboutUris"],
          eventObjectAboutUris
        );
      }
    }
  }

  const [draftObjectImm, setDraftObjectImm] = useState(
    get(useCaseImm, "draft")
  );

  const defaultNodeUri = useSelector(generalSelectors.getDefaultNodeUri);
  const loggedIn = accountUtils.isLoggedIn(accountState);

  const isFromAtomUsableAsTemplate = useSelector(
    generalSelectors.isAtomUsableAsTemplate(getUri(fromAtom))
  );

  const ownedPersonas = useSelector(generalSelectors.getOwnedPersonas);
  useEffect(
    () => {
      if (ownedPersonas) {
        const unloadedPersonas = ownedPersonas.filter((persona, personaUri) =>
          processUtils.isAtomFetchNecessary(processState, personaUri, persona)
        );
        if (unloadedPersonas.size > 0) {
          unloadedPersonas.mapKeys(personaUri => {
            console.debug("fetch personaUri, ", personaUri);
            dispatch(actionCreators.atoms__fetchUnloadedAtom(personaUri));
          });
        }
      }
    },
    [ownedPersonas]
  );

  function updateDraftSeeksImm(updatedDraftBranchImm) {
    updateDraftImm(updatedDraftBranchImm, "seeks");
  }
  function updateDraftContentImm(updatedDraftBranchImm) {
    updateDraftImm(updatedDraftBranchImm, "content");
  }

  function updateDraftImm(updatedDraftImm, branch) {
    setDraftObjectImm(draftObjectImm.set(branch, updatedDraftImm));
  }

  function publish({ personaId }) {
    if (processUtils.isProcessingPublish(processState)) {
      console.debug("publish in process, do not take any action");
      return;
    }
    const tempDraftImm = useCaseUtils.getSanitizedDraftObjectImm(
      draftObjectImm,
      useCaseImm
    );

    if (connect) {
      dispatch(
        actionCreators.connections__connectReactionAtom(
          fromAtomUri,
          tempDraftImm.toJS(),
          personaId,
          targetSocketType,
          senderSocketType,
          () => {
            //if fromAtomUri is the uri of the activePinnedAtom we simply go to the tab of the inventory instead of the fromAtomUri itself
            if (activePinnedAtomUri === fromAtomUri) {
              dispatch(
                actionCreators.view__setActivePinnedAtomTab(
                  isTagViewSocket(targetSocketType)
                    ? "DETAIL"
                    : targetSocketType
                )
              );
              history.replace(
                generateLink(history.location, {}, "/inventory", false)
              );
            } else {
              history.replace(
                generateLink(
                  history.location,
                  {
                    postUri: fromAtomUri,
                    connectionUri: undefined,
                    tab: isTagViewSocket(targetSocketType)
                      ? "DETAIL"
                      : targetSocketType,
                  },
                  "/post"
                )
              );
            }
          }
        )
      );
    } else {
      dispatch(
        actionCreators.atoms__create(
          tempDraftImm.toJS(),
          personaId,
          defaultNodeUri,
          () => {
            history.replace(
              generateLink(history.location, {}, "/inventory", false)
            );
          }
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
    let headerTitleElement;
    if (duplicate && fromAtom) {
      headerTitleElement = (
        <span className="cp__header__title">
          {"Duplicate from '" + useCaseLabel + "'"}
        </span>
      );
    } else if (connect && fromAtom) {
      headerTitleElement = (
        <span className="cp__header__title">
          {wonLabelUtils.getAddNewSocketItemLabel(
            isFromAtomOwned,
            fromAtomUseCase,
            targetSocketType,
            useCaseIdentifier,
            senderSocketType
          )}
        </span>
      );
    } else {
      headerTitleElement = (
        <span className="cp__header__title">{useCaseLabel}</span>
      );
    }

    const contentDetailsImm = get(useCaseImm, "details");

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

    return (
      <won-create-atom>
        {connect && fromAtom ? (
          <WonAtomHeaderBig
            atomUri={fromAtomUri}
            atom={fromAtom}
            disableActions={true}
          />
        ) : (
          undefined
        )}
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
          <WonPublishButton
            onPublish={publish}
            buttonEnabled={
              !connectionHasBeenLost &&
              useCaseUtils.isValidDraftImm(draftObjectImm, useCaseImm)
            }
            showPersonas={
              useCaseUtils.isHoldable(useCaseImm) &&
              loggedIn &&
              senderSocketType !== vocab.HOLD.HoldableSocketCompacted &&
              targetSocketType !== vocab.HOLD.HolderSocketCompacted
            }
            ownedPersonas={ownedPersonas}
            extendedLabel={true}
            presetHolderUri={
              isHolderAtomValid &&
              senderSocketType !== vocab.HOLD.HoldableSocketCompacted &&
              targetSocketType !== vocab.HOLD.HolderSocketCompacted
                ? holderUri
                : undefined
            }
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
