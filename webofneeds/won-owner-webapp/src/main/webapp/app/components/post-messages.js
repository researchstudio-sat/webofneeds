import won from "../won-es6.js";
import Immutable from "immutable";
import angular from "angular";
import chatTextFieldSimpleModule from "./chat-textfield-simple.js";
import connectionMessageModule from "./messages/connection-message.js";
import postContentMessageModule from "./messages/post-content-message.js";
import petrinetStateModule from "./petrinet-state.js";
import connectionHeaderModule from "./connection-header.js";
import labelledHrModule from "./labelled-hr.js";
import connectionContextDropdownModule from "./connection-context-dropdown.js";
import feedbackGridModule from "./feedback-grid.js";
import { connect2Redux } from "../won-utils.js";
import { attach, delay } from "../utils.js";
import {
  fetchAgreementProtocolUris,
  fetchPetriNetUris,
  fetchMessage,
} from "../won-message-utils.js";
import { actionCreators } from "../actions/actions.js";
import {
  selectOpenConnectionUri,
  selectNeedByConnectionUri,
  selectAgreementMessagesByConnectionUri,
  selectCancellationPendingMessagesByConnectionUri,
  selectProposalMessagesByConnectionUri,
  selectUnreadMessagesByConnectionUri,
} from "../selectors.js";
import autoresizingTextareaModule from "../directives/textarea-autogrow.js";
import { classOnComponentRoot } from "../cstm-ng-utils.js";

import "style/_post-messages.scss";
import "style/_rdflink.scss";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];

function genComponentConf() {
  let template = `
        <div class="pm__header" ng-if="self.showChatData">
            <a class="pm__header__back clickable"
               ng-click="self.router__stateGoCurrent({connectionUri : undefined})">
                <svg style="--local-primary:var(--won-primary-color);"
                     class="pm__header__back__icon clickable">
                    <use xlink:href="#ico36_backarrow" href="#ico36_backarrow"></use>
                </svg>
            </a>
            <won-connection-header
                connection-uri="self.connectionUri"
                timestamp="self.lastUpdateTimestamp"
                hide-image="::false">
            </won-connection-header>
            <won-connection-context-dropdown ng-if="self.isConnected || self.isSentRequest || self.isReceivedRequest || (self.isSuggested && self.connection.get('isRated'))" show-petri-net-data-field="::self.showPetriNetDataField()" show-agreement-data-field="::self.showAgreementDataField()"></won-connection-context-dropdown>
        </div>
        <div class="pm__header" ng-if="self.showAgreementData">
            <a class="pm__header__back clickable"
                ng-click="self.setShowAgreementData(false)">
                <svg style="--local-primary:var(--won-primary-color);"
                     class="pm__header__back__icon clickable">
                    <use xlink:href="#ico36_backarrow" href="#ico36_backarrow"></use>
                </svg>
            </a>
            <div class="pm__header__title clickable"
                ng-click="self.setShowAgreementData(false)">
              Showing Agreement Data
            </div>
            <won-connection-context-dropdown ng-if="self.isConnected || self.isSentRequest || self.isReceivedRequest || (self.isSuggested && self.connection.get('isRated'))" show-petri-net-data-field="::self.showPetriNetDataField()" show-agreement-data-field="::self.showAgreementDataField()"></won-connection-context-dropdown>
        </div>
        <div class="pm__header" ng-if="self.showPetriNetData">
            <a class="pm__header__back clickable"
                ng-click="self.setShowPetriNetData(false)">
                <svg style="--local-primary:var(--won-primary-color);"
                     class="pm__header__back__icon clickable">
                    <use xlink:href="#ico36_backarrow" href="#ico36_backarrow"></use>
                </svg>
            </a>
            <div class="pm__header__title clickable"
                ng-click="self.setShowAgreementData(false)">
              Showing PetriNet Data
            </div>
            <won-connection-context-dropdown ng-if="self.isConnected || self.isSentRequest || self.isReceivedRequest || (self.isSuggested && self.connection.get('isRated'))" show-petri-net-data-field="::self.showPetriNetDataField()" show-agreement-data-field="::self.showAgreementDataField()"></won-connection-context-dropdown>
        </div>
        <div
          class="pm__content"
          ng-class="{
            'won-agreement-content': self.showAgreementData,
            'won-petrinet-content': self.showPetriNetData,
          }">
            <div class="pm__content__unreadindicator"
              ng-if="self.unreadMessageCount && (!self._snapBottom || !self.showChatView)">
              <div class="pm__content__unreadindicator__content won-button--filled red"
                ng-click="self.goToUnreadMessages()">
                {{self.unreadMessageCount}} unread Messages
              </div>
            </div>
            <won-post-content-message
              class="won-cm--left"
              ng-if="self.showChatData && !self.multiSelectType && self.theirNeedUri"
              post-uri="self.theirNeedUri">
            </won-post-content-message>
            <div class="pm__content__loadspinner"
                ng-if="self.isLoadingMessages || (self.showAgreementData && self.isLoadingAgreementData) || (self.showPetriNetData && self.isLoadingPetriNetData)">
                <svg class="hspinner">
                  <use xlink:href="#ico_loading_anim" href="#ico_loading_anim"></use>
              </svg>
            </div>
            <div class="pm__content__agreement__loadingtext"  ng-if="self.showAgreementData && self.isLoadingAgreementData">
              Calculating Agreement Status
            </div>
            <div class="pm__content__petrinet__loadingtext"  ng-if="self.showPetriNetData && self.isLoadingPetriNetData">
              Calculating PetriNet Status
            </div>
            <button class="pm__content__loadbutton won-button--outlined thin red"
                ng-if="!self.isSuggested && self.showChatData && !self.isLoadingMessages && !self.allMessagesLoaded"
                ng-click="self.loadPreviousMessages()">
                Load previous messages
            </button>

            <!-- CHATVIEW SPECIFIC CONTENT START-->
            <won-connection-message
                ng-if="self.showChatData"
                ng-click="self.multiSelectType && self.selectMessage(msg)"
                ng-repeat="msg in self.sortedMessages"
                connection-uri="self.connectionUri"
                message-uri="msg.get('uri')">
            </won-connection-message>
            <!-- CHATVIEW SPECIFIC CONTENT END-->

            <!-- AGREEMENTVIEW SPECIFIC CONTENT START-->
            <div class="pm__content__agreement__emptytext"  ng-if="self.showAgreementData && !(self.hasAgreementMessages || self.hasCancellationPendingMessages || self.hasProposalMessages) && !self.isLoadingAgreementData">
              No Agreements within this Conversation
            </div>
            <div class="pm__content__agreement__title" ng-if="self.showAgreementData && self.hasAgreementMessages && !self.isLoadingAgreementData">
              Agreements
            </div>
            <won-connection-message
              ng-if="self.showAgreementData && !self.isLoadingAgreementData"
              ng-click="self.multiSelectType && self.selectMessage(agreement)"
              ng-repeat="agreement in self.agreementMessagesArray"
              connection-uri="self.connectionUri"
              message-uri="agreement.get('uri')">
            </won-connection-message>
            <div class="pm__content__agreement__title" ng-if="self.showAgreementData && self.hasCancellationPendingMessages && !self.isLoadingAgreementData">
              Agreements with Pending Cancellation
            </div>
            <won-connection-message
              ng-if="self.showAgreementData && !self.isLoadingAgreementData"
              ng-click="self.multiSelectType && self.selectMessage(proposesToCancel)"
              ng-repeat="proposesToCancel in self.cancellationPendingMessagesArray"
              connection-uri="self.connectionUri"
              message-uri="proposesToCancel.get('uri')">
            </won-connection-message>
            <div class="pm__content__agreement__title" ng-if="self.showAgreementData && self.hasProposalMessages && !self.isLoadingAgreementData">
              Open Proposals
            </div>
            <won-connection-message
              ng-if="self.showAgreementData && !self.isLoadingAgreementData"
              ng-click="self.multiSelectType && self.selectMessage(proposal)"
              ng-repeat="proposal in self.proposalMessagesArray"
              connection-uri="self.connectionUri"
              message-uri="proposal.get('uri')">
            </won-connection-message>
            <!-- AGREEMENTVIEW SPECIFIC CONTENT END-->

            <!-- PETRINETVIEW SPECIFIC CONTENT START -->
            <div class="pm__content__petrinet__emptytext"  ng-if="self.showPetriNetData && !self.isLoadingPetriNetData && !self.hasPetriNetData">
              No PetriNet Data within this Conversation
            </div>
            <div class="pm__content__petrinet__process"
              ng-if="self.showPetriNetData && !self.isLoadingPetriNetData && self.hasPetriNetData && process.get('processURI')"
              ng-repeat="process in self.petriNetDataArray">
              <div class="pm__content__petrinet__process__header">
                ProcessURI: {{ process.get('processURI') }}
              </div>
              <won-petrinet-state
                class="pm__content__petrinet__process__content"
                process-uri="process.get('processURI')">
              </won-petrinet-state>
            </div>
            <!-- PETRINETVIEW SPECIFIC CONTENT END -->

            <a class="rdflink clickable"
               ng-if="self.shouldShowRdf"
               target="_blank"
               href="{{ self.connection.get('uri') }}">
                    <svg class="rdflink__small">
                        <use xlink:href="#rdf_logo_1" href="#rdf_logo_1"></use>
                    </svg>
                    <span class="rdflink__label">Connection</span>
            </a>
        </div>
        <div class="pm__footer" ng-if="!self.showPetriNetData && self.isConnected">
            <chat-textfield-simple
                class="pm__footer__chattexfield"
                placeholder="self.shouldShowRdf? 'Enter TTL...' : 'Your message...'"
                submit-button-label="self.shouldShowRdf? 'Send RDF' : 'Send'"
                on-submit="self.send(value, additionalContent, referencedContent, self.shouldShowRdf)"
                help-text="self.shouldShowRdf? self.rdfTextfieldHelpText : ''"
                allow-empty-submit="::false"
                allow-details="!self.shouldShowRdf"
                is-code="self.shouldShowRdf? 'true' : ''"
            >
            </chat-textfield-simple>
        </div>
        <div class="pm__footer" ng-if="!self.showPetriNetData && !self.multiSelectType && self.isSentRequest">
            Waiting for them to accept your chat request.
        </div>

        <div class="pm__footer" ng-if="!self.showPetriNetData && !self.multiSelectType && self.isReceivedRequest">
            <chat-textfield-simple
                class="pm__footer__chattexfield"
                placeholder="::'Message (optional)'"
                on-submit="::self.openRequest(value)"
                allow-details="::false"
                allow-empty-submit="::true"
                submit-button-label="::'Accept Chat'"
            >
            </chat-textfield-simple>
            <won-labelled-hr label="::'Or'" class="pm__footer__labelledhr"></won-labelled-hr>
            <button class="pm__footer__button won-button--filled black" ng-click="self.closeConnection()">
                Decline
            </button>
        </div>
        <div class="pm__footer" ng-if="!self.showPetriNetData && !self.multiSelectType && self.isSuggested">
            <won-feedback-grid ng-if="self.connection && !self.connection.get('isRated')" connection-uri="self.connectionUri"></won-feedback-grid>

            <chat-textfield-simple
                placeholder="::'Message (optional)'"
                on-submit="::self.sendRequest(value)"
                allow-details="::false"
                allow-empty-submit="::true"
                submit-button-label="::'Ask to Chat'"
                ng-if="!self.connection || self.connection.get('isRated')"
            >
            </chat-textfield-simple>
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
        const connectionUri = selectOpenConnectionUri(state);
        const ownNeed = selectNeedByConnectionUri(state, connectionUri);
        const connection =
          ownNeed && ownNeed.getIn(["connections", connectionUri]);

        const theirNeedUri = connection && connection.get("remoteNeedUri");
        const theirNeed = theirNeedUri && state.getIn(["needs", theirNeedUri]);
        const chatMessages =
          connection &&
          connection.get("messages") &&
          connection.get("messages").filter(msg => !msg.get("forwardMessage"));
        const allMessagesLoaded =
          chatMessages &&
          chatMessages.filter(
            msg => msg.get("messageType") === won.WONMSG.connectMessage
          ).size > 0;

        const agreementData = connection && connection.get("agreementData");
        const petriNetData = connection && connection.get("petriNetData");

        const agreementMessages = selectAgreementMessagesByConnectionUri(
          state,
          connectionUri
        );
        const cancellationPendingMessages = selectCancellationPendingMessagesByConnectionUri(
          state,
          connectionUri
        );
        const proposalMessages = selectProposalMessagesByConnectionUri(
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

        const unreadMessages = selectUnreadMessagesByConnectionUri(
          state,
          connectionUri
        );

        const chatMessagesWithUnknownState =
          chatMessages &&
          chatMessages.filter(msg => !msg.get("isMessageStatusUpToDate"));

        return {
          ownNeed,
          theirNeed,
          theirNeedUri,
          connectionUri,
          connection,

          sortedMessages: sortedMessages,
          chatMessages,
          chatMessagesWithUnknownState,
          unreadMessageCount: unreadMessages && unreadMessages.size,
          isLoadingMessages: connection && connection.get("isLoadingMessages"),
          isLoadingAgreementData:
            connection && connection.get("isLoadingAgreementData"),
          isLoadingPetriNetData:
            connection && connection.get("isLoadingPetriNetData"),
          showAgreementData: connection && connection.get("showAgreementData"),
          showPetriNetData: connection && connection.get("showPetriNetData"),
          showChatData:
            connection &&
            !(
              connection.get("showAgreementData") ||
              connection.get("showPetriNetData")
            ),
          agreementData,
          petriNetData,
          petriNetDataArray:
            petriNetData &&
            petriNetData.get("data") &&
            petriNetData.get("data").toArray(),
          agreementDataLoaded: agreementData && agreementData.get("isLoaded"),
          petriNetDataLoaded: petriNetData && petriNetData.get("isLoaded"),
          multiSelectType: connection && connection.get("multiSelectType"),
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
          shouldShowRdf: state.get("showRdf"),
          // if the connect-message is here, everything else should be as well
          allMessagesLoaded,
          hasAgreementMessages: agreementMessages && agreementMessages.size > 0,
          hasPetriNetData:
            petriNetData &&
            petriNetData.get("data") &&
            petriNetData.get("data").size > 0,
          agreementMessagesArray:
            agreementMessages && agreementMessages.toArray(),
          hasProposalMessages: proposalMessages && proposalMessages.size > 0,
          proposalMessagesArray: proposalMessages && proposalMessages.toArray(),
          hasCancellationPendingMessages:
            cancellationPendingMessages && cancellationPendingMessages.size > 0,
          cancellationPendingMessagesArray:
            cancellationPendingMessages &&
            cancellationPendingMessages.toArray(),
        };
      };

      connect2Redux(selectFromState, actionCreators, [], this);

      this.snapToBottom();

      this.$scope.$watchGroup(["self.connection"], () => {
        this.ensureMessagesAreLoaded();
        this.ensureAgreementDataIsLoaded();
        this.ensurePetriNetDataIsLoaded();
        this.ensureMessageStateIsUpToDate();
      });

      this.$scope.$watch(
        () => this.sortedMessages && this.sortedMessages.length, // trigger if there's messages added (or removed)
        () =>
          delay(0).then(() =>
            // scroll to bottom directly after rendering, if snapped
            this.updateScrollposition()
          )
      );

      classOnComponentRoot("won-is-loading", () => this.isLoading(), this);
    }

    isLoading() {
      return (
        !this.connection ||
        !this.theirNeed ||
        !this.ownNeed ||
        this.ownNeed.get("isLoading") ||
        this.theirNeed.get("isLoading") ||
        this.connection.get("isLoading")
      );
    }

    ensureMessagesAreLoaded() {
      delay(0).then(() => {
        // make sure latest messages are loaded
        const INITIAL_MESSAGECOUNT = 15;
        if (
          this.connection &&
          !this.isLoadingMessages &&
          !(this.allMessagesLoaded || this.connection.get("messages").size > 0)
        ) {
          this.connections__showLatestMessages(
            this.connection.get("uri"),
            INITIAL_MESSAGECOUNT
          );
        }
      });
    }

    ensurePetriNetDataIsLoaded(forceFetch = false) {
      delay(0).then(() => {
        if (
          forceFetch ||
          (this.isConnected &&
            !this.isLoadingPetriNetData &&
            !this.petriNetDataLoaded)
        ) {
          const connectionUri = this.connection && this.connection.get("uri");

          this.connections__setLoadingPetriNetData({
            connectionUri: connectionUri,
            isLoadingPetriNetData: true,
          });

          fetchPetriNetUris(connectionUri)
            .then(response => {
              const petriNetData = {};

              response.forEach(entry => {
                if (entry.processURI) {
                  petriNetData[entry.processURI] = entry;
                }
              });

              const petriNetDataImm = Immutable.fromJS(petriNetData);

              this.connections__updatePetriNetData({
                connectionUri: connectionUri,
                petriNetData: petriNetDataImm,
              });
            })
            .catch(error => {
              console.error("Error:", error);
              this.connections__setLoadingPetriNetData({
                connectionUri: connectionUri,
                isLoadingPetriNetData: false,
              });
            });
        }
      });
    }

    ensureAgreementDataIsLoaded(forceFetch = false) {
      delay(0).then(() => {
        if (
          forceFetch ||
          (this.isConnected &&
            !this.isLoadingAgreementData &&
            !this.agreementDataLoaded)
        ) {
          this.connections__setLoadingAgreementData({
            connectionUri: this.connectionUri,
            isLoadingAgreementData: true,
          });

          fetchAgreementProtocolUris(this.connection.get("uri"))
            .then(response => {
              console.log("retrieved agreement Protocol Uris: ", response);
              const agreementData = Immutable.fromJS({
                agreementUris: Immutable.Set(response.agreementUris),
                pendingProposalUris: Immutable.Set(
                  response.pendingProposalUris
                ),
                acceptedCancellationProposalUris: Immutable.Set(
                  response.acceptedCancellationProposalUris
                ),
                cancellationPendingAgreementUris: Immutable.Set(
                  response.cancellationPendingAgreementUris
                ),
                pendingCancellationProposalUris: Immutable.Set(
                  response.pendingCancellationProposalUris
                ),
                cancelledAgreementUris: Immutable.Set(
                  response.cancelledAgreementUris
                ),
                rejectedMessageUris: Immutable.Set(
                  response.rejectedMessageUris
                ),
                retractedMessageUris: Immutable.Set(
                  response.retractedMessageUris
                ),
              });

              this.connections__updateAgreementData({
                connectionUri: this.connectionUri,
                agreementData: agreementData,
              });

              //Retrieve all the relevant messages
              agreementData.map((uriList, key) =>
                uriList.map(uri => this.addMessageToState(uri, key))
              );
            })
            .catch(error => {
              console.error("Error:", error);
              this.connections__setLoadingAgreementData({
                connectionUri: this.connectionUri,
                isLoadingAgreementData: false,
              });
            });
        }
      });
    }

    ensureMessageStateIsUpToDate() {
      delay(0).then(() => {
        if (
          this.isConnected &&
          !this.isLoadingAgreementData &&
          !this.isLoadingMessages &&
          this.agreementDataLoaded &&
          this.chatMessagesWithUnknownState &&
          this.chatMessagesWithUnknownState.size > 0
        ) {
          console.log(
            "Ensure Message Status is up-to-date for: ",
            this.chatMessagesWithUnknownState.size,
            " Messages"
          );
          this.chatMessagesWithUnknownState.forEach(msg => {
            let messageStatus = msg && msg.get("messageStatus");
            const msgUri = msg.get("uri");
            const remoteMsgUri = msg.get("remoteUri");

            const acceptedUris =
              this.agreementData && this.agreementData.get("agreementUris");
            const rejectedUris =
              this.agreementData &&
              this.agreementData.get("rejectedMessageUris");
            const retractedUris =
              this.agreementData &&
              this.agreementData.get("retractedMessageUris");
            const cancelledUris =
              this.agreementData &&
              this.agreementData.get("cancelledAgreementUris");
            const cancellationPendingUris =
              this.agreementData &&
              this.agreementData.get("cancellationPendingAgreementUris");

            const isAccepted = messageStatus && messageStatus.get("isAccepted");
            const isRejected = messageStatus && messageStatus.get("isRejected");
            const isRetracted =
              messageStatus && messageStatus.get("isRetracted");
            const isCancelled =
              messageStatus && messageStatus.get("isCancelled");
            const isCancellationPending =
              messageStatus && messageStatus.get("isCancellationPending");

            const isOldAccepted =
              (acceptedUris && acceptedUris.get(msgUri)) ||
              acceptedUris.get(remoteMsgUri);
            const isOldRejected =
              (rejectedUris && rejectedUris.get(msgUri)) ||
              rejectedUris.get(remoteMsgUri);
            const isOldRetracted =
              (retractedUris && retractedUris.get(msgUri)) ||
              retractedUris.get(remoteMsgUri);
            const isOldCancelled =
              (cancelledUris && cancelledUris.get(msgUri)) ||
              cancelledUris.get(remoteMsgUri);
            const isOldCancellationPending =
              (cancellationPendingUris &&
                cancellationPendingUris.get(msgUri)) ||
              cancellationPendingUris.get(remoteMsgUri);

            messageStatus = messageStatus
              .set("isAccepted", isAccepted || isOldAccepted)
              .set("isRejected", isRejected || isOldRejected)
              .set("isRetracted", isRetracted || isOldRetracted)
              .set("isCancelled", isCancelled || isOldCancelled)
              .set(
                "isCancellationPending",
                isCancellationPending || isOldCancellationPending
              );

            this.messages__updateMessageStatus({
              messageUri: msgUri,
              connectionUri: this.connectionUri,
              needUri: this.ownNeed.get("uri"),
              messageStatus: messageStatus,
            });
          });
        }
      });
    }

    loadPreviousMessages() {
      delay(0).then(() => {
        const MORE_MESSAGECOUNT = 5;
        if (this.connection && !this.isLoadingMessages) {
          this.connections__showMoreMessages(
            this.connection.get("uri"),
            MORE_MESSAGECOUNT
          );
        }
      });
    }

    goToUnreadMessages() {
      if (this.showAgreementData) {
        this.setShowAgreementData(false);
      }
      if (this.showPetriNetData) {
        this.setShowPetriNetData(false);
      }
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
        this._scrollContainer = this.$element[0].querySelector(".pm__content");
      }
      return this._scrollContainer;
    }

    send(chatMessage, additionalContent, referencedContent, isTTL = false) {
      this.setShowAgreementData(false);
      this.hideAddMessageContentDisplay();

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

    showAgreementDataField() {
      this.setShowPetriNetData(false);
      this.setShowAgreementData(true);
    }

    showPetriNetDataField() {
      this.setShowAgreementData(false);
      this.setShowPetriNetData(true);
    }

    setShowAgreementData(value) {
      this.connections__showAgreementData({
        connectionUri: this.connectionUri,
        showAgreementData: value,
      });
    }

    setShowPetriNetData(value) {
      this.connections__showPetriNetData({
        connectionUri: this.connectionUri,
        showPetriNetData: value,
      });
    }

    addMessageToState(eventUri, key) {
      console.log(
        "addMessageToState: key:[",
        key,
        "] eventUri: [",
        eventUri,
        "]"
      );
      const ownNeedUri = this.ownNeed.get("uri");
      return fetchMessage(ownNeedUri, eventUri).then(response => {
        won.wonMessageFromJsonLd(response).then(msg => {
          if (msg.isFromOwner() && msg.getReceiverNeed() === ownNeedUri) {
            console.log(
              "eventUri was from other try again with remoteMessageUri"
            );
            /*if we find out that the receiverneed of the crawled event is actually our
              need we will call the method again but this time with the correct eventUri
            */
            this.addMessageToState(msg.getRemoteMessageUri(), key);
          } else {
            //If message isnt in the state we add it
            if (!this.chatMessages.get(eventUri)) {
              console.log(
                "AgreementMessage not present in state, adding message: key:[",
                key,
                "] eventUri: [",
                eventUri,
                "]"
              );
              this.messages__processAgreementMessage(msg);
            } else {
              console.log(
                "AgreementMessage already present in state: key:[",
                key,
                "] eventUri: [",
                eventUri,
                "]"
              );
            }
          }
        });
      });
    }

    openRequest(message) {
      this.connections__open(this.connectionUri, message);
    }

    sendRequest(message) {
      const isOwnNeedWhatsX =
        this.ownNeed &&
        (this.ownNeed.get("isWhatsAround") || this.ownNeed.get("isWhatsNew"));

      if (!this.connection || isOwnNeedWhatsX) {
        this.router__stateGoResetParams("connections");

        if (isOwnNeedWhatsX) {
          //Close the connection if there was a present connection for a whatsaround need
          this.connections__close(this.connectionUri);
        }

        if (this.theirNeedUri) {
          this.connections__connectAdHoc(this.theirNeedUri, message);
        }

        //this.router__stateGoCurrent({connectionUri: null, sendAdHocRequest: null});
      } else {
        this.needs__connect(
          this.ownNeed.get("uri"),
          this.connectionUri,
          this.theirNeedUri,
          message
        );
        this.router__stateGoCurrent({ connectionUri: this.connectionUri });
      }
    }

    closeConnection() {
      this.connections__close(this.connection.get("uri"));
      this.router__stateGoCurrent({ connectionUri: null });
    }

    selectMessage(msg) {
      const selected = msg.get("isSelected");

      this.messages__setMessageSelected({
        messageUri: msg.get("uri"),
        connectionUri: this.connection.get("uri"),
        needUri: this.ownNeed.get("uri"),
        isSelected: !selected,
      });
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
  .module("won.owner.components.postMessages", [
    autoresizingTextareaModule,
    chatTextFieldSimpleModule,
    connectionMessageModule,
    connectionHeaderModule,
    labelledHrModule,
    connectionContextDropdownModule,
    feedbackGridModule,
    postContentMessageModule,
    petrinetStateModule,
  ])
  .directive("wonPostMessages", genComponentConf).name;
