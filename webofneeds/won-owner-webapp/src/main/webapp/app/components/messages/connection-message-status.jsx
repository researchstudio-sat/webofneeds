/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import PropTypes from "prop-types";
import { relativeTime } from "../../won-label-utils.js";
import { get } from "../../utils.js";
import { useSelector } from "react-redux";
import { selectLastUpdateTime } from "../../redux/selectors/general-selectors.js";
import * as messageUtils from "../../redux/utils/message-utils.js";

import "~/style/_connection-message-status.scss";
import ico16_indicator_warning from "~/images/won-icons/ico16_indicator_warning.svg";
import ico36_added_circle from "~/images/won-icons/ico36_added_circle.svg";

export default function WonConnectionMessageStatus({ message }) {
  const lastUpdateTime = useSelector(selectLastUpdateTime);

  const isOutgoingMessage = get(message, "outgoingMessage");
  const isFailedToSend = messageUtils.hasFailedToSend(message);
  const isReceivedByOwn = messageUtils.isReceivedByOwn(message);
  const isReceivedByRemote = messageUtils.isReceivedByRemote(message);

  let statusIcons;

  if (isOutgoingMessage) {
    let icons;
    if (isFailedToSend) {
      icons = (
        <svg
          className="msgstatus__icons__icon"
          style={{ "--local-primary": "red" }}
        >
          <use
            xlinkHref={ico16_indicator_warning}
            href={ico16_indicator_warning}
          />
        </svg>
      );
    } else {
      icons = (
        <React.Fragment>
          <svg
            className={
              "msgstatus__icons__icon " + (isReceivedByOwn ? " received " : "")
            }
          >
            <use xlinkHref={ico36_added_circle} href={ico36_added_circle} />
          </svg>
          <svg
            className={
              "msgstatus__icons__icon " +
              (isReceivedByRemote ? " received " : "")
            }
          >
            <use xlinkHref={ico36_added_circle} href={ico36_added_circle} />
          </svg>
        </React.Fragment>
      );
    }

    statusIcons = <div className="msgstatus__icons">{icons}</div>;
  }

  let timeStampLabel;
  let failedLabel;
  let sendingLabel;

  if (
    !isOutgoingMessage ||
    (!isFailedToSend && (isReceivedByRemote && isReceivedByOwn))
  ) {
    timeStampLabel = (
      <div className="msgstatus__time">
        {relativeTime(lastUpdateTime, get(message, "date"))}
      </div>
    );
  }

  if (
    isOutgoingMessage &&
    !isFailedToSend &&
    (!isReceivedByRemote || !isReceivedByOwn)
  ) {
    sendingLabel = (
      <div className="msgstatus__time--pending">Sending&nbsp;&hellip;</div>
    );
  }

  if (isOutgoingMessage && isFailedToSend) {
    failedLabel = (
      <div className="msgstatus__time--failure">Sending failed</div>
    );
  }

  return (
    <won-connection-message-status>
      {statusIcons}
      {timeStampLabel}
      {sendingLabel}
      {failedLabel}
    </won-connection-message-status>
  );
}
WonConnectionMessageStatus.propTypes = {
  message: PropTypes.object.isRequired,
};
