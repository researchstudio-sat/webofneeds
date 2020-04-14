/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import WonAtomIcon from "./atom-icon.jsx";
import WonGroupIcon from "./group-icon.jsx";
import WonConnectionState from "./connection-state.jsx";
import PropTypes from "prop-types";
import VisibilitySensor from "react-visibility-sensor";
import { get, getIn, generateQueryString } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import { connect } from "react-redux";
import { labels, relativeTime } from "../won-label-utils.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as messageSelectors from "../redux/selectors/message-selectors.js";
import * as connectionSelectors from "../redux/selectors/connection-selectors.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as connectionUtils from "../redux/utils/connection-utils.js";
import * as messageUtils from "../redux/utils/message-utils.js";
import * as processUtils from "../redux/utils/process-utils.js";
import * as accountUtils from "../redux/utils/account-utils.js";
import Immutable from "immutable";

import { getHumanReadableStringFromMessage } from "../reducers/atom-reducer/parse-message.js";

import "~/style/_connection-header.scss";
import ico36_incoming from "~/images/won-icons/ico36_incoming.svg";
import { withRouter } from "react-router-dom";

const mapStateToProps = (state, ownProps) => {
  const ownedAtom = generalSelectors.getOwnedAtomByConnectionUri(
    state,
    ownProps.connectionUri
  );
  const connection = getIn(ownedAtom, ["connections", ownProps.connectionUri]);
  const targetAtomUri = get(connection, "targetAtomUri");
  const targetAtom = get(generalSelectors.getAtoms(state), targetAtomUri);
  const allMessages = messageSelectors.getMessagesByConnectionUri(
    state,
    ownProps.connectionUri
  );
  const unreadMessages = messageSelectors.getUnreadMessagesByConnectionUri(
    state,
    ownProps.connectionUri
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

  const targetAtomLoading = processUtils.isAtomLoading(
    processState,
    get(targetAtom, "uri")
  );

  const isTargetAtomOwned = accountUtils.isAtomOwned(
    get(state, "account"),
    targetAtomUri
  );

  const groupConnections =
    isTargetAtomOwned &&
    get(targetAtom, "connections")
      .filter(
        con => atomUtils.getGroupSocket(targetAtom) === get(con, "socketUri")
      )
      .filter(con => connectionUtils.isRequestReceived(con));

  const hasGroupRequests = groupConnections && groupConnections.size > 0;
  const hasNewGroupRequests = !!(
    hasGroupRequests &&
    groupConnections.find(con => connectionUtils.isUnread(con))
  );

  return {
    connectionUri: ownProps.connectionUri,
    onClick: ownProps.onClick,
    connection,
    groupMembersArray: groupMembers && groupMembers.toArray(),
    groupMembersSize: groupMembers ? groupMembers.size : 0,
    ownedAtom,
    targetAtomUri,
    targetAtom,
    remotePersonaName,
    isConnectionToGroup: connectionSelectors.isChatToGroupConnection(
      generalSelectors.getAtoms(state),
      connection
    ),
    hasGroupRequests: hasGroupRequests,
    hasNewGroupRequests: hasNewGroupRequests,
    isTargetAtomOwned: isTargetAtomOwned,
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
        get(targetAtom, "lastUpdateDate")
      ),
    targetAtomFailedToLoad:
      targetAtom &&
      processUtils.hasAtomFailedToLoad(processState, get(targetAtom, "uri")),
    targetAtomToLoad: processUtils.isAtomToLoad(
      processState,
      get(targetAtom, "uri")
    ),
    targetAtomLoading,
    connectionOrAtomsLoading:
      !connection ||
      !targetAtom ||
      !ownedAtom ||
      processUtils.isAtomLoading(processState, get(ownedAtom, "uri")) ||
      targetAtomLoading ||
      processUtils.isConnectionLoading(processState, get(connection, "uri")),
  };
};

const mapDispatchToProps = dispatch => {
  return {
    fetchAtom: atomUri => {
      dispatch(actionCreators.atoms__fetchUnloadedAtom(atomUri));
    },

    selectTab: (atomUri, tab) => {
      dispatch(
        actionCreators.atoms__selectTab(
          Immutable.fromJS({ atomUri: atomUri, selectTab: tab })
        )
      );
    },
  };
};

class WonConnectionHeader extends React.Component {
  render() {
    if (this.props.connectionOrAtomsLoading) {
      return (
        <won-connection-header class="won-is-loading">
          <VisibilitySensor
            onChange={isVisible => {
              this.onChange(isVisible);
            }}
            intervalDelay={200}
            partialVisibility={true}
            offset={{ top: -300, bottom: -300 }}
          >
            <div className="ch__icon">
              <div className="ch__icon__skeleton" />
            </div>
          </VisibilitySensor>
          <div className="ch__right">
            <div className="ch__right__topline">
              <div className="ch__right__topline__title" />
              <div className="ch__right__topline__date" />
            </div>
            <div className="ch__right__subtitle">
              <span className="ch__right__subtitle__type" />
            </div>
          </div>
        </won-connection-header>
      );
    } else {
      const headerIcon = this.props.isConnectionToGroup ? (
        <div
          className="ch__icon"
          onClick={this.props.onClick ? () => this.props.onClick() : undefined}
        >
          <WonGroupIcon connectionUri={this.props.connectionUri} />
        </div>
      ) : (
        <div
          className="ch__icon"
          onClick={this.props.onClick ? () => this.props.onClick() : undefined}
        >
          <WonAtomIcon atomUri={get(this.props.targetAtom, "uri")} />
        </div>
      );

      let headerRightContent;
      if (this.props.targetAtomFailedToLoad) {
        headerRightContent = (
          <React.Fragment>
            <div className="ch__right__topline">
              <div className="ch__right__topline__notitle">
                Remote Atom Loading failed
              </div>
            </div>
            <div className="ch__right__subtitle">
              <span className="ch__right__subtitle__type">
                <span className="ch__right__subtitle__type__state">
                  Atom might have been deleted, you might want to close this
                  connection.
                </span>
              </span>
            </div>
          </React.Fragment>
        );
      } else {
        let headerRightToplineContent;
        if (!this.props.isDirectResponseFromRemote) {
          if (get(this.props.targetAtom, "humanReadable")) {
            headerRightToplineContent = (
              <div
                className="ch__right__topline__title"
                title={get(this.props.targetAtom, "humanReadable")}
              >
                {get(this.props.targetAtom, "humanReadable")}
              </div>
            );
          } else {
            headerRightToplineContent = (
              <div className="ch__right__topline__notitle" title="no title">
                no title
              </div>
            );
          }
        } else {
          headerRightToplineContent = (
            <div
              className="ch__right__topline__notitle"
              title="Direct Response"
            >
              Direct Response
            </div>
          );
        }

        const personaName =
          this.props.remotePersonaName && !this.props.isGroupChatEnabled ? (
            <span className="ch__right__subtitle__type__persona">
              {this.props.remotePersonaName}
            </span>
          ) : (
            undefined
          );

        const groupChatLabel = this.props.isGroupChatEnabled ? (
          <span className="ch__right__subtitle__type__groupchat">
            {"Group Chat" + (this.props.isChatEnabled ? " enabled" : "")}
          </span>
        ) : (
          undefined
        );

        let unreadCount;

        if (this.props.unreadMessageCount > 1) {
          unreadCount = (
            <span className="ch__right__subtitle__type__unreadcount">
              {this.props.unreadMessageCount + " unread Messages"}
            </span>
          );
        } else if (
          this.props.unreadMessageCount == 1 &&
          !this.props.latestMessageHumanReadableString
        ) {
          unreadCount = (
            <span className="ch__right__subtitle__type__unreadcount">
              1 unread Message
            </span>
          );
        }

        let messageOrState;
        if (this.props.latestMessageHumanReadableString) {
          messageOrState = !(this.props.unreadMessageCount > 1) ? (
            <span
              className={
                "ch__right__subtitle__type__message " +
                (this.props.latestMessageUnread ? "won-unread" : "")
              }
            >
              {this.props.latestMessageHumanReadableString}
            </span>
          ) : (
            undefined
          );
        } else {
          messageOrState = !this.props.unreadMessageCount ? (
            <span className="ch__right__subtitle__type__state">
              {labels.connectionState[get(this.props.connection, "state")]}
            </span>
          ) : (
            undefined
          );
        }

        headerRightContent = (
          <React.Fragment>
            <div className="ch__right__topline">
              {headerRightToplineContent}
            </div>

            <div className="ch__right__subtitle">
              <span className="ch__right__subtitle__type">
                {personaName}
                {groupChatLabel}
                <WonConnectionState
                  connectionUri={get(this.props.connection, "uri")}
                />
                {unreadCount}
                {messageOrState}
              </span>
              <div className="ch__right__subtitle__date">
                {this.props.friendlyTimestamp}
              </div>
            </div>
          </React.Fragment>
        );
      }
      let incomingRequestsIcon =
        this.props.isTargetAtomOwned && this.props.hasGroupRequests ? (
          <div className="ch__indicator">
            <won-connection-indicators>
              <svg
                className={
                  "indicators__item " +
                  (!this.props.hasNewGroupRequests
                    ? " indicators__item--reads "
                    : "") +
                  (this.props.hasNewGroupRequests
                    ? " indicators__item--unreads "
                    : "")
                }
                //TODO
                onClick={() => this.selectMembersTab()}
              >
                <use xlinkHref={ico36_incoming} href={ico36_incoming} />
              </svg>
            </won-connection-indicators>
          </div>
        ) : (
          undefined
        );

      return (
        <won-connection-header class={this.props.onClick ? "clickable" : ""}>
          {headerIcon}
          <div
            className="ch__right"
            onClick={
              this.props.onClick ? () => this.props.onClick() : undefined
            }
          >
            {headerRightContent}
          </div>
          {incomingRequestsIcon}
        </won-connection-header>
      );
    }
  }
  onChange(isVisible) {
    if (isVisible) {
      this.ensureAtomIsLoaded();
    }
  }

  selectMembersTab() {
    this.props.selectTab(this.props.targetAtomUri, "PARTICIPANTS");
    this.props.history.push(
      generateQueryString("/post", { postUri: this.props.targetAtomUri })
    );
  }

  ensureAtomIsLoaded() {
    if (
      this.props.targetAtomUri &&
      (!this.props.targetAtom ||
        (this.props.targetAtomToLoad && !this.props.targetAtomLoading))
    ) {
      this.props.fetchAtom(this.props.targetAtomUri);
    }
  }
}
WonConnectionHeader.propTypes = {
  connectionUri: PropTypes.string.isRequired,
  onClick: PropTypes.func,
  selectTab: PropTypes.func,
  connection: PropTypes.object,
  groupMembersArray: PropTypes.arrayOf(PropTypes.object),
  groupMembersSize: PropTypes.number,
  ownedAtom: PropTypes.object,
  targetAtom: PropTypes.object,
  remotePersonaName: PropTypes.string,
  isConnectionToGroup: PropTypes.bool,
  hasGroupRequests: PropTypes.bool,
  hasNewGroupRequests: PropTypes.bool,
  isTargetAtomOwned: PropTypes.bool,
  isDirectResponseFromRemote: PropTypes.bool,
  isGroupChatEnabled: PropTypes.bool,
  isChatEnabled: PropTypes.bool,
  latestMessageHumanReadableString: PropTypes.string,
  latestMessageUnread: PropTypes.bool,
  unreadMessageCount: PropTypes.number,
  friendlyTimestamp: PropTypes.any,
  targetAtomUri: PropTypes.string,
  targetAtomToLoad: PropTypes.bool,
  targetAtomLoading: PropTypes.bool,
  targetAtomFailedToLoad: PropTypes.bool,
  connectionOrAtomsLoading: PropTypes.bool,
  fetchAtom: PropTypes.func,
  history: PropTypes.object,
};

export default withRouter(
  connect(
    mapStateToProps,
    mapDispatchToProps
  )(WonConnectionHeader)
);
