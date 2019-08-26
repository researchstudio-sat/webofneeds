/**
 * Component for rendering the icon of a groupChat (renders participants icons)
 * Can be included with either:
 *    connection-uri: then the participants of the targetAtom are shown
 *    atom-uri: then the participants of the atom behind the atom uri are shown
 * Created by quasarchimaere on 15.01.2019.
 */
import React from "react";
import PropTypes from "prop-types";
import { labels } from "../won-label-utils.js";
import { get, getIn } from "../utils.js";
import { connect } from "react-redux";
import * as generalSelectors from "../redux/selectors/general-selectors.js";

import * as connectionUtils from "../redux/utils/connection-utils.js";
import won from "../won-es6";

const mapStateToDispatch = (state, ownProps) => {
  const atom = generalSelectors.getOwnedAtomByConnectionUri(
    state,
    ownProps.connectionUri
  );
  const connection = getIn(atom, ["connections", ownProps.connectionUri]);

  return {
    connectionUri: ownProps.connectionUri,
    connectionState: get(connection, "state"),
    unread: connectionUtils.isUnread(connection),
  };
};

class WonConnectionState extends React.Component {
  render() {
    let icon;

    switch (this.props.connectionState) {
      case won.WON.Suggested:
        icon = <use xlinkHref="#ico36_match" href="#ico36_match" />;
        break;
      case won.WON.RequestSent:
        icon = <use xlinkHref="#ico36_outgoing" href="#ico36_outgoing" />;
        break;
      case won.WON.RequestReceived:
        icon = <use xlinkHref="#ico36_incoming" href="#ico36_incoming" />;
        break;
      case won.WON.Connected:
        icon = <use xlinkHref="#ico36_message" href="#ico36_message" />;
        break;
      case won.WON.Closed:
      default:
        icon = (
          <use xlinkHref="#ico36_close_circle" href="#ico36_close_circle" />
        );
        break;
    }

    return (
      <won-connection-state>
        <div
          className="cs__state"
          title={labels.connectionState[this.props.connectionState]}
        >
          {this.props.unread &&
            this.props.connectionState === won.WON.Suggested && (
              <svg
                className={
                  "cs__state__icon " + (this.props.unread ? " won-unread " : "")
                }
              >
                {icon}
              </svg>
            )}
        </div>
      </won-connection-state>
    );
  }
}
WonConnectionState.propTypes = {
  connectionUri: PropTypes.string.isRequired,
  connectionState: PropTypes.string,
  unread: PropTypes.bool,
};

export default connect(mapStateToDispatch)(WonConnectionState);
