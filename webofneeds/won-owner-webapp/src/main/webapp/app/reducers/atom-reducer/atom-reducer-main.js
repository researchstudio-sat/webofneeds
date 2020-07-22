/**
 * Created by syim on 11.12.2015.
 */
import { actionTypes } from "../../actions/actions.js";
import Immutable from "immutable";
import vocab from "../../service/vocab.js";
import {
  get,
  getIn,
  msStringToDate,
  extractAtomUriBySocketUri,
} from "../../utils.js";
import {
  addAtom,
  addAtomInCreation,
  addAtomStub,
  addMetaAtomStubs,
  deleteAtom,
} from "./reduce-atoms.js";
import {
  addExistingMessages,
  addMessage,
  markMessageAsAccepted,
  markMessageAsAgreed,
  markMessageAsCancellationPending,
  markMessageAsCancelled,
  markMessageAsClaimed,
  markMessageAsCollapsed,
  markMessageAsProposed,
  markMessageAsRead,
  markMessageAsRejected,
  markMessageAsRetracted,
  markMessageAsSelected,
  markMessageExpandReferences,
  markMessageShowActions,
  updateMessageStatus,
} from "./reduce-messages.js";
import {
  addMetaConnections,
  changeConnectionState,
  changeConnectionStateByFun,
  getAtomByConnectionUri,
  markConnectionAsRated,
  markConnectionAsRead,
  setMultiSelectType,
  setShowAgreementData,
  setShowPetriNetData,
  storeConnectionsData,
  updateAgreementStateData,
  updateAgreementStateDataset,
  updatePetriNetStateData,
} from "./reduce-connections.js";
import * as atomUtils from "../../redux/utils/atom-utils.js";
import * as connectionUtils from "../../redux/utils/connection-utils.js";

const initialState = Immutable.fromJS({});

export default function(allAtomsInState = initialState, action = {}) {
  switch (action.type) {
    case actionTypes.account.reset:
      return initialState;

    case actionTypes.account.loginStarted:
      // starting a new login process. this could mean switching
      // to a different session. we need to remove all connections
      // that are already of loaded atoms loaded.
      return allAtomsInState.map(atom =>
        atom.set("connections", Immutable.Map())
      );

    case actionTypes.atoms.storeMetaAtoms:
    case actionTypes.atoms.storeWhatsNew:
    case actionTypes.atoms.storeWhatsAround:
    case actionTypes.atoms.storeOwnedMetaAtoms: {
      return addMetaAtomStubs(allAtomsInState, action.payload.get("metaAtoms"));
    }

    case actionTypes.atoms.storeUriInLoading: {
      return addAtomStub(allAtomsInState, action.payload.get("uri"));
    }

    case actionTypes.atoms.store: {
      let atoms = action.payload.get("atoms");
      atoms = atoms ? atoms : Immutable.Set();

      return atoms.reduce(
        (updatedState, atom) => addAtom(updatedState, atom),
        allAtomsInState
      );
    }

    case actionTypes.connections.storeMetaConnections: {
      return addMetaConnections(
        allAtomsInState,
        action.payload.get("atomUri"),
        action.payload.get("connections")
      );
    }

    case actionTypes.connections.storeActive: {
      return storeConnectionsData(
        allAtomsInState,
        action.payload.get("connections")
      );
    }

    case actionTypes.atoms.reopen:
      return allAtomsInState.setIn(
        [action.payload.ownedAtomUri, "state"],
        vocab.WON.ActiveCompacted
      );

    case actionTypes.atoms.close:
      return allAtomsInState.setIn(
        [action.payload.ownedAtomUri, "state"],
        vocab.WON.InactiveCompacted
      );

    case actionTypes.atoms.removeDeleted:
    case actionTypes.atoms.delete:
      return deleteAtom(allAtomsInState, action.payload.get("uri"));

    case actionTypes.atoms.edit: {
      console.debug(
        "payload = {eventUri, message, atomUri, atom: draft, oldAtom}"
      );
      console.debug(
        "atom-reducer-main actionTypes.atoms.edit todo: impl / payload-> ",
        action.payload
      );
      //TODO: IMPL Optimistic change
      return allAtomsInState;
    }

    case actionTypes.atoms.create: // optimistic atom adding
      return addAtomInCreation(
        allAtomsInState,
        action.payload.atom,
        action.payload.atomUri
      );

    case actionTypes.atoms.editFailure: {
      console.debug(
        "atom-reducer-main actionTypes.atoms.editFailure todo: impl / payload-> ",
        action.payload
      );
      //TODO: IMPL change
      return allAtomsInState;
    }

    case actionTypes.atoms.editSuccessful: {
      console.debug(
        "atom-reducer-main actionTypes.atoms.editSuccessful todo: impl / payload-> ",
        action.payload
      );
      //TODO: IMPL change
      return allAtomsInState;
    }

    case actionTypes.atoms.createSuccessful:
      return addAtom(allAtomsInState, action.payload.atom);

    case actionTypes.messages.connectMessageReceived: {
      const ownedAtomFromState = allAtomsInState.get(
        action.payload.ownedAtomUri
      );

      if (!ownedAtomFromState) {
        throw new Error("Open or connect received for non owned hint!");
      }

      if (action.payload.message) {
        allAtomsInState = addMessage(allAtomsInState, action.payload.message);
      }
      allAtomsInState = changeConnectionStateByFun(
        allAtomsInState,
        action.payload.updatedConnectionUri,
        state => {
          if (!state) return vocab.WON.RequestReceived; //fallback if no state present
          if (state === vocab.WON.Connected) return vocab.WON.Connected; //stay in connected if it was already the case
          if (state === vocab.WON.RequestSent) return vocab.WON.Connected;
          if (state === vocab.WON.Suggested) return vocab.WON.RequestReceived;
          if (state === vocab.WON.Closed) return vocab.WON.RequestReceived;
          return vocab.WON.RequestReceived;
        }
      );
      return allAtomsInState;
    }

    // NEW CONNECTIONS STATE UPDATES
    case actionTypes.connections.close:
      return changeConnectionState(
        allAtomsInState,
        action.payload.connectionUri,
        vocab.WON.Closed
      );

    case actionTypes.atoms.connectSockets: {
      const messageUri = action.payload.eventUri;
      const wonMessage = action.payload.optimisticEvent;
      const senderSocketUri = action.payload.senderSocketUri;
      const targetSocketUri = action.payload.targetSocketUri;

      const senderAtomUri = extractAtomUriBySocketUri(senderSocketUri);
      const targetAtomUri = extractAtomUriBySocketUri(targetSocketUri);
      const senderAtom = get(allAtomsInState, senderAtomUri);
      const affectedConnection =
        senderAtom &&
        get(senderAtom, "connections").find(conn =>
          connectionUtils.hasSocketUris(conn, senderSocketUri, targetSocketUri)
        );

      if (affectedConnection) {
        const cnctStateUpdated = changeConnectionStateByFun(
          allAtomsInState,
          get(affectedConnection, "uri"),
          state => {
            if (!state) return vocab.WON.RequestSent; //fallback if no state present
            if (state === vocab.WON.Connected) return vocab.WON.Connected; //stay in connected if it was already the case
            if (state === vocab.WON.RequestReceived) return vocab.WON.Connected;
            if (state === vocab.WON.Suggested) return vocab.WON.RequestSent;
            if (state === vocab.WON.Closed) return vocab.WON.RequestSent;
          }
        );
        return addMessage(cnctStateUpdated, wonMessage, false, messageUri);
      } else {
        const tmpConnectionUri = "connectionFrom:" + messageUri;

        //need to wait for success-response to set that
        const optimisticConnection = Immutable.fromJS({
          uri: tmpConnectionUri,
          usingTemporaryUri: true,
          state: vocab.WON.RequestSent,
          targetAtomUri: targetAtomUri,
          targetConnectionUri: undefined,
          unread: false,
          socketUri: senderSocketUri,
          targetSocketUri: targetSocketUri,
          agreementDataset: undefined,
          agreementData: {
            agreementUris: Immutable.Set(),
            pendingProposalUris: Immutable.Set(),
            pendingCancellationProposalUris: Immutable.Set(),
            cancellationPendingAgreementUris: Immutable.Set(),
            acceptedCancellationProposalUris: Immutable.Set(),
            cancelledAgreementUris: Immutable.Set(),
            rejectedMessageUris: Immutable.Set(),
            retractedMessageUris: Immutable.Set(),
            proposedMessageUris: Immutable.Set(),
            claimedMessageUris: Immutable.Set(),
          },
          petriNetData: Immutable.Map(),
          creationDate: undefined,
          lastUpdateDate: undefined,
          isRated: false,
          showAgreementData: false,
          showPetriNetData: false,
          multiSelectType: undefined,
          messages: {
            [messageUri]: {
              uri: messageUri,
              content: {
                text: wonMessage.getTextMessage(),
              },
              isParsable: !!wonMessage.getTextMessage(),
              hasContent: !!wonMessage.getTextMessage(),
              hasReferences: false,
              date: msStringToDate(wonMessage.getTimestamp()),
              outgoingMessage: true,
              unread: false,
              messageType: vocab.WONMSG.connectMessage,
            },
          },
        });
        return allAtomsInState.setIn(
          [senderAtomUri, "connections", tmpConnectionUri],
          optimisticConnection
        );
      }
    }

    case actionTypes.messages.connectMessageSent: {
      // received a message saying we sent a connect request
      const senderConnectionUri = action.payload.updatedConnectionUri;
      let stateUpdated;

      if (senderConnectionUri) {
        stateUpdated = changeConnectionStateByFun(
          allAtomsInState,
          senderConnectionUri,
          state => {
            if (!state) return vocab.WON.RequestSent; //fallback if no state present
            if (state === vocab.WON.Connected) return vocab.WON.Connected; //stay in connected if it was already the case
            if (state === vocab.WON.RequestReceived) return vocab.WON.Connected;
            if (state === vocab.WON.Suggested) return vocab.WON.RequestSent;
            if (state === vocab.WON.Closed) return vocab.WON.RequestSent;
            return vocab.WON.RequestSent;
          }
        );

        return addMessage(stateUpdated, action.payload.event, false);
      } else {
        console.warn(
          "actionTypes.messages.connectMessageSent: senderConnectionUri was undefined for payload: ",
          action.payload,
          " -> return unchangedState"
        );
      }
      return allAtomsInState;
    }

    case actionTypes.messages.connect.successRemote: {
      const wonMessage = getIn(action, ["payload", "message"]);
      const connUri = getIn(action, ["payload", "connUri"]);
      const messageUri = wonMessage.getIsResponseTo();

      const tmpConnUri = "connectionFrom:" + messageUri;
      const tmpAtom = getAtomByConnectionUri(allAtomsInState, tmpConnUri);
      const tmpConnection = getIn(tmpAtom, ["connections", tmpConnUri]);

      if (tmpConnection) {
        // connection was established from scratch without having a
        // connection uri. now that we have the uri, we can store it
        // (see connectAdHoc)
        const atomUri = tmpAtom.get("uri");
        const properConnection = tmpConnection
          .delete("usingTemporaryUri")
          .set("uri", connUri);

        allAtomsInState = allAtomsInState
          .deleteIn([atomUri, "connections", tmpConnUri])
          .mergeDeepIn([atomUri, "connections", connUri], properConnection);
        const path = [atomUri, "connections", connUri, "messages", messageUri];
        if (getIn(allAtomsInState, path)) {
          allAtomsInState = allAtomsInState
            .setIn([...path, "isReceivedByOwn"], true)
            .setIn([...path, "isReceivedByRemote"], true);
        }
      } else {
        const atomByConnectionUri = getAtomByConnectionUri(
          allAtomsInState,
          connUri
        );

        if (atomByConnectionUri) {
          const path = [
            get(atomByConnectionUri, "uri"),
            "connections",
            connUri,
            "messages",
            messageUri,
          ];

          if (getIn(allAtomsInState, path)) {
            allAtomsInState = allAtomsInState
              .setIn([...path, "isReceivedByOwn"], true)
              .setIn([...path, "isReceivedByRemote"], true);
          }
        }
      }
      return allAtomsInState;
    }

    case actionTypes.messages.connect.successOwn: {
      const wonMessage = getIn(action, ["payload", "message"]);
      const connUri = getIn(action, ["payload", "connUri"]);

      const messageUri = wonMessage.getIsResponseTo();

      const tmpConnUri = "connectionFrom:" + messageUri;
      const tmpAtom = getAtomByConnectionUri(allAtomsInState, tmpConnUri);
      const tmpConnection = getIn(tmpAtom, ["connections", tmpConnUri]);

      if (tmpConnection) {
        // connection was established from scratch without having a
        // connection uri. now that we have the uri, we can store it
        // (see connectAdHoc)
        const atomUri = tmpAtom.get("uri");
        const properConnection = tmpConnection
          .delete("usingTemporaryUri")
          .set("uri", connUri);

        allAtomsInState = allAtomsInState
          .deleteIn([atomUri, "connections", tmpConnUri])
          .mergeDeepIn([atomUri, "connections", connUri], properConnection);
        const path = [atomUri, "connections", connUri, "messages", messageUri];
        if (getIn(allAtomsInState, path)) {
          allAtomsInState = allAtomsInState.setIn(
            [...path, "isReceivedByOwn"],
            true
          );
        }
      } else {
        const atomByConnectionUri = getAtomByConnectionUri(
          allAtomsInState,
          connUri
        );

        if (atomByConnectionUri) {
          const path = [
            get(atomByConnectionUri, "uri"),
            "connections",
            connUri,
            "messages",
            messageUri,
          ];

          if (getIn(allAtomsInState, path)) {
            allAtomsInState = allAtomsInState.setIn(
              [...path, "isReceivedByOwn"],
              true
            );
          }
        }
      }
      return allAtomsInState;
    }

    case actionTypes.messages.close.success: {
      const senderSocketUri = action.payload.getSenderSocket();
      const targetSocketUri = action.payload.getTargetSocket();

      const targetAtom = get(
        allAtomsInState,
        extractAtomUriBySocketUri(targetSocketUri)
      );
      const senderAtom = get(
        allAtomsInState,
        extractAtomUriBySocketUri(senderSocketUri)
      );

      const senderConnection = atomUtils.getConnectionBySocketUris(
        senderAtom,
        senderSocketUri,
        targetSocketUri
      );
      const targetConnection = atomUtils.getConnectionBySocketUris(
        targetAtom,
        targetSocketUri,
        senderSocketUri
      );

      if (senderConnection) {
        allAtomsInState = changeConnectionState(
          allAtomsInState,
          get(senderConnection, "uri"),
          vocab.WON.Closed
        );
      }
      if (targetConnection) {
        allAtomsInState = changeConnectionState(
          allAtomsInState,
          get(targetConnection, "uri"),
          vocab.WON.Closed
        );
      }

      return allAtomsInState;
    }

    case actionTypes.messages.viewState.markExpandReference:
      return markMessageExpandReferences(
        allAtomsInState,
        action.payload.messageUri,
        action.payload.connectionUri,
        action.payload.atomUri,
        action.payload.isExpanded,
        action.payload.reference
      );
    case actionTypes.messages.viewState.markAsSelected:
      return markMessageAsSelected(
        allAtomsInState,
        action.payload.messageUri,
        action.payload.connectionUri,
        action.payload.atomUri,
        action.payload.isSelected
      );
    case actionTypes.messages.viewState.markAsCollapsed:
      return markMessageAsCollapsed(
        allAtomsInState,
        action.payload.messageUri,
        action.payload.connectionUri,
        action.payload.atomUri,
        action.payload.isCollapsed
      );
    case actionTypes.messages.viewState.markShowActions:
      return markMessageShowActions(
        allAtomsInState,
        action.payload.messageUri,
        action.payload.connectionUri,
        action.payload.atomUri,
        action.payload.showActions
      );
    case actionTypes.messages.markAsRead: {
      return markMessageAsRead(
        allAtomsInState,
        action.payload.messageUri,
        action.payload.connectionUri,
        action.payload.atomUri,
        action.payload.read
      );
    }

    case actionTypes.messages.updateMessageStatus:
      return updateMessageStatus(
        allAtomsInState,
        action.payload.messageUri,
        action.payload.connectionUri,
        action.payload.atomUri,
        action.payload.messageStatus
      );

    case actionTypes.connections.agreementData.markAsProposed:
      return markMessageAsProposed(
        allAtomsInState,
        action.payload.messageUri,
        action.payload.connectionUri,
        action.payload.atomUri,
        action.payload.proposed
      );

    case actionTypes.connections.agreementData.markAsClaimed:
      return markMessageAsClaimed(
        allAtomsInState,
        action.payload.messageUri,
        action.payload.connectionUri,
        action.payload.atomUri,
        action.payload.claimed
      );

    case actionTypes.connections.agreementData.markAsRejected:
      return markMessageAsRejected(
        allAtomsInState,
        action.payload.messageUri,
        action.payload.connectionUri,
        action.payload.atomUri,
        action.payload.rejected
      );

    case actionTypes.connections.agreementData.markAsRetracted:
      return markMessageAsRetracted(
        allAtomsInState,
        action.payload.messageUri,
        action.payload.connectionUri,
        action.payload.atomUri,
        action.payload.retracted
      );

    case actionTypes.connections.agreementData.markAsAccepted:
      return markMessageAsAccepted(
        allAtomsInState,
        action.payload.messageUri,
        action.payload.connectionUri,
        action.payload.atomUri,
        action.payload.accepted
      );

    case actionTypes.connections.agreementData.markAsAgreed:
      return markMessageAsAgreed(
        allAtomsInState,
        action.payload.messageUri,
        action.payload.connectionUri,
        action.payload.atomUri,
        action.payload.agreed
      );

    case actionTypes.connections.agreementData.markAsCancelled:
      return markMessageAsCancelled(
        allAtomsInState,
        action.payload.messageUri,
        action.payload.connectionUri,
        action.payload.atomUri,
        action.payload.cancelled
      );

    case actionTypes.connections.agreementData.markAsCancellationPending:
      return markMessageAsCancellationPending(
        allAtomsInState,
        action.payload.messageUri,
        action.payload.connectionUri,
        action.payload.atomUri,
        action.payload.cancellationPending
      );

    case actionTypes.connections.markAsRead:
      return markConnectionAsRead(
        allAtomsInState,
        action.payload.connectionUri
      );
    case actionTypes.connections.rate:
      return markConnectionAsRated(
        allAtomsInState,
        action.payload.connectionUri
      );
    case actionTypes.connections.updatePetriNetData:
      return updatePetriNetStateData(
        allAtomsInState,
        action.payload.connectionUri,
        action.payload.petriNetData
      );
    case actionTypes.connections.updateAgreementData:
      return updateAgreementStateData(
        allAtomsInState,
        action.payload.connectionUri,
        action.payload.agreementData
      );
    case actionTypes.connections.updateAgreementDataset:
      return updateAgreementStateDataset(
        allAtomsInState,
        action.payload.connectionUri,
        action.payload.agreementDataset
      );
    case actionTypes.connections.showPetriNetData:
      return setShowPetriNetData(
        allAtomsInState,
        action.payload.connectionUri,
        action.payload.showPetriNetData
      );
    case actionTypes.connections.showAgreementData:
      return setShowAgreementData(
        allAtomsInState,
        action.payload.connectionUri,
        action.payload.showAgreementData
      );
    case actionTypes.connections.setMultiSelectType:
      return setMultiSelectType(
        allAtomsInState,
        action.payload.connectionUri,
        action.payload.multiSelectType
      );
    case actionTypes.messages.processAgreementMessage:
      //add a message that has been already processed (so sent status is ommitted)
      return addMessage(allAtomsInState, action.payload, true);
    // NEW MESSAGE STATE UPDATES

    case actionTypes.messages.processChangeNotificationMessage:
    case actionTypes.messages.processConnectionMessage:
      // ADD RECEIVED CHAT MESSAGES
      // payload; { events }
      return addMessage(allAtomsInState, action.payload, false);

    case actionTypes.connections.sendChatMessage:
    case actionTypes.connections.sendChatMessageClaimOnSuccess:
    case actionTypes.connections.sendChatMessageRefreshDataOnSuccess: {
      //const messageUri = action.payload.eventUri;

      let state = addMessage(
        allAtomsInState,
        action.payload.optimisticEvent,
        false,
        action.payload.eventUri
      );
      if (action.payload.claimed) {
        return markMessageAsClaimed(
          state,
          action.payload.eventUri,
          action.payload.connectionUri,
          action.payload.atomUri,
          action.payload.claimed
        );
      } else {
        return state;
      }
    }

    // update timestamp on success response
    case actionTypes.messages.chatMessage.successOwn: {
      const wonMessage = get(action, "payload");
      const senderSocketUri = wonMessage.getSenderSocket();
      const messageUri = wonMessage.getIsResponseTo();
      const atomUri = extractAtomUriBySocketUri(senderSocketUri);

      const atom = get(allAtomsInState, atomUri);
      const affectedConnection =
        atom &&
        get(atom, "connections")
          .filter(conn => get(conn, "socketUri") === senderSocketUri)
          .filter(conn => !!connectionUtils.getMessage(conn, messageUri))
          .first();

      if (!affectedConnection) {
        // If the connection is not stored we simply ignore the success of the chatMessage -> (success of a received msg)
        return allAtomsInState;
      }

      const connectionUri = get(affectedConnection, "uri");

      // we want to use the response date to update the original message
      // date
      // in order to use server timestamps everywhere
      const responseDateOnServer = msStringToDate(wonMessage.getTimestamp());
      // make sure we have an event with that uri:
      const path = [
        atomUri,
        "connections",
        connectionUri,
        "messages",
        messageUri,
      ];

      if (getIn(allAtomsInState, path)) {
        allAtomsInState = allAtomsInState
          .setIn([...path, "date"], responseDateOnServer)
          .setIn([...path, "isReceivedByOwn"], true);
      }
      return allAtomsInState;
    }

    case actionTypes.messages.chatMessage.failure: {
      const wonMessage = getIn(action, ["payload"]);
      const messageUri = wonMessage.getIsResponseTo();
      const atomUri = extractAtomUriBySocketUri(wonMessage.getTargetSocket());
      const connectionUri = wonMessage.getConnection(); // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

      const path = [
        atomUri,
        "connections",
        connectionUri,
        "messages",
        messageUri,
      ];

      if (getIn(allAtomsInState, path)) {
        allAtomsInState = allAtomsInState.setIn(
          [...path, "failedToSend"],
          true
        );
      }

      return allAtomsInState;
    }

    case actionTypes.messages.chatMessage.successRemote: {
      const wonMessage = get(action, "payload");
      const targetSocketUri = wonMessage.getTargetSocket();
      const senderSocketUri = wonMessage.getSenderSocket();
      const messageUri = wonMessage.getIsResponseTo();
      const atomUri = extractAtomUriBySocketUri(targetSocketUri);

      const atom = get(allAtomsInState, atomUri);
      const affectedConnection = atomUtils.getConnectionBySocketUris(
        atom,
        targetSocketUri,
        senderSocketUri
      );

      if (!affectedConnection) {
        // If the connection is not stored we simply ignore the success of the chatMessage -> (success of a received msg)
        return allAtomsInState;
      }

      const connectionUri = get(affectedConnection, "uri");

      const path = [
        atomUri,
        "connections",
        connectionUri,
        "messages",
        messageUri,
      ];
      if (getIn(allAtomsInState, path)) {
        allAtomsInState = allAtomsInState
          .setIn([...path, "isReceivedByRemote"], true)
          .setIn([...path, "isReceivedByOwn"], true);
      }
      return allAtomsInState;
    }

    case actionTypes.connections.fetchMessagesSuccess: {
      const loadedMessages = action.payload.get("events");
      if (loadedMessages) {
        allAtomsInState = addExistingMessages(allAtomsInState, loadedMessages);
      }

      return allAtomsInState;
    }

    default:
      return allAtomsInState;
  }
}
