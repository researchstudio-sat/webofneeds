/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import Immutable from "immutable";
import PropTypes from "prop-types";
import { relativeTime } from "../../won-label-utils.js";
import { get, getIn } from "../../utils.js";
import { actionCreators } from "../../actions/actions.js";
import { getOwnedAtomByConnectionUri } from "../../redux/selectors/general-selectors.js";

import "~/style/_connection-message-status.scss";

export default class WonConnectionMessageStatus extends React.Component {
  componentDidMount() {
    this.messageUri = this.props.messageUri;
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
    this.messageUri = nextProps.messageUri;
    this.connectionUri = nextProps.connectionUri;
    this.setState(this.selectFromState(this.props.ngRedux.getState()));
  }

  selectFromState(state) {
    const ownedAtom =
      this.connectionUri &&
      getOwnedAtomByConnectionUri(state, this.connectionUri);
    const connection = getIn(ownedAtom, ["connections", this.connectionUri]);
    const message =
      connection && this.messageUri
        ? getIn(connection, ["messages", this.messageUri])
        : Immutable.Map();

    return {
      connection,
      message,
      lastUpdateTime: get(state, "lastUpdateTime"),
    };
  }

  render() {
    if (!this.state) {
      console.debug("render with null state");
      return <div />;
    }

    const isOutgoingMessage = get(this.state.message, "outgoingMessage");
    const isFailedToSend = get(this.state.message, "failedToSend");
    const isReceivedByOwn = get(this.state.message, "isReceivedByOwn");
    const isReceivedByRemote = get(this.state.message, "isReceivedByRemote");

    let statusIcons;

    if (isOutgoingMessage) {
      let icons;
      if (isFailedToSend) {
        icons = (
          <svg className="msgstatus__icons__icon" style="--local-primary: red;">
            <use
              xlinkHref="#ico16_indicator_warning"
              href="#ico16_indicator_warning"
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
              <use xlinkHref="#ico36_added_circle" href="#ico36_added_circle" />
            </svg>
            <svg
              className={
                "msgstatus__icons__icon " +
                (isReceivedByRemote ? " received " : "")
              }
            >
              <use xlinkHref="#ico36_added_circle" href="#ico36_added_circle" />
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
            this.state.lastUpdateTime,
            get(this.state.message, "date")
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
  ngRedux: PropTypes.object.isRequired,
};
