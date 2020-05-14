/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import Immutable from "immutable";
import PropTypes from "prop-types";
import { relativeTime } from "../../won-label-utils.js";
import { get, getIn } from "../../utils.js";
import { connect } from "react-redux";
import {
  getOwnedAtomByConnectionUri,
  selectLastUpdateTime,
} from "../../redux/selectors/general-selectors.js";

import "~/style/_connection-message-status.scss";
import ico16_indicator_warning from "~/images/won-icons/ico16_indicator_warning.svg";
import ico36_added_circle from "~/images/won-icons/ico36_added_circle.svg";

const mapStateToProps = (state, ownProps) => {
  const ownedAtom =
    ownProps.connectionUri &&
    getOwnedAtomByConnectionUri(state, ownProps.connectionUri);
  const connection = getIn(ownedAtom, ["connections", ownProps.connectionUri]);
  const message =
    connection && ownProps.messageUri
      ? getIn(connection, ["messages", ownProps.messageUri])
      : Immutable.Map();

  return {
    messageUri: ownProps.messageUri,
    connectionUri: ownProps.connectionUri,
    connection,
    message,
    lastUpdateTime: selectLastUpdateTime(state),
  };
};

class WonConnectionMessageStatus extends React.Component {
  render() {
    const isOutgoingMessage = get(this.props.message, "outgoingMessage");
    const isFailedToSend = get(this.props.message, "failedToSend");
    const isReceivedByOwn = get(this.props.message, "isReceivedByOwn");
    const isReceivedByRemote = get(this.props.message, "isReceivedByRemote");

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
                "msgstatus__icons__icon " +
                (isReceivedByOwn ? " received " : "")
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
          {relativeTime(
            this.props.lastUpdateTime,
            get(this.props.message, "date")
          )}
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
}

WonConnectionMessageStatus.propTypes = {
  messageUri: PropTypes.string.isRequired,
  connectionUri: PropTypes.string.isRequired,
  connection: PropTypes.object,
  message: PropTypes.object,
  lastUpdateTime: PropTypes.number,
};

export default connect(mapStateToProps)(WonConnectionMessageStatus);
