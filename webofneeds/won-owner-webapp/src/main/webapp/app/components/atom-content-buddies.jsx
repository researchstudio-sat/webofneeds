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
import WonLabelledHr from "./labelled-hr.jsx";
import WonSuggestAtomPicker from "./details/picker/suggest-atom-picker.jsx";
import WonAtomCard from "./atom-card.jsx";

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
      true
    );

  let excludedFromRequestUris = [ownProps.atomUri];

  if (buddyConnections) {
    buddyConnections.map(conn =>
      excludedFromRequestUris.push(get(conn, "targetAtomUri"))
    );
  }

  return {
    atomUri: ownProps.atomUri,
    atom,
    isOwned,
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
    //TODO: FETCH OTHER PERSONAS AND LIMIT TO ONE FETCH I FEEL THATS IMPORTANT
    this.props.fetchPersonas();
  }

  render() {
    let buddies;

    if (this.props.isOwned) {
      if (this.props.hasBuddyConnections) {
        buddies = this.props.buddyConnectionsArray.map(conn => {
          if (!connectionUtils.isClosed(conn)) {
            let actionButtons;

            if (connectionUtils.isRequestReceived(conn)) {
              actionButtons = (
                <div className="acb__buddy__actions">
                  <div
                    className="acb__buddy__actions__button red won-button--outlined thin"
                    onClick={() => this.openRequest(conn)}
                  >
                    Accept
                  </div>
                  <div
                    className="acb__buddy__actions__button red won-button--outlined thin"
                    onClick={() =>
                      this.closeConnection(conn, "Reject Buddy Request?")
                    }
                  >
                    Reject
                  </div>
                </div>
              );
            } else if (connectionUtils.isSuggested(conn)) {
              actionButtons = (
                <div className="acb__buddy__actions">
                  <div
                    className="acb__buddy__actions__button red won-button--outlined thin"
                    onClick={() => this.requestBuddy(conn)}
                  >
                    Request
                  </div>
                  <div
                    className="acb__buddy__actions__button red won-button--outlined thin"
                    onClick={() =>
                      this.closeConnection(conn, "Reject Buddy Suggestion?")
                    }
                  >
                    Remove
                  </div>
                </div>
              );
            } else if (connectionUtils.isRequestSent(conn)) {
              actionButtons = (
                <div className="acb__buddy__actions">
                  <div
                    className="acb__buddy__actions__button red won-button--outlined thin"
                    disabled={true}
                  >
                    Waiting for Accept...
                  </div>
                  <div
                    className="acb__buddy__actions__button red won-button--outlined thin"
                    onClick={() =>
                      this.closeConnection(conn, "Cancel Buddy Request?")
                    }
                  >
                    Cancel
                  </div>
                </div>
              );
            } else if (connectionUtils.isConnected(conn)) {
              actionButtons = (
                <div className="acb__buddy__actions">
                  <div
                    className="acb__buddy__actions__button red won-button--outlined thin"
                    onClick={() => this.closeConnection(conn)}
                  >
                    Remove
                  </div>
                </div>
              );
            } else {
              actionButtons = <div className="acb__buddy__actions" />;
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
                  <WonAtomCard
                    atomUri={get(conn, "targetAtomUri")}
                    currentLocation={this.props.currentLocation}
                    showSuggestions={false}
                    showPersona={false}
                  />
                  {actionButtons}
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
          return (
            <div className="acb__buddy" key={memberUri}>
              <WonAtomCard
                atomUri={memberUri}
                currentLocation={this.props.currentLocation}
                showSuggestions={false}
                showPersona={false}
              />
              <div className="acb__buddy__actions" />
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
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(WonAtomContentBuddies);
