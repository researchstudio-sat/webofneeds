/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import PropTypes from "prop-types";
import { relativeTime } from "../../won-label-utils.js";
import { useSelector } from "react-redux";
import { selectLastUpdateTime } from "../../redux/selectors/general-selectors.js";
import * as messageUtils from "../../redux/utils/message-utils.js";

import "~/style/_connection-message-status.scss";
import ico16_indicator_warning from "~/images/won-icons/ico16_indicator_warning.svg";
import ico36_added_circle from "~/images/won-icons/ico36_added_circle.svg";

export default function WonConnectionMessageStatus({ message }) {
  const lastUpdateTime = useSelector(selectLastUpdateTime);

  const isOutgoingMessage = messageUtils.isOutgoingMessage(message);

  if (isOutgoingMessage) {
    const isFailedToSend = messageUtils.hasFailedToSend(message);

    let icons;
    let label;

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
      label = <div className="msgstatus__time--failure">Sending failed</div>;
    } else {
      const isReceivedByOwn = messageUtils.isReceivedByOwn(message);
      const isReceivedByRemote = messageUtils.isReceivedByRemote(message);

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

      label =
        isReceivedByRemote && isReceivedByOwn ? (
          <div className="msgstatus__time">
            {relativeTime(lastUpdateTime, messageUtils.getDate(message))}
          </div>
        ) : (
          <div className="msgstatus__time--pending">Sending&nbsp;&hellip;</div>
        );
    }

    return (
      <won-connection-message-status>
        <div className="msgstatus__icons">{icons}</div>
        {label}
      </won-connection-message-status>
    );
  } else {
    return (
      <won-connection-message-status>
        <div className="msgstatus__time">
          {relativeTime(lastUpdateTime, messageUtils.getDate(message))}
        </div>
      </won-connection-message-status>
    );
  }
}
WonConnectionMessageStatus.propTypes = {
  message: PropTypes.object.isRequired,
};
