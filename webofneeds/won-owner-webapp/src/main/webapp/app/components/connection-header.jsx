/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import {get, getIn} from "../utils.js";
import {actionCreators} from "../actions/actions.js";
import {labels, relativeTime} from "../won-label-utils.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as messageSelectors from "../redux/selectors/message-selectors.js";
import * as connectionSelectors from "../redux/selectors/connection-selectors.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as messageUtils from "../redux/utils/message-utils.js";
import * as processUtils from "../redux/utils/process-utils.js";
import {getHumanReadableStringFromMessage} from "../reducers/atom-reducer/parse-message.js";

import "~/style/_connection-header.scss";
import WonAtomIcon from "./atom-icon.jsx";
import WonGroupIcon from "./group-icon";
import WonConnectionState from "./connection-state";

export default class WonConnectionHeader extends React.Component {
  componentDidMount() {
    this.connectionUri = this.props.connectionUri;
    this.disconnect = this.props.ngRedux.connect(
      this.selectFromState.bind(this),
      actionCreators
    )(state => {
      this.setState(state);
    });
  }

  componentWillUnmount() {
    this.disconnect();
  }

  UNSAFE_componentWillReceiveProps(nextProps) {
    this.connectionUri = nextProps.connectionUri;
    this.setState(this.selectFromState(this.props.ngRedux.getState()));
  }

  selectFromState(state) {
    const ownedAtom = generalSelectors.getOwnedAtomByConnectionUri(
      state,
      this.connectionUri
    );
    const connection = getIn(ownedAtom, ["connections", this.connectionUri]);
    const targetAtom = get(generalSelectors.getAtoms(state), get(connection, "targetAtomUri"));
    const allMessages = messageSelectors.getMessagesByConnectionUri(
      state,
      this.connectionUri
    );
    const unreadMessages = messageSelectors.getUnreadMessagesByConnectionUri(
      state,
      this.connectionUri
    );

    const sortedMessages = allMessages && allMessages.toArray();
    if (sortedMessages) {
      sortedMessages.sort(function(a, b) {
        const aDate = get(a, "date");
        const bDate = get(b, "date");

        const aTime = aDate && aDate.getTime();
        const bTime = bDate && bDate.getTime();

        return bTime - aTime;
      });
    }

    const latestMessage = sortedMessages && sortedMessages[0];
    const latestMessageHumanReadableString =
      latestMessage && getHumanReadableStringFromMessage(latestMessage);
    const latestMessageUnread = messageUtils.isMessageUnread(latestMessage);

    const groupMembers = get(targetAtom, "groupMembers");

    const remotePersonaUri = atomUtils.getHeldByUri(targetAtom);
    const remotePersona = getIn(state, ["atoms", remotePersonaUri]);
    const remotePersonaName = get(remotePersona, "humanReadable");

    const processState = get(state, "process");

    return {
      connection,
      groupMembersArray: groupMembers && groupMembers.toArray(),
      groupMembersSize: groupMembers ? groupMembers.size : 0,
      ownedAtom,
      targetAtom,
      remotePersonaName,
      isConnectionToGroup: connectionSelectors.isChatToGroupConnection(
        generalSelectors.getAtoms(state),
        connection
      ),
      isDirectResponseFromRemote: atomUtils.isDirectResponseAtom(targetAtom),
      isGroupChatEnabled: atomUtils.hasGroupSocket(targetAtom),
      isChatEnabled: atomUtils.hasChatSocket(targetAtom),
      latestMessageHumanReadableString,
      latestMessageUnread,
      unreadMessageCount:
        unreadMessages && unreadMessages.size > 0
          ? unreadMessages.size
          : undefined,
      friendlyTimestamp:
        targetAtom &&
        relativeTime(
          generalSelectors.selectLastUpdateTime(state),
          this.timestamp || get(targetAtom, "lastUpdateDate")
        ),
      targetAtomFailedToLoad:
        targetAtom &&
        processUtils.hasAtomFailedToLoad(processState, get(targetAtom, "uri")),
      connectionOrAtomsLoading:
        !connection ||
        !targetAtom ||
        !ownedAtom ||
        processUtils.isAtomLoading(processState, get(ownedAtom, "uri")) ||
        processUtils.isAtomLoading(processState, get(targetAtom, "uri")) ||
        processUtils.isConnectionLoading(processState, get(connection, "uri")),
    };
  }

  render() {
    if (!this.state) {
      console.debug("render with null state");
      return <div/>;
    }

    if (this.state.connectionOrAtomsLoading) {
      return (
        <won-connection-header class="won-is-loading">
          <div className="ch__icon">
            <div className="ch__icon__skeleton"/>
          </div>
          <div className="ch__right">
            <div className="ch__right__topline">
              <div className="ch__right__topline__title"/>
              <div className="ch__right__topline__date"/>
            </div>
            <div className="ch__right__subtitle">
              <span className="ch__right__subtitle__type"/>
            </div>
          </div>
        </won-connection-header>
      );
    } else {
      const headerIcon = this.state.isConnectionToGroup
        ? (
          <WonGroupIcon connectionUri={this.connectionUri} ngRedux={this.props.ngRedux}/>
        )
        : (
          <div className="ch__icon">
            <WonAtomIcon atomUri={get(this.state.targetAtom, "uri")} ngRedux={this.props.ngRedux}/>
          </div>
        );

      let headerRightContent;
      if(this.state.targetAtomFailedToLoad) {
        headerRightContent = (
          <React.Fragment>
            <div className="ch__right__topline">
              <div className="ch__right__topline__notitle">Remote Atom Loading failed</div>
            </div>
            <div className="ch__right__subtitle">
              <span className="ch__right__subtitle__type">
                <span className="ch__right__subtitle__type__state">
                  Atom might have been deleted, you might want to close this connection.
                </span>
              </span>
            </div>
          </React.Fragment>
        )
      } else {
        let headerRightToplineContent;
        if (!this.state.isDirectResponseFromRemote) {
          if (get(this.state.targetAtom, "humanReadable")) {
            headerRightToplineContent = (
              <div className="ch__right__topline__title" title={get(this.state.targetAtom, "humanReadable")}>
                {get(this.state.targetAtom, "humanReadable")}
              </div>
            );
          } else {
            headerRightToplineContent = (
              <div className="ch__right__topline__notitle" title="no title">no title</div>
            );
          }
        } else {
          headerRightToplineContent = (
            <div className="ch__right__topline__notitle" title="Direct Response">Direct Response</div>
          );
        }

        const personaName = (this.state.remotePersonaName && !this.state.isGroupChatEnabled)
          ? (<span className="ch__right__subtitle__type__persona">{this.state.remotePersonaName}</span>)
          : undefined;

        const groupChatLabel = this.state.isGroupChatEnabled
          ? (<span className="ch__right__subtitle__type__groupchat">{"Group Chat" + (this.state.isChatEnabled? " enabled" : "")}</span>)
          : undefined;

        let unreadCount;

        if(this.state.unreadMessageCount > 1) {
          unreadCount = (
            <span className="ch__right__subtitle__type__unreadcount">
              {this.state.unreadMessageCount + " unread Messages"}
            </span>
          );
        } else if(this.state.unreadMessageCount == 1 && !this.state.latestMessageHumanReadableString) {
          unreadCount = (
            <span className="ch__right__subtitle__type__unreadcount">1 unread Message</span>
          );
        }

        let messageOrState;
        if (this.state.latestMessageHumanReadableString) {
          messageOrState = !(this.state.unreadMessageCount > 1)
            ? (
              <span className={"ch__right__subtitle__type__message " + (this.state.latestMessageUnread ? "won-unread" : "")}>
                  {this.state.latestMessageHumanReadableString}
              </span>
            )
            : undefined;
        } else {
          messageOrState = !this.state.unreadMessageCount
            ? (
              <span className="ch__right__subtitle__type__state">
                {labels.connectionState[get(this.state.connection, "state")]}
              </span>
            )
            : undefined;
        }

        headerRightContent = (
          <React.Fragment>
            <div className="ch__right__topline">{headerRightContentTitle}</div>
            <div className="ch__right__subtitle">
              <span className="ch__right__subtitle__type">
                {personaName}
                {groupChatLabel}
                <WonConnectionState connectionUri={get(this.state.connection, "uri")} ngRedux={this.props.ngRedux}/>
                {unreadCount}
                {messageOrState}
              </span>
              <div className="ch__right__subtitle__date">{this.state.friendlyTimestamp}</div>
            </div>
          </React.Fragment>
        );
      }


      return (
        <won-connection-header>
          {headerIcon}
          <div className="ch__right">
            {headerRightContent}
          </div>
        </won-connection-header>
      );
    }
  }

  ensureAtomIsLoaded() {
    //TODO: Fetch atoms if not present
    if (
      this.state.atomUri &&
      (!this.state.atom || (this.state.atomToLoad && !this.state.atomLoading))
    ) {
      this.props.ngRedux.dispatch(actionCreators.atoms__fetchUnloadedAtom(this.state.atomUri));
    }
  }
}