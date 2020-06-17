/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import { Link, useHistory } from "react-router-dom";
import WonAtomIcon from "./atom-icon.jsx";
import WonConnectionState from "./connection-state.jsx";
import PropTypes from "prop-types";
import VisibilitySensor from "react-visibility-sensor";
import {
  get,
  getIn,
  generateLink,
  extractAtomUriFromConnectionUri,
} from "../utils.js";
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

import { getHumanReadableStringFromMessage } from "../reducers/atom-reducer/parse-message.js";

import "~/style/_connection-header.scss";
import "~/style/_connection-indicators.scss";

import ico36_incoming from "~/images/won-icons/ico36_incoming.svg";

export default function WonConnectionHeader({ connection, toLink, flip }) {
  const connectionUri = get(connection, "uri");
  const history = useHistory();
  const dispatch = useDispatch();

  const senderAtomUri = flip
    ? get(connection, "targetAtomUri")
    : extractAtomUriFromConnectionUri(connectionUri);
  const targetAtomUri = flip
    ? extractAtomUriFromConnectionUri(connectionUri)
    : get(connection, "targetAtomUri");

  const senderAtom = useSelector(state =>
    getIn(state, ["atoms", senderAtomUri])
  );
  const targetAtom = useSelector(state =>
    getIn(state, ["atoms", targetAtomUri])
  );

  const targetHolderName = useSelector(
    state =>
      atomUtils.hasHoldableSocket(targetAtom) &&
      (getIn(state, [
        "atoms",
        atomUtils.getHeldByUri(targetAtom),
        "humanReadable",
      ]) ||
        get(targetAtom, "fakePersonaName"))
  );

  const processState = useSelector(generalSelectors.getProcessState);
  const accountState = useSelector(generalSelectors.getAccountState);

  const isTargetAtomOwned = accountUtils.isAtomOwned(
    accountState,
    targetAtomUri
  );

  const globalLastUpdateTime = useSelector(
    generalSelectors.selectLastUpdateTime
  );
  const friendlyTimestamp =
    connection &&
    relativeTime(globalLastUpdateTime, get(connection, "lastUpdateDate"));
  const connectionOrAtomsLoading =
    !connection ||
    !targetAtom ||
    !senderAtom ||
    processUtils.isAtomToLoad(processState, targetAtomUri) ||
    processUtils.isAtomLoading(processState, get(senderAtom, "uri")) ||
    processUtils.isAtomLoading(processState, get(targetAtom, "uri")) ||
    processUtils.isConnectionLoading(processState, get(connection, "uri"));

  function onChange(isVisible) {
    if (isVisible) {
      ensureAtomIsLoaded();
    }
  }

  function selectMembersTab() {
    history.push(
      generateLink(
        history.location,
        {
          connectionUri: undefined,
          tab: vocab.GROUP.GroupSocketCompacted,
          postUri: targetAtomUri,
        },
        "/post"
      )
    );
  }

  function ensureAtomIsLoaded() {
    if (
      targetAtomUri &&
      (!targetAtom ||
        (processUtils.isAtomToLoad(processState, get(targetAtom, "uri")) &&
          !processUtils.isAtomLoading(processState, get(targetAtom, "uri"))))
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
    const headerIcon = toLink ? (
      <Link className="ch__icon" to={toLink}>
        <WonAtomIcon
          atom={targetAtom}
          flipIcons={
            atomUtils.getGroupSocket(targetAtom) !==
            get(connection, "targetSocketUri")
          }
        />
      </Link>
    ) : (
      <div className="ch__icon">
        <WonAtomIcon
          atom={targetAtom}
          flipIcons={
            atomUtils.getGroupSocket(targetAtom) !==
            get(connection, "targetSocketUri")
          }
        />
      </div>
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

      const title = get(targetAtom, "humanReadable");

      const headerRightToplineContent = title ? (
        <div className="ch__right__topline__title" title={title}>
          {title}
        </div>
      ) : (
        <div className="ch__right__topline__notitle" title="No Title">
          No Title
        </div>
      );

      const groupChatLabelOrPersonaName = atomUtils.hasGroupSocket(
        targetAtom
      ) ? (
        <span className="ch__right__subtitle__type__groupchat">
          {"Group Chat" +
            (atomUtils.hasChatSocket(targetAtom) ? " enabled" : "")}
        </span>
      ) : targetHolderName ? (
        <span className="ch__right__subtitle__type__persona">
          {targetHolderName}
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
              {groupChatLabelOrPersonaName}
              <WonConnectionState connection={connection} />
              {unreadCount}
              {messageOrState}
            </span>
            <div className="ch__right__subtitle__date">{friendlyTimestamp}</div>
          </div>
        </React.Fragment>
      );
    }

    let incomingRequestsIcon;

    if (isTargetAtomOwned) {
      const groupConnectionRequests = atomUtils
        .getConnections(targetAtom, vocab.GROUP.GroupSocketCompacted)
        .filter(con => connectionUtils.isRequestReceived(con));

      if (groupConnectionRequests && groupConnectionRequests.size > 0) {
        const hasNewGroupRequests = !!groupConnectionRequests.find(con =>
          connectionUtils.isUnread(con)
        );

        incomingRequestsIcon = (
          <div className="ch__indicator">
            <won-connection-indicators>
              <svg
                className={
                  "indicators__item " +
                  (hasNewGroupRequests
                    ? " indicators__item--unreads "
                    : " indicators__item--reads ")
                }
                onClick={selectMembersTab}
              >
                <use xlinkHref={ico36_incoming} href={ico36_incoming} />
              </svg>
            </won-connection-indicators>
          </div>
        );
      }
    }

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
  flip: PropTypes.bool,
};
