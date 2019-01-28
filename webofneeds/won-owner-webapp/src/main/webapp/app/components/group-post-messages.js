import won from "../won-es6.js";
import angular from "angular";
import chatTextFieldModule from "./chat-textfield.js";
import connectionMessageModule from "./messages/connection-message.js";
import postContentMessageModule from "./messages/post-content-message.js";
import connectionHeaderModule from "./connection-header.js";
import labelledHrModule from "./labelled-hr.js";
import connectionContextDropdownModule from "./connection-context-dropdown.js";
import { connect2Redux } from "../won-utils.js";
import { attach, delay, getIn, get } from "../utils.js";
import { isWhatsAroundNeed, isWhatsNewNeed } from "../need-utils.js";
import { fetchMessage } from "../won-message-utils.js";
import { actionCreators } from "../actions/actions.js";
import {
  getConnectionUriFromRoute,
  getOwnedNeedByConnectionUri,
} from "../selectors/general-selectors.js";
import { hasMessagesToLoad } from "../selectors/connection-selectors.js";
import { getUnreadMessagesByConnectionUri } from "../selectors/message-selectors.js";
import autoresizingTextareaModule from "../directives/textarea-autogrow.js";
import { classOnComponentRoot } from "../cstm-ng-utils.js";

import "style/_group-post-messages.scss";
import "style/_rdflink.scss";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];

function genComponentConf() {
  let template = `
        <div class="gpm__header">
            <a class="gpm__header__back clickable"
               ng-click="self.router__stateGoCurrent({connectionUri : undefined})">
                <svg style="--local-primary:var(--won-primary-color);"
                     class="gpm__header__back__icon clickable">
                    <use xlink:href="#ico36_backarrow" href="#ico36_backarrow"></use>
                </svg>
            </a>
            <won-connection-header
                connection-uri="self.connectionUri"
                timestamp="self.lastUpdateTimestamp"
                hide-image="::false">
            </won-connection-header>
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
              class="won-cm--left"
              ng-if="self.nonOwnedNeedUri"
              post-uri="self.nonOwnedNeedUri">
            </won-post-content-message>
            <div class="gpm__content__loadspinner"
                ng-if="self.isProcessingLoadingMessages">
                <svg class="hspinner">
                  <use xlink:href="#ico_loading_anim" href="#ico_loading_anim"></use>
                </svg>
            </div>
            <button class="gpm__content__loadbutton won-button--outlined thin red"
                ng-if="!self.isSuggested && !self.isProcessingLoadingMessages && self.hasConnectionMessagesToLoad"
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
            <won-labelled-hr label="::'Or'" class="gpm__footer__labelledhr"></won-labelled-hr>
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
                show-personas="self.isOwnedNeedWhatsX"
                submit-button-label="::'Ask&#160;to&#160;Join'"
            >
            </chat-textfield>
            <won-labelled-hr label="::'Or'" class="gpm__footer__labelledhr"></won-labelled-hr>
            <button class="gpm__footer__button won-button--filled black" ng-click="self.closeConnection(true)">
                Bad match - remove!
            </button>
        </div>
    `;

  class Controller {
    constructor(/* arguments = dependency injections */) {
      attach(this, serviceDependencies, arguments);
      window.pm4dbg = this;

      this.rdfTextfieldHelpText =
        "Expects valid turtle. " +
        `<${won.WONMSG.uriPlaceholder.event}> will ` +
        "be replaced by the uri generated for this message. " +
        "Use it, so your TTL can be found when parsing the messages. " +
        "See `won.defaultTurtlePrefixes` " +
        "for prefixes that will be added automatically. E.g." +
        `\`<${
          won.WONMSG.uriPlaceholder.event
        }> won:hasTextMessage "hello world!". \``;

      this.scrollContainer().addEventListener("scroll", e => this.onScroll(e));

      const selectFromState = state => {
        const connectionUri = getConnectionUriFromRoute(state);
        const ownedNeed = getOwnedNeedByConnectionUri(state, connectionUri);
        const connection = getIn(ownedNeed, ["connections", connectionUri]);
        const isOwnedNeedWhatsX =
          isWhatsAroundNeed(this.ownedNeed) || isWhatsNewNeed(this.ownedNeed);
        const nonOwnedNeedUri = get(connection, "remoteNeedUri");
        const nonOwnedNeed = getIn(state, ["needs", nonOwnedNeedUri]);
        const allChatMessages = get(connection, "messages");
        const chatMessages =
          allChatMessages &&
          allChatMessages.filter(msg => !msg.getIn(["references", "forwards"])); //FILTER OUT ALL FORWARD MESSAGES JUST IN CASE
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

        return {
          ownedNeed,
          nonOwnedNeed,
          nonOwnedNeedUri,
          connectionUri,
          connection,
          isOwnedNeedWhatsX,
          sortedMessageUris: sortedMessages && [
            ...sortedMessages.flatMap(msg => msg.get("uri")),
          ],
          chatMessages,
          unreadMessageCount: unreadMessages && unreadMessages.size,
          isProcessingLoadingMessages:
            connection &&
            getIn(state, [
              "process",
              "connections",
              connectionUri,
              "loadingMessages",
            ]),
          lastUpdateTimestamp: connection && connection.get("lastUpdateDate"),
          isSentRequest:
            connection && connection.get("state") === won.WON.RequestSent,
          isReceivedRequest:
            connection && connection.get("state") === won.WON.RequestReceived,
          isConnected:
            connection && connection.get("state") === won.WON.Connected,
          isSuggested:
            connection && connection.get("state") === won.WON.Suggested,
          debugmode: won.debugmode,
          shouldShowRdf: state.getIn(["view", "showRdf"]),
          // if the connect-message is here, everything else should be as well
          hasConnectionMessagesToLoad,
          connectionOrNeedsLoading:
            !connection ||
            !nonOwnedNeed ||
            !ownedNeed ||
            getIn(state, [
              "process",
              "needs",
              ownedNeed.get("uri"),
              "loading",
            ]) ||
            getIn(state, [
              "process",
              "needs",
              nonOwnedNeed.get("uri"),
              "loading",
            ]) ||
            getIn(state, ["process", "connections", connectionUri, "loading"]),
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
        () => this.connectionOrNeedsLoading,
        this
      );
    }

    ensureMessagesAreLoaded() {
      delay(0).then(() => {
        // make sure latest messages are loaded
        const INITIAL_MESSAGECOUNT = 15;
        if (
          this.connection &&
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
        if (this.connection && !this.isProcessingLoadingMessages) {
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
      const ownedNeedUri = this.ownedNeed.get("uri");
      return fetchMessage(ownedNeedUri, eventUri).then(response => {
        won.wonMessageFromJsonLd(response).then(msg => {
          if (msg.isFromOwner() && msg.getReceiverNeed() === ownedNeedUri) {
            /*if we find out that the receiverneed of the crawled event is actually our
              need we will call the method again but this time with the correct eventUri
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
      if (!this.connection || this.isOwnedNeedWhatsX) {
        this.router__stateGoResetParams("connections");

        if (this.isOwnedNeedWhatsX) {
          //Close the connection if there was a present connection for a whatsaround need
          this.connections__close(this.connectionUri);
        }

        if (this.nonOwnedNeedUri) {
          this.connections__connectAdHoc(
            this.nonOwnedNeedUri,
            message,
            persona
          );
        }

        //this.router__stateGoCurrent({connectionUri: null, sendAdHocRequest: null});
      } else {
        this.connections__rate(this.connectionUri, won.WON.binaryRatingGood);
        this.needs__connect(
          this.ownedNeed.get("uri"),
          this.connectionUri,
          this.nonOwnedNeedUri,
          message
        );
        this.router__stateGoCurrent({ connectionUri: this.connectionUri });
      }
    }

    closeConnection(rateBad = false) {
      rateBad &&
        this.connections__rate(
          this.connection.get("uri"),
          won.WON.binaryRatingBad
        );
      this.connections__close(this.connection.get("uri"));
      this.router__stateGoCurrent({ connectionUri: null });
    }

    rateMatch(rating) {
      if (!this.isConnected) {
        return;
      }
      switch (rating) {
        case won.WON.binaryRatingGood:
          this.connections__rate(this.connectionUri, won.WON.binaryRatingGood);
          break;

        case won.WON.binaryRatingBad:
          this.connections__close(this.connectionUri);
          this.connections__rate(this.connectionUri, won.WON.binaryRatingBad);
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
    connectionHeaderModule,
    labelledHrModule,
    connectionContextDropdownModule,
    postContentMessageModule,
  ])
  .directive("wonGroupPostMessages", genComponentConf).name;
