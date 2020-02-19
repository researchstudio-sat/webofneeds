/**
 * Created by ksinger on 05.11.2015.
 */

/* TODO this fragment is part of an attempt to sketch a different
 * approach to asynchronity (Remove it or the thunk-based
 * solution afterwards)
 */

/*
* This redux wrapper for the old message-service consists of:
*
* * an "agent" that registers with the service, receives messages
* from it and triggers redux actions.
* * an "component" that listens to state changes and triggers
* messages to the server via the service.
 */

import won from "./won-es6.js";
import { getIn } from "./utils.js";

import { ownerBaseUrl } from "~/config/default.js";
import urljoin from "url-join";

import { actionTypes, actionCreators } from "./actions/actions.js";
import SockJS from "sockjs-client";

const ECHO_STRING = "e";

export function runMessagingAgent(redux) {
  /**
   * The messageProcessingArray and responseProcessingArray encapsulates all currently implemented message handlers with their respective redux dispatch
   * events, to process another message you add another anonymous function that checks on the necessary message properties/values
   * and dispatches the needed redux action/event
   */

  const responseProcessingArray = [
    function(message) {
      if (
        message.isFromSystem() &&
        message.isSuccessResponse() &&
        message.isResponseToCreateMessage()
      ) {
        redux.dispatch(actionCreators.messages__create__success(message));
        return true;
      }
      return false;
    },
    function(message) {
      if (
        message.isFromSystem() &&
        message.isSuccessResponse() &&
        message.isResponseToReplaceMessage()
      ) {
        redux.dispatch(actionCreators.messages__edit__success(message));
        return true;
      }
      return false;
    },
    function(message) {
      if (message.isResponseToConnectMessage()) {
        if (message.isSuccessResponse()) {
          if (isFromRemote(redux, message)) {
            console.debug(
              "remoteSuccess for connect: ",
              message.getIsResponseTo()
            );
            // got the second success-response (from the remote-node) - 2nd ACK
            redux.dispatch(
              actionCreators.messages__connect__successRemote(message)
            );
            return true;
          } else {
            console.debug(
              "ownSuccess for connect: ",
              message.getIsResponseTo()
            );
            // got the first success-response (from our own node) - 1st ACK
            redux.dispatch(
              actionCreators.messages__connect__successOwn(message)
            );
            return true;
          }
        } else if (message.isFailureResponse()) {
          console.debug("Received Failure to Connect Message: ", message);
          redux.dispatch(actionCreators.messages__connect__failure(message));
          return true;
        }
      }
      return false;
    },
    function(message) {
      if (message.isResponseToConnectionMessage()) {
        if (message.isSuccessResponse()) {
          if (isFromRemote(redux, message)) {
            console.debug(
              "remoteSuccess for chatmsg: ",
              message.getIsResponseTo()
            );
            // got the second success-response (from the remote-node) - 2nd ACK
            redux.dispatch(
              actionCreators.messages__chatMessage__successRemote(message)
            );
            return true;
          } else {
            console.debug(
              "ownSuccess for chatmsg: ",
              message.getIsResponseTo()
            );
            // got the first success-response (from our own node) - 1st ACK
            redux.dispatch(
              actionCreators.messages__chatMessage__successOwn(message)
            );
            return true;
          }
        } else if (message.isFailureResponse()) {
          redux.dispatch(
            actionCreators.messages__chatMessage__failure(message)
          );
          return true;
        }
      }
      return false;
    },
    function(message) {
      if (message.isResponseToCloseMessage()) {
        if (message.isSuccessResponse()) {
          //JUMP HERE AND ONLY HERE WHEN CLOSE MESSAGES COME IN!
          redux.dispatch(actionCreators.messages__close__success(message));
          return true;
        } else if (message.isFailureResponse()) {
          //Resend the failed close message
          const connectionUri = message.getConnection(); // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
          if (connectionUri) {
            console.warn("RESEND CLOSE MESSAGE FOR: ", connectionUri);
            redux.dispatch(actionCreators.connections__closeRemote(message));
          }
          return true;
        }
      }
      return false;
    },
    function(message) {
      if (message.isResponseToActivateMessage()) {
        if (message.isSuccessResponse()) {
          redux.dispatch(actionCreators.messages__reopenAtom__success(message));
          return true;
        } else {
          redux.dispatch(actionCreators.messages__reopenAtom__failure(message));
          return true;
        }
      }
      return false;
    },
    function(message) {
      if (message.isResponseToDeactivateMessage()) {
        if (message.isSuccessResponse()) {
          redux.dispatch(actionCreators.messages__closeAtom__success(message));
          return true;
        } else {
          redux.dispatch(actionCreators.messages__closeAtom__failure(message));
          return true;
        }
      }
      return false;
    },
    function(message) {
      if (message.isResponseToDeleteMessage()) {
        if (message.isSuccessResponse()) {
          redux.dispatch(actionCreators.atoms__delete(message.getAtom()));
          return true;
        }
      }
      return false;
    },
    function(message) {
      if (message.isResponseToHintFeedbackMessage()) {
        if (message.isSuccessResponse()) {
          //Rating successful
          //TODO?
          return true;
        } else {
          //Failed Rating
          //TODO?
          return false;
        }
      }
    },
  ];

  const messageProcessingArray = [
    function(message) {
      if (message.isCreateMessage()) {
        console.debug(
          "Received Create Message: ",
          message,
          "... no further handling for now"
        );
        return true;
      }
      return false;
    },
    function(message) {
      /* Other clients or matcher initiated stuff: */
      if (message.isAtomHintMessage()) {
        console.debug(
          "Received AtomHint Message: ",
          message,
          "... no further handling for now"
        );
        /*redux.dispatch(
          actionCreators.messages__processAtomHintMessage(message)
        );
        return true;*/
      }
      return false;
    },
    function(message) {
      /* Other clients or matcher initiated stuff: */
      if (message.isSocketHintMessage()) {
        console.debug("Received SocketHint Message: ", message);
        redux.dispatch(
          actionCreators.messages__processSocketHintMessage(message)
        );
        return true;
      }
      return false;
    },
    function(message) {
      if (message.isConnectMessage()) {
        console.debug("Received Connect Message: ", message);
        redux.dispatch(actionCreators.messages__processConnectMessage(message));
        return true;
      }
      return false;
    },
    function(message) {
      if (message.isConnectionMessage()) {
        console.debug("Received Connection Message: ", message);
        redux.dispatch(
          actionCreators.messages__processConnectionMessage(message)
        );
        return true;
      }
      return false;
    },
    function(message) {
      if (message.isChangeNotificationMessage()) {
        console.debug("Received ChangeNotification Message: ", message);
        redux.dispatch(
          actionCreators.messages__processChangeNotificationMessage(message)
        );
        return true;
      }
      return false;
    },
    function(message) {
      if (message.isCloseMessage()) {
        console.debug("Received Close Message: ", message);
        redux.dispatch(actionCreators.messages__close__success(message));
        return true;
      }
      return false;
    },
    function(message) {
      if (message.isFromSystem() && message.isAtomMessage()) {
        redux.dispatch(actionCreators.messages__atomMessageReceived(message));
        return true;
      }
      return false;
    },
    function(message) {
      if (message.isFromSystem() && message.isCloseMessage()) {
        console.debug("Received Close Message From System: ", message);
        redux.dispatch({
          type: actionTypes.messages.close.success,
          payload: message,
        });
        return true;
      }
      return false;
    },
    function(message) {
      if (message.isFromSystem() && message.isDeactivateMessage()) {
        //dispatch an action that is suitable for displaying a toast
        redux.dispatch(actionCreators.atoms__closedBySystem(message));
        //adapt the state and GUI
        redux.dispatch({
          type: actionTypes.atoms.close,
          payload: {
            ownedAtomUri: message.getAtom(),
          },
        });
        return true;
      }
      return false;
    },
    function(message) {
      if (message.isDeleteMessage()) {
        return true;
      }
      return false;
    },
  ];

  function onMessage(receivedMsg) {
    //reset the heartbeat counter when we receive a message.
    missedHeartbeats = 0;

    const data = JSON.parse(receivedMsg.data);

    const graphArray = data["@graph"];
    const messages = {};

    graphArray.forEach(graph => {
      const msgUri = graph["@id"].split("#")[0];
      const singleMessage = messages[msgUri];

      if (singleMessage) {
        singleMessage["@graph"].push(graph);
      } else {
        messages[msgUri] = { "@graph": [graph] };
      }
    });

    const promiseArray = [];

    for (const msgUri in messages) {
      const msg = messages[msgUri];
      promiseArray.push(
        won
          .addJsonLdData(msg)
          .catch(error => {
            console.error(
              "Could add JsonLdData of msg: ",
              msg,
              "error: ",
              error
            );
          })
          .then(() => won.wonMessageFromJsonLd(msg))
          .catch(error => {
            console.error(
              "Could not parse msg to wonMessage: ",
              msg,
              "error: ",
              error
            );
          })
      );
    }

    Promise.all(promiseArray)
      .then(wonMessages => {
        console.debug("## Received WS-Message:", wonMessages);

        wonMessages
          .filter(
            wonMessage =>
              wonMessage &&
              !wonMessage.isSuccessResponse() &&
              !wonMessage.isFailureResponse()
          )
          .map(wonMessage => {
            console.debug(
              "##### Processing Message: ",
              wonMessage.getMessageType(),
              " - ",
              wonMessage.getMessageUri(),
              " - ",
              wonMessage.getTextMessage(),
              " -- ",
              wonMessage
            );
            let messageProcessed = false;

            //process message
            for (const messageProcessor of messageProcessingArray) {
              messageProcessed =
                messageProcessed || messageProcessor(wonMessage);
            }

            if (!messageProcessed) {
              console.warn(
                "MESSAGE WASN'T PROCESSED DUE TO MISSING HANDLER FOR ITS TYPE: ",
                wonMessage
              );
            }
          });

        wonMessages
          .filter(
            wonMessage =>
              wonMessage &&
              (wonMessage.isSuccessResponse() || wonMessage.isFailureResponse())
          )
          .map(wonMessage => {
            console.debug(
              "##### Processing Response Message: ",
              wonMessage.getMessageType(),
              " - ",
              wonMessage.getMessageUri(),
              " -- ",
              wonMessage
            );
            let messageProcessed = false;

            //process message
            for (const messageProcessor of responseProcessingArray) {
              messageProcessed =
                messageProcessed || messageProcessor(wonMessage);
            }

            if (!messageProcessed) {
              console.warn(
                "MESSAGE WASN'T PROCESSED DUE TO MISSING HANDLER FOR ITS TYPE: ",
                wonMessage
              );
            }
          });
      })
      .catch(error => {
        console.warn("MESSAGE WASN'T PROCESSED DUE TO PARSING ERROR ", error);
      });
  }

  /* TODOs
     * + make it generic?
     *      + make the url a parameter?
     *      + extract the watch? / make the path a parameter?
     *      + registering a processor for the incoming messages (that
     *        can trigger actions but lets the messaging agent stay generic)
     *           + pass a callback
     *           + make this a signal/observable
     * + framing -> NOPE
     * + reconnecting
     * + lazy socket initialisation
     */

  let ws = newSock();
  window.ws4dbg = ws;
  let unsubscribeWatches = [];

  let reconnectAttempts = 0; // should be increased when a socket is opened, reset to 0 after opening was successful
  let reconnecting = false; // true after the user has requested a reconnect

  let missedHeartbeats = 0; // deadman-switch variable. should count up every 30s and gets reset onHeartbeat
  setInterval(checkHeartbeat, 30000); // heartbeats should arrive roughly every 30s

  document.addEventListener("visibilitychange", function() {
    if (document.visibilityState == "visible") {
      onVisible();
    }
  });

  function onVisible() {
    ws.send(ECHO_STRING);
  }

  function newSock() {
    const ws = new SockJS(urljoin(ownerBaseUrl, "/msg"), null, { debug: true });
    ws.onopen = onOpen;
    ws.onmessage = onMessage;
    ws.onerror = onError;
    ws.onclose = onClose;
    ws.onheartbeat = onHeartbeat;
    missedHeartbeats = 0;

    reconnectAttempts++;

    return ws;
  }

  function onHeartbeat() {
    missedHeartbeats = 0; // reset the deadman count
  }

  function checkHeartbeat() {
    if (missedHeartbeats > 0) {
      console.debug(
        "messaging-agent.js: checking heartbeat presence: ",
        missedHeartbeats,
        " full intervals of 30s have passed since the last heartbeat."
      );
    }

    if (++missedHeartbeats > 3) {
      console.error(
        "messaging-agent.js: no websocket-heartbeat present. closing socket."
      );
      ws.close();
    }
  }

  function onOpen() {
    /* Set up message-queue watch */

    reconnectAttempts = 0; // successful opening of socket. we can reset the reconnect counter.

    if (reconnecting) {
      // successful reconnect (failure is handled via connectionLost)
      reconnecting = false;
      redux.dispatch(actionCreators.reconnect__success());
    }

    if (unsubscribeWatches.length === 0) {
      const unsubscribeReconnectWatch = watchImmutableRdxState(
        redux,
        ["messages", "reconnecting"],
        (newState, oldState) => {
          reconnecting = newState;
          if (!oldState && newState) {
            console.debug(
              "messaging-agent.js: Resetting web-socket connection"
            );
            reconnectAttempts = 0;
            if (ws.readyState !== WebSocket.CLOSED) {
              ws.close(); // a new ws-connection should be opened automatically in onClose
            } else {
              ws = newSock(); // onClose won't trigger for already closed websockets, so create a new one here.
            }
          }
        }
      );

      unsubscribeWatches.push(unsubscribeReconnectWatch);
    }
  }
  function onError(e) {
    console.error("messaging-agent.js: websocket error: ", e);
    this.close();
  }

  function onClose(e) {
    if (e.wasClean) {
      console.debug(
        "messaging-agent.js: websocket closed cleanly. reconnectAttempts = ",
        reconnectAttempts
      );
    } else {
      console.error(
        "messaging-agent.js: websocket crashed. reconnectAttempts = ",
        reconnectAttempts
      );
    }

    if (e.code === 1011 || reconnectAttempts > 1) {
      console.error(
        "messaging-agent.js: either your session timed out or you encountered an unexpected server condition: \n",
        e.reason
      );
      // TODO instead show a slide-in "Lost connection" with a reload button (that allows to copy typed text out)
      // TODO recovery from timed out session
      //redux.dispatch(actionCreators.logout())
      redux.dispatch(actionCreators.lostConnection());
      /*
            fetch('rest/users/isSignedIn', {credentials: 'include'}) // attempt to get a new session
                .then(checkHttpStatus) // will reject if not logged in
                .then(() => {
                    // session is still valid -- reopen the socket
                    ws = newSock();
                }).catch(error => {
                    // cleanup - unsubscribe all watches and empty the array
                    for(let unsubscribe; unsubscribe = unsubscribeWatches.pop(); !!unsubscribe) {
                        if(unsubscribe && is("Function", unsubscribe))
                            unsubscribe();
                    }

                    console.error('messaging-agent.js: either your session timed out or you encountered an unexpected server condition: \n', e.reason);
                    // TODO instead show a slide-in "Lost connection" with a reload button (that allows to copy typed text out)
                    //redux.dispatch(actionCreators.logout())
                    redux.dispatch(actionCreators.lostConnection())
                });
                */
    } else {
      /*
             * first reconnect happens immediately (to facilitate
             * anonymous posting and the reset-hack necessary for
             * login atm)
             */
      ws = newSock();
    }
  }
}

/**
 * An oppinioned constiant of the generic watch that
 * for usage with redux-stores containing immutablejs-objects
 * @param redux {object} should provide `.subscribe` and `.getState`
 *                       (with the latter yielding an immutablejs-object)
 * @param path {array} an array of strings for usage with store.getIn
 * @param callback
 */
function watchImmutableRdxState(redux, path, callback) {
  /**
   * `subscribe`s and watches the output of `select` for changes,
   * calling `callback` if those happen.
   * @param subscribe {function} used to subscribe
   * @param select {function} a clojure that's called to get the
   *                          value to be watched
   * @param callback {function}
   * @return {function} the unsubscribe function generated by `subscribe`
   */
  const watch = (subscribe, select, callback) => {
    let unsubscribe = null;

    /*
     * creating this function (and instantly executing it)
     * allows attaching individual previousValue to it
     */
    (function() {
      let previousValue = select();
      unsubscribe = subscribe(() => {
        const currentValue = select();
        if (currentValue !== previousValue)
          callback(currentValue, previousValue);
        previousValue = currentValue;
      });
    })();

    return unsubscribe;
  };

  return watch(redux.subscribe, () => getIn(redux.getState(), path), callback);
}

function isFromRemote(redux, wonMessage) {
  const senderSocketUri = wonMessage.getSenderSocket();
  const targetSocketUri = wonMessage.getTargetSocket();

  return senderSocketUri !== targetSocketUri;
}
