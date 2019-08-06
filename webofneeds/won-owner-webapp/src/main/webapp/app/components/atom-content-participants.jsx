/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import {get, getIn} from "../utils.js";
import {actionCreators} from "../actions/actions.js";
import * as atomUtils from "../redux/utils/atom-utils";
import * as generalSelectors from "../redux/selectors/general-selectors";
import * as connectionSelectors from "../redux/selectors/connection-selectors";
import * as connectionUtils from "../redux/utils/connection-utils";
import won from "../won-es6";
import WonLabelledHr from "./labelled-hr.jsx";
import WonSuggestAtomPicker from "./details/picker/suggest-atom-picker.jsx";
import WonAtomCard from "./atom-card.jsx";

import "~/style/_atom-content-participants.scss";
import VisibilitySensor from "react-visibility-sensor";

export default class WonAtomContentParticipants extends React.Component {
  constructor(props){
    super(props);
    this.localState = {
      suggestAtomExpanded: false
    };
  }


  componentDidMount() {
    this.atomUri = this.props.atomUri;
    this.disconnect = this.props.ngRedux.connect(
      this.selectFromState.bind(this),
      actionCreators
    )(state => {
      this.setState({...state, ...this.localState});
    });
  }

  componentWillUnmount() {
    this.disconnect();
  }

  UNSAFE_componentWillReceiveProps(nextProps) {
    this.atomUri = nextProps.atomUri;
    this.setState({...this.selectFromState(this.props.ngRedux.getState()), ...this.localState});
  }

  toggleSuggestions() {
    this.localState.suggestAtomExpanded = !this.localState.suggestAtomExpanded;
    this.setState({...this.state, ...this.localState});
  }

  selectFromState(state) {
    const post = getIn(state, ["atoms", this.atomUri]);
    const isOwned = generalSelectors.isAtomOwned(state, this.atomUri);

    const hasGroupSocket = atomUtils.hasGroupSocket(post);

    const groupMembers = hasGroupSocket && get(post, "groupMembers");
    const groupChatConnections =
      isOwned &&
      hasGroupSocket &&
      connectionSelectors.getGroupChatConnectionsByAtomUri(
        state,
        this.atomUri
      );

    let excludedFromInviteUris = [this.atomUri];

    if (groupChatConnections) {
      groupChatConnections
        .filter(conn => !connectionUtils.isClosed(conn))
        .map(conn =>
          excludedFromInviteUris.push(get(conn, "targetAtomUri"))
        );
    }

    return {
      post,
      isOwned,
      hasGroupSocket,
      groupMembers: groupMembers && groupMembers.size > 0,
      hasGroupChatConnections:
        groupChatConnections && groupChatConnections.size > 0,
      groupChatConnectionsArray:
        groupChatConnections && groupChatConnections.toArray(),
      excludedFromInviteUris,
      groupMembersArray: groupMembers && groupMembers.toArray(),
    };
  }

  render() {
    if (!this.state) {
      console.debug("render with null state");
      return <div/>;
    }

    let participants;

    if (this.state.isOwned) {
      if (this.state.hasGroupChatConnections) {
        participants = this.state.groupChatConnectionsArray.map(conn => {
          if(!connectionUtils.isClosed(conn)) {
            let actionButtons;

            if(connectionUtils.isRequestReceived(conn)) {
              actionButtons = (
                <div className="acp__participant__actions">
                  <div
                    className="acp__participant__actions__button red won-button--outlined thin"
                    onClick={() => this.openRequest(conn)}>
                    Accept
                  </div>
                  <div
                    className="acp__participant__actions__button red won-button--outlined thin"
                    onClick={() => this.closeConnection(conn)}>
                    Reject
                  </div>
                </div>
              );
            } else if (connectionUtils.isSuggested(conn)) {
              actionButtons = (
                <div className="acp__participant__actions">
                  <div
                    className="acp__participant__actions__button red won-button--outlined thin"
                    onClick={() => this.sendRequest(conn)}>
                    Request
                  </div>
                  <div
                    className="acp__participant__actions__button red won-button--outlined thin"
                    onClick={() => this.closeConnection(conn)}>
                    Remove
                  </div>
                </div>
              )
            } else if (connectionUtils.isRequestSent(conn)) {
              actionButtons = (
                <div className="acp__participant__actions">
                  <div className="acp__participant__actions__button red won-button--outlined thin" disabled={true}>
                    Waiting for Accept...
                  </div>
                </div>
              );
            } else if (connectionUtils.isConnected(conn)) {
              actionButtons = (
                <div className="acp__participant__actions">
                  <div
                    className="acp__participant__actions__button red won-button--outlined thin"
                    onClick={() => this.closeConnection(conn)}>
                    Remove
                  </div>
                </div>
              );
            } else {
              actionButtons = (
                <div className="acp__participant__actions"/>
              );
            }

            return (
              <VisibilitySensor key={get(conn, "uri")} onChange={(isVisible) => { isVisible && connectionUtils.isUnread(conn) && this.markAsRead(conn) }} intervalDelay={2000}>
                <div className={"acp__participant " + (connectionUtils.isUnread(conn) ? " won-unread " : "")}>
                  <WonAtomCard atomUri={get(conn, 'targetAtomUri')} currentLocation={this.state.currentLocation} showSuggestions={false} showPersona={true} ngRedux={this.props.ngRedux}/>
                  {actionButtons}
                </div>
              </VisibilitySensor>
            );
          }
        });
      } else {
        participants = (
          <div className="acp__empty">No Groupmembers present.</div>
        );
      }

      return (
        <won-atom-content-participants>
          {participants}
          <WonLabelledHr label="Invite" arrow={this.state.suggestAtomExpanded? "up" : "down"} onClick={() => this.toggleSuggestions()}/>
          {
            this.state.suggestAtomExpanded
            ? (
              <WonSuggestAtomPicker
                initialValue={undefined}
                onUpdate={({value}) => this.inviteParticipant(value)}
                detail={{placeholder: "Insert AtomUri to invite"}}
                excludedUris={this.state.excludedFromInviteUris}
                allowedSockets={[won.CHAT.ChatSocketCompacted, won.GROUP.GroupSocketCompacted]}
                excludedText="Invitation does not work for atoms that are already part of the Group, or the group itself"
                notAllowedSocketText="Invitation does not work on atoms without Group or Chat Socket"
                noSuggestionsText="No Participants available to invite"
                ngRedux={this.props.ngRedux}
              />
            )
            : undefined
          }
        </won-atom-content-participants>
      );
    } else {
      if (this.state.groupMembers) {
        participants = this.state.groupMembersArray.map(memberUri => {
          return (
            <div className="acp__participant" key={memberUri}>
              <WonAtomCard atomUri={memberUri} currentLocation={this.state.currentLocation} showSuggestions={false} showPersona={true} ngRedux={this.props.ngRedux}/>
              <div className="acp__participant__actions"></div>
            </div>
          );
        });
      } else {
        participants = (
          <div className="acp__empty">No Groupmembers present.</div>
        );
      }

      return (
        <won-atom-content-participants>
          {participants}
        </won-atom-content-participants>
      );
    }
  }

  closeConnection(conn, rateBad = false) {
    if (!conn) {
      return;
    }

    const connUri = get(conn, "uri");

    if (rateBad) {
      this.props.ngRedux.dispatch(actionCreators.connections__rate(connUri, won.WONCON.binaryRatingBad));
    }

    if (connectionUtils.isUnread(conn)) {
      this.props.ngRedux.dispatch(actionCreators.connections__markAsRead({
        connectionUri: connUri,
        atomUri: this.atomUri,
      }));
    }

    this.props.ngRedux.dispatch(actionCreators.connections__close(connUri));
  }

  openRequest(conn, message = "") {
    if (!conn) {
      return;
    }

    const connUri = get(conn, "uri");

    if (connectionUtils.isUnread(conn)) {
      this.props.ngRedux.dispatch(actionCreators.connections__markAsRead({
        connectionUri: connUri,
        atomUri: this.atomUri,
      }));
    }

    this.props.ngRedux.dispatch(actionCreators.connections__open(connUri, message));
  }

  sendRequest(conn, message = "") {
    if (!conn) {
      return;
    }

    const payload = {
      caption: "Group",
      text: "Add as Participant?",
      buttons: [
        {
          caption: "Yes",
          callback: () => {
            const connUri = get(conn, "uri");
            const targetAtomUri = get(conn, "targetAtomUri");

            if (connectionUtils.isUnread(conn)) {
              this.props.ngRedux.dispatch(actionCreators.connections__markAsRead({
                connectionUri: connUri,
                atomUri: this.atomUri,
              }));
            }

            this.props.ngRedux.dispatch(actionCreators.connections__rate(connUri, won.WONCON.binaryRatingGood));
            this.props.ngRedux.dispatch(actionCreators.atoms__connect(
              this.atomUri,
              connUri,
              targetAtomUri,
              message
            ));
            this.props.ngRedux.dispatch(actionCreators.view__hideModalDialog());
          },
        },
        {
          caption: "No",
          callback: () => {
            this.props.ngRedux.dispatch(actionCreators.view__hideModalDialog());
          },
        },
      ],
    };
    this.props.ngRedux.dispatch(actionCreators.view__showModalDialog(payload));
  }

  inviteParticipant(atomUri, message = "") {
    if (!this.state.isOwned || !this.state.hasGroupSocket) {
      console.warn("Trying to invite to a non-owned or non groupSocket atom");
      return;
    }
    this.props.ngRedux.dispatch(actionCreators.atoms__connect(this.atomUri, undefined, atomUri, message));
  }

  markAsRead(conn) {
    if (connectionUtils.isUnread(conn)) {
      this.props.ngRedux.dispatch(actionCreators.connections__markAsRead({
        connectionUri: get(conn, "uri"),
        atomUri: this.atomUri,
      }));
    }
  }
}