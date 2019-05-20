import won from "../won-es6.js";
import Immutable from "immutable";
import angular from "angular";
import chatTextFieldModule from "./chat-textfield.js";
import connectionMessageModule from "./messages/connection-message.js";
import postContentMessageModule from "./messages/post-content-message.js";
import petrinetStateModule from "./petrinet-state.js";
import connectionHeaderModule from "./connection-header.js";
import shareDropdownModule from "./share-dropdown.js";
import labelledHrModule from "./labelled-hr.js";
import connectionContextDropdownModule from "./connection-context-dropdown.js";
import { connect2Redux } from "../won-utils.js";
import { attach, delay, getIn, get } from "../utils.js";
import * as processUtils from "../process-utils.js";
import * as connectionUtils from "../connection-utils.js";
import * as messageUtils from "../message-utils.js";
import {
  fetchAgreementProtocolUris,
  fetchPetriNetUris,
  fetchMessage,
} from "../won-message-utils.js";
import { actionCreators } from "../actions/actions.js";
import * as generalSelectors from "../selectors/general-selectors.js";
import { hasMessagesToLoad } from "../selectors/connection-selectors.js";
import {
  getAgreementMessagesByConnectionUri,
  getCancellationPendingMessagesByConnectionUri,
  getProposalMessagesByConnectionUri,
  getUnreadMessagesByConnectionUri,
} from "../selectors/message-selectors.js";
import autoresizingTextareaModule from "../directives/textarea-autogrow.js";
import { classOnComponentRoot } from "../cstm-ng-utils.js";
import "~/style/_post-messages.scss";
import "~/style/_rdflink.scss";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];

function genComponentConf() {
  let template = `
        <div class="pm__header" ng-if="self.showChatData">
            <div class="pm__header__back">
              <a class="pm__header__back__button clickable show-in-responsive"
                 ng-if="!self.showOverlayConnection"
                 ng-click="self.router__back()">
                  <svg class="pm__header__back__button__icon">
                      <use xlink:href="#ico36_backarrow" href="#ico36_backarrow"></use>
                  </svg>
              </a>
              <a class="pm__header__back__button clickable hide-in-responsive"
                 ng-if="!self.showOverlayConnection"
                 ng-click="self.router__stateGoCurrent({connectionUri : undefined})">
                  <svg class="pm__header__back__button__icon">
                      <use xlink:href="#ico36_backarrow" href="#ico36_backarrow"></use>
                  </svg>
              </a>
              <a class="pm__header__back__button clickable"
                  ng-if="self.showOverlayConnection"
                  ng-click="self.router__back()">
                  <svg class="pm__header__back__button__icon clickable hide-in-responsive">
                      <use xlink:href="#ico36_close" href="#ico36_close"></use>
                  </svg>
                  <svg class="pm__header__back__button__icon clickable show-in-responsive">
                      <use xlink:href="#ico36_backarrow" href="#ico36_backarrow"></use>
                  </svg>
              </a>
            </div>
            <won-connection-header
                connection-uri="self.selectedConnectionUri">
            </won-connection-header>
            <won-share-dropdown atom-uri="self.targetAtomUri"></won-share-dropdown>
            <won-connection-context-dropdown show-petri-net-data-field="::self.showPetriNetDataField()" show-agreement-data-field="::self.showAgreementDataField()"></won-connection-context-dropdown>
        </div>
        <div class="pm__header" ng-if="self.showAgreementData">
            <div class="pm__header__back">
              <a class="pm__header__back__button clickable"
                  ng-click="self.setShowAgreementData(false)">
                  <svg class="pm__header__back__button__icon clickable">
                      <use xlink:href="#ico36_backarrow" href="#ico36_backarrow"></use>
                  </svg>
              </a>
            </div>
            <div class="pm__header__title clickable"
                ng-click="self.setShowAgreementData(false)">
              Showing Agreement Data
            </div>
            <won-connection-context-dropdown show-petri-net-data-field="::self.showPetriNetDataField()" show-agreement-data-field="::self.showAgreementDataField()"></won-connection-context-dropdown>
        </div>
        <div class="pm__header" ng-if="self.showPetriNetData">
            <div class="pm__header__back">
              <a class="pm__header__back__button clickable"
                  ng-click="self.setShowPetriNetData(false)">
                  <svg class="pm__header__back__button__icon clickable">
                      <use xlink:href="#ico36_backarrow" href="#ico36_backarrow"></use>
                  </svg>
              </a>
            </div>
            <div class="pm__header__title clickable"
                ng-click="self.setShowAgreementData(false)">
              Showing PetriNet Data
            </div>
            <won-connection-context-dropdown show-petri-net-data-field="::self.showPetriNetDataField()" show-agreement-data-field="::self.showAgreementDataField()"></won-connection-context-dropdown>
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
              ng-if="self.showPostContentMessage"
              post-uri="self.targetAtomUri"
              connection-uri="self.selectedConnectionUri">
            </won-post-content-message>
            <div class="pm__content__loadspinner"
                ng-if="self.isConnectionLoading || self.isProcessingLoadingMessages || (self.showAgreementData && self.isProcessingLoadingAgreementData) || (self.showPetriNetData && self.isProcessingLoadingPetriNetData && !self.hasPetriNetData)">
                <svg class="hspinner">
                  <use xlink:href="#ico_loading_anim" href="#ico_loading_anim"></use>
                </svg>
            </div>
            <div class="pm__content__agreement__loadingtext"  ng-if="self.showAgreementData && self.isProcessingLoadingAgreementData">
              Calculating Agreement Status
            </div>
            <div class="pm__content__petrinet__loadingtext"  ng-if="self.showPetriNetData && self.isProcessingLoadingPetriNetData && !self.hasPetriNetData">
              Calculating PetriNet Status
            </div>
            <button class="pm__content__loadbutton won-button--outlined thin red"
                ng-if="!self.isSuggested && self.showChatData && !self.isConnectionLoading && !self.isProcessingLoadingMessages && self.hasConnectionMessagesToLoad"
                ng-click="self.loadPreviousMessages()">
                Load previous messages
            </button>

            <!-- CHATVIEW SPECIFIC CONTENT START-->
            <won-connection-message
                ng-if="self.showChatData"
                ng-click="self.multiSelectType && self.selectMessage(msgUri)"
                ng-repeat="msgUri in self.sortedMessageUris"
                connection-uri="self.selectedConnectionUri"
                message-uri="::msgUri">
            </won-connection-message>
            <!-- CHATVIEW SPECIFIC CONTENT END-->

            <!-- AGREEMENTVIEW SPECIFIC CONTENT START-->
            <div class="pm__content__agreement__emptytext"  ng-if="self.showAgreementData && !(self.hasAgreementMessages || self.hasCancellationPendingMessages || self.hasProposalMessages) && !self.isProcessingLoadingAgreementData">
              No Agreements within this Conversation
            </div>
            <div class="pm__content__agreement__title" ng-if="self.showAgreementData && self.hasAgreementMessages && !self.isProcessingLoadingAgreementData">
              Agreements
            </div>
            <won-connection-message
              ng-if="self.showAgreementData && !self.isProcessingLoadingAgreementData"
              ng-click="self.multiSelectType && self.selectMessage(agreementUri)"
              ng-repeat="agreementUri in self.agreementMessageUris"
              connection-uri="self.selectedConnectionUri"
              message-uri="::agreementUri">
            </won-connection-message>
            <div class="pm__content__agreement__title" ng-if="self.showAgreementData && self.hasCancellationPendingMessages && !self.isProcessingLoadingAgreementData">
              Agreements with Pending Cancellation
            </div>
            <won-connection-message
              ng-if="self.showAgreementData && !self.isProcessingLoadingAgreementData"
              ng-click="self.multiSelectType && self.selectMessage(proposesToCancelUri)"
              ng-repeat="proposesToCancelUri in self.cancellationPendingMessageUris"
              connection-uri="self.selectedConnectionUri"
              message-uri="::proposesToCancelUri">
            </won-connection-message>
            <div class="pm__content__agreement__title" ng-if="self.showAgreementData && self.hasProposalMessages && !self.isProcessingLoadingAgreementData">
              Open Proposals
            </div>
            <won-connection-message
              ng-if="self.showAgreementData && !self.isProcessingLoadingAgreementData"
              ng-click="self.multiSelectType && self.selectMessage(proposalUri)"
              ng-repeat="proposalUri in self.proposalMessageUris"
              connection-uri="self.selectedConnectionUri"
              message-uri="::proposalUri">
            </won-connection-message>
            <!-- AGREEMENTVIEW SPECIFIC CONTENT END-->

            <!-- PETRINETVIEW SPECIFIC CONTENT START -->
            <div class="pm__content__petrinet__emptytext"  ng-if="self.showPetriNetData && !self.isProcessingLoadingPetriNetData && !self.hasPetriNetData">
              No PetriNet Data within this Conversation
            </div>
            <div class="pm__content__petrinet__process"
              ng-if="self.showPetriNetData && (!self.isProcessingLoadingPetriNetData || self.hasPetriNetData) && process.get('processURI')"
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
               href="{{ self.selectedConnectionUri }}">
                    <svg class="rdflink__small">
                        <use xlink:href="#rdf_logo_1" href="#rdf_logo_1"></use>
                    </svg>
                    <span class="rdflink__label">Connection</span>
            </a>
        </div>
        <div class="pm__footer" ng-if="!self.showPetriNetData && self.isConnected">
            <chat-textfield
                class="pm__footer__chattexfield"
                connection-uri="self.selectedConnectionUri"
                placeholder="self.shouldShowRdf? 'Enter TTL...' : 'Your message...'"
                submit-button-label="self.shouldShowRdf? 'Send&#160;RDF' : 'Send'"
                on-submit="::self.send(value, additionalContent, referencedContent, self.shouldShowRdf)"
                help-text="self.shouldShowRdf? self.rdfTextfieldHelpText : ''"
                allow-empty-submit="::false"
                allow-details="!self.shouldShowRdf"
                is-code="self.shouldShowRdf? 'true' : ''"
            >
            </chat-textfield>
        </div>
        <div class="pm__footer" ng-if="!self.showPetriNetData && !self.multiSelectType && self.isSentRequest">
            Waiting for them to accept your chat request.
        </div>

        <div class="pm__footer" ng-if="!self.showPetriNetData && !self.multiSelectType && self.isReceivedRequest">
            <chat-textfield
                class="pm__footer__chattexfield"
                connection-uri="self.selectedConnectionUri"
                placeholder="::'Message (optional)'"
                on-submit="::self.openRequest(value)"
                allow-details="::false"
                allow-empty-submit="::true"
                submit-button-label="::'Accept&#160;Chat'"
            >
            </chat-textfield>
            <won-labelled-hr label="::'Or'" class="pm__footer__labelledhr"></won-labelled-hr>
            <button class="pm__footer__button won-button--filled black" ng-click="self.closeConnection()">
                Decline
            </button>
        </div>
        <div class="pm__footer" ng-if="!self.showPetriNetData && !self.multiSelectType && self.isSuggested">
            <chat-textfield
                placeholder="::'Message (optional)'"
                connection-uri="self.selectedConnectionUri"
                on-submit="::self.sendRequest(value, selectedPersona)"
                allow-details="::false"
                allow-empty-submit="::true"
                show-personas="!self.connection"
                submit-button-label="::'Ask&#160;to&#160;Chat'"
            >
            </chat-textfield>
            <won-labelled-hr label="::'Or'" class="pm__footer__labelledhr"></won-labelled-hr>
            <button class="pm__footer__button won-button--filled black" ng-click="self.closeConnection(true)">
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
        }> won:textMessage "hello world!". \``;

      this.scrollContainer().addEventListener("scroll", e => this.onScroll(e));

      const selectFromState = state => {
        const selectedConnectionUri = this.connectionUri
          ? this.connectionUri
          : generalSelectors.getConnectionUriFromRoute(state);
        const ownedAtom = generalSelectors.getOwnedAtomByConnectionUri(
          state,
          selectedConnectionUri
        );
        const connection =
          ownedAtom && ownedAtom.getIn(["connections", selectedConnectionUri]);
        const targetAtomUri = connection && connection.get("targetAtomUri");
        const targetAtom =
          targetAtomUri && state.getIn(["atoms", targetAtomUri]);
        const chatMessages =
          connection &&
          connection.get("messages") &&
          connection
            .get("messages")
            .filter(msg => !msg.get("forwardMessage"))
            .filter(msg => !messageUtils.isHintMessage(msg));
        const hasConnectionMessagesToLoad = hasMessagesToLoad(
          state,
          selectedConnectionUri
        );

        const agreementData = connection && connection.get("agreementData");
        const petriNetData = connection && connection.get("petriNetData");

        const agreementMessages = getAgreementMessagesByConnectionUri(
          state,
          selectedConnectionUri
        );
        const cancellationPendingMessages = getCancellationPendingMessagesByConnectionUri(
          state,
          selectedConnectionUri
        );
        const proposalMessages = getProposalMessagesByConnectionUri(
          state,
          selectedConnectionUri
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
          selectedConnectionUri
        );

        const chatMessagesWithUnknownState =
          chatMessages &&
          chatMessages.filter(msg => !msg.get("isMessageStatusUpToDate"));

        const showChatData =
          connection &&
          !(
            connection.get("showAgreementData") ||
            connection.get("showPetriNetData")
          );

        const multiSelectType = connection && connection.get("multiSelectType");

        const process = get(state, "process");

        return {
          ownedAtom,
          targetAtom,
          targetAtomUri,
          selectedConnectionUri,
          connection,
          sortedMessageUris: sortedMessages && [
            ...sortedMessages.flatMap(msg => msg.get("uri")),
          ],
          chatMessages,
          chatMessagesWithUnknownState,
          unreadMessageCount: unreadMessages && unreadMessages.size,
          isProcessingLoadingMessages:
            connection &&
            processUtils.isConnectionLoadingMessages(
              process,
              selectedConnectionUri
            ),
          isProcessingLoadingAgreementData:
            connection &&
            processUtils.isConnectionAgreementDataLoading(
              process,
              selectedConnectionUri
            ),
          isProcessingLoadingPetriNetData:
            connection &&
            processUtils.isConnectionPetriNetDataLoading(
              process,
              selectedConnectionUri
            ),
          showAgreementData: connection && connection.get("showAgreementData"),
          showPetriNetData: connection && connection.get("showPetriNetData"),
          showChatData,
          agreementData,
          petriNetData,
          petriNetDataArray: petriNetData && petriNetData.toArray(),
          agreementDataLoaded:
            agreementData &&
            processUtils.isConnectionAgreementDataLoaded(
              process,
              selectedConnectionUri
            ),
          petriNetDataLoaded:
            petriNetData &&
            processUtils.isConnectionPetriNetDataLoaded(
              process,
              selectedConnectionUri
            ),
          multiSelectType,
          lastUpdateTimestamp: connection && connection.get("lastUpdateDate"),
          isSentRequest: connectionUtils.isRequestSent(connection),
          isReceivedRequest: connectionUtils.isRequestReceived(connection),
          isConnected: connectionUtils.isConnected(connection),
          isSuggested: connectionUtils.isSuggested(connection),
          debugmode: won.debugmode,
          shouldShowRdf: state.getIn(["view", "showRdf"]),
          hasConnectionMessagesToLoad,
          hasAgreementMessages: agreementMessages && agreementMessages.size > 0,
          hasPetriNetData: petriNetData && petriNetData.size > 0,
          agreementMessageUris:
            agreementMessages && agreementMessages.toArray(),
          hasProposalMessages: proposalMessages && proposalMessages.size > 0,
          proposalMessageUris: proposalMessages && proposalMessages.toArray(),
          hasCancellationPendingMessages:
            cancellationPendingMessages && cancellationPendingMessages.size > 0,
          cancellationPendingMessageUris:
            cancellationPendingMessages &&
            cancellationPendingMessages.toArray(),
          connectionOrAtomsLoading:
            !connection ||
            !targetAtom ||
            !ownedAtom ||
            processUtils.isAtomLoading(process, ownedAtom.get("uri")) ||
            processUtils.isAtomLoading(process, targetAtomUri) ||
            processUtils.isConnectionLoading(process, selectedConnectionUri),
          isConnectionLoading: processUtils.isConnectionLoading(
            process,
            selectedConnectionUri
          ),
          showPostContentMessage:
            showChatData && !multiSelectType && targetAtomUri,
          showOverlayConnection: !!this.connectionUri,
        };
      };

      connect2Redux(
        selectFromState,
        actionCreators,
        ["self.connectionUri"],
        this
      );

      this._snapBottom = true; //Don't snap to bottom immediately, because this scrolls the whole page... somehow?

      this.$scope.$watchGroup(["self.connection"], () => {
        this.ensureMessagesAreLoaded();
        this.ensureAgreementDataIsLoaded();
        this.ensurePetriNetDataIsLoaded();
        this.ensureMessageStateIsUpToDate();
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

    ensurePetriNetDataIsLoaded(forceFetch = false) {
      delay(0).then(() => {
        if (
          forceFetch ||
          (this.isConnected &&
            !this.isProcessingLoadingPetriNetData &&
            !this.petriNetDataLoaded)
        ) {
          const connectionUri = this.connection && this.connection.get("uri");

          this.connections__setLoadingPetriNetData({
            connectionUri: connectionUri,
            loadingPetriNetData: true,
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
                loadingPetriNetData: false,
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
            !this.isProcessingLoadingAgreementData &&
            !this.agreementDataLoaded)
        ) {
          this.connections__setLoadingAgreementData({
            connectionUri: this.selectedConnectionUri,
            loadingAgreementData: true,
          });
          fetchAgreementProtocolUris(this.connection.get("uri"))
            .then(response => {
              let proposedMessageUris = [];
              const pendingProposals = response.pendingProposals;

              if (pendingProposals) {
                pendingProposals.forEach(prop => {
                  if (prop.proposes) {
                    proposedMessageUris = proposedMessageUris.concat(
                      prop.proposes
                    );
                  }
                });
              }

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
                proposedMessageUris: Immutable.Set(proposedMessageUris),
                claimedMessageUris: Immutable.Set(response.claimedMessageUris),
              });

              this.connections__updateAgreementData({
                connectionUri: this.selectedConnectionUri,
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
                connectionUri: this.selectedConnectionUri,
                loadingAgreementData: false,
              });
            });
        }
      });
    }

    ensureMessageStateIsUpToDate() {
      delay(0).then(() => {
        if (
          this.isConnected &&
          !this.isConnectionLoading &&
          !this.isProcessingLoadingAgreementData &&
          !this.isProcessingLoadingMessages &&
          this.agreementDataLoaded &&
          this.chatMessagesWithUnknownState &&
          this.chatMessagesWithUnknownState.size > 0
        ) {
          console.debug(
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
            const claimedUris =
              this.agreementData &&
              this.agreementData.get("claimedMessageUris"); //TODO not sure if this is correct
            const proposedUris =
              this.agreementData &&
              this.agreementData.get("proposedMessageUris"); //TODO not sure if this is correct

            const isProposed = messageStatus && messageStatus.get("isProposed");
            const isClaimed = messageStatus && messageStatus.get("isClaimed");
            const isAccepted = messageStatus && messageStatus.get("isAccepted");
            const isRejected = messageStatus && messageStatus.get("isRejected");
            const isRetracted =
              messageStatus && messageStatus.get("isRetracted");
            const isCancelled =
              messageStatus && messageStatus.get("isCancelled");
            const isCancellationPending =
              messageStatus && messageStatus.get("isCancellationPending");

            const isOldProposed =
              proposedUris &&
              !!(proposedUris.get(msgUri) || proposedUris.get(remoteMsgUri));
            const isOldClaimed =
              claimedUris &&
              !!(claimedUris.get(msgUri) || claimedUris.get(remoteMsgUri));
            const isOldAccepted =
              acceptedUris &&
              !!(acceptedUris.get(msgUri) || acceptedUris.get(remoteMsgUri));
            const isOldRejected =
              rejectedUris &&
              !!(rejectedUris.get(msgUri) || rejectedUris.get(remoteMsgUri));
            const isOldRetracted =
              retractedUris &&
              !!(retractedUris.get(msgUri) || retractedUris.get(remoteMsgUri));
            const isOldCancelled =
              cancelledUris &&
              !!(cancelledUris.get(msgUri) || cancelledUris.get(remoteMsgUri));
            const isOldCancellationPending =
              cancellationPendingUris &&
              !!(
                cancellationPendingUris.get(msgUri) ||
                cancellationPendingUris.get(remoteMsgUri)
              );

            messageStatus = messageStatus
              .set("isProposed", isProposed || isOldProposed)
              .set("isClaimed", isClaimed || isOldClaimed)
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
              connectionUri: this.selectedConnectionUri,
              atomUri: this.ownedAtom.get("uri"),
              messageStatus: messageStatus,
            });
          });
        }
      });
    }

    loadPreviousMessages() {
      delay(0).then(() => {
        const MORE_MESSAGECOUNT = 5;
        if (
          this.connection &&
          !this.isConnectionLoading &&
          !this.isProcessingLoadingMessages
        ) {
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
        connectionUri: this.selectedConnectionUri,
        showAgreementData: value,
      });
    }

    setShowPetriNetData(value) {
      this.connections__showPetriNetData({
        connectionUri: this.selectedConnectionUri,
        showPetriNetData: value,
      });
    }

    addMessageToState(eventUri, key) {
      const ownedAtomUri = this.ownedAtom.get("uri");
      return fetchMessage(ownedAtomUri, eventUri).then(response => {
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
      this.connections__open(this.selectedConnectionUri, message);
    }

    sendRequest(message, persona) {
      if (!this.connection) {
        this.router__stateGoResetParams("connections");

        if (this.targetAtomUri) {
          this.connections__connectAdHoc(this.targetAtomUri, message, persona);
        }

        //this.router__stateGoCurrent({connectionUri: null, sendAdHocRequest: null});
      } else {
        this.connections__rate(
          this.selectedConnectionUri,
          won.WON.binaryRatingGood
        );
        this.atoms__connect(
          this.ownedAtom.get("uri"),
          this.selectedConnectionUri,
          this.targetAtomUri,
          message
        );
        if (this.showOverlayConnection) {
          this.router__back();
        } else {
          this.router__stateGoCurrent({
            connectionUri: this.selectedConnectionUri,
          });
        }
      }
    }

    closeConnection(rateBad = false) {
      rateBad &&
        this.connections__rate(
          this.connection.get("uri"),
          won.WON.binaryRatingBad
        );
      this.connections__close(this.connection.get("uri"));

      if (this.showOverlayConnection) {
        this.router__back();
      } else {
        this.router__stateGoCurrent({ connectionUri: null });
      }
    }

    selectMessage(msgUri) {
      const msg = getIn(this.connection, ["messages", msgUri]);

      if (msg) {
        this.messages__viewState__markAsSelected({
          messageUri: msgUri,
          connectionUri: this.connection.get("uri"),
          atomUri: this.ownedAtom.get("uri"),
          isSelected: !msg.getIn(["viewState", "isSelected"]),
        });
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
      connectionUri: "=",
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.postMessages", [
    autoresizingTextareaModule,
    chatTextFieldModule,
    connectionMessageModule,
    connectionHeaderModule,
    labelledHrModule,
    connectionContextDropdownModule,
    postContentMessageModule,
    petrinetStateModule,
    shareDropdownModule,
  ])
  .directive("wonPostMessages", genComponentConf).name;
