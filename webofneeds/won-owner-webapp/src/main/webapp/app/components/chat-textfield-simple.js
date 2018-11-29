/**
 * Also a resizing textfield that can produce messages but it only uses
 * a standard text-area instead of contenteditable. Thus it should be
 * stabler but can't do rich text / wysiwyg-formatting.
 *
 * Created by ksinger on 16.02.2018.
 */

// import Medium from '../mediumjs-es6.js';
import angular from "angular";
import "ng-redux";
import ngAnimate from "angular-animate";
import { dispatchEvent, attach, delay } from "../utils.js";
import won from "../won-es6.js";
import {
  getConnectionUriFromRoute,
  getOwnedNeedByConnectionUri,
} from "../selectors/general-selectors.js";
import { getMessagesByConnectionUri } from "../selectors/message-selectors.js";
import {
  isMessageProposable,
  isMessageClaimable,
  isMessageCancelable,
  isMessageRetractable,
  isMessageAcceptable,
  isMessageRejectable,
  isMessageSelected,
} from "../message-utils.js";
import { getAllMessageDetails } from "../won-utils.js";
import autoresizingTextareaModule from "../directives/textarea-autogrow.js";
import { actionCreators } from "../actions/actions.js";
import labelledHrModule from "./labelled-hr.js";
import { getHumanReadableStringFromMessage } from "../reducers/need-reducer/parse-message.js";
import submitButtonModule from "./submit-button.js";

import "style/_chattextfield.scss";
import "style/_textfield.scss";

function genComponentConf() {
  let template = `
      <!-- DETAILS DRAWER START -->  
        <div class="cts__details"
          ng-if="self.allowDetails && self.showAddMessageContent">
          <div class="cts__details__grid"
              ng-if="!self.selectedDetail && !self.multiSelectType">
            <won-labelled-hr label="::'Actions'" class="cts__details__grid__hr"
              ng-if="!self.multiSelectType && self.isConnected"></won-labelled-hr>
            <button
                ng-if="!self.showAgreementData"
                class="cts__details__grid__action won-button--filled red"
                ng-click="self.activateMultiSelect('proposes')"
                ng-disabled="!self.hasProposableMessages">
                Make Proposal
            </button>
            <button
                ng-if="!self.showAgreementData"
                class="cts__details__grid__action won-button--filled red"
                ng-click="self.activateMultiSelect('claims')"
                ng-disabled="!self.hasClaimableMessages">
                Make Claim
            </button>
            <button
                ng-if="self.showAgreementData"
                class="cts__details__grid__action won-button--filled red"
                ng-click="self.activateMultiSelect('accepts')"
                ng-disabled="!self.hasAcceptableMessages">
                Accept Proposal(s)
            </button>
            <button
                ng-if="self.showAgreementData"
                class="cts__details__grid__action won-button--filled red"
                ng-click="self.activateMultiSelect('rejects')"
                ng-disabled="!self.hasRejectableMessages">
                Reject Proposal(s)
            </button>
            <button
                class="cts__details__grid__action won-button--filled red"
                ng-click="self.activateMultiSelect('proposesToCancel')"
                ng-disabled="!self.hasCancelableMessages">
                Cancel Agreement(s)
            </button>
            <button class="cts__details__grid__action won-button--filled red"
                ng-click="self.activateMultiSelect('retracts')"
                ng-disabled="!self.hasRetractableMessages">
                Retract Message(s)
            </button>
            <won-labelled-hr label="::'Details'" class="cts__details__grid__hr"
              ng-if="!self.multiSelectType && self.isConnected"></won-labelled-hr>
            <div class="cts__details__grid__detail"
              ng-repeat="detail in self.allMessageDetails"
              ng-if="detail.component"
              ng-click="self.pickDetail(detail)">
              <svg class="cts__details__grid__detail__icon" ng-if="detail.icon">
                <use xlink:href={{detail.icon}} href={{detail.icon}}></use>
              </svg>
              <div class="cts__details__grid__detail__label" ng-if="detail.label">
                {{ detail.label }}
              </div>
            </div>
          </div>
          <div class="cts__details__input"
            ng-if="!self.selectedDetail && self.multiSelectType">
            <div class="cts__details__input__header">
              <svg class="cts__details__input__header__back clickable"
                ng-click="self.cancelMultiSelect()">
                <use xlink:href="#ico36_backarrow" href="#ico36_backarrow"></use>
              </svg>
              <svg class="cts__details__input__header__icon">
                <use xlink:href="#ico36_plus_circle" href="#ico36_plus_circle"></use>
              </svg>
              <div class="cts__details__input__header__label hide-in-responsive">
                {{ self.getMultiSelectActionLabel() }} ({{ self.selectedMessages.size }} Messages)
              </div>
              <div class="cts__details__input__header__label show-in-responsive">
                {{ self.getMultiSelectActionLabel() }}
              </div>
              <div class="cts__details__input__header__add" ng-click="self.saveReferencedContent()">
                <svg class="cts__details__input__header__add__icon">
                  <use xlink:href="#ico36_added_circle" href="#ico36_added_circle"></use>
                </svg>
                <span class="cts__details__input__header__add__label hide-in-responsive">
                  Save
                </span>
              </div>
              <div class="cts__details__input__header__discard" ng-click="self.removeReferencedContent()">
                <svg class="cts__details__input__header__discard__icon">
                  <use xlink:href="#ico36_close_circle" href="#ico36_close_circle"></use>
                </svg>
                <span class="cts__details__input__header__discard__label hide-in-responsive">
                  Discard
                </span>
              </div>
            </div>
            <div class="cts__details__input__refcontent hide-in-responsive" ng-if="self.selectedMessages">
              <div class="cts__details__input__refcontent__message"
                ng-repeat="msg in self.selectedMessages.toArray()">
                <div class="cts__details__input__refcontent__message__label">{{ self.getHumanReadableMessageString(msg) }}</div>
                <svg class="cts__details__input__refcontent__message__discard clickable"
                  ng-click="self.removeMessageFromSelection(msg)">
                  <use xlink:href="#ico36_close" href="#ico36_close"></use>
                </svg>
              </div>
            </div>
            <div class="cts__details__input__refcontent" ng-if="!self.selectedMessages || self.selectedMessages.size == 0">
              Select Messages above
            </div>
            <div class="cts__details__input__refcontent show-in-responsive" ng-if="self.selectedMessages && self.selectedMessages.size > 0">
              {{ self.selectedMessages.size }} Messages selected
            </div>
          </div>
          <div class="cts__details__input"
            ng-if="self.selectedDetail && !self.multiSelectType">
            <div class="cts__details__input__header">
              <svg class="cts__details__input__header__back clickable"
                ng-click="self.view.removeAddMessageContent()">
                <use xlink:href="#ico36_backarrow" href="#ico36_backarrow"></use>
              </svg>
              <svg class="cts__details__input__header__icon">
                <use xlink:href={{self.selectedDetail.icon}} href={{self.selectedDetail.icon}}></use>
              </svg>
              <div class="cts__details__input__header__label">
                {{ self.selectedDetail.label }}
              </div>
              <!--div class="cts__details__input__header__add">
                <svg class="cts__details__input__header__add__icon">
                  <use xlink:href="#ico36_added_circle" href="#ico36_added_circle"></use>
                </svg>
                <span class="cts__details__input__header__add__label hide-in-responsive">
                  Save
                </span>
              </div>
              <div class="cts__details__input__header__discard">
                <svg class="cts__details__input__header__discard__icon">
                  <use xlink:href="#ico36_close_circle" href="#ico36_close_circle"></use>
                </svg>
                <span class="cts__details__input__header__discard__label hide-in-responsive">
                  Discard
                </span>
              </div-->
            </div>
            <div class="cts__details__input__content"
              message-detail-element="{{self.selectedDetailComponent}}"
              ng-if="self.selectedDetailComponent"
              on-update="::self.updateDetail(identifier, value)"
              initial-value="self.additionalContent.get(self.selectedDetail.identifier)"
              identifier="self.selectedDetail.identifier"
              detail="self.selectedDetail">
            </div>
          </div>
        </div>
      <!-- DETAILS DRAWER END -->
      <!-- OPEN/CLOSE DETAILS BTN -->
        <button class="cts__add"
          ng-disabled="!self.allowDetails"
          ng-click="self.toggleAdditionalContentDisplay()">
            <svg class="cts__add__icon" ng-if="!self.showAddMessageContent">
                <use xlink:href="#ico36_plus" href="#ico36_plus"></use>
            </svg>
            <svg class="cts__add__icon" ng-if="self.showAddMessageContent">
                <use xlink:href="#ico36_close" href="#ico36_close"></use>
            </svg>
        </button>
      <!-- OPEN/CLOSE DETAILS BTN -->
      
        <textarea 
            won-textarea-autogrow
            data-min-rows="1"
            data-max-rows="4"
            class="cts__text won-txt"
            ng-class="{'won-txt--code': self.isCode, 'won-txt--valid' : self.belowMaxLength(), 'won-txt--invalid' : !self.belowMaxLength() }"
            tabindex="0"
            placeholder="{{self.placeholder}}"></textarea>

      <!-- PERSONA SELECTION START -->
        <div class="cts__submitbutton">
            <won-submit-button
                is-valid="self.valid()"
                on-submit="self.submit(persona)" 
                show-personas="self.showPersonasSelection"
                label="self.submitButtonLabel">
            </won-submit-button>
        </div>
      <!-- PERSONA SELECTION END -->

      <!-- ADDED DETAILS START -->  
        <div class="cts__additionalcontent" ng-if="self.hasAdditionalContent() || self.hasReferencedContent()">
          <div class="cts__additionalcontent__header">Additional Content to send:</div>
          <div class="cts__additionalcontent__list">
            <div class="cts__additionalcontent__list__item" ng-repeat="ref in self.getReferencedContentKeysArray()">
              <svg class="cts__additionalcontent__list__item__icon clickable"
                ng-click="self.activateMultiSelect(ref)">
                <use xlink:href="#ico36_plus" href="#ico36_plus"></use>
              </svg>
              <span class="cts__additionalcontent__list__item__label clickable"
                ng-click="self.activateMultiSelect(ref)">
                {{ self.getHumanReadableReferencedContent(ref) }}
              </span>
              <svg class="cts__additionalcontent__list__item__discard clickable"
                ng-click="self.removeReferencedContent(ref)">
                <use xlink:href="#ico36_close" href="#ico36_close"></use>
              </svg>
            </div>
            <div class="cts__additionalcontent__list__item" ng-repeat="key in self.getAdditionalContentKeysArray()">
              <svg class="cts__additionalcontent__list__item__icon clickable"
                ng-click="self.pickDetail(self.allMessageDetails[key])">
                <use xlink:href={{self.allMessageDetails[key].icon}} href={{self.allMessageDetails[key].icon}}></use>
              </svg>
              <span class="cts__additionalcontent__list__item__label clickable"
                ng-click="self.pickDetail(self.allMessageDetails[key])">
                {{ self.getHumanReadableDetailString(key, self.additionalContent.get(key)) }}
              </span>
              <svg class="cts__additionalcontent__list__item__discard clickable"
                ng-click="self.updateDetail(key, undefined, true)">
                <use xlink:href="#ico36_close" href="#ico36_close"></use>
              </svg>
            </div>
          </div>
        </div>
      <!-- ADDED DETAILS END -->
        <div class="cts__charcount" ng-show="self.maxChars">
            {{ self.charactersLeft() }} characters left
        </div>

        <div class="cts__helptext" ng-show="self.helpText">
            {{ self.helpText }}
        </div>
    `;

  const serviceDependencies = [
    "$scope",
    "$element",
    "$ngRedux" /*injections as strings here*/,
  ];

  class Controller {
    constructor(/* arguments <- serviceDependencies */) {
      attach(this, serviceDependencies, arguments);
      window.ctfs4dbg = this;
      this.allMessageDetails = getAllMessageDetails();

      this.draftObject = {};
      this.additionalContent = new Map(); //Stores the additional Detail content of a message
      this.referencedContent = new Map(); //Stores the reference Content of a message (e.g. proposes, retracts...)

      // keep up-to-date on whether we need to show personas or not
      this.showPersonasSelection = this.showPersonas || undefined;
      this.$scope.$watch("self.showPersonas", newValue => {
        this.showPersonasSelection = newValue;
      });

      const selectFromState = state => {
        const connectionUri = getConnectionUriFromRoute(state);
        const post =
          connectionUri && getOwnedNeedByConnectionUri(state, connectionUri);
        const connection = post && post.getIn(["connections", connectionUri]);
        const connectionState = connection && connection.get("state");

        const messages = getMessagesByConnectionUri(state, connectionUri);

        const selectedMessages =
          messages && messages.filter(msg => isMessageSelected(msg));
        const rejectableMessages =
          messages && messages.filter(msg => isMessageRejectable(msg));
        const retractableMessages =
          messages && messages.filter(msg => isMessageRetractable(msg));
        const acceptableMessages =
          messages && messages.filter(msg => isMessageAcceptable(msg));
        const proposableMessages =
          messages && messages.filter(msg => isMessageProposable(msg));
        const cancelableMessages =
          messages && messages.filter(msg => isMessageCancelable(msg));
        const claimableMessages =
          messages && messages.filter(msg => isMessageClaimable(msg));

        const hasRejectableMessages =
          rejectableMessages && rejectableMessages.size > 0;
        const hasRetractableMessages =
          retractableMessages && retractableMessages.size > 0;

        const hasAcceptableMessages =
          acceptableMessages && acceptableMessages.size > 0;
        const hasProposableMessages =
          proposableMessages && proposableMessages.size > 0;
        const hasCancelableMessages =
          cancelableMessages && cancelableMessages.size > 0;
        const hasClaimableMessages =
          claimableMessages && claimableMessages.size > 0;

        const selectedDetailIdentifier = state.getIn([
          "view",
          "selectedAddMessageContent",
        ]);
        const selectedDetail =
          this.allMessageDetails &&
          selectedDetailIdentifier &&
          this.allMessageDetails[selectedDetailIdentifier];
        return {
          connectionUri,
          post,
          multiSelectType: connection && connection.get("multiSelectType"),
          showAgreementData: connection && connection.get("showAgreementData"),
          isConnected: connectionState && connectionState === won.WON.Connected,
          selectedMessages: selectedMessages,
          hasClaimableMessages,
          hasProposableMessages,
          hasCancelableMessages,
          hasAcceptableMessages,
          hasRetractableMessages,
          hasRejectableMessages,
          connectionHasBeenLost:
            state.getIn(["messages", "reconnecting"]) ||
            state.getIn(["messages", "lostConnection"]),
          showAddMessageContent: state.getIn(["view", "showAddMessageContent"]),
          selectedDetail,
          selectedDetailComponent: selectedDetail && selectedDetail.component,
          isLoggedIn: state.getIn(["account", "loggedIn"]),
        };
      };

      const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(
        this
      );
      this.$scope.$on("$destroy", disconnect);

      this.textFieldNg().bind("input", () => {
        this.input();
        return false;
      });
      this.textFieldNg().bind("paste", () => {
        this.paste();
      });
      this.textFieldNg().bind("keydown", e => {
        this.keydown(e);
        return false;
      });
    }

    keydown(e) {
      if (e.keyCode === 13 && !e.shiftKey) {
        e.preventDefault(); // prevent a newline from being entered
        this.submit();
        return false;
      }
    }

    paste() {
      const payload = {
        value: this.value(),
        valid: this.valid(),
      };
      this.onPaste(payload);
      dispatchEvent(this.$element[0], "paste", payload);
    }

    input() {
      const payload = {
        value: this.value(),
        valid: this.valid(),
      };
      this.onInput(payload);
      dispatchEvent(this.$element[0], "input", payload);

      /* trigger digest so button and counter update
             * delay is because submit triggers an input-event
             * and is in a digest-cycle already. opposed to user-
             * triggered input-events. dunno why the latter doesn't
             * do that tho.
             */
      delay(0).then(() => this.$scope.$digest());
    }

    submit(selectedPersona) {
      const value = this.value();
      const valid = this.valid();
      if (valid) {
        const txtEl = this.textField();
        if (txtEl) {
          txtEl.value = "";
          txtEl.dispatchEvent(new Event("input")); // dispatch input event so autoresizer notices value-change
          txtEl.focus(); //refocus so people can keep writing
        }
        const payload = {
          value,
          valid,
          additionalContent: this.additionalContent,
          referencedContent: this.referencedContent,
          selectedPersona: selectedPersona || undefined,
        };
        if (this.additionalContent) {
          this.additionalContent = new Map();
        }
        if (this.referencedContent) {
          this.referencedContent = new Map();
        }
        this.cancelMultiSelect();
        this.onSubmit(payload);
        dispatchEvent(this.$element[0], "submit", payload);
      }
    }

    charactersLeft() {
      return this.maxChars - this.value().length;
    }

    belowMaxLength() {
      return !this.maxChars || this.charactersLeft() >= 0;
    }

    valid() {
      return (
        !this.connectionHasBeenLost &&
        (this.allowEmptySubmit ||
          this.hasAdditionalContent() ||
          this.hasReferencedContent() ||
          this.value().length > 0) &&
        this.belowMaxLength()
      );
    }

    value() {
      const txtEl = this.textField();
      if (txtEl) {
        return txtEl.value.trim();
      }
    }

    textFieldNg() {
      return angular.element(this.textField());
    }
    textField() {
      if (!this._textField) {
        this._textField = this.$element[0].querySelector(".cts__text");
      }
      return this._textField;
    }

    pickDetail(detail) {
      this.view__selectAddMessageContent({ selectedDetail: detail.identifier });
    }

    updateDetail(name, value, closeOnDelete = false) {
      if (!value) {
        this.additionalContent.delete(name);
        if (closeOnDelete) {
          this.view__hideAddMessageContentDisplay();
        }
      } else {
        this.additionalContent.set(name, value);
      }
    }

    hasAdditionalContent() {
      return this.additionalContent && this.additionalContent.size > 0;
    }

    hasReferencedContent() {
      return this.referencedContent && this.referencedContent.size > 0;
    }

    getAdditionalContentKeysArray() {
      return (
        this.additionalContent &&
        this.additionalContent.keys() &&
        Array.from(this.additionalContent.keys())
      );
    }

    getReferencedContentKeysArray() {
      return (
        this.referencedContent &&
        this.referencedContent.keys() &&
        Array.from(this.referencedContent.keys())
      );
    }

    getHumanReadableDetailString(key, value) {
      const usedDetail = this.allMessageDetails[key];

      return (
        usedDetail &&
        usedDetail.generateHumanReadable({ value: value, includeLabel: true })
      );
    }

    getHumanReadableMessageString(msg) {
      return (
        getHumanReadableStringFromMessage(msg) || "«Message does not have text»"
      );
    }

    activateMultiSelect(type) {
      this.cancelMultiSelect(); //close the multiselection if its already open

      this.connections__setMultiSelectType({
        connectionUri: this.connectionUri,
        multiSelectType: type,
      });

      const referencedContent =
        this.referencedContent && this.referencedContent.get(type);
      if (referencedContent) {
        referencedContent.forEach(msg => {
          this.messages__viewState__markAsSelected({
            messageUri: msg.get("uri"),
            connectionUri: this.connectionUri,
            needUri: this.post.get("uri"),
            isSelected: true,
          });
        });
      }
    }

    getMultiSelectActionLabel() {
      if (this.multiSelectType) {
        switch (this.multiSelectType) {
          case "rejects":
            return "Reject selected";
          case "retracts":
            return "Retract selected";
          case "proposes":
            return "Propose selected";
          case "accepts":
            return "Accept selected";
          case "proposesToCancel":
            return "Propose To Cancel selected";
          default:
            return "illegal state";
        }
      }
    }

    cancelMultiSelect() {
      this.connections__setMultiSelectType({
        connectionUri: this.connectionUri,
        multiSelectType: undefined,
      });
    }

    saveReferencedContent() {
      if (!this.selectedMessages || this.selectedMessages.size == 0) {
        this.referencedContent.delete(this.multiSelectType);
      } else {
        this.referencedContent.set(this.multiSelectType, this.selectedMessages);
      }
      this.cancelMultiSelect();
    }

    removeReferencedContent(ref = this.multiSelectType) {
      this.referencedContent.delete(ref);
      this.cancelMultiSelect();
    }

    removeMessageFromSelection(msg) {
      this.messages__viewState__markAsSelected({
        messageUri: msg.get("uri"),
        connectionUri: this.connectionUri,
        needUri: this.post.get("uri"),
        isSelected: false,
      });
    }

    getHumanReadableReferencedContent(ref) {
      const referencedMessages = this.referencedContent.get(ref);
      const referencedMessagesSize = referencedMessages
        ? referencedMessages.size
        : 0;

      let humanReadableReferenceString = "";

      switch (ref) {
        case "rejects":
          humanReadableReferenceString = "Reject ";
          break;
        case "retracts":
          humanReadableReferenceString = "Retract ";
          break;
        case "claims":
          humanReadableReferenceString = "Claims ";
          break;
        case "proposes":
          humanReadableReferenceString = "Propose ";
          break;
        case "accepts":
          humanReadableReferenceString = "Accept ";
          break;
        case "proposesToCancel":
          humanReadableReferenceString = "Propose To Cancel ";
          break;
        default:
          return "illegal state";
      }

      humanReadableReferenceString +=
        referencedMessagesSize +
        (referencedMessagesSize > 1 ? " Messages" : " Message");
      return humanReadableReferenceString;
    }

    toggleAdditionalContentDisplay() {
      this.cancelMultiSelect();
      this.view__toggleAddMessageContentDisplay();
    }
  }
  Controller.$inject = serviceDependencies;

  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    scope: {
      placeholder: "=", // NOTE: bound only once
      maxChars: "=",
      helpText: "=",

      isCode: "=", // whether or not the text is code and e.g. should use monospace
      allowDetails: "=", //whether or not it is allowed to add content other than text

      allowEmptySubmit: "=", // allows submitting empty messages
      showPersonas: "=", // show a persona drop-up

      /*
             * Usage:
             *  on-input="::myCallback(value, valid)"
             */
      onInput: "&",
      /*
             * Usage:
             *  on-paste="::myCallback(value, valid)"
             */
      onPaste: "&",

      submitButtonLabel: "=",
      /*
             * Usage:
             *  on-submit="::myCallback(value)"
             */
      onSubmit: "&",
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.chatTextfieldSimple", [
    labelledHrModule,
    autoresizingTextareaModule,
    submitButtonModule,
    ngAnimate,
  ])
  .directive("messageDetailElement", [
    "$compile",
    function($compile) {
      return {
        restrict: "A",
        scope: {
          onUpdate: "&",
          initialValue: "=",
          identifier: "=",
          detail: "=",
        },
        link: function(scope, element, attrs) {
          const customTag = attrs.messageDetailElement;
          if (!customTag) return;

          const customElem = angular.element(
            `<${customTag} initial-value="initialValue" on-update="internalUpdate(value)" detail="detail"></${customTag}>`
          );

          scope.internalUpdate = function(value) {
            scope.onUpdate({
              identifier: scope.identifier,
              value: value,
            });
          };
          element.append($compile(customElem)(scope));
        },
      };
    },
  ])
  .directive("chatTextfieldSimple", genComponentConf).name;
