/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import { get, getIn } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import { connect } from "react-redux";
import * as atomUtils from "../redux/utils/atom-utils";
import * as generalSelectors from "../redux/selectors/general-selectors";
import * as connectionSelectors from "../redux/selectors/connection-selectors";
import * as connectionUtils from "../redux/utils/connection-utils";
import won from "../won-es6";
import WonAtomContextSwipeableView from "./atom-context-swipeable-view";
import WonLabelledHr from "./labelled-hr.jsx";
import WonSuggestAtomPicker from "./details/picker/suggest-atom-picker.jsx";
import "~/style/_atom-content-buddies.scss";
import VisibilitySensor from "react-visibility-sensor";
import PropTypes from "prop-types";

const mapStateToProps = (state, ownProps) => {
  const atom = getIn(state, ["atoms", ownProps.atomUri]);
  const isOwned = generalSelectors.isAtomOwned(state, ownProps.atomUri);

  const hasBuddySocket = atomUtils.hasBuddySocket(atom);

  const buddies = hasBuddySocket && get(atom, "buddies");

  const buddyConnections =
    isOwned &&
    hasBuddySocket &&
    connectionSelectors.getBuddyConnectionsByAtomUri(
      state,
      ownProps.atomUri,
      true,
      false
    );

  let excludedFromRequestUris = [ownProps.atomUri];

  if (buddyConnections) {
    buddyConnections.map(conn =>
      excludedFromRequestUris.push(get(conn, "targetAtomUri"))
    );
  }

  const chatConnections =
    isOwned &&
    connectionSelectors.getChatConnectionsByAtomUri(state, ownProps.atomUri);

  const chatSocketUri = atomUtils.getSocketUri(
    atom,
    won.CHAT.ChatSocketCompacted
  );

  return {
    atomUri: ownProps.atomUri,
    atom,
    isOwned,
    chatSocketUri,
    hasChatConnections: chatConnections && chatConnections.size > 0,
    chatConnectionsArray: chatConnections && chatConnections.toArray(),
    hasBuddySocket,
    hasBuddies: buddies && buddies.size > 0,
    hasBuddyConnections: buddyConnections && buddyConnections.size > 0,
    buddyConnectionsArray: buddyConnections && buddyConnections.toArray(),
    excludedFromRequestUris,
    buddiesArray: buddies && buddies.toArray(),
    currentLocation: generalSelectors.getCurrentLocation(state),
  };
};

const mapDispatchToProps = dispatch => {
  return {
    connectionMarkAsRead: (connectionUri, atomUri) => {
      dispatch(
        actionCreators.connections__markAsRead({
          connectionUri: connectionUri,
          atomUri: atomUri,
        })
      );
    },
    fetchPersonas: () => {
      dispatch(actionCreators.atoms__fetchPersonas());
    },
    hideModalDialog: () => {
      dispatch(actionCreators.view__hideModalDialog());
    },
    showModalDialog: payload => {
      dispatch(actionCreators.view__showModalDialog(payload));
    },
    connectionClose: connectionUri => {
      dispatch(actionCreators.connections__close(connectionUri));
    },
    connectionOpen: (connectionUri, message) => {
      dispatch(actionCreators.connections__open(connectionUri, message));
    },
    connectAdHoc: (targetAtomUri, message, connectToSocketUri, persona) => {
      dispatch(
        actionCreators.connections__connectAdHoc(
          targetAtomUri,
          message,
          connectToSocketUri,
          persona
        )
      );
    },
    connect: (
      ownedAtomUri,
      connectionUri,
      targetAtomUri,
      message,
      ownSocket,
      targetSocket
    ) => {
      dispatch(
        actionCreators.atoms__connect(
          ownedAtomUri,
          connectionUri,
          targetAtomUri,
          message,
          ownSocket,
          targetSocket
        )
      );
    },
    routerGo: (path, props) => {
      dispatch(actionCreators.router__stateGo(path, props));
    },
    routerGoResetParams: path => {
      dispatch(actionCreators.router__stateGoResetParams(path));
    },
  };
};

class WonAtomContentBuddies extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      suggestAtomExpanded: false,
    };
    this.toggleSuggestions = this.toggleSuggestions.bind(this);
  }

  toggleSuggestions() {
    this.setState({ suggestAtomExpanded: !this.state.suggestAtomExpanded });
  }

  componentDidMount() {
    // TODO: FETCH OTHER PERSONAS (Performance of the call is too slow for large datasets so we won't do this for now)
    // this.props.fetchPersonas();
  }

  render() {
    let buddies;

    if (this.props.isOwned) {
      if (this.props.hasBuddyConnections) {
        buddies = this.props.buddyConnectionsArray.map(conn => {
          if (!connectionUtils.isClosed(conn)) {
            let actionButtons;
            let headerClassName;

            if (connectionUtils.isRequestReceived(conn)) {
              headerClassName = "status--received";
              actionButtons = (
                <div className="acb__buddy__actions">
                  <svg
                    className="acb__buddy__actions__icon request won-icon"
                    onClick={() => this.openRequest(conn)}
                  >
                    <use
                      xlinkHref="#ico32_buddy_accept"
                      href="#ico32_buddy_accept"
                    />
                  </svg>
                  <svg
                    className="acb__buddy__actions__icon primary won-icon"
                    onClick={() =>
                      this.closeConnection(conn, "Reject Buddy Request?")
                    }
                  >
                    <use
                      xlinkHref="#ico32_buddy_deny"
                      href="#ico32_buddy_deny"
                    />
                  </svg>
                </div>
              );
            } else if (connectionUtils.isSuggested(conn)) {
              headerClassName = "status--suggested";
              actionButtons = (
                <div className="acb__buddy__actions">
                  <svg
                    className="acb__buddy__actions__icon request won-icon"
                    onClick={() => this.requestBuddy(conn)}
                  >
                    <use
                      xlinkHref="#ico32_buddy_accept"
                      href="#ico32_buddy_accept"
                    />
                  </svg>
                  <svg
                    className="acb__buddy__actions__icon primary won-icon"
                    onClick={() =>
                      this.closeConnection(conn, "Reject Buddy Suggestion?")
                    }
                  >
                    <use
                      xlinkHref="#ico32_buddy_deny"
                      href="#ico32_buddy_deny"
                    />
                  </svg>
                </div>
              );
            } else if (connectionUtils.isRequestSent(conn)) {
              headerClassName = "status--sent";
              actionButtons = (
                <div className="acb__buddy__actions">
                  <svg
                    className="acb__buddy__actions__icon disabled won-icon"
                    disabled={true}
                  >
                    <use
                      xlinkHref="#ico32_buddy_waiting"
                      href="#ico32_buddy_waiting"
                    />
                  </svg>
                  <svg
                    className="acb__buddy__actions__icon secondary won-icon"
                    onClick={() =>
                      this.closeConnection(conn, "Cancel Buddy Request?")
                    }
                  >
                    <use
                      xlinkHref="#ico32_buddy_deny"
                      href="#ico32_buddy_deny"
                    />
                  </svg>
                </div>
              );
            } else if (connectionUtils.isConnected(conn)) {
              //TODO: Check chat socket connection
              actionButtons = (
                <div className="acb__buddy__actions">
                  <svg
                    className="acb__buddy__actions__icon primary won-icon"
                    onClick={() => this.sendChatMessage(conn)}
                  >
                    <use xlinkHref="#ico36_message" href="#ico36_message" />
                  </svg>
                  <svg
                    className="acb__buddy__actions__icon secondary won-icon"
                    onClick={() => this.closeConnection(conn)}
                  >
                    <use
                      xlinkHref="#ico32_buddy_deny"
                      href="#ico32_buddy_deny"
                    />
                  </svg>
                </div>
              );
            } else if (connectionUtils.isClosed(conn)) {
              headerClassName = "status--closed";
              actionButtons = (
                <div className="acb__buddy__actions">
                  Buddy has been removed
                </div>
              );
            } else {
              actionButtons = (
                <div className="acb__buddy__actions">Unknown State</div>
              );
            }

            return (
              <VisibilitySensor
                key={get(conn, "uri")}
                onChange={isVisible => {
                  isVisible &&
                    connectionUtils.isUnread(conn) &&
                    this.markAsRead(conn);
                }}
                intervalDelay={2000}
              >
                <div
                  className={
                    "acb__buddy " +
                    (connectionUtils.isUnread(conn) ? " won-unread " : "")
                  }
                >
                  <WonAtomContextSwipeableView
                    className={headerClassName}
                    actionButtons={actionButtons}
                    atomUri={get(conn, "targetAtomUri")}
                    onClick={() =>
                      this.props.routerGo("post", {
                        postUri: get(conn, "targetAtomUri"),
                      })
                    }
                  />
                </div>
              </VisibilitySensor>
            );
          }
        });
      } else {
        buddies = <div className="acp__empty">No Buddies present.</div>;
      }

      return (
        <won-atom-content-buddies>
          {buddies}
          <WonLabelledHr
            label="Request"
            arrow={this.state.suggestAtomExpanded ? "up" : "down"}
            onClick={this.toggleSuggestions}
          />
          {this.state.suggestAtomExpanded ? (
            <WonSuggestAtomPicker
              initialValue={undefined}
              onUpdate={({ value }) => this.requestBuddy(value)}
              detail={{ placeholder: "Insert AtomUri to invite" }}
              excludedUris={this.props.excludedFromRequestUris}
              allowedSockets={[won.BUDDY.BuddySocketCompacted]}
              excludedText="Requesting yourself or someone who is already your Buddy is not allowed"
              notAllowedSocketText="Request does not work on atoms without the Buddy Socket"
              noSuggestionsText="No known Personas available"
            />
          ) : (
            undefined
          )}
        </won-atom-content-buddies>
      );
    } else {
      if (this.props.hasBuddies) {
        buddies = this.props.buddiesArray.map(memberUri => {
          //TODO: Define possible actions
          // if actionButtons = undefined: No SwipableView
          return (
            <div className="acb__buddy" key={memberUri}>
              <WonAtomContextSwipeableView
                atomUri={memberUri}
                onClick={() =>
                  this.props.routerGo("post", {
                    postUri: memberUri,
                  })
                }
              />
            </div>
          );
        });
      } else {
        buddies = <div className="acp__empty">No Buddies present.</div>;
      }

      return <won-atom-content-buddies>{buddies}</won-atom-content-buddies>;
    }
  }

  closeConnection(conn, dialogText = "Remove Buddy?") {
    if (!conn) {
      return;
    }

    const payload = {
      caption: "Persona",
      text: dialogText,
      buttons: [
        {
          caption: "Yes",
          callback: () => {
            const connUri = get(conn, "uri");

            if (connectionUtils.isUnread(conn)) {
              this.props.connectionMarkAsRead(connUri, this.props.atomUri);
            }

            this.props.connectionClose(connUri);
            this.props.hideModalDialog();
          },
        },
        {
          caption: "No",
          callback: () => {
            this.props.hideModalDialog();
          },
        },
      ],
    };
    this.props.showModalDialog(payload);
  }

  openRequest(conn, message = "") {
    if (!conn) {
      return;
    }

    const connUri = get(conn, "uri");

    if (connectionUtils.isUnread(conn)) {
      this.props.connectionMarkAsRead(connUri, this.props.atomUri);
    }
    this.props.connectionOpen(connUri, message);
  }

  requestBuddy(targetAtomUri, message = "") {
    if (!this.props.isOwned || !this.props.hasBuddySocket) {
      console.warn("Trying to request a non-owned or non buddySocket atom");
      return;
    }

    const payload = {
      caption: "Persona",
      text: "Send Buddy Request?",
      buttons: [
        {
          caption: "Yes",
          callback: () => {
            this.props.connect(
              this.props.atomUri,
              undefined,
              targetAtomUri,
              message,
              won.BUDDY.BuddySocketCompacted,
              won.BUDDY.BuddySocketCompacted
            );
            this.props.hideModalDialog();
          },
        },
        {
          caption: "No",
          callback: () => {
            this.props.hideModalDialog();
          },
        },
      ],
    };
    this.props.showModalDialog(payload);
  }

  sendChatMessage(connection) {
    if (this.props.chatConnectionsArray && this.props.hasChatConnections) {
      //Check if connection is already an existing chatConnection
      const targetAtomUri = get(connection, "targetAtomUri");
      //const targetSocketUri = get(connection, "socketUri");
      const chatConnections = this.props.chatConnectionsArray.filter(
        conn => get(conn, "targetAtomUri") === targetAtomUri
      );
      //.filter(conn => get(conn, "socketUri") === targetSocketUri);

      if (chatConnections.length == 0) {
        //No chatConnection between buddies exists => connect
        this.props.connect(
          this.props.atomUri,
          undefined,
          get(connection, "targetAtomUri"),
          "",
          won.CHAT.ChatSocketCompacted,
          won.CHAT.ChatSocketCompacted
        );
        this.props.routerGoResetParams("connections");
      } else if (chatConnections.length == 1) {
        const chatConnection = chatConnections[0];
        const chatConnectionUri = get(chatConnection, "uri");

        if (
          connectionUtils.isSuggested(chatConnection) ||
          connectionUtils.isClosed(chatConnection)
        ) {
          this.props.connect(
            this.props.atomUri,
            undefined,
            chatConnectionUri,
            "",
            won.CHAT.ChatSocketCompacted,
            won.CHAT.ChatSocketCompacted
          );
        } else if (
          connectionUtils.isConnected(chatConnection) ||
          connectionUtils.isRequestSent(chatConnection) ||
          connectionUtils.isRequestReceived(chatConnection)
        ) {
          this.props.routerGo("connections", {
            connectionUri: chatConnectionUri,
          });
        }
      } else {
        console.error(
          "more than one connection stored between two atoms that use the same exact sockets",
          this.props.atom,
          this.props.chatSocketUri
        );
      }
    } else {
      //No chatConnection between buddies exists => connect
      this.props.connect(
        this.props.atomUri,
        undefined,
        get(connection, "targetAtomUri"),
        "",
        won.CHAT.ChatSocketCompacted,
        won.CHAT.ChatSocketCompacted
      );
      this.props.routerGoResetParams("connections");
    }
  }

  markAsRead(conn) {
    if (connectionUtils.isUnread(conn)) {
      this.props.connectionMarkAsRead(get(conn, "uri"), this.props.atomUri);
    }
  }
}
WonAtomContentBuddies.propTypes = {
  atomUri: PropTypes.string.isRequired,
  atom: PropTypes.object,
  isOwned: PropTypes.bool,
  hasBuddySocket: PropTypes.bool,
  hasBuddies: PropTypes.bool,
  hasBuddyConnections: PropTypes.bool,
  buddyConnectionsArray: PropTypes.arrayOf(PropTypes.object),
  excludedFromRequestUris: PropTypes.arrayOf(PropTypes.string),
  buddiesArray: PropTypes.arrayOf(PropTypes.string),
  currentLocation: PropTypes.object,
  connectionMarkAsRead: PropTypes.func,
  fetchPersonas: PropTypes.func,
  hideModalDialog: PropTypes.func,
  showModalDialog: PropTypes.func,
  connectionClose: PropTypes.func,
  connectionOpen: PropTypes.func,
  connect: PropTypes.func,
  connectAdHoc: PropTypes.func,
  routerGo: PropTypes.func,
  routerGoResetParams: PropTypes.func,
  chatSocketUri: PropTypes.string,
  chatConnectionsArray: PropTypes.arrayOf(PropTypes.object),
  hasChatConnections: PropTypes.bool,
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(WonAtomContentBuddies);
