import React from "react";

import PropTypes from "prop-types";
import { actionCreators } from "../actions/actions.js";
import * as generalSelectors from "../redux/selectors/general-selectors";
import { get, getIn, toAbsoluteURL } from "../utils";
import * as connectionSelectors from "../redux/selectors/connection-selectors";
import * as connectionUtils from "../redux/utils/connection-utils";
import * as processUtils from "../redux/utils/process-utils";
import { ownerBaseUrl } from "~/config/default.js";

import "~/style/_context-dropdown.scss";

export default class WonConnectionContextDropdown extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      contextMenuOpen: false,
    };
    //TODO: REACT ON CLICK OUTSIDE OF COMPONENT AND CLOSE THE DIALOG (maybe with hooks)
  }

  componentDidMount() {
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

  selectFromState(state) {
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
      showAgreementData: connection && connection.get("showAgreementData"),
      isConnected: connectionUtils.isConnected(connection),
      isSentRequest: connectionUtils.isRequestSent(connection),
      isReceivedRequest: connectionUtils.isRequestReceived(connection),
      isSuggested: connectionUtils.isSuggested(connection),
      isTargetAtomUsableAsTemplate: generalSelectors.isAtomUsableAsTemplate(
        state,
        targetAtomUri
      ),
      isTargetAtomEditable: generalSelectors.isAtomEditable(
        state,
        targetAtomUri
      ),
      connectionLoading:
        !connection || processUtils.isConnectionLoading(process, connectionUri),
    };
  }

  render() {
    if (!this.state) {
      console.debug("render with null state");
      return <div />;
    }

    const iconElement = this.state.connectionLoading ? (
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

      !this.state.isSuggested &&
        buttons.push(
          <button
            key="details"
            className="won-button--outlined thin red"
            onClick={() => this.goToPost(this.state.targetAtomUri)}
          >
            Show Details
          </button>
        );

      !this.state.isConnectionToGroup &&
        this.state.isConnected &&
        !this.state.showAgreementData &&
        buttons.push(
          <button
            key="agrdata"
            className="won-button--outlined thin red"
            onClick={() => this.props.showAgreementDataField()}
          >
            Show Agreement Data
          </button>
        );

      !this.state.isConnectionToGroup &&
        this.state.isConnected &&
        !this.state.showPetriNetData &&
        buttons.push(
          <button
            key="petrinetdata"
            className="won-button--outlined thin red"
            onClick={() => this.props.showPetriNetDataField()}
          >
            Show PetriNet Data
          </button>
        );
      this.state.isTargetAtomUsableAsTemplate &&
        buttons.push(
          <button
            key="duplicate"
            className="won-button--outlined thin red"
            onClick={() =>
              this.props.ngRedux.dispatch(
                actionCreators.router__stateGoAbs("create", {
                  fromAtomUri: this.state.targetAtomUri,
                  mode: "DUPLICATE",
                })
              )
            }
          >
            Post this too!
          </button>
        );
      this.state.isTargetAtomEditable &&
        buttons.push(
          <button
            key="edit"
            className="won-button--outlined thin red"
            onClick={() =>
              this.props.ngRedux.dispatch(
                actionCreators.router__stateGoAbs("create", {
                  fromAtomUri: this.state.targetAtomUri,
                  mode: "EDIT",
                })
              )
            }
          >
            Edit
          </button>
        );
      this.state.adminEmail &&
        buttons.push(
          <a
            key="report"
            className="won-button--outlined thin red"
            href={
              "mailto:" +
              this.state.adminEmail +
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
        class={this.props.className ? this.props.className : undefined}
      >
        {iconElement}
        {dropdownElement}
      </won-connection-context-dropdown>
    );
  }

  generateReportPostMailParams() {
    const subject = `[Report Post] - ${this.state.targetAtomUri}`;
    const body = `Link to Post: ${this.state.linkToPost}%0D%0AReason:%0D%0A`; //hint: %0D%0A adds a linebreak

    return `subject=${subject}&body=${body}`;
  }

  generateCloseConnectionLabel() {
    if (this.state.isConnected) {
      return "Close Connection";
    } else if (this.state.isSuggested) {
      return "Remove Connection";
    } else if (this.state.isSentRequest) {
      return "Cancel Request";
    } else if (this.state.isReceivedRequest) {
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
            this.props.ngRedux.dispatch(
              actionCreators.connections__close(this.connectionUri)
            );
            this.props.ngRedux.dispatch(
              actionCreators.router__stateGoCurrent({
                useCase: undefined,
                connectionUri: undefined,
              })
            );
            this.props.ngRedux.dispatch(actionCreators.view__hideModalDialog());
          },
        },
        {
          caption: "No",
          callback: () => {
            this.props.ngRedux.dispatch(actionCreators.view__hideModalDialog());
          },
        },
      ],
    };
    this.props.ngRedux.dispatch(actionCreators.view__showModalDialog(payload));
  }

  goToPost(postUri) {
    this.props.ngRedux.dispatch(
      actionCreators.router__stateGo("post", {
        postUri: postUri,
      })
    );
  }
}
WonConnectionContextDropdown.propTypes = {
  showAgreementDataField: PropTypes.func,
  showPetriNetDataField: PropTypes.func,
  ngRedux: PropTypes.object.isRequired,
  className: PropTypes.string,
};
