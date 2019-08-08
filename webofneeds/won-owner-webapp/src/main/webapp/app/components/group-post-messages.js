import won from "../won-es6.js";
import angular from "angular";
import chatTextFieldModule from "./chat-textfield.js";
import connectionMessageModule from "./messages/connection-message.js";
import postContentMessageModule from "./messages/post-content-message.js";
import shareDropdownModule from "./share-dropdown.js";
import WonLabelledHr from "./labelled-hr.jsx";
import connectionContextDropdownModule from "./connection-context-dropdown.js";
import { connect2Redux } from "../configRedux.js";
import { delay, get, getIn } from "../utils.js";
import * as messageUtils from "../redux/utils/message-utils.js";
import * as connectionUtils from "../redux/utils/connection-utils.js";
import * as processUtils from "../redux/utils/process-utils.js";
import * as ownerApi from "../api/owner-api.js";
import { actionCreators } from "../actions/actions.js";
import {
  getConnectionUriFromRoute,
  getOwnedAtomByConnectionUri,
} from "../redux/selectors/general-selectors.js";
import { hasMessagesToLoad } from "../redux/selectors/connection-selectors.js";
import { getUnreadMessagesByConnectionUri } from "../redux/selectors/message-selectors.js";
import autoresizingTextareaModule from "../directives/textarea-autogrow.js";
import { attach, classOnComponentRoot } from "../cstm-ng-utils.js";
import WonConnectionHeader from "./connection-header.jsx";
import "~/style/_group-post-messages.scss";
import "~/style/_rdflink.scss";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];

function genComponentConf() {
  let template = `
        <div class="gpm__header">
            <div class="gpm__header__back">
                <a class="gpm__header__back__button clickable show-in-responsive"
                   ng-click="self.router__back()"> <!-- TODO: Clicking on the back button in non-mobile view might lead to some confusing changes -->
                    <svg class="gpm__header__back__button__icon">
                        <use xlink:href="#ico36_backarrow" href="#ico36_backarrow"></use>
                    </svg>
                </a>
                <a class="gpm__header__back__button clickable hide-in-responsive"
                   ng-click="self.router__stateGoCurrent({connectionUri : undefined})">
                    <svg class="gpm__header__back__button__icon">
                        <use xlink:href="#ico36_backarrow" href="#ico36_backarrow"></use>
                    </svg>
                </a>
            </div>
            <won-preact class="connectionHeader" component="self.WonConnectionHeader" props="{connectionUri: self.connectionUri}"></won-preact>
            <won-share-dropdown atom-uri="self.targetAtomUri"></won-share-dropdown>
            <won-connection-context-dropdown show-petri-net-data-field="" show-agreement-data-field=""></won-connection-context-dropdown>
        </div>
        <div
          class="gpm__content">
            <div class="gpm__content__unreadindicator"
              ng-if="self.unreadMessageCount && !self._snapBottom">
              <div class="gpm__content__unreadindicator__content won-button--filled red"
                ng-click="self.goToUnreadMessages()">
                {{self.unreadMessageCount}} unread Messages
              </div>
            </div>
            <won-post-content-message
              ng-if="self.targetAtomUri"
              post-uri="self.targetAtomUri"
              connection-uri="self.connectionUri">
            </won-post-content-message>
            <div class="gpm__content__loadspinner"
                ng-if="self.isProcessingLoadingMessages || self.isConnectionLoading">
                <svg class="hspinner">
                  <use xlink:href="#ico_loading_anim" href="#ico_loading_anim"></use>
                </svg>
            </div>
            <button class="gpm__content__loadbutton won-button--outlined thin red"
                ng-if="!self.isSuggested && !self.isProcessingLoadingMessages && !self.isConnectionLoading && self.hasConnectionMessagesToLoad"
                ng-click="self.loadPreviousMessages()">
                Load previous messages
            </button>

            <!-- CHATVIEW SPECIFIC CONTENT START-->
            <won-connection-message
                ng-repeat="msgUri in self.sortedMessageUris"
                connection-uri="::self.connectionUri"
                message-uri="::msgUri"
                group-chat-message="::true">
            </won-connection-message>
            <!-- CHATVIEW SPECIFIC CONTENT END-->

            <a class="rdflink clickable"
               ng-if="self.shouldShowRdf"
               target="_blank"
               href="{{ self.connectionUri }}">
                    <svg class="rdflink__small">
                        <use xlink:href="#rdf_logo_1" href="#rdf_logo_1"></use>
                    </svg>
                    <span class="rdflink__label">Connection</span>
            </a>
        </div>
        <div class="gpm__footer" ng-if="self.isConnected">
            <chat-textfield
                class="gpm__footer__chattexfield"
                connection-uri="self.connectionUri"
                placeholder="self.shouldShowRdf? 'Enter TTL...' : 'Your message...'"
                submit-button-label="self.shouldShowRdf? 'Send&#160;RDF' : 'Send'"
                on-submit="self.send(value, additionalContent, referencedContent, self.shouldShowRdf)"
                help-text="self.shouldShowRdf? self.rdfTextfieldHelpText : ''"
                allow-empty-submit="::false"
                allow-details="!self.shouldShowRdf"
                is-code="self.shouldShowRdf? 'true' : ''"
            >
            </chat-textfield>
        </div>
        <div class="gpm__footer" ng-if="self.isSentRequest">
            Waiting for the Group Administrator to accept your request.
        </div>

        <div class="gpm__footer" ng-if="self.isReceivedRequest">
            <chat-textfield
                class="gpm__footer__chattexfield"
                connection-uri="self.connectionUri"
                placeholder="::'Message (optional)'"
                on-submit="::self.openRequest(value)"
                allow-details="::false"
                allow-empty-submit="::true"
                submit-button-label="::'Accept&#160;Invite'"
            >
            </chat-textfield>
            <won-preact component="self.WonLabelledHr" class="labelledHr gpm__footer__labelledhr" props="{label: 'Or'}"></won-preact>
            <button class="gpm__footer__button won-button--filled black" ng-click="self.closeConnection()">
                Decline
            </button>
        </div>
        <div class="gpm__footer" ng-if="self.isSuggested">
            <chat-textfield
                placeholder="::'Message (optional)'"
                connection-uri="self.connectionUri"
                on-submit="::self.sendRequest(value, selectedPersona)"
                allow-details="::false"
                allow-empty-submit="::true"
                show-personas="!self.connection"
                submit-button-label="::'Ask&#160;to&#160;Join'"
            >
            </chat-textfield>
            <won-preact component="self.WonLabelledHr" class="labelledHr gpm__footer__labelledhr" props="{label: 'Or'}"></won-preact>
            <button class="gpm__footer__button won-button--filled black" ng-click="self.closeConnection(true)">
                Bad match - remove!
            </button>
        </div>
    `;

  class Controller {
    constructor(/* arguments = dependency injections */) {
      attach(this, serviceDependencies, arguments);
      window.pm4dbg = this;
      this.WonLabelledHr = WonLabelledHr;
      this.WonConnectionHeader = WonConnectionHeader;
      this.rdfTextfieldHelpText =
        "Expects valid turtle. " +
        `<${won.WONMSG.uriPlaceholder.event}> will ` +
        "be replaced by the uri generated for this message. " +
        "Use it, so your TTL can be found when parsing the messages. " +
        "See `won.defaultTurtlePrefixes` " +
        "for prefixes that will be added automatically. E.g." +
        `\`<${won.WONMSG.uriPlaceholder.event}> con:text "hello world!". \``;

      this.scrollContainer().addEventListener("scroll", e => this.onScroll(e));

      const selectFromState = state => {
        const connectionUri = getConnectionUriFromRoute(state);
        const ownedAtom = getOwnedAtomByConnectionUri(state, connectionUri);
        const connection = getIn(ownedAtom, ["connections", connectionUri]);
        const targetAtomUri = get(connection, "targetAtomUri");
        const targetAtom = getIn(state, ["atoms", targetAtomUri]);
        const allChatMessages = get(connection, "messages");
        const chatMessages =
          allChatMessages &&
          allChatMessages
            .filter(msg => !msg.getIn(["references", "forwards"])) //FILTER OUT ALL FORWARD MESSAGE ENVELOPES JUST IN CASE
            .filter(msg => !messageUtils.isAtomHintMessage(msg)) //FILTER OUT ALL HINT MESSAGES
            .filter(msg => !messageUtils.isSocketHintMessage(msg));
        const hasConnectionMessagesToLoad = hasMessagesToLoad(
          state,
          connectionUri
        );

        let sortedMessages = chatMessages && chatMessages.toArray();
        sortedMessages &&
          sortedMessages.sort(function(a, b) {
            const aDate = a.get("date");
            const bDate = b.get("date");

            const aTime = aDate && aDate.getTime();
            const bTime = bDate && bDate.getTime();

            return aTime - bTime;
          });

        const unreadMessages = getUnreadMessagesByConnectionUri(
          state,
          connectionUri
        );

        const process = get(state, "process");

        return {
          ownedAtom,
          targetAtom,
          targetAtomUri,
          connectionUri,
          connection,
          sortedMessageUris: sortedMessages && [
            ...sortedMessages.flatMap(msg => msg.get("uri")),
          ],
          chatMessages,
          unreadMessageCount: unreadMessages && unreadMessages.size,
          isProcessingLoadingMessages:
            connection &&
            processUtils.isConnectionLoadingMessages(process, connectionUri),
          lastUpdateTimestamp: connection && connection.get("lastUpdateDate"),
          isSentRequest:
            connection && connectionUtils.isRequestSent(connection),
          isReceivedRequest:
            connection && connectionUtils.isRequestReceived(connection),
          isConnected: connection && connectionUtils.isConnected(connection),
          isSuggested: connection && connectionUtils.isSuggested(connection),
          debugmode: won.debugmode,
          shouldShowRdf: state.getIn(["view", "showRdf"]),
          // if the connect-message is here, everything else should be as well
          hasConnectionMessagesToLoad,
          connectionOrAtomsLoading:
            !connection ||
            !targetAtom ||
            !ownedAtom ||
            processUtils.isAtomLoading(process, ownedAtom.get("uri")) ||
            processUtils.isAtomLoading(process, targetAtomUri) ||
            processUtils.isConnectionLoading(process, connectionUri),
          isConnectionLoading: processUtils.isConnectionLoading(
            process,
            connectionUri
          ),
        };
      };

      connect2Redux(selectFromState, actionCreators, [], this);

      this._snapBottom = true; //Don't snap to bottom immediately, because this scrolls the whole page... somehow?

      this.$scope.$watchGroup(["self.connection"], () => {
        this.ensureMessagesAreLoaded();
      });

      this.$scope.$watch(
        () => this.sortedMessageUris && this.sortedMessageUris.length, // trigger if there's messages added (or removed)
        () =>
          delay(0).then(() =>
            // scroll to bottom directly after rendering, if snapped
            this.updateScrollposition()
          )
      );

      classOnComponentRoot(
        "won-is-loading",
        () => this.connectionOrAtomsLoading,
        this
      );
    }

    ensureMessagesAreLoaded() {
      delay(0).then(() => {
        // make sure latest messages are loaded
        const INITIAL_MESSAGECOUNT = 15;
        if (
          this.connection &&
          !this.isConnectionLoading &&
          !this.isProcessingLoadingMessages &&
          this.connection.get("messages").size < INITIAL_MESSAGECOUNT &&
          this.hasConnectionMessagesToLoad
        ) {
          this.connections__showLatestMessages(
            this.connection.get("uri"),
            INITIAL_MESSAGECOUNT
          );
        }
      });
    }

    loadPreviousMessages() {
      delay(0).then(() => {
        const MORE_MESSAGECOUNT = 5;
        if (
          this.connection &&
          !this.isProcessingLoadingMessages &&
          !this.isConnectionLoading
        ) {
          this.connections__showMoreMessages(
            this.connection.get("uri"),
            MORE_MESSAGECOUNT
          );
        }
      });
    }

    goToUnreadMessages() {
      this.snapToBottom();
    }

    snapToBottom() {
      this._snapBottom = true;
      this.scrollToBottom();
    }
    unsnapFromBottom() {
      this._snapBottom = false;
    }
    updateScrollposition() {
      if (this._snapBottom) {
        this.scrollToBottom();
      }
    }
    scrollToBottom() {
      this._programmaticallyScrolling = true;

      this.scrollContainer().scrollTop = this.scrollContainer().scrollHeight;
    }
    onScroll() {
      if (!this._programmaticallyScrolling) {
        //only unsnap if the user scrolled themselves
        this.unsnapFromBottom();
      }

      const sc = this.scrollContainer();
      const isAtBottom = sc.scrollTop + sc.offsetHeight >= sc.scrollHeight;
      if (isAtBottom) {
        this.snapToBottom();
      }

      this._programmaticallyScrolling = false;
    }
    scrollContainer() {
      if (!this._scrollContainer) {
        this._scrollContainer = this.$element[0].querySelector(".gpm__content");
      }
      return this._scrollContainer;
    }

    send(chatMessage, additionalContent, referencedContent, isTTL = false) {
      this.view__hideAddMessageContent();

      const trimmedMsg = chatMessage.trim();
      if (trimmedMsg || additionalContent || referencedContent) {
        this.connections__sendChatMessage(
          trimmedMsg,
          additionalContent,
          referencedContent,
          this.connection.get("uri"),
          isTTL
        );
      }
    }

    addMessageToState(eventUri, key) {
      const ownedAtomUri = this.ownedAtom.get("uri");
      return ownerApi.getMessage(ownedAtomUri, eventUri).then(response => {
        won.wonMessageFromJsonLd(response).then(msg => {
          if (msg.isFromOwner() && msg.getRecipientAtom() === ownedAtomUri) {
            /*if we find out that the recipientatom of the crawled event is actually our
              atom we will call the method again but this time with the correct eventUri
            */
            this.addMessageToState(msg.getRemoteMessageUri(), key);
          } else {
            //If message isnt in the state we add it
            if (!this.chatMessages.get(eventUri)) {
              this.messages__processAgreementMessage(msg);
            }
          }
        });
      });
    }

    openRequest(message) {
      this.connections__open(this.connectionUri, message);
    }

    sendRequest(message, persona) {
      if (!this.connection) {
        this.router__stateGoResetParams("connections");

        if (this.targetAtomUri) {
          this.connections__connectAdHoc(this.targetAtomUri, message, persona);
        }

        //this.router__stateGoCurrent({connectionUri: null, sendAdHocRequest: null});
      } else {
        this.connections__rate(this.connectionUri, won.WONCON.binaryRatingGood);
        this.atoms__connect(
          this.ownedAtom.get("uri"),
          this.connectionUri,
          this.targetAtomUri,
          message
        );
        this.router__stateGoCurrent({ connectionUri: this.connectionUri });
      }
    }

    closeConnection(rateBad = false) {
      rateBad &&
        this.connections__rate(
          this.connection.get("uri"),
          won.WONCON.binaryRatingBad
        );
      this.connections__close(this.connection.get("uri"));
      this.router__stateGoCurrent({ connectionUri: null });
    }

    rateMatch(rating) {
      if (!this.isConnected) {
        return;
      }
      switch (rating) {
        case won.WONCON.binaryRatingGood:
          this.connections__rate(
            this.connectionUri,
            won.WONCON.binaryRatingGood
          );
          break;

        case won.WONCON.binaryRatingBad:
          this.connections__close(this.connectionUri);
          this.connections__rate(
            this.connectionUri,
            won.WONCON.binaryRatingBad
          );
          this.router__stateGoCurrent({ connectionUri: null });
          break;
      }
    }
  }
  Controller.$inject = serviceDependencies;

  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    scope: {},
    template: template,
  };
}

export default angular
  .module("won.owner.components.groupPostMessages", [
    autoresizingTextareaModule,
    chatTextFieldModule,
    connectionMessageModule,
    connectionContextDropdownModule,
    postContentMessageModule,
    shareDropdownModule,
  ])
  .directive("wonGroupPostMessages", genComponentConf).name;
