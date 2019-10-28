import React from "react";
import won from "../won-es6";
import { connect } from "react-redux";
import { get, getIn } from "../utils.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as connectionUtils from "../redux/utils/connection-utils.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import WonAtomHeader from "../components/atom-header.jsx";

import PropTypes from "prop-types";

import "~/style/_add-buddy.scss";
import { actionCreators } from "../actions/actions";

const mapStateToProps = (state, ownProps) => {
  const ownedAtomsWithBuddySocket = generalSelectors.getOwnedAtomsWithBuddySocket(
    state
  );

  const atom = getIn(state, ["atoms", ownProps.atomUri]);

  const ownedBuddyOptions =
    ownedAtomsWithBuddySocket &&
    ownedAtomsWithBuddySocket
      .filter(atom => atomUtils.isActive(atom))
      .filter(atom => get(atom, "uri") !== ownProps.atomUri);

  return {
    immediateConnectBuddy:
      ownedBuddyOptions.size == 1 ? ownedBuddyOptions.first() : undefined,
    ownedAtomsWithBuddySocketArray:
      ownedBuddyOptions && ownedBuddyOptions.toArray(),
    targetBuddySocketUri: atomUtils.getSocketUri(
      atom,
      won.BUDDY.BuddySocketCompacted
    ),
    atomUri: ownProps.atomUri,
  };
};

const mapDispatchToProps = dispatch => {
  return {
    hideModalDialog: () => {
      dispatch(actionCreators.view__hideModalDialog());
    },
    showModalDialog: payload => {
      dispatch(actionCreators.view__showModalDialog(payload));
    },
    connect: (
      ownedAtomUri,
      connectionUri,
      targetAtomUri,
      message,
      ownSocket,
      targetSocket
    ) => {
      dispatch(
        actionCreators.atoms__connect(
          ownedAtomUri,
          connectionUri,
          targetAtomUri,
          message,
          ownSocket,
          targetSocket
        )
      );
    },
    connectionOpen: (connectionUri, message) => {
      dispatch(actionCreators.connections__open(connectionUri, message));
    },
  };
};

// TODO: Change Icon: suggestion maybe a PersonIcon with a Plus
class WonAddBuddy extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      contextMenuOpen: false,
    };
    this.handleClick = this.handleClick.bind(this);
  }

  render() {
    let buddySelectionElement =
      this.props.ownedAtomsWithBuddySocketArray &&
      this.props.ownedAtomsWithBuddySocketArray.map(atom => {
        const existingBuddyConnection = get(atom, "connections").find(
          conn =>
            get(conn, "targetSocketUri") === this.props.targetBuddySocketUri
        );

        let connectionStateClass;
        let onClickAction = undefined;
        let connectionStateIcon;

        if (connectionUtils.isConnected(existingBuddyConnection)) {
          connectionStateClass = "connected";
          connectionStateIcon = "#ico16_checkmark";
        } else if (connectionUtils.isRequestSent(existingBuddyConnection)) {
          connectionStateClass = "sent";
          connectionStateIcon = "#ico36_outgoing";
        } else if (connectionUtils.isClosed(existingBuddyConnection)) {
          connectionStateClass = "closed";
          connectionStateIcon = "#ico36_close";
        } else if (connectionUtils.isRequestReceived(existingBuddyConnection)) {
          connectionStateClass = "received";
          connectionStateIcon = "#ico36_incoming";
          onClickAction = () => {
            this.connectBuddy(get(atom, "uri"), existingBuddyConnection);
          };
        } else {
          // also includes suggested (BuddySocket)Connections
          connectionStateClass = "requestable";
          connectionStateIcon = "#ico36_plus_circle";
          onClickAction = () => {
            this.connectBuddy(get(atom, "uri"), existingBuddyConnection);
          };
        }

        return (
          <div
            className={
              "add-buddy__addbuddymenu__content__selection__buddy " +
              connectionStateClass
            }
            key={get(atom, "uri")}
            onClick={onClickAction}
          >
            <WonAtomHeader atomUri={get(atom, "uri")} hideTimestamp={true} />
            <svg className="add-buddy__addbuddymenu__content__selection__buddy__status">
              <use xlinkHref={connectionStateIcon} href={connectionStateIcon} />
            </svg>
          </div>
        );
      });

    const dropdownElement = this.state.contextMenuOpen && (
      <div className="add-buddy__addbuddymenu">
        <div className="add-buddy__addbuddymenu__content">
          <div className="topline">
            <div
              className="add-buddy__addbuddymenu__header clickable"
              onClick={() => this.setState({ contextMenuOpen: false })}
            >
              <svg className="add-buddy__addbuddymenu__header__icon">
                <use xlinkHref="#ico36_plus_circle" href="#ico36_plus_circle" />
              </svg>
              <span className="add-buddy__addbuddymenu__header__text hide-in-responsive">
                Add as Buddy&#8230;
              </span>
            </div>
          </div>
          <div className="add-buddy__addbuddymenu__content__selection">
            {buddySelectionElement}
          </div>
        </div>
      </div>
    );

    let actionButton;

    if (this.props.immediateConnectBuddy) {
      const existingBuddyConnection = get(
        this.props.immediateConnectBuddy,
        "connections"
      ).find(
        conn => get(conn, "targetSocketUri") === this.props.targetBuddySocketUri
      );

      let connectionStateClass;
      let onClickAction = undefined;
      let connectionStateIcon;
      let connectionStateLabel;

      if (connectionUtils.isConnected(existingBuddyConnection)) {
        connectionStateClass = "connected";
        connectionStateIcon = "#ico16_checkmark";
        connectionStateLabel = "Already Buddies";
      } else if (connectionUtils.isRequestSent(existingBuddyConnection)) {
        connectionStateClass = "sent";
        connectionStateIcon = "#ico36_outgoing";
        connectionStateLabel = "Buddy Request sent";
      } else if (connectionUtils.isClosed(existingBuddyConnection)) {
        connectionStateClass = "closed";
        connectionStateIcon = "#ico36_close";
        connectionStateLabel = "Buddy Request denied";
      } else if (connectionUtils.isRequestReceived(existingBuddyConnection)) {
        connectionStateClass = "received";
        connectionStateIcon = "#ico36_incoming";
        connectionStateLabel = "Accept Buddy Request";
        onClickAction = () => {
          console.debug(
            "IMMEDIATE CONNECT FROM",
            get(this.props.immediateConnectBuddy, "uri"),
            " to ",
            this.props.atomUri
          );
          this.connectBuddy(
            get(this.props.immediateConnectBuddy, "uri"),
            existingBuddyConnection
          );
        };
      } else {
        // also includes suggested (BuddySocket)Connections
        connectionStateClass = "requestable";
        connectionStateIcon = "#ico36_plus_circle";
        connectionStateLabel = "Accept as Buddy";
        onClickAction = () => {
          console.debug(
            "IMMEDIATE CONNECT FROM",
            get(this.props.immediateConnectBuddy, "uri"),
            " to ",
            this.props.atomUri
          );
          this.connectBuddy(
            get(this.props.immediateConnectBuddy, "uri"),
            existingBuddyConnection
          );
        };
      }

      actionButton = (
        <div
          className={"add-buddy__addbuddymenu__header " + connectionStateClass}
          onClick={onClickAction}
        >
          <svg className="add-buddy__addbuddymenu__header__icon">
            <use xlinkHref={connectionStateIcon} href={connectionStateIcon} />
          </svg>
          <span className="add-buddy__addbuddymenu__header__text hide-in-responsive">
            {connectionStateLabel}
          </span>
        </div>
      );
    } else {
      actionButton = (
        <div
          className="add-buddy__addbuddymenu__header clickable"
          onClick={() => this.setState({ contextMenuOpen: true })}
        >
          <svg className="add-buddy__addbuddymenu__header__icon">
            <use xlinkHref="#ico36_plus_circle" href="#ico36_plus_circle" />
          </svg>
          <span className="add-buddy__addbuddymenu__header__text hide-in-responsive">
            Add as Buddy&#8230;
          </span>
        </div>
      );
    }

    return (
      <won-add-buddy
        class={this.props.className ? this.props.className : ""}
        ref={node => (this.node = node)}
      >
        {actionButton}
        {dropdownElement}
      </won-add-buddy>
    );
  }

  componentWillMount() {
    document.addEventListener("mousedown", this.handleClick, false);
  }
  componentWillUnmount() {
    document.removeEventListener("mousedown", this.handleClick, false);
  }

  handleClick(e) {
    if (!this.node.contains(e.target) && this.state.contextMenuOpen) {
      this.setState({ contextMenuOpen: false });

      return;
    }
  }

  connectBuddy(selectedAtomUri, existingBuddyConnection, message = "") {
    const dialogText = connectionUtils.isRequestReceived(
      existingBuddyConnection
    )
      ? "Accept Buddy Request?"
      : "Send Buddy Request?";

    const existingBuddyConnectionUri = get(existingBuddyConnection, "uri");

    const payload = {
      caption: "Buddy",
      text: dialogText,
      buttons: [
        {
          caption: "Yes",
          callback: () => {
            if (connectionUtils.isRequestReceived(existingBuddyConnection)) {
              this.props.open(existingBuddyConnectionUri, message);
            } else {
              this.props.connect(
                selectedAtomUri,
                existingBuddyConnectionUri,
                this.props.atomUri,
                message,
                won.BUDDY.BuddySocketCompacted,
                won.BUDDY.BuddySocketCompacted
              );
            }
            this.props.hideModalDialog();
          },
        },
        {
          caption: "No",
          callback: () => {
            this.props.hideModalDialog();
          },
        },
      ],
    };
    this.props.showModalDialog(payload);
  }
}
WonAddBuddy.propTypes = {
  atomUri: PropTypes.string.isRequired,
  className: PropTypes.string,
  ownedAtomsWithBuddySocketArray: PropTypes.arrayOf(PropTypes.object),
  immediateConnectBuddy: PropTypes.string,
  targetBuddySocketUri: PropTypes.string,
  hideModalDialog: PropTypes.func,
  showModalDialog: PropTypes.func,
  connect: PropTypes.func,
  open: PropTypes.func,
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(WonAddBuddy);
