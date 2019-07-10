/**
 * Created by ksinger on 24.08.2015.
 */
import angular from "angular";
import Immutable from "immutable";
import ngAnimate from "angular-animate";

import "ng-redux";
import labelledHrModule from "./labelled-hr.js";
import createIsseeksModule from "./create-isseeks.js";
import { get, delay, getIn } from "../utils.js";
import { attach } from "../cstm-ng-utils.js";
import { actionCreators } from "../actions/actions.js";
import { connect2Redux } from "../configRedux.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as processSelectors from "../redux/selectors/process-selectors.js";
import * as useCaseUtils from "../usecase-utils.js";
import * as accountUtils from "../redux/utils/account-utils.js";
import { Elm } from "../../elm/PublishButton.elm";
import elmModule from "./elm.js";

import "~/style/_create-post.scss";
import "~/style/_responsiveness-utils.scss";

const serviceDependencies = [
  "$ngRedux",
  "$scope",
  "$element" /*'$routeParams' /*injections as strings here*/,
];

function genComponentConf() {
  const template = `
        <div class="cp__header">
            <svg class="cp__header__icon"
                title="{{self.useCase['label']}}"
                ng-if="self.useCase['icon']">
                    <use xlink:href="{{self.useCase['icon']}}" href="{{self.useCase['icon']}}"></use>
            </svg>
            <span class="cp__header__title" ng-if="!self.isCreateFromAtom && !self.isEditFromAtom">{{self.useCase.label}}</span>
            <span class="cp__header__title" ng-if="self.isCreateFromAtom">Duplicate from '{{self.useCase.label}}'</span>
            <span class="cp__header__title" ng-if="self.isEditFromAtom">Edit Post</span>
        </div>
        <div class="cp__content">
            <div class="cp__content__loading" ng-if="!self.showCreateInput && self.isFromAtomLoading">
                <svg class="cp__content__loading__spinner hspinner">
                    <use xlink:href="#ico_loading_anim" href="#ico_loading_anim"></use>
                </svg>
                <span class="cp__content__loading__label">
                    Loading...
                </span>
            </div>
            <div class="cp__content__failed" ng-if="!self.showCreateInput && self.hasFromAtomFailedToLoad">
                <svg class="cp__content__failed__icon">
                    <use xlink:href="#ico16_indicator_error" href="#ico16_indicator_error"></use>
                </svg>
                <span class="cp__content__failed__label">
                    Failed To Load - Post might have been deleted
                </span>
                <div class="cp__content__failed__actions">
                    <button class="cp__content__failed__actions__button red won-button--outlined thin"
                        ng-click="self.atoms__fetchUnloadedAtom(self.fromAtomUri)()">
                        Try Reload
                    </button>
                </div>
            </div>
            <!-- ADD TITLE AND DETAILS -->
            <div class="cp__content__branchheader"
              ng-if="self.showCreateInput && self.useCase.details">
              Your offer or self description
            </div>
            <won-create-isseeks 
                ng-if="self.showCreateInput && self.useCase.details"
                is-or-seeks="::'Description'"
                detail-list="self.useCase.details"
                initial-draft="self.useCase.draft.content"
                on-update="::self.updateDraft(draft, 'content')"
                on-scroll="::self.scrollIntoView(element)">
            </won-create-isseeks>
            <div class="cp__content__branchheader"
              ng-if="self.showCreateInput && self.useCase.seeksDetails">
              Looking For
            </div>
            <won-create-isseeks 
                ng-if="self.showCreateInput && self.useCase.seeksDetails"
                is-or-seeks="::'Search'" 
                detail-list="self.useCase.seeksDetails"
                initial-draft="self.useCase.draft.seeks"
                on-update="::self.updateDraft(draft, 'seeks')" 
                on-scroll="::self.scrollIntoView(element)">
            </won-create-isseeks>
        </div>
        <div class="cp__footer" ng-if="self.initialLoadFinished">
            <won-labelled-hr label="::'done?'" class="cp__footer__labelledhr"></won-labelled-hr>
            <won-elm
              module="self.publishButton"
              props="{
                buttonEnabled: self.isValid(),
                showPersonas: self.isHoldable && self.loggedIn,
                personas: self.personas,
                presetHolderUri: self.isHolderAtomValid ? self.holderUri : undefined,
              }"
              on-publish="self.publish(personaId)"
              ng-if="self.showCreateInput && !self.isEditFromAtom">
            </won-elm>
            <div class="cp__footer__edit" ng-if="self.loggedIn && self.showCreateInput && self.isEditFromAtom && self.isFromAtomEditable">
              <button class="cp__footer__edit__save won-button--filled red" ng-click="self.save()" ng-disabled="!self.isValid()">
                  Save
              </button>
              <button class="cp__footer__edit__cancel won-button--outlined thin red" ng-click="self.router__back()">
                  Cancel
              </button>
            </div>
            <div class="cp__footer__error" ng-if="self.showCreateInput && self.isEditFromAtom && !self.isFromAtomEditable">
              Can't edit this atom (atom not owned or doesn't have a matching usecase)
            </div>
            <div class="cp__footer__error" ng-if="self.showCreateInput && self.isCreateFromAtom && !self.isFromAtomUsableAsTemplate">
              Can't use this atom as a template (atom is owned or doesn't have a matching usecase)
            </div>
        </div>
    `;

  class Controller {
    constructor(/* arguments <- serviceDependencies */) {
      attach(this, serviceDependencies, arguments);
      this.focusedElement = null;
      window.cnc4dbg = this;

      this.windowHeight = window.screen.height;
      this.scrollContainer().addEventListener("scroll", e => this.onResize(e));

      this.draftObject = {};

      this.details = { is: [], seeks: [] };
      this.isNew = true;

      this.publishButton = Elm.PublishButton;
      const selectFromState = state => {
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

        const connectToAtomUri = mode === "CONNECT" && fromAtomUri;

        if (isCreateFromAtom || isEditFromAtom) {
          isFromAtomLoading = processSelectors.isAtomLoading(
            state,
            fromAtomUri
          );
          isFromAtomToLoad = processSelectors.isAtomToLoad(state, fromAtomUri);
          hasFromAtomFailedToLoad = processSelectors.hasAtomFailedToLoad(
            state,
            fromAtomUri
          );
          fromAtom =
            !isFromAtomLoading &&
            !isFromAtomToLoad &&
            !hasFromAtomFailedToLoad &&
            getIn(state, ["atoms", fromAtomUri]);

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
        const isHolderAtomValid =
          holderAtom && atomUtils.hasHolderSocket(holderAtom);

        return {
          loggedIn: accountUtils.isLoggedIn(get(state, "account")),
          holderUri,
          isHolderAtomValid,
          connectToAtomUri,
          processingPublish: state.getIn(["process", "processingPublish"]),
          connectionHasBeenLost: !generalSelectors.selectIsConnected(state),
          useCaseString,
          useCase,
          fromAtom,
          fromAtomUri,
          isFromAtomOwned: generalSelectors.isAtomOwned(state, fromAtomUri),
          isCreateFromAtom,
          isEditFromAtom,
          isFromAtomLoading,
          isFromAtomToLoad,
          isFromAtomEditable: generalSelectors.isAtomEditable(
            state,
            fromAtomUri
          ),
          isFromAtomUsableAsTemplate: generalSelectors.isAtomUsableAsTemplate(
            state,
            fromAtomUri
          ),
          isHoldable: useCaseUtils.isHoldable(useCase),
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
      // Using actionCreators like this means that every action defined there is available in the template.
      connect2Redux(selectFromState, actionCreators, [], this);

      this.$scope.$watch(
        () => this.isFromAtomToLoad,
        () => delay(0).then(() => this.ensureFromAtomIsLoaded())
      );

      this.$scope.$watch(
        () => this.showCreateInput,
        () => delay(0).then(() => this.loadInitialDraft())
      );
    }

    ensureFromAtomIsLoaded() {
      if (this.isFromAtomToLoad) {
        this.atoms__fetchUnloadedAtom(this.fromAtomUri);
      }
    }

    onResize() {
      if (this.focusedElement) {
        if (this.windowHeight < window.screen.height) {
          this.windowHeight < window.screen.height;
          this.scrollIntoView(document.querySelector(this.focusedElement));
        } else {
          this.windowHeight = window.screen.height;
        }
      }
    }

    scrollIntoView(element) {
      this._programmaticallyScrolling = true;

      if (element) {
        element.scrollIntoView({ behavior: "smooth", block: "nearest" });
      }
    }

    scrollContainer() {
      if (!this._scrollContainer) {
        this._scrollContainer = this.$element[0].querySelector(".cp__content");
      }
      return this._scrollContainer;
    }

    toggleTuningOptions() {}

    isValid() {
      const draft = this.draftObject;
      const draftContent = get(draft, "content");
      const seeksBranch = get(draft, "seeks");

      if (draftContent || seeksBranch) {
        const mandatoryContentDetailsSet = mandatoryDetailsSet(
          draftContent,
          this.useCase.details
        );
        const mandatorySeeksDetailsSet = mandatoryDetailsSet(
          seeksBranch,
          this.useCase.seeksDetails
        );
        if (mandatoryContentDetailsSet && mandatorySeeksDetailsSet) {
          const hasContent = isBranchContentPresent(draftContent);
          const hasSeeksContent = isBranchContentPresent(seeksBranch);

          return !this.connectionHasBeenLost && (hasContent || hasSeeksContent);
        }
      }
      return false;
    }

    loadInitialDraft() {
      if (this.showCreateInput && this.useCase.draft) {
        // deep clone of draft
        this.draftObject = JSON.parse(JSON.stringify(this.useCase.draft));
        delay(0).then(() => this.$scope.$digest());
      }
    }

    updateDraft(updatedDraft, branch) {
      if (this.isNew) {
        this.isNew = false;
      }

      this.draftObject[branch] = updatedDraft;
    }

    save() {
      if (this.loggedIn && this.isFromAtomOwned) {
        this.sanitizeDraftObject();

        this.atoms__edit(this.draftObject, this.fromAtom);
      }
    }

    /**
     * Removes empty branches from the draft, and adds the proper useCase to the draft
     */
    sanitizeDraftObject() {
      this.draftObject.useCase = get(this.useCase, "identifier");

      if (!isBranchContentPresent(this.draftObject.content, true)) {
        delete this.draftObject.content;
      }
      if (!isBranchContentPresent(this.draftObject.seeks, true)) {
        delete this.draftObject.seeks;
      }
    }

    publish(persona) {
      if (this.processingPublish) {
        console.debug("publish in process, do not take any action");
        return;
      }

      this.sanitizeDraftObject();

      if (this.connectToAtomUri) {
        const tempConnectToAtomUri = this.connectToAtomUri;
        const tempDraft = this.draftObject;

        if (this.loggedIn) {
          this.connections__connectReactionAtom(
            tempConnectToAtomUri,
            tempDraft,
            persona
          );
          this.router__stateGo("connections", {
            useCase: undefined,
            connectionUri: undefined,
          });
        } else {
          this.view__showTermsDialog(
            Immutable.fromJS({
              acceptCallback: () => {
                this.view__hideModalDialog();
                this.connections__connectReactionAtom(
                  tempConnectToAtomUri,
                  tempDraft,
                  persona
                );
                this.router__stateGo("connections", {
                  useCase: undefined,
                  connectionUri: undefined,
                });
              },
              cancelCallback: () => {
                this.view__hideModalDialog();
              },
            })
          );
        }
      } else {
        const tempDraft = this.draftObject;
        const tempDefaultNodeUri = this.$ngRedux
          .getState()
          .getIn(["config", "defaultNodeUri"]);

        if (this.loggedIn) {
          this.atoms__create(tempDraft, persona, tempDefaultNodeUri);
          this.router__stateGo("inventory");
        } else {
          this.view__showTermsDialog(
            Immutable.fromJS({
              acceptCallback: () => {
                this.view__hideModalDialog();
                this.atoms__create(tempDraft, persona, tempDefaultNodeUri);
                this.router__stateGo("inventory");
              },
              cancelCallback: () => {
                this.view__hideModalDialog();
              },
            })
          );
        }
      }
    }
  }

  Controller.$inject = serviceDependencies;

  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    scope: {
      /*scope-isolation*/
    },
    template: template,
  };
}

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

export default //.controller('CreateAtomController', [...serviceDependencies, CreateAtomController])
angular
  .module("won.owner.components.createPost", [
    labelledHrModule,
    createIsseeksModule,
    ngAnimate,
    elmModule,
  ])
  .directive("wonCreatePost", genComponentConf).name;
