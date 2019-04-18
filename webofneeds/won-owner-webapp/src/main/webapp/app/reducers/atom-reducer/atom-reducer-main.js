/**
 * Created by syim on 11.12.2015.
 */
import { actionTypes } from "../../actions/actions.js";
import Immutable from "immutable";
import won from "../../won-es6.js";
import { msStringToDate, get, getIn } from "../../utils.js";
import {
  addAtomStubs,
  addAtom,
  addAtomInCreation,
  deleteAtom,
} from "./reduce-atoms.js";
import {
  addMessage,
  addExistingMessages,
  updateMessageStatus,
  markMessageAsSelected,
  markMessageAsCollapsed,
  markMessageShowActions,
  markMessageAsRead,
  markMessageAsClaimed,
  markMessageAsProposed,
  markMessageAsRejected,
  markMessageAsRetracted,
  markMessageAsAccepted,
  markMessageAsCancelled,
  markMessageAsCancellationPending,
  markMessageExpandReferences,
} from "./reduce-messages.js";
import {
  addConnectionsToLoad,
  markConnectionAsRated,
  markConnectionAsRead,
  getAtomByConnectionUri,
  changeConnectionState,
  changeConnectionStateByFun,
  storeConnectionsData,
  updateAgreementStateData,
  updatePetriNetStateData,
  setShowAgreementData,
  setShowPetriNetData,
  setMultiSelectType,
} from "./reduce-connections.js";
import * as atomUtils from "../../atom-utils.js";

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

    case actionTypes.atoms.storeOwnedActiveUris: {
      return addAtomStubs(
        allAtomsInState,
        action.payload.get("uris"),
        won.WON.ActiveCompacted
      );
    }

    case actionTypes.atoms.storeOwnedInactiveUrisInLoading:
    case actionTypes.atoms.storeOwnedInactiveUris: {
      return addAtomStubs(
        allAtomsInState,
        action.payload.get("uris"),
        won.WON.InactiveCompacted
      );
    }

    case actionTypes.personas.storeTheirUrisInLoading:
    case actionTypes.atoms.storeAtomUrisFromOwner:
    case actionTypes.atoms.storeTheirUrisInLoading: {
      return addAtomStubs(allAtomsInState, action.payload.get("uris"));
    }

    case actionTypes.atoms.storeOwned:
    case actionTypes.atoms.storeTheirs:
    case actionTypes.personas.storeTheirs: {
      let atoms = action.payload.get("atoms");
      atoms = atoms ? atoms : Immutable.Set();

      return atoms.reduce(
        (updatedState, atom) => addAtom(updatedState, atom),
        allAtomsInState
      );
    }

    case actionTypes.connections.storeUrisToLoad: {
      return addConnectionsToLoad(
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

    case actionTypes.messages.closeAtom.failed:
      return storeConnectionsData(
        allAtomsInState,
        action.payload.get("connections")
      );

    case actionTypes.messages.reopenAtom.failed:
      return storeConnectionsData(
        allAtomsInState,
        action.payload.get("connections")
      );

    case actionTypes.atoms.reopen:
      return allAtomsInState.setIn(
        [action.payload.ownedAtomUri, "state"],
        won.WON.ActiveCompacted
      );

    case actionTypes.atoms.close:
      return allAtomsInState.setIn(
        [action.payload.ownedAtomUri, "state"],
        won.WON.InactiveCompacted
      );

    case actionTypes.atoms.removeDeleted:
    case actionTypes.personas.removeDeleted:
    case actionTypes.atoms.delete:
      return deleteAtom(allAtomsInState, action.payload.get("uri"));

    case actionTypes.personas.create: {
      //FIXME: Please let us use the addAtom method as a single entry point to add Atoms(even Personas) to the State
      return allAtomsInState.set(
        action.payload.atomUri,
        Immutable.fromJS({
          jsonld: action.payload.persona,
          isBeingCreated: true,
          uri: action.payload.atomUri,
          creationDate: new Date(),
          content: {
            type: Immutable.Set(["won:Atom", "won:Persona"]),
            sockets: Immutable.Map(),
          },
          connections: Immutable.Map(),
          holds: Immutable.List(),
          rating: { aggregateRating: 0.0, reviewCount: 0 },
        })
      );
    }

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

    case actionTypes.messages.openMessageReceived:
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
          if (!state) return won.WON.RequestReceived; //fallback if no state present
          if (state === won.WON.Connected) return won.WON.Connected; //stay in connected if it was already the case
          if (state === won.WON.RequestSent) return won.WON.Connected;
          if (state === won.WON.Suggested) return won.WON.RequestReceived;
          if (state === won.WON.Closed) return won.WON.RequestReceived;
          return won.WON.RequestReceived;
        }
      );
      return allAtomsInState;
    }

    // NEW CONNECTIONS STATE UPDATES
    case actionTypes.connections.close:
      return changeConnectionState(
        allAtomsInState,
        action.payload.connectionUri,
        won.WON.Closed
      );

    case actionTypes.atoms.connect: {
      // user has sent a connect request
      const optimisticEvent = action.payload.optimisticEvent;
      const ownedAtomUri = optimisticEvent.getSenderAtom();
      const theirAtomUri = optimisticEvent.getRecipientAtom();
      const eventUri = optimisticEvent.getMessageUri();
      let stateUpdated;

      if (action.payload.ownConnectionUri) {
        stateUpdated = changeConnectionState(
          allAtomsInState,
          action.payload.ownConnectionUri,
          won.WON.RequestSent
        );
        //because we have a connection uri, we can add the message
        return addMessage(stateUpdated, optimisticEvent);
      } else {
        const tmpConnectionUri = "connectionFrom:" + eventUri;
        //TODO: FIGURE OUT A WAY TO INCLUDE THE CORRECT SOCKET FOR ALL POSSIBLE CASES (e.g senderSocket -> get Socket from atom -> store said socket)
        let connSenderSocket = won.WON.ChatSocketCompacted; //Default add optimistic Connection as ChatConnection
        const ownedAtom = get(allAtomsInState, ownedAtomUri);
        if (
          !atomUtils.hasChatSocket(ownedAtom) &&
          atomUtils.hasGroupSocket(ownedAtom)
        ) {
          connSenderSocket = won.WON.GroupSocketCompacted; //assume the connection is from group to x if the atom has the group but not the chat socket
        }

        //atom to wait for success-response to set that
        const optimisticConnection = Immutable.fromJS({
          uri: tmpConnectionUri,
          usingTemporaryUri: true,
          state: won.WON.RequestSent,
          targetAtomUri: theirAtomUri,
          targetConnectionUri: undefined,
          unread: false,
          socket: connSenderSocket,
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
            [eventUri]: {
              uri: eventUri,
              content: {
                text: optimisticEvent.getTextMessage(),
              },
              isParsable: !!optimisticEvent.getTextMessage(),
              hasContent: !!optimisticEvent.getTextMessage(),
              hasReferences: false,
              date: msStringToDate(optimisticEvent.getSentTimestamp()),
              outgoingMessage: true,
              unread: false,
              messageType: won.WONMSG.connectMessage,
              messageStatus: {
                isProposed: false,
                isClaimed: false,
                isRetracted: false,
                isRejected: false,
                isAccepted: false,
                isCancelled: false,
                isCancellationPending: false,
              },
            },
          },
        });
        return allAtomsInState.setIn(
          [ownedAtomUri, "connections", tmpConnectionUri],
          optimisticConnection
        );
      }
    }
    case actionTypes.messages.connectMessageSent: {
      // received a message saying we sent a connect request
      const senderConnectionUri = action.payload.senderConnectionUri;
      let stateUpdated;

      if (senderConnectionUri) {
        stateUpdated = changeConnectionState(
          allAtomsInState,
          senderConnectionUri,
          won.WON.RequestSent
        );
        return addMessage(stateUpdated, action.payload.event);
      } else {
        console.warn(
          "actionTypes.messages.connectMessageSent: senderConnectionUri was undefined for payload: ",
          action.payload,
          " -> return unchangedState"
        );
      }
      return allAtomsInState;
    }

    case actionTypes.messages.openMessageSent: {
      const senderAtomUri = action.payload.senderAtomUri;
      const senderConnectionUri = action.payload.senderConnectionUri;
      let stateUpdated;

      if (senderConnectionUri) {
        const senderAtom = allAtomsInState.get(senderAtomUri);
        const existingConnection =
          senderAtom && senderAtom.getIn(["connections", senderConnectionUri]);

        if (existingConnection) {
          stateUpdated = changeConnectionState(
            allAtomsInState,
            senderConnectionUri,
            won.WON.Connected //TODO EITHER SET TO REQUEST SENT OR CONNECTED DEPENDING ON THE CURRENT STATE
          );
          //because we have a connection uri, we can add the message
          return addMessage(stateUpdated, action.payload.event);
        }
      } else {
        console.warn(
          "actionTypes.messages.openMessageSent: senderConnectionUri was undefined for payload: ",
          action.payload,
          " -> return unchangedState"
        );
      }
      return allAtomsInState;
    }

    case actionTypes.connections.open: {
      // user has sent an open request
      const cnctStateUpdated = changeConnectionStateByFun(
        allAtomsInState,
        action.payload.connectionUri,
        state => {
          if (!state) return won.WON.RequestSent; //fallback if no state present
          if (state === won.WON.Connected) return won.WON.Connected; //stay in connected if it was already the case
          if (state === won.WON.RequestReceived) return won.WON.Connected;
          if (state === won.WON.Suggested) return won.WON.RequestSent;
          if (state === won.WON.Closed) return won.WON.RequestSent;
        }
      );
      return addMessage(cnctStateUpdated, action.payload.optimisticEvent);
    }
    case actionTypes.messages.open.failure:
      return changeConnectionState(
        allAtomsInState,
        action.payload.events["msg:FromSystem"].recipient,
        won.WON.RequestReceived
      );

    case actionTypes.messages.open.successRemote:
    case actionTypes.messages.connect.successRemote: {
      // use the remote success message to obtain the remote connection
      // uri (which we may not have known)
      const wonMessage = action.payload;
      const connectionUri = wonMessage.getReceiver();
      const atomUri = wonMessage.getRecipientAtom();
      const targetConnectionUri = wonMessage.getSender();

      if (allAtomsInState.getIn([atomUri, "connections", connectionUri])) {
        const eventUri = wonMessage.getIsRemoteResponseTo();
        // we want to use the response date to update the original message
        // date
        allAtomsInState = allAtomsInState.setIn(
          [
            atomUri,
            "connections",
            connectionUri,
            "messages",
            eventUri,
            "isReceivedByRemote",
          ],
          true
        );

        return allAtomsInState.setIn(
          [atomUri, "connections", connectionUri, "targetConnectionUri"],
          targetConnectionUri
        );
      } else {
        console.warn(
          "Open/Connect success for a connection that is not stored in the state yet, connUri: ",
          connectionUri
        );
        return allAtomsInState;
      }
    }

    case actionTypes.messages.connect.successOwn: {
      // TODO SRP; split in isSuccessOfAdHocConnect, addAddHoc(?) and
      // changeConnectionState
      const wonMessage = action.payload;
      const eventUri = wonMessage.getIsResponseTo();
      const connUri = wonMessage.getReceiver();

      const tmpConnUri = "connectionFrom:" + wonMessage.getIsResponseTo();
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
        const path = [atomUri, "connections", connUri, "messages", eventUri];
        if (allAtomsInState.getIn(path)) {
          allAtomsInState = allAtomsInState.setIn(
            [...path, "isReceivedByOwn"],
            true
          );
        } else {
          console.error(
            "connect.successOwn for message that was not sent(or was not loaded in the state yet, wonMessage: ",
            wonMessage,
            "messageUri: ",
            eventUri
          );
        }
        return allAtomsInState;
      } else {
        const atomByConnectionUri = getAtomByConnectionUri(
          allAtomsInState,
          connUri
        );

        if (atomByConnectionUri) {
          // connection has been stored as match first
          allAtomsInState = changeConnectionState(
            allAtomsInState,
            connUri,
            won.WON.RequestSent
          );

          if (
            allAtomsInState.getIn([
              atomByConnectionUri.get("uri"),
              "connections",
              connUri,
              "messages",
              eventUri,
            ])
          ) {
            allAtomsInState = allAtomsInState.setIn(
              [
                atomByConnectionUri.get("uri"),
                "connections",
                connUri,
                "messages",
                eventUri,
                "isReceivedByOwn",
              ],
              true
            );
          } else {
            console.error(
              "connect.successOwn for message that was not sent(or was not loaded in the state yet, wonMessage: ",
              wonMessage,
              "messageUri: ",
              eventUri
            );
          }
        } else {
          console.warn(
            "Can't add the connection(",
            connUri,
            ") the atom is not stored in the state yet"
          );
        }
        return allAtomsInState;
      }
    }

    case actionTypes.messages.close.success:
      return changeConnectionState(
        allAtomsInState,
        action.payload.getReceiver(),
        won.WON.Closed
      );
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
    case actionTypes.messages.markAsRead:
      return markMessageAsRead(
        allAtomsInState,
        action.payload.messageUri,
        action.payload.connectionUri,
        action.payload.atomUri
      );

    case actionTypes.messages.updateMessageStatus:
      return updateMessageStatus(
        allAtomsInState,
        action.payload.messageUri,
        action.payload.connectionUri,
        action.payload.atomUri,
        action.payload.messageStatus
      );

    case actionTypes.messages.messageStatus.markAsProposed:
      return markMessageAsProposed(
        allAtomsInState,
        action.payload.messageUri,
        action.payload.connectionUri,
        action.payload.atomUri,
        action.payload.proposed
      );

    case actionTypes.messages.messageStatus.markAsClaimed:
      return markMessageAsClaimed(
        allAtomsInState,
        action.payload.messageUri,
        action.payload.connectionUri,
        action.payload.atomUri,
        action.payload.claimed
      );

    case actionTypes.messages.messageStatus.markAsRejected:
      return markMessageAsRejected(
        allAtomsInState,
        action.payload.messageUri,
        action.payload.connectionUri,
        action.payload.atomUri,
        action.payload.rejected
      );

    case actionTypes.messages.messageStatus.markAsRetracted:
      return markMessageAsRetracted(
        allAtomsInState,
        action.payload.messageUri,
        action.payload.connectionUri,
        action.payload.atomUri,
        action.payload.retracted
      );

    case actionTypes.messages.messageStatus.markAsAccepted:
      return markMessageAsAccepted(
        allAtomsInState,
        action.payload.messageUri,
        action.payload.connectionUri,
        action.payload.atomUri,
        action.payload.accepted
      );

    case actionTypes.messages.messageStatus.markAsCancelled:
      return markMessageAsCancelled(
        allAtomsInState,
        action.payload.messageUri,
        action.payload.connectionUri,
        action.payload.atomUri,
        action.payload.cancelled
      );

    case actionTypes.messages.messageStatus.markAsCancellationPending:
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
        action.payload.connectionUri,
        action.payload.atomUri
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
      return addMessage(allAtomsInState, action.payload);

    case actionTypes.connections.sendChatMessageClaimOnSuccess:
    case actionTypes.connections.sendChatMessageRefreshDataOnSuccess: {
      return addMessage(allAtomsInState, action.payload.optimisticEvent);
    }

    case actionTypes.connections.sendChatMessage:
      // ADD SENT TEXT MESSAGE
      /*
             * payload: { eventUri: optimisticEvent.uri, message,
             * optimisticEvent, }
             */
      return addMessage(allAtomsInState, action.payload.optimisticEvent);

    // update timestamp on success response
    case actionTypes.messages.open.successOwn:
    case actionTypes.messages.chatMessage.successOwn: {
      const wonMessage = getIn(action, ["payload"]);
      const eventUri = wonMessage.getIsResponseTo();
      const atomUri = wonMessage.getRecipientAtom();
      const connectionUri = wonMessage.getReceiver();
      // we want to use the response date to update the original message
      // date
      // in order to use server timestamps everywhere
      const responseDateOnServer = msStringToDate(wonMessage.getTimestamp());
      // make sure we have an event with that uri:
      const eventToUpdate = allAtomsInState.getIn([
        atomUri,
        "connections",
        connectionUri,
        "messages",
        eventUri,
      ]);
      if (eventToUpdate) {
        allAtomsInState = allAtomsInState.setIn(
          [atomUri, "connections", connectionUri, "messages", eventUri, "date"],
          responseDateOnServer
        );
        allAtomsInState = allAtomsInState.setIn(
          [
            atomUri,
            "connections",
            connectionUri,
            "messages",
            eventUri,
            "isReceivedByOwn",
          ],
          true
        );
      }
      return allAtomsInState;
    }

    case actionTypes.messages.chatMessage.failure: {
      const wonMessage = getIn(action, ["payload"]);
      const eventUri = wonMessage.isFromExternal()
        ? wonMessage.getIsRemoteResponseTo()
        : wonMessage.getIsResponseTo();
      const atomUri = wonMessage.getRecipientAtom();
      const connectionUri = wonMessage.getReceiver();

      allAtomsInState = allAtomsInState.setIn(
        [
          atomUri,
          "connections",
          connectionUri,
          "messages",
          eventUri,
          "failedToSend",
        ],
        true
      );
      return allAtomsInState;
    }

    case actionTypes.messages.chatMessage.successRemote: {
      const wonMessage = getIn(action, ["payload"]);
      const eventUri = wonMessage.getIsRemoteResponseTo();
      const atomUri = wonMessage.getRecipientAtom();
      const connectionUri = wonMessage.getReceiver();
      const path = [
        atomUri,
        "connections",
        connectionUri,
        "messages",
        eventUri,
      ];
      if (allAtomsInState.getIn(path)) {
        allAtomsInState = allAtomsInState
          .setIn([...path, "isReceivedByRemote"], true)
          .setIn([...path, "isReceivedByOwn"], true);
      } else {
        console.error(
          "chatMessage.successRemote for message that was not sent(or was not loaded in the state yet, wonMessage: ",
          wonMessage,
          "messageUri: ",
          eventUri
        );
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
