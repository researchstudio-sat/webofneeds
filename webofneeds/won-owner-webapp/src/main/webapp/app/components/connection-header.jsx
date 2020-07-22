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
  generateLink,
  extractAtomUriFromConnectionUri,
} from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import { useSelector, useDispatch } from "react-redux";
import { relativeTime } from "../won-label-utils.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as connectionUtils from "../redux/utils/connection-utils.js";
import * as messageUtils from "../redux/utils/message-utils.js";
import * as processUtils from "../redux/utils/process-utils.js";
import * as accountUtils from "../redux/utils/account-utils.js";
import vocab from "../service/vocab.js";

import "~/style/_connection-header.scss";
import "~/style/_connection-indicators.scss";

import ico36_incoming from "~/images/won-icons/ico36_incoming.svg";

/**
 * React Component that shows the Connection Header
 * @param connection connection to show Header for
 * @param toLink link to go to on click
 * @param flip reverses the displayed atom of the connection
 * @param hideTimestamp hides Timestamp
 * @param hideMessageIndicator hides MessageIndicator (either text of latestMessage, or unread count)
 * @returns {*}
 * @constructor
 */
export default function WonConnectionHeader({
  connection,
  toLink,
  flip,
  hideTimestamp,
  hideMessageIndicator,
}) {
  const connectionUri = get(connection, "uri");
  const history = useHistory();
  const dispatch = useDispatch();

  const senderAtomUri = flip
    ? get(connection, "targetAtomUri")
    : extractAtomUriFromConnectionUri(connectionUri);
  const targetAtomUri = flip
    ? extractAtomUriFromConnectionUri(connectionUri)
    : get(connection, "targetAtomUri");

  const senderAtom = useSelector(generalSelectors.getAtom(senderAtomUri));
  const targetAtom = useSelector(generalSelectors.getAtom(targetAtomUri));

  const processState = useSelector(generalSelectors.getProcessState);
  const accountState = useSelector(generalSelectors.getAccountState);
  const externalDataState = useSelector(generalSelectors.getExternalDataState);

  const targetHolderUri = atomUtils.getHeldByUri(targetAtom);
  const targetHolderAtom = useSelector(
    generalSelectors.getAtom(targetHolderUri)
  );

  const globalLastUpdateTime = useSelector(
    generalSelectors.selectLastUpdateTime
  );

  const isTargetAtomFetchNecessary = processUtils.isAtomFetchNecessary(
    processState,
    targetAtomUri,
    targetAtom
  );
  const isTargetHolderFetchNecessary = processUtils.isAtomFetchNecessary(
    processState,
    targetHolderUri,
    targetHolderAtom
  );

  const connectionOrAtomsLoading =
    !connection ||
    isTargetAtomFetchNecessary ||
    isTargetHolderFetchNecessary ||
    !senderAtom ||
    processUtils.isAtomLoading(processState, get(senderAtom, "uri")) ||
    processUtils.isConnectionLoading(processState, get(connection, "uri"));

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

  function ensureTargetAtomIsFetched() {
    if (isTargetAtomFetchNecessary) {
      console.debug("fetch targetAtomUri, ", targetAtomUri);
      dispatch(actionCreators.atoms__fetchUnloadedAtom(targetAtomUri));
    }
  }

  function ensureTargetHolderIsFetched() {
    if (isTargetHolderFetchNecessary) {
      console.debug("fetch targetHolderUri, ", targetHolderUri);
      dispatch(actionCreators.atoms__fetchUnloadedAtom(targetHolderUri));
    }
  }

  if (connectionOrAtomsLoading) {
    const onChange = isVisible => {
      if (isVisible) {
        ensureTargetAtomIsFetched();
        ensureTargetHolderIsFetched();
      }
    };

    return (
      <won-connection-header class="won-is-loading">
        <VisibilitySensor
          onChange={onChange}
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
              Atom might have been deleted, you might want to close this
              connection.
            </span>
          </div>
        </React.Fragment>
      );
    } else {
      const title = atomUtils.getTitle(targetAtom, externalDataState);

      let subtitleElement;
      if (!hideMessageIndicator) {
        const allMessages = connectionUtils.getMessages(connection);
        const unreadMessages =
          allMessages &&
          allMessages.filter(msg => messageUtils.isMessageUnread(msg));

        const unreadMessageCount =
          unreadMessages && unreadMessages.size > 0
            ? unreadMessages.size
            : undefined;

        if (unreadMessageCount > 1) {
          subtitleElement = (
            <span className="ch__right__subtitle__type__unreadcount">
              {`${unreadMessageCount} unread Messages`}
            </span>
          );
        } else {
          const latestMessage = allMessages && allMessages.last();

          if (latestMessage) {
            subtitleElement = (
              <span
                className={
                  "ch__right__subtitle__type__message " +
                  (messageUtils.isMessageUnread(latestMessage)
                    ? "won-unread"
                    : "")
                }
              >
                {messageUtils.getHumanReadableString(latestMessage)}
              </span>
            );
          } else if (unreadMessageCount === 1) {
            subtitleElement = (
              <span className="ch__right__subtitle__type__unreadcount">
                1 unread Message
              </span>
            );
          }
        }
      }

      let groupChatLabelOrPersonaName;
      if (atomUtils.hasGroupSocket(targetAtom)) {
        groupChatLabelOrPersonaName = (
          <span className="ch__right__subtitle__type__groupchat">
            {`Group Chat ${
              atomUtils.hasChatSocket(targetAtom) ? "enabled" : ""
            }`}
          </span>
        );
      } else {
        const targetHolderName = atomUtils.hasHoldableSocket(targetAtom)
          ? atomUtils.getTitle(targetHolderAtom, externalDataState) ||
            get(targetAtom, "fakePersonaName")
          : undefined;

        if (targetHolderName) {
          groupChatLabelOrPersonaName = (
            <span className="ch__right__subtitle__type__holder">
              {targetHolderName}
            </span>
          );
        }
      }

      let timeStampElement;
      if (!hideTimestamp) {
        const friendlyTimestamp =
          connection &&
          relativeTime(globalLastUpdateTime, get(connection, "lastUpdateDate"));
        timeStampElement = friendlyTimestamp ? (
          <div className="ch__right__subtitle__date">{friendlyTimestamp}</div>
        ) : (
          undefined
        );
      }

      headerRightContent = (
        <React.Fragment>
          <div className="ch__right__topline">
            {title ? (
              <div className="ch__right__topline__title" title={title}>
                {title}
              </div>
            ) : (
              <div className="ch__right__topline__notitle" title="No Title">
                No Title
              </div>
            )}
          </div>

          <div className="ch__right__subtitle">
            <span className="ch__right__subtitle__type">
              {groupChatLabelOrPersonaName}
              <WonConnectionState connection={connection} />
              {subtitleElement}
            </span>
            {timeStampElement}
          </div>
        </React.Fragment>
      );
    }

    let incomingRequestsIcon;

    if (
      atomUtils.hasGroupSocket(targetAtom) &&
      accountUtils.isAtomOwned(accountState, targetAtomUri)
    ) {
      const groupConnectionRequests = atomUtils.getRequestReceivedConnections(
        targetAtom,
        vocab.GROUP.GroupSocketCompacted
      );

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
  hideTimestamp: PropTypes.bool,
  hideMessageIndicator: PropTypes.bool,
};
