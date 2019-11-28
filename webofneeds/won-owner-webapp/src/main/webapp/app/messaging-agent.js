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
   * The messageProcessingArray encapsulates all currently implemented message handlers with their respective redux dispatch
   * events, to process another message you add another anonymous function that checks on the necessary message properties/values
   * and dispatches the needed redux action/event
   */
  const messageProcessingArray = [
    function(message) {
      /* Other clients or matcher initiated stuff: */
      if (message.isFromExternal() && message.isAtomHintMessage()) {
        console.warn(
          "Omit further handling of received AtomHintMessage: ",
          message,
          "TODO: IMPL"
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
      if (message.isFromExternal() && message.isSocketHintMessage()) {
        redux.dispatch(
          actionCreators.messages__processSocketHintMessage(message)
        );
        console.debug(
          "dispatch actionCreators.messages__processSocketHintMessage"
        );
        return true;
      }
      return false;
    },
    function(message) {
      if (message.isConnectMessage()) {
        redux.dispatch(actionCreators.messages__processConnectMessage(message));
        console.debug(
          "dispatch actionCreators.messages__processConnectMessage"
        );
        return true;
      }
      return false;
    },
    function(message) {
      if (message.isOpenMessage()) {
        //someone accepted our contact request
        redux.dispatch(actionCreators.messages__processOpenMessage(message));
        console.debug("dispatch actionCreators.messages__processOpenMessage");
        return true;
      }
      return false;
    },
    function(message) {
      if (message.isConnectionMessage()) {
        redux.dispatch(
          actionCreators.messages__processConnectionMessage(message)
        );
        console.debug(
          "dispatch actionCreators.messages__processConnectionMessage"
        );
        return true;
      }
      return false;
    },
    function(message) {
      if (message.isChangeNotificationMessage()) {
        redux.dispatch(
          actionCreators.messages__processChangeNotificationMessage(message)
        );
        console.debug(
          "dispatch actionCreators.messages__processChangeNotificationMessage"
        );
        return true;
      }
      return false;
    },
    function(message) {
      if (message.isFromExternal() && message.isCloseMessage()) {
        redux.dispatch(actionCreators.messages__close__success(message));
        console.debug("dispatch actionCreators.messages__close__success");
        return true;
      }
      return false;
    },
    function(message) {
      if (
        message.isFromSystem() &&
        message.isSuccessResponse() &&
        message.isResponseToCreateMessage()
      ) {
        redux.dispatch(actionCreators.messages__create__success(message));
        console.debug("dispatch actionCreators.messages__create__success");
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
        console.debug("dispatch actionCreators.messages__edit__success");
        return true;
      }
      return false;
    },
    function(message) {
      if (message.isResponseToConnectMessage()) {
        if (message.isSuccessResponse()) {
          if (message.isFromExternal()) {
            // got the second success-response (from the remote-node) - 2nd ACK
            redux.dispatch(
              actionCreators.messages__connect__successRemote(message)
            );
            console.debug(
              "dispatch actionCreators.messages__connect__successRemote"
            );
            return true;
          } else {
            // got the first success-response (from our own node) - 1st ACK
            redux.dispatch(
              actionCreators.messages__connect__successOwn(message)
            );
            console.debug(
              "dispatch actionCreators.messages__connect__successOwn"
            );
            return true;
          }
        } else if (message.isFailureResponse()) {
          redux.dispatch(actionCreators.messages__connect__failure(message));
          console.debug("dispatch actionCreators.messages__connect__failure");
          return true;
        }
      }
      return false;
    },
    function(message) {
      if (message.isResponseToConnectionMessage()) {
        if (message.isSuccessResponse()) {
          if (message.isFromExternal()) {
            // got the second success-response (from the remote-node) - 2nd ACK
            redux.dispatch(
              actionCreators.messages__chatMessage__successRemote(message)
            );
            console.debug(
              "dispatch actionCreators.messages__chatMessage__successRemote"
            );
            return true;
          } else {
            // got the first success-response (from our own node) - 1st ACK
            redux.dispatch(
              actionCreators.messages__chatMessage__successOwn(message)
            );
            console.debug(
              "dispatch actionCreators.messages__chatMessage__successOwn"
            );
            return true;
          }
        } else if (message.isFailureResponse()) {
          redux.dispatch(
            actionCreators.messages__chatMessage__failure(message)
          );
          console.debug(
            "dispatch actionCreators.messages__chatMessage__failure"
          );
          return true;
        }
      }
      return false;
    },
    function(message) {
      if (message.isResponseToOpenMessage()) {
        if (message.isSuccessResponse()) {
          if (message.isFromExternal()) {
            // got the second success-response (from the remote-node) - 2nd ACK
            redux.dispatch(
              actionCreators.messages__open__successRemote(message)
            );
            console.debug(
              "dispatch actionCreators.messages__open__successRemote"
            );
            return true;
          } else {
            // got the first success-response (from our own node) - 1st ACK
            redux.dispatch(actionCreators.messages__open__successOwn(message));
            console.debug("dispatch actionCreators.messages__open__successOwn");
            return true;
          }
        } else if (message.isFailureResponse()) {
          redux.dispatch(actionCreators.messages__open__failure(message));
          console.debug("dispatch actionCreators.messages__open__failure");
          /*
                     * as the failure should hit right after the open went out
                     * and should be rather rare, we can redirect in the optimistic
                     * case (see connection-actions.js) and go back if it fails.
                     */

          redux.dispatch(
            actionCreators.router__stateGoAbs("connections", {
              connectionUri: message.getSenderConnection(),
            })
          );
          console.debug("dispatch actionCreators.router__stateGoAbs");
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
          console.debug("dispatch actionCreators.messages__close__success");
          return true;
        } else if (message.isFailureResponse()) {
          //Resend the failed close message
          const connectionUri = message.getSenderConnection();
          if (connectionUri) {
            console.warn("RESEND CLOSE MESSAGE FOR: ", connectionUri);
            redux.dispatch(actionCreators.connections__closeRemote(message));
            console.debug("dispatch actionCreators.connections__closeRemote");
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
          console.debug(
            "dispatch actionCreators.messages__reopenAtom__success"
          );
          return true;
        } else {
          redux.dispatch(actionCreators.messages__reopenAtom__failure(message));
          console.debug(
            "dispatch actionCreators.messages__reopenAtom__failure"
          );
          return true;
        }
      }
      return false;
    },
    function(message) {
      if (message.isResponseToDeactivateMessage()) {
        if (message.isSuccessResponse()) {
          redux.dispatch(actionCreators.messages__closeAtom__success(message));
          console.debug("dispatch actionCreators.messages__closeAtom__success");
          return true;
        } else {
          redux.dispatch(actionCreators.messages__closeAtom__failure(message));
          console.debug("dispatch actionCreators.messages__closeAtom__failure");
          return true;
        }
      }
      return false;
    },
    function(message) {
      if (message.isFromSystem() && message.isAtomMessage()) {
        redux.dispatch(actionCreators.messages__atomMessageReceived(message));
        console.debug("dispatch actionCreators.messages__atomMessageReceived");
        return true;
      }
      return false;
    },
    function(message) {
      if (message.isFromSystem() && message.isCloseMessage()) {
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
        console.debug("dispatch actionCreators.atoms__closedBySystem");
        //adapt the state and GUI
        redux.dispatch({
          type: actionTypes.atoms.close,
          payload: {
            ownedAtomUri: message.getAtom(),
          },
        });
        console.debug("dispatch actionTypes.atoms.close");
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
    function(message) {
      if (message.isResponseToDeleteMessage()) {
        if (message.isSuccessResponse()) {
          redux.dispatch(actionCreators.atoms__delete(message.getAtom()));
          console.debug("dispatch actionCreators.atoms__delete");
          /*redux.dispatch({
            type: actionTypes.atoms.delete,
            payload: {
              ownedAtomUri: message.getRecipientAtom(),
            },
          });*/
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

  // processors that are used for reacting to certain messages after they
  // have been processed normally
  const messagePostProcessingArray = [
    function(message) {
      if (message.isFromSystem() && message.isSuccessResponse()) {
        redux.dispatch(
          actionCreators.messages__dispatchActionOn__successOwn(message)
        );
        console.debug(
          "dispatch actionCreators.messages__dispatchActionOn__successOwn"
        );
        return true;
      }
      return false;
    },
    function(message) {
      if (message.isFromSystem() && message.isFailureResponse()) {
        redux.dispatch(
          actionCreators.messages__dispatchActionOn__failureOwn(message)
        );
        console.debug(
          "dispatch actionCreators.messages__dispatchActionOn__failureOwn"
        );
        return true;
      }
      return false;
    },
    function(message) {
      if (message.isFromExternal() && message.isSuccessResponse()) {
        redux.dispatch(
          actionCreators.messages__dispatchActionOn__successRemote(message)
        );
        console.debug(
          "dispatch actionCreators.messages__dispatchActionOn__successRemote"
        );
        return true;
      }
      return false;
    },
    function(message) {
      if (message.isFromExternal() && message.isFailureResponse()) {
        redux.dispatch(
          actionCreators.messages__dispatchActionOn__failureRemote(message)
        );
        console.debug(
          "dispatch actionCreators.messages__dispatchActionOn__failureRemote"
        );
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

    console.debug("WS received Msgs:", messages);

    for (const msgUri in messages) {
      const msg = messages[msgUri];
      won.wonMessageFromJsonLd(msg).then(message => {
        console.debug("Processing WonMessage from WS: ", message);
        won.addJsonLdData(msg);

        let messageProcessed = false;

        //process message
        for (const messageProcessor of messageProcessingArray) {
          messageProcessed = messageProcessed || messageProcessor(message);
        }

        //post-process message
        for (const messagePostprocessor of messagePostProcessingArray) {
          messagePostprocessor(message);
        }

        if (!messageProcessed) {
          console.warn(
            "MESSAGE WASN'T PROCESSED DUE TO MISSING HANDLER FOR ITS TYPE: ",
            message
          );
        }
      });
    }
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
