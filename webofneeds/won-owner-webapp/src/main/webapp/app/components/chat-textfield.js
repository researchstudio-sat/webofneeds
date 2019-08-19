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
import { delay, dispatchEvent } from "../utils.js";
import { attach } from "../cstm-ng-utils.js";
import * as useCaseUtils from "../usecase-utils.js";
import autoresizingTextareaModule from "../directives/textarea-autogrow.js";
import WonLabelledHr from "./labelled-hr.jsx";
import { getHumanReadableStringFromMessage } from "../reducers/atom-reducer/parse-message.js";

import { Elm } from "../../elm/PublishButton.elm";
import elmModule from "./elm.js";

import "~/style/_chattextfield.scss";

function genComponentConf() {
  let template = `
      <!-- DETAILS DRAWER START -->  
        <div class="cts__details"
          ng-if="self.allowDetails && self.showAddMessageContent">
          <div class="cts__details__grid"
              ng-if="!self.selectedDetail && !self.multiSelectType">
            <won-preact component="self.WonLabelledHr" class="labelledHr cts__details__grid__hr" ng-if="!self.multiSelectType && self.isConnected && !self.isChatToGroupConnection" props="{label: 'Actions'}"></won-preact>
            <button
                ng-if="!self.showAgreementData && !self.isChatToGroupConnection"
                class="cts__details__grid__action won-button--filled red"
                ng-click="self.activateMultiSelect('proposes')"
                ng-disabled="!self.hasProposableMessages">
                Make Proposal
            </button>
            <button
                ng-if="!self.showAgreementData && !self.isChatToGroupConnection"
                class="cts__details__grid__action won-button--filled red"
                ng-click="self.activateMultiSelect('claims')"
                ng-disabled="!self.hasClaimableMessages">
                Make Claim
            </button>
            <button
                ng-if="self.showAgreementData && !self.isChatToGroupConnection"
                class="cts__details__grid__action won-button--filled red"
                ng-click="self.activateMultiSelect('accepts')"
                ng-disabled="!self.hasAcceptableMessages">
                Accept Proposal(s)
            </button>
            <button
                ng-if="self.showAgreementData && !self.isChatToGroupConnection"
                class="cts__details__grid__action won-button--filled red"
                ng-click="self.activateMultiSelect('rejects')"
                ng-disabled="!self.hasRejectableMessages">
                Reject Proposal(s)
            </button>
            <button
                class="cts__details__grid__action won-button--filled red"
                ng-if="!self.isChatToGroupConnection"
                ng-click="self.activateMultiSelect('proposesToCancel')"
                ng-disabled="!self.hasCancelableMessages">
                Cancel Agreement(s)
            </button>
            <button class="cts__details__grid__action won-button--filled red"
                ng-if="!self.isChatToGroupConnection"
                ng-click="self.activateMultiSelect('retracts')"
                ng-disabled="!self.hasRetractableMessages">
                Retract Message(s)
            </button>
            <won-preact component="self.WonLabelledHr" class="labelledHr cts__details__grid__hr" ng-if="!self.multiSelectType && self.isConnected" props="{label: 'Details'}"></won-preact>
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
                ng-click="self.view__removeAddMessageContent()">
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
          <won-elm
            class="cts__submitbutton"
            module="self.publishButton"
            props="{
              buttonEnabled: self.valid(),
              showPersonas: self.showPersonasSelection,
              personas: self.personas,
              label: self.submitButtonLabel
            }"
            on-publish="self.submit(personaId)" >
          </won-elm>
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
      this.allMessageDetails = useCaseUtils.getAllMessageDetails();

      this.WonLabelledHr = WonLabelledHr;
      this.draftObject = {};
      this.publishButton = Elm.PublishButton;
      this.additionalContent = new Map(); //Stores the additional Detail content of a message
      this.referencedContent = new Map(); //Stores the reference Content of a message (e.g. proposes, retracts...)

      // keep up-to-date on whether we need to show personas or not
      this.showPersonasSelection = this.showPersonas || false;
      this.$scope.$watch("self.showPersonas", newValue => {
        this.showPersonasSelection = newValue || false;
      });

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
          this.view__hideAddMessageContent();
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
            atomUri: this.post.get("uri"),
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
        atomUri: this.post.get("uri"),
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
      this.view__toggleAddMessageContent();
    }
  }
  Controller.$inject = serviceDependencies;

  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl

    template: template,
  };
}

export default angular
  .module("won.owner.components.chatTextfield", [
    autoresizingTextareaModule,
    elmModule,
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
  .directive("chatTextfield", genComponentConf).name;
