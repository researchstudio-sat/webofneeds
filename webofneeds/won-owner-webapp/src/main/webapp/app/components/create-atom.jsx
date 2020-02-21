/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import PropTypes from "prop-types";
import { connect } from "react-redux";
import { get, getIn } from "../utils.js";

import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as processSelectors from "../redux/selectors/process-selectors.js";
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

//TODO: figure out setting initial draft
//TODO: figure out initial load of atom

/* old code:
      this.$scope.$watch(
        () => this.isFromAtomToLoad,
        () => delay(0).then(() => this.ensureFromAtomIsLoaded())
      );

      this.$scope.$watch(
        () => this.showCreateInput,
        () => delay(0).then(() => this.loadInitialDraft())
      );

  //was called if "isFromAtomToLoad" was true
  ensureFromAtomIsLoaded() {
    if (this.props.isFromAtomToLoad) {
      this.props.fetchUnloadedAtom(this.props.fromAtomUri);
    }
  }
 */

const mapStateToProps = state => {
  const fromAtomUri = generalSelectors.getFromAtomUriFromRoute(state);
  const mode = generalSelectors.getModeFromRoute(state);

  let fromAtom;

  let useCaseString;
  let useCase;

  const isCreateFromAtom = !!(fromAtomUri && mode === "DUPLICATE");
  const isEditFromAtom = !!(fromAtomUri && mode === "EDIT");

  let isFromAtomLoading = false;
  let isFromAtomToLoad = false;
  let hasFromAtomFailedToLoad = false;

  const connectToAtomUri = mode === "CONNECT" ? fromAtomUri : undefined;
  const atomDraftSocketType =
    mode === "CONNECT"
      ? generalSelectors.getSenderSocketTypeFromRoute(state)
      : undefined;
  const connectToSocketType =
    mode === "CONNECT"
      ? generalSelectors.getTargetSocketTypeFromRoute(state)
      : undefined;

  if (isCreateFromAtom || isEditFromAtom) {
    isFromAtomLoading = processSelectors.isAtomLoading(state, fromAtomUri);
    isFromAtomToLoad = processSelectors.isAtomToLoad(state, fromAtomUri);
    hasFromAtomFailedToLoad = processSelectors.hasAtomFailedToLoad(
      state,
      fromAtomUri
    );
    fromAtom =
      !isFromAtomLoading && !isFromAtomToLoad && !hasFromAtomFailedToLoad
        ? getIn(state, ["atoms", fromAtomUri])
        : undefined;

    if (fromAtom) {
      const matchedUseCaseIdentifier = atomUtils.getMatchedUseCaseIdentifier(
        fromAtom
      );

      useCaseString = matchedUseCaseIdentifier || "customUseCase";
      useCase = useCaseUtils.getUseCase(useCaseString);

      const fromAtomContent = get(fromAtom, "content");
      const fromAtomSeeks = get(fromAtom, "seeks");
      const socketsReset = atomUtils.getSocketsWithKeysReset(fromAtom);
      const defaultSocketReset = atomUtils.getDefaultSocketWithKeyReset(
        fromAtom
      );
      const seeksSocketsReset = atomUtils.getSeeksSocketsWithKeysReset(
        fromAtom
      );
      const seeksDefaultSocketReset = atomUtils.getSeeksDefaultSocketWithKeyReset(
        fromAtom
      );

      if (fromAtomContent) {
        useCase.draft.content = fromAtomContent.toJS();
      }
      if (fromAtomSeeks) {
        useCase.draft.seeks = fromAtomSeeks.toJS();
      }

      if (!isEditFromAtom) {
        if (socketsReset) {
          useCase.draft.content.sockets = socketsReset.toJS();
        }
        if (defaultSocketReset) {
          useCase.draft.content.defaultSocket = defaultSocketReset.toJS();
        }

        if (seeksSocketsReset) {
          useCase.draft.seeks.sockets = seeksSocketsReset.toJS();
        }
        if (seeksDefaultSocketReset) {
          useCase.draft.seeks.defaultSocket = seeksDefaultSocketReset.toJS();
        }
      }
    }
  } else {
    useCaseString = generalSelectors.getUseCaseFromRoute(state);
    useCase = useCaseUtils.getUseCase(useCaseString);
  }

  const holderUri = generalSelectors.getHolderUriFromRoute(state);
  const isHolderOwned = accountUtils.isAtomOwned(
    get(state, "account"),
    holderUri
  );
  const holderAtom = isHolderOwned && getIn(state, ["atoms", holderUri]);
  const isHolderAtomValid = holderAtom && atomUtils.hasHolderSocket(holderAtom);

  return {
    defaultNodeUri: getIn(state, ["config", "defaultNodeUri"]),
    loggedIn: accountUtils.isLoggedIn(get(state, "account")),
    holderUri,
    isHolderAtomValid,
    connectToAtomUri,
    processingPublish: processSelectors.isProcessingPublish(state),
    connectionHasBeenLost: !generalSelectors.selectIsConnected(state),
    useCase,
    fromAtom,
    fromAtomUri,
    isFromAtomOwned: generalSelectors.isAtomOwned(state, fromAtomUri),
    isCreateFromAtom,
    isEditFromAtom,
    isFromAtomLoading,
    isFromAtomToLoad,
    isFromAtomEditable: generalSelectors.isAtomEditable(state, fromAtomUri),
    isFromAtomUsableAsTemplate: generalSelectors.isAtomUsableAsTemplate(
      state,
      fromAtomUri
    ),
    isHoldable: useCaseUtils.isHoldable(useCase),
    atomDraftSocketType,
    connectToSocketType,
    hasFromAtomFailedToLoad,
    personas: generalSelectors.getOwnedCondensedPersonaList(state).toJS(),
    showCreateInput:
      useCase &&
      !(
        isCreateFromAtom &&
        isEditFromAtom &&
        (isFromAtomLoading || hasFromAtomFailedToLoad || isFromAtomToLoad)
      ),
  };
};

const mapDispatchToProps = dispatch => {
  return {
    fetchUnloadedAtom: atomUri => {
      dispatch(actionCreators.atoms__fetchUnloadedAtom(atomUri));
    },
    routerBack: () => {
      dispatch(actionCreators.router__back());
    },
    routerGo: (path, props) => {
      dispatch(actionCreators.router__stateGo(path, props));
    },
    atomsEdit: (draft, atom) => {
      dispatch(actionCreators.atoms__edit(draft, atom));
    },
    atomsCreate: (draft, persona, nodeUri) => {
      dispatch(actionCreators.atoms__create(draft, persona, nodeUri));
    },
    hideModalDialog: () => {
      dispatch(actionCreators.view__hideModalDialog());
    },
    showTermsDialog: payload => {
      dispatch(actionCreators.view__showTermsDialog(payload));
    },
    connectionsConnectReactionAtom: (
      connectToAtomUri,
      draft,
      persona,
      connectToSocketType,
      atomDraftSocketType
    ) => {
      dispatch(
        actionCreators.connections__connectReactionAtom(
          connectToAtomUri,
          draft,
          persona,
          connectToSocketType,
          atomDraftSocketType
        )
      );
    },
  };
};

class CreateAtom extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      isNew: true,
      draftObject: {},
    };

    this.save = this.save.bind(this);
    this.publish = this.publish.bind(this);
    this.updateDraftContent = this.updateDraftContent.bind(this);
    this.updateDraftSeeks = this.updateDraftSeeks.bind(this);
  }

  componentDidMount() {
    if (this.props.showCreateInput && this.props.useCase.draft) {
      // deep clone of draft
      this.setState({
        draftObject: JSON.parse(JSON.stringify(this.props.useCase.draft)),
      });
    }
  }

  render() {
    if (!this.props.useCase) {
      console.debug("no usecase specified, return empty div");
      return <div />;
    }

    const headerIconElement = this.props.useCase["icon"] && (
      <svg className="cp__header__icon" title={this.props.useCase["label"]}>
        <use
          xlinkHref={this.props.useCase["icon"]}
          href={this.props.useCase["icon"]}
        />
      </svg>
    );
    let headerTitleElement;
    if (this.props.isCreateFromAtom) {
      headerTitleElement = (
        <span className="cp__header__title">
          {"Duplicate from '" + this.props.useCase.label + "'"}
        </span>
      );
    } else if (this.props.isEditFromAtom) {
      headerTitleElement = <span className="cp__header__title">Edit Post</span>;
    } else {
      headerTitleElement = (
        <span className="cp__header__title">{this.props.useCase.label}</span>
      );
    }

    if (this.props.showCreateInput) {
      const createContentFragment = this.props.useCase.details &&
        Object.keys(this.props.useCase.details).length > 0 && (
          <React.Fragment>
            <div className="cp__content__branchheader">
              Your offer or self description
            </div>
            <WonCreateIsSeeks
              detailList={this.props.useCase.details}
              initialDraft={this.props.useCase.draft.content}
              onUpdate={this.updateDraftContent}
            />
          </React.Fragment>
        );

      const createSeeksFragment = this.props.useCase.seeksDetails &&
        Object.keys(this.props.useCase.seeksDetails).length > 0 && (
          <React.Fragment>
            <div className="cp__content__branchheader">Looking For</div>
            <WonCreateIsSeeks
              detailList={this.props.useCase.seeksDetails}
              initialDraft={this.props.useCase.draft.seeks}
              onUpdate={this.updateDraftSeeks}
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
            {!this.props.isEditFromAtom && (
              <ElmReact
                src={Elm.PublishButton}
                flags={{
                  buttonEnabled: this.isValid(),
                  showPersonas: this.props.isHoldable && this.props.loggedIn,
                  personas: this.props.personas,
                  presetHolderUri: this.props.isHolderAtomValid
                    ? this.props.holderUri
                    : undefined,
                }}
                onPublish={this.publish}
              />
            )}
            {this.props.loggedIn &&
              this.props.isEditFromAtom &&
              this.props.isFromAtomEditable && (
                <div className="cp__footer__edit">
                  <button
                    className="cp__footer__edit__save won-button--filled red"
                    onClick={this.save}
                    disabled={!this.isValid()}
                  >
                    Save
                  </button>
                  <button
                    className="cp__footer__edit__cancel won-button--outlined thin red"
                    onClick={this.props.routerBack}
                  >
                    Cancel
                  </button>
                </div>
              )}
            {this.props.isEditFromAtom &&
              !this.props.isFromAtomEditable && (
                <div className="cp__footer__error">
                  {
                    "Can't edit this atom (atom not owned or doesn't have a matching usecase)"
                  }
                </div>
              )}
            {this.props.isCreateFromAtom &&
              !this.props.isFromAtomUsableAsTemplate && (
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
      let unavailableContentElement;

      if (this.props.isFromAtomLoading) {
        unavailableContentElement = (
          <div className="cp__content__loading">
            <svg className="cp__content__loading__spinner hspinner">
              <use xlinkHref="#ico_loading_anim" href="#ico_loading_anim" />
            </svg>
            <span className="cp__content__loading__label">Loading...</span>
          </div>
        );
      } else if (this.props.hasFromAtomFailedToLoad) {
        unavailableContentElement = (
          <div className="cp__content__failed">
            <svg className="cp__content__failed__icon">
              <use
                xlinkHref="#ico16_indicator_error"
                href="#ico16_indicator_error"
              />
            </svg>
            <span className="cp__content__failed__label">
              Failed To Load - Post might have been deleted
            </span>
            <div className="cp__content__failed__actions">
              <button
                className="cp__content__failed__actions__button red won-button--outlined thin"
                onClick={() =>
                  this.props.fetchUnloadedAtom(this.props.fromAtomUri)
                }
              >
                Try Reload
              </button>
            </div>
          </div>
        );
      }

      return (
        <won-create-atom>
          <div className="cp__header">
            {headerIconElement}
            {headerTitleElement}
          </div>
          <div className="cp__content">{unavailableContentElement}</div>
          <div className="cp__footer">
            <WonLabelledHr label="done?" className="cp__footer__labelledhr" />
          </div>
        </won-create-atom>
      );
    }
  }

  isValid() {
    const draft = this.state.draftObject;
    const draftContent = get(draft, "content");
    const seeksBranch = get(draft, "seeks");

    if (draftContent || seeksBranch) {
      const mandatoryContentDetailsSet = mandatoryDetailsSet(
        draftContent,
        this.props.useCase.details
      );
      const mandatorySeeksDetailsSet = mandatoryDetailsSet(
        seeksBranch,
        this.props.useCase.seeksDetails
      );
      if (mandatoryContentDetailsSet && mandatorySeeksDetailsSet) {
        const hasContent = isBranchContentPresent(draftContent);
        const hasSeeksContent = isBranchContentPresent(seeksBranch);

        return (
          !this.props.connectionHasBeenLost && (hasContent || hasSeeksContent)
        );
      }
    }
    return false;
  }

  updateDraftSeeks(updatedDraftJson) {
    this.updateDraft(updatedDraftJson.draft, "seeks");
  }

  updateDraftContent(updatedDraftJson) {
    this.updateDraft(updatedDraftJson.draft, "content");
  }

  updateDraft(updatedDraft, branch) {
    const _draftObject = this.state.draftObject;
    _draftObject[branch] = updatedDraft;

    if (this.state.isNew) {
      this.setState({
        isNew: false,
        draftObject: _draftObject,
      });
    } else {
      this.setState({
        draftObject: _draftObject,
      });
    }
  }

  save() {
    if (this.props.loggedIn && this.props.isFromAtomOwned) {
      this.sanitizeDraftObject(() => {
        this.props.atomsEdit(this.state.draftObject, this.props.fromAtom);
      });
    }
  }

  /**
   * Removes empty branches from the draft, and adds the proper useCase to the draft
   */
  sanitizeDraftObject(callback) {
    const _draftObject = this.state.draftObject;
    _draftObject.useCase = get(this.props.useCase, "identifier");

    if (!isBranchContentPresent(_draftObject.content, true)) {
      delete _draftObject.content;
    }
    if (!isBranchContentPresent(_draftObject.seeks, true)) {
      delete _draftObject.seeks;
    }

    this.setState({ draftObject: _draftObject }, callback);
  }

  publish({ personaId }) {
    if (this.props.processingPublish) {
      console.debug("publish in process, do not take any action");
      return;
    }

    this.sanitizeDraftObject(() => {
      if (this.props.connectToAtomUri) {
        const tempConnectToAtomUri = this.props.connectToAtomUri;
        const tempAtomDraftSocketType = this.props.atomDraftSocketType;
        const tempConnectToSocketType = this.props.connectToSocketType;
        const tempDraft = this.state.draftObject;

        if (this.props.loggedIn) {
          this.props.connectionsConnectReactionAtom(
            tempConnectToAtomUri,
            tempDraft,
            personaId,
            tempConnectToSocketType,
            tempAtomDraftSocketType
          );
          this.props.routerGo("connections", {
            useCase: undefined,
            connectionUri: undefined,
          });
        } else {
          this.props.showTermsDialog(
            Immutable.fromJS({
              acceptCallback: () => {
                this.props.hideModalDialog();
                this.props.connectionsConnectReactionAtom(
                  tempConnectToAtomUri,
                  tempDraft,
                  personaId,
                  tempConnectToSocketType,
                  tempAtomDraftSocketType
                );
                this.props.routerGo("connections", {
                  useCase: undefined,
                  connectionUri: undefined,
                });
              },
              cancelCallback: () => {
                this.props.hideModalDialog();
              },
            })
          );
        }
      } else {
        const tempDraft = this.state.draftObject;
        const tempDefaultNodeUri = this.props.defaultNodeUri;

        if (this.props.loggedIn) {
          this.props.atomsCreate(tempDraft, personaId, tempDefaultNodeUri);
          this.props.routerGo("inventory");
        } else {
          this.props.showTermsDialog(
            Immutable.fromJS({
              acceptCallback: () => {
                this.props.hideModalDialog();
                this.props.atomsCreate(
                  tempDraft,
                  personaId,
                  tempDefaultNodeUri
                );
                this.props.routerGo("inventory");
              },
              cancelCallback: () => {
                this.props.hideModalDialog();
              },
            })
          );
        }
      }
    });
  }
}

CreateAtom.propTypes = {
  defaultNodeUri: PropTypes.string,
  atomsCreate: PropTypes.func,
  atomsEdit: PropTypes.func,
  connectToAtomUri: PropTypes.string,
  atomDraftSocketType: PropTypes.string,
  connectToSocketType: PropTypes.string,
  connectionHasBeenLost: PropTypes.bool,
  fetchUnloadedAtom: PropTypes.func,
  fromAtom: PropTypes.object,
  fromAtomUri: PropTypes.string,
  hasFromAtomFailedToLoad: PropTypes.bool,
  hideModalDialog: PropTypes.func,
  holderUri: PropTypes.string,
  isCreateFromAtom: PropTypes.bool,
  isEditFromAtom: PropTypes.bool,
  isFromAtomEditable: PropTypes.bool,
  isFromAtomLoading: PropTypes.bool,
  isFromAtomOwned: PropTypes.bool,
  isFromAtomToLoad: PropTypes.bool,
  isFromAtomUsableAsTemplate: PropTypes.bool,
  isHoldable: PropTypes.bool,
  isHolderAtomValid: PropTypes.bool,
  loggedIn: PropTypes.bool,
  personas: PropTypes.arrayOf(PropTypes.object),
  processingPublish: PropTypes.bool,
  routerBack: PropTypes.func,
  routerGo: PropTypes.func,
  showCreateInput: PropTypes.bool,
  showTermsDialog: PropTypes.func,
  connectionsConnectReactionAtom: PropTypes.func,
  useCase: PropTypes.object,
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(CreateAtom);

// returns true if the branch has any content present
function isBranchContentPresent(isOrSeeks, includeType = false) {
  if (isOrSeeks) {
    const details = Object.keys(isOrSeeks);
    for (let d of details) {
      if (isOrSeeks[d] && (includeType || d !== "type")) {
        return true;
      }
    }
  }
  return false;
}
// returns true if the part in isOrSeeks, has all the mandatory details of the useCaseBranchDetails
function mandatoryDetailsSet(isOrSeeks, useCaseBranchDetails) {
  if (!useCaseBranchDetails) {
    return true;
  }

  for (const key in useCaseBranchDetails) {
    if (useCaseBranchDetails[key].mandatory) {
      const detailSaved = isOrSeeks && isOrSeeks[key];
      if (!detailSaved) {
        return false;
      }
    }
  }
  return true;
}
