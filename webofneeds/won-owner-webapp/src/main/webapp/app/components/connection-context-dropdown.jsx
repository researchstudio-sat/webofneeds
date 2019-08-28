import React from "react";

import PropTypes from "prop-types";
import { actionCreators } from "../actions/actions.js";
import { connect } from "react-redux";
import * as generalSelectors from "../redux/selectors/general-selectors";
import { get, getIn, toAbsoluteURL } from "../utils";
import * as connectionSelectors from "../redux/selectors/connection-selectors";
import * as connectionUtils from "../redux/utils/connection-utils";
import * as processUtils from "../redux/utils/process-utils";
import { ownerBaseUrl } from "~/config/default.js";

import "~/style/_context-dropdown.scss";

const mapStateToProps = (state, ownProps) => {
  const connectionUri = generalSelectors.getConnectionUriFromRoute(state);

  const post =
    connectionUri &&
    generalSelectors.getOwnedAtomByConnectionUri(state, connectionUri);
  const connection = post && post.getIn(["connections", connectionUri]);

  const targetAtomUri = getIn(connection, ["targetAtomUri"]);

  let linkToPost;
  if (ownerBaseUrl && targetAtomUri) {
    const path = "#!post/" + `?postUri=${encodeURI(targetAtomUri)}`;

    linkToPost = toAbsoluteURL(ownerBaseUrl).toString() + path;
  }
  const process = get(state, "process");

  return {
    connection,
    connectionUri,
    adminEmail: getIn(state, ["config", "theme", "adminEmail"]),
    targetAtomUri,
    linkToPost,
    isConnectionToGroup: connectionSelectors.isChatToGroupConnection(
      get(state, "atoms"),
      connection
    ),
    showAgreementData: get(connection, "showAgreementData"),
    showPetriNetData: get(connection, "showPetriNetData"),
    isConnected: connectionUtils.isConnected(connection),
    isSentRequest: connectionUtils.isRequestSent(connection),
    isReceivedRequest: connectionUtils.isRequestReceived(connection),
    isSuggested: connectionUtils.isSuggested(connection),
    isTargetAtomUsableAsTemplate: generalSelectors.isAtomUsableAsTemplate(
      state,
      targetAtomUri
    ),
    isTargetAtomEditable: generalSelectors.isAtomEditable(state, targetAtomUri),
    connectionLoading:
      !connection || processUtils.isConnectionLoading(process, connectionUri),
    showAgreementDataField: ownProps.showAgreementDataField,
    showPetriNetDataField: ownProps.showPetriNetDataField,
    className: ownProps.className,
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
    routerGoCurrent: props => {
      dispatch(actionCreators.router__stateGoCurrent(props));
    },
    routerGoAbs: (path, props) => {
      dispatch(actionCreators.router__stateGoAbs(path, props));
    },
    routerGo: (path, props) => {
      dispatch(actionCreators.router__stateGo(path, props));
    },
    connectionClose: connectionUri => {
      dispatch(actionCreators.connections__close(connectionUri));
    },
  };
};

class WonConnectionContextDropdown extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      contextMenuOpen: false,
    };
    //TODO: REACT ON CLICK OUTSIDE OF COMPONENT AND CLOSE THE DIALOG (maybe with hooks)
  }

  render() {
    const iconElement = this.props.connectionLoading ? (
      <svg className="cdd__icon__small">
        <use xlinkHref="#ico16_contextmenu" href="#ico16_contextmenu" />
      </svg>
    ) : (
      <svg
        className="cdd__icon__small clickable"
        onClick={() => this.setState({ contextMenuOpen: true })}
      >
        <use xlinkHref="#ico16_contextmenu" href="#ico16_contextmenu" />
      </svg>
    );

    let dropdownElement;

    if (this.state.contextMenuOpen) {
      const buttons = [];

      !this.props.isSuggested &&
        buttons.push(
          <button
            key="details"
            className="won-button--outlined thin red"
            onClick={() => this.goToPost(this.props.targetAtomUri)}
          >
            Show Details
          </button>
        );

      !this.props.isConnectionToGroup &&
        this.props.isConnected &&
        !this.props.showAgreementData &&
        buttons.push(
          <button
            key="agrdata"
            className="won-button--outlined thin red"
            onClick={() => this.props.showAgreementDataField()}
          >
            Show Agreement Data
          </button>
        );

      !this.props.isConnectionToGroup &&
        this.props.isConnected &&
        !this.props.showPetriNetData &&
        buttons.push(
          <button
            key="petrinetdata"
            className="won-button--outlined thin red"
            onClick={() => this.props.showPetriNetDataField()}
          >
            Show PetriNet Data
          </button>
        );
      this.props.isTargetAtomUsableAsTemplate &&
        buttons.push(
          <button
            key="duplicate"
            className="won-button--outlined thin red"
            onClick={() =>
              this.props.routerGoAbs("create", {
                fromAtomUri: this.props.targetAtomUri,
                mode: "DUPLICATE",
              })
            }
          >
            Post this too!
          </button>
        );
      this.props.isTargetAtomEditable &&
        buttons.push(
          <button
            key="edit"
            className="won-button--outlined thin red"
            onClick={() =>
              this.props.routerGoAbs("create", {
                fromAtomUri: this.props.targetAtomUri,
                mode: "EDIT",
              })
            }
          >
            Edit
          </button>
        );
      this.props.adminEmail &&
        buttons.push(
          <a
            key="report"
            className="won-button--outlined thin red"
            href={
              "mailto:" +
              this.props.adminEmail +
              "?" +
              this.generateReportPostMailParams()
            }
          >
            Report
          </a>
        );
      buttons.push(
        <button
          key="close"
          className="won-button--filled red"
          onClick={() => this.closeConnection()}
        >
          {this.generateCloseConnectionLabel()}
        </button>
      );

      dropdownElement = (
        <div className="cdd__contextmenu">
          <div
            className="cdd__contextmenu__content"
            onClick={() => this.setState({ contextMenuOpen: false })}
          >
            <div className="topline">
              <svg className="cdd__icon__small__contextmenu clickable">
                <use xlinkHref="#ico16_contextmenu" href="#ico16_contextmenu" />
              </svg>
            </div>
            {/* Buttons when connection is available -->*/}
            {buttons}
          </div>
        </div>
      );
    }

    return (
      <won-connection-context-dropdown
        class={this.props.className ? this.props.className : ""}
      >
        {iconElement}
        {dropdownElement}
      </won-connection-context-dropdown>
    );
  }

  generateReportPostMailParams() {
    const subject = `[Report Post] - ${this.props.targetAtomUri}`;
    const body = `Link to Post: ${this.props.linkToPost}%0D%0AReason:%0D%0A`; //hint: %0D%0A adds a linebreak

    return `subject=${subject}&body=${body}`;
  }

  generateCloseConnectionLabel() {
    if (this.props.isConnected) {
      return "Close Connection";
    } else if (this.props.isSuggested) {
      return "Remove Connection";
    } else if (this.props.isSentRequest) {
      return "Cancel Request";
    } else if (this.props.isReceivedRequest) {
      return "Deny Request";
    }
  }

  closeConnection() {
    const payload = {
      caption: "Attention!",
      text: "Do you want to close the connection?",
      buttons: [
        {
          caption: "Yes",
          callback: () => {
            this.props.connectionClose(this.props.connectionUri);
            this.props.routerGoCurrent({
              useCase: undefined,
              connectionUri: undefined,
            });
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

  goToPost(postUri) {
    this.props.routerGo("post", {
      postUri: postUri,
    });
  }
}
WonConnectionContextDropdown.propTypes = {
  connection: PropTypes.object,
  connectionUri: PropTypes.string,
  adminEmail: PropTypes.string,
  targetAtomUri: PropTypes.string,
  linkToPost: PropTypes.string,
  isConnectionToGroup: PropTypes.bool,
  showAgreementData: PropTypes.bool,
  showPetriNetData: PropTypes.bool,
  isConnected: PropTypes.bool,
  isSentRequest: PropTypes.bool,
  isReceivedRequest: PropTypes.bool,
  isSuggested: PropTypes.bool,
  isTargetAtomUsableAsTemplate: PropTypes.bool,
  isTargetAtomEditable: PropTypes.bool,
  connectionLoading: PropTypes.bool,
  showAgreementDataField: PropTypes.func,
  showPetriNetDataField: PropTypes.func,
  className: PropTypes.string,
  hideModalDialog: PropTypes.func,
  showModalDialog: PropTypes.func,
  routerGoCurrent: PropTypes.func,
  routerGoAbs: PropTypes.func,
  routerGo: PropTypes.func,
  connectionClose: PropTypes.func,
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(WonConnectionContextDropdown);
