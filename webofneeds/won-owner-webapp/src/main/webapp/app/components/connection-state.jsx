/**
 * Component for rendering the icon of a groupChat (renders participants icons)
 * Can be included with either:
 *    connection-uri: then the participants of the targetAtom are shown
 *    atom-uri: then the participants of the atom behind the atom uri are shown
 * Created by quasarchimaere on 15.01.2019.
 */
import React from "react";
import PropTypes from "prop-types";
import * as wonLabelUtils from "../won-label-utils.js";
import { get } from "../utils.js";

import * as connectionUtils from "../redux/utils/connection-utils.js";
import vocab from "../service/vocab.js";

import ico36_match from "~/images/won-icons/ico36_match.svg";
import ico36_outgoing from "~/images/won-icons/ico36_outgoing.svg";
import ico36_incoming from "~/images/won-icons/ico36_incoming.svg";
import ico36_message from "~/images/won-icons/ico36_message.svg";
import ico36_close_circle from "~/images/won-icons/ico36_close_circle.svg";

export default function WonConnectionState({ connection }) {
  const connectionState = get(connection, "state");
  const unread = connectionUtils.isUnread(connection);

  let icon;

  switch (connectionState) {
    case vocab.WON.Suggested:
      icon = <use xlinkHref={ico36_match} href={ico36_match} />;
      break;
    case vocab.WON.RequestSent:
      icon = <use xlinkHref={ico36_outgoing} href={ico36_outgoing} />;
      break;
    case vocab.WON.RequestReceived:
      icon = <use xlinkHref={ico36_incoming} href={ico36_incoming} />;
      break;
    case vocab.WON.Connected:
      icon = <use xlinkHref={ico36_message} href={ico36_message} />;
      break;
    case vocab.WON.Closed:
    default:
      icon = <use xlinkHref={ico36_close_circle} href={ico36_close_circle} />;
      break;
  }

  return (
    <won-connection-state>
      <div
        className="cs__state"
        title={wonLabelUtils.getConnectionStateLabel(connectionState)}
      >
        {unread &&
          connectionState === vocab.WON.Suggested && (
            <svg
              className={"cs__state__icon " + (unread ? " won-unread " : "")}
            >
              {icon}
            </svg>
          )}
      </div>
    </won-connection-state>
  );
}
WonConnectionState.propTypes = {
  connection: PropTypes.object,
};
