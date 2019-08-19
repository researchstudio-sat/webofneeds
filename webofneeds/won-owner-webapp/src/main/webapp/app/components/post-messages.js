//THIS COMPONENT IS NOT USED ANYMORE AND CAN BE DELETED ONCE WE MOVED ALL THE RELEVANT PARTS TO atom-messages.jsx

import won from "../won-es6.js";
import Immutable from "immutable";
import angular from "angular";
import { delay, getIn } from "../utils.js";
import * as ownerApi from "../api/owner-api.js";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];

function genComponentConf() {
  let template = ``;

  class Controller {
    constructor(/* arguments = dependency injections */) {
      this.scrollContainer().addEventListener("scroll", e => this.onScroll(e));

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

          ownerApi
            .getPetriNetUris(connectionUri)
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
          ownerApi
            .getAgreementProtocolUris(this.connection.get("uri"))
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
          won.WONCON.binaryRatingGood
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
          won.WONCON.binaryRatingBad
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
  .module("won.owner.components.postMessages", [])
  .directive("wonPostMessages", genComponentConf).name;
