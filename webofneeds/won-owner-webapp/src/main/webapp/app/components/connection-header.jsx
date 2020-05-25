/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import { Link, useHistory } from "react-router-dom";
import WonAtomIcon from "./atom-icon.jsx";
import WonGroupIcon from "./group-icon.jsx";
import WonConnectionState from "./connection-state.jsx";
import PropTypes from "prop-types";
import VisibilitySensor from "react-visibility-sensor";
import { get, getIn, generateLink } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import { useSelector, useDispatch } from "react-redux";
import { labels, relativeTime } from "../won-label-utils.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as connectionUtils from "../redux/utils/connection-utils.js";
import * as messageUtils from "../redux/utils/message-utils.js";
import * as processUtils from "../redux/utils/process-utils.js";
import * as accountUtils from "../redux/utils/account-utils.js";
import vocab from "../service/vocab.js";
import Immutable from "immutable";

import { getHumanReadableStringFromMessage } from "../reducers/atom-reducer/parse-message.js";

import "~/style/_connection-header.scss";
import "~/style/_connection-indicators.scss";

import ico36_incoming from "~/images/won-icons/ico36_incoming.svg";

export default function WonConnectionHeader({ connection, toLink }) {
  const connectionUri = get(connection, "uri");
  const history = useHistory();
  const dispatch = useDispatch();
  const senderAtom = useSelector(state =>
    generalSelectors.getOwnedAtomByConnectionUri(state, connectionUri)
  );

  const targetAtomUri = get(connection, "targetAtomUri");
  const targetAtom = useSelector(state =>
    get(generalSelectors.getAtoms(state), targetAtomUri)
  );

  const targetHolderName = useSelector(state =>
    getIn(state, ["atoms", atomUtils.getHeldByUri(targetAtom), "humanReadble"])
  );

  const processState = useSelector(generalSelectors.getProcessState);

  const targetAtomLoading = processUtils.isAtomLoading(
    processState,
    get(targetAtom, "uri")
  );

  const isTargetAtomOwned = useSelector(state =>
    accountUtils.isAtomOwned(
      generalSelectors.getAccountState(state),
      targetAtomUri
    )
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

  const isConnectionToGroup =
    atomUtils.getGroupSocket(targetAtom) === get(connection, "targetSocketUri");

  const isDirectResponseFromRemote = atomUtils.isDirectResponseAtom(targetAtom);

  const globalLastUpdateTime = useSelector(
    generalSelectors.selectLastUpdateTime
  );
  const friendlyTimestamp =
    connection &&
    relativeTime(globalLastUpdateTime, get(connection, "lastUpdateDate"));
  const connectionOrAtomsLoading =
    !connection ||
    !targetAtom ||
    processUtils.isAtomToLoad(processState, targetAtomUri) ||
    !senderAtom ||
    processUtils.isAtomLoading(processState, get(senderAtom, "uri")) ||
    targetAtomLoading ||
    processUtils.isConnectionLoading(processState, get(connection, "uri"));

  function onChange(isVisible) {
    if (isVisible) {
      ensureAtomIsLoaded();
    }
  }

  function selectMembersTab() {
    dispatch(
      actionCreators.atoms__selectTab(
        Immutable.fromJS({
          atomUri: targetAtomUri,
          selectTab: vocab.GROUP.GroupSocketCompacted,
        })
      )
    );
    history.push(
      generateLink(history.location, { postUri: targetAtomUri }, "/post")
    );
  }

  function ensureAtomIsLoaded() {
    if (
      targetAtomUri &&
      (!targetAtom ||
        (processUtils.isAtomToLoad(processState, get(targetAtom, "uri")) &&
          !targetAtomLoading))
    ) {
      dispatch(actionCreators.atoms__fetchUnloadedAtom(targetAtomUri));
    }
  }

  if (connectionOrAtomsLoading) {
    return (
      <won-connection-header class="won-is-loading">
        <VisibilitySensor
          onChange={isVisible => {
            onChange(isVisible);
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
    const headerIconElement = isConnectionToGroup ? (
      <WonGroupIcon connection={connection} />
    ) : (
      <WonAtomIcon atom={targetAtom} />
    );

    const headerIcon = toLink ? (
      <Link className="ch__icon" to={toLink}>
        {headerIconElement}
      </Link>
    ) : (
      <div className="ch__icon">{headerIconElement}</div>
    );

    let headerRightContent;
    if (
      processUtils.hasAtomFailedToLoad(processState, get(targetAtom, "uri"))
    ) {
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
      const allMessages = get(connection, "messages");
      const unreadMessages =
        allMessages &&
        allMessages.filter(msg => messageUtils.isMessageUnread(msg));

      const unreadMessageCount =
        unreadMessages && unreadMessages.size > 0
          ? unreadMessages.size
          : undefined;

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

      let headerRightToplineContent;
      if (!isDirectResponseFromRemote) {
        if (get(targetAtom, "humanReadable")) {
          headerRightToplineContent = (
            <div
              className="ch__right__topline__title"
              title={get(targetAtom, "humanReadable")}
            >
              {get(targetAtom, "humanReadable")}
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
          <div className="ch__right__topline__notitle" title="Direct Response">
            Direct Response
          </div>
        );
      }

      const personaName =
        targetHolderName && !atomUtils.hasGroupSocket(targetAtom) ? (
          <span className="ch__right__subtitle__type__persona">
            {targetHolderName}
          </span>
        ) : (
          undefined
        );

      const groupChatLabel = atomUtils.hasGroupSocket(targetAtom) ? (
        <span className="ch__right__subtitle__type__groupchat">
          {"Group Chat" +
            (atomUtils.hasChatSocket(targetAtom) ? " enabled" : "")}
        </span>
      ) : (
        undefined
      );

      let unreadCount;

      if (unreadMessageCount > 1) {
        unreadCount = (
          <span className="ch__right__subtitle__type__unreadcount">
            {unreadMessageCount + " unread Messages"}
          </span>
        );
      } else if (unreadMessageCount == 1 && !latestMessageHumanReadableString) {
        unreadCount = (
          <span className="ch__right__subtitle__type__unreadcount">
            1 unread Message
          </span>
        );
      }

      let messageOrState;
      if (latestMessageHumanReadableString) {
        messageOrState = !(unreadMessageCount > 1) ? (
          <span
            className={
              "ch__right__subtitle__type__message " +
              (latestMessageUnread ? "won-unread" : "")
            }
          >
            {latestMessageHumanReadableString}
          </span>
        ) : (
          undefined
        );
      } else {
        messageOrState = !unreadMessageCount ? (
          <span className="ch__right__subtitle__type__state">
            {labels.connectionState[get(connection, "state")]}
          </span>
        ) : (
          undefined
        );
      }

      headerRightContent = (
        <React.Fragment>
          <div className="ch__right__topline">{headerRightToplineContent}</div>

          <div className="ch__right__subtitle">
            <span className="ch__right__subtitle__type">
              {personaName}
              {groupChatLabel}
              <WonConnectionState connection={connection} />
              {unreadCount}
              {messageOrState}
            </span>
            <div className="ch__right__subtitle__date">{friendlyTimestamp}</div>
          </div>
        </React.Fragment>
      );
    }
    let incomingRequestsIcon =
      isTargetAtomOwned && hasGroupRequests ? (
        <div className="ch__indicator">
          <won-connection-indicators>
            <svg
              className={
                "indicators__item " +
                (!hasNewGroupRequests ? " indicators__item--reads " : "") +
                (hasNewGroupRequests ? " indicators__item--unreads " : "")
              }
              //TODO
              onClick={() => selectMembersTab()}
            >
              <use xlinkHref={ico36_incoming} href={ico36_incoming} />
            </svg>
          </won-connection-indicators>
        </div>
      ) : (
        undefined
      );

    return (
      <won-connection-header class={toLink ? "clickable" : ""}>
        {headerIcon}
        {toLink ? (
          <Link className="ch__right" to={toLink}>
            {headerRightContent}
          </Link>
        ) : (
          <div className="ch__right">{headerRightContent}</div>
        )}
        {incomingRequestsIcon}
      </won-connection-header>
    );
  }
}
WonConnectionHeader.propTypes = {
  connection: PropTypes.object.isRequired,
  toLink: PropTypes.string,
};
