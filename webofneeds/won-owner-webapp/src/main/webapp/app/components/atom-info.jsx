import React from "react";
import PropTypes from "prop-types";
import Immutable from "immutable";
import { connect } from "react-redux";
import { get, getIn } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import WonAtomHeaderBig from "./atom-header-big.jsx";
import WonAtomMenu from "./atom-menu.jsx";
import WonAtomContent from "./atom-content.jsx";
import ChatTextfield from "./chat-textfield.jsx";
import * as generalSelectors from "../redux/selectors/general-selectors";
import * as atomUtils from "../redux/utils/atom-utils";
import * as viewUtils from "../redux/utils/view-utils";
import * as processSelectors from "../redux/selectors/process-selectors";
import * as accountUtils from "../redux/utils/account-utils";
import * as useCaseUtils from "../usecase-utils.js";

import "~/style/_atom-info.scss";

const mapStateToProps = (state, ownProps) => {
  const atom = getIn(state, ["atoms", ownProps.atomUri]);

  const isOwned = generalSelectors.isAtomOwned(state, ownProps.atomUri);

  const isConnectible = atomUtils.isConnectible(atom);
  const hasReactionUseCases = atomUtils.hasReactionUseCases(atom);
  const hasEnabledUseCases = atomUtils.hasEnabledUseCases(atom);

  const showEnabledUseCases = isConnectible && isOwned && hasEnabledUseCases;
  const showReactionUseCases = isConnectible && !isOwned && hasReactionUseCases;

  const showAdHocRequestField =
    !isOwned && isConnectible && !showEnabledUseCases && !showReactionUseCases;

  const viewState = get(state, "view");
  const visibleTab = viewUtils.getVisibleTabByAtomUri(
    viewState,
    ownProps.atomUri
  );

  const atomLoading =
    !atom || processSelectors.isAtomLoading(state, ownProps.atomUri);

  const holderUri = atomUtils.getHeldByUri(atom);

  return {
    className: ownProps.className,
    atomUri: ownProps.atomUri,
    loggedIn: accountUtils.isLoggedIn(get(state, "account")),
    isInactive: atomUtils.isInactive(atom),
    showAdHocRequestField,
    showEnabledUseCases,
    showReactionUseCases,
    reactionUseCasesArray: showReactionUseCases
      ? atomUtils.getReactionUseCases(atom).toArray()
      : [],
    enabledUseCasesArray: showEnabledUseCases
      ? atomUtils.getEnabledUseCases(atom).toArray()
      : [],
    atomLoading,
    showFooter:
      !atomLoading &&
      visibleTab === "DETAIL" &&
      (showEnabledUseCases || showReactionUseCases || showAdHocRequestField),
    addHolderUri: showEnabledUseCases ? holderUri : undefined,
    holderUri,
  };
};

const mapDispatchToProps = dispatch => {
  return {
    viewRemoveAddMessageContent: () => {
      dispatch(actionCreators.view__removeAddMessageContent());
    },
    routerGo: (path, props) => {
      dispatch(actionCreators.router__stateGo(path, props));
    },
    routerGoResetParams: path => {
      dispatch(actionCreators.router__stateGoResetParams(path));
    },
    hideModalDialog: () => {
      dispatch(actionCreators.view__hideModalDialog());
    },
    showTermsDialog: payload => {
      dispatch(actionCreators.view__showTermsDialog(payload));
    },
    connectionsConnectAdHoc: (connectToAtomUri, message, persona) => {
      dispatch(
        actionCreators.connections__connectAdHoc(
          connectToAtomUri,
          message,
          persona
        )
      );
    },
  };
};

class AtomInfo extends React.Component {
  render() {
    let footerElement;

    if (this.props.showFooter) {
      const reactionUseCaseElements =
        this.props.showReactionUseCases &&
        this.props.reactionUseCasesArray &&
        this.props.reactionUseCasesArray.map((ucIdentifier, index) => {
          return (
            <button
              key={ucIdentifier + "-" + index}
              className="won-button--filled red atom-info__footer__button"
              onClick={() => this.selectUseCase(ucIdentifier)}
            >
              {useCaseUtils.getUseCaseIcon(ucIdentifier) && (
                <svg className="won-button-icon">
                  <use
                    xlinkHref={useCaseUtils.getUseCaseIcon(ucIdentifier)}
                    href={useCaseUtils.getUseCaseIcon(ucIdentifier)}
                  />
                </svg>
              )}
              <span>{useCaseUtils.getUseCaseLabel(ucIdentifier)}</span>
            </button>
          );
        });

      const enabledUseCaseElements =
        this.props.showEnabledUseCases &&
        this.props.enabledUseCasesArray &&
        this.props.enabledUseCasesArray.map((ucIdentifier, index) => {
          return (
            <button
              key={ucIdentifier + "-" + index}
              className="won-button--filled red atom-info__footer__button"
              onClick={() => this.selectUseCase(ucIdentifier)}
            >
              {useCaseUtils.getUseCaseIcon(ucIdentifier) && (
                <svg className="won-button-icon">
                  <use
                    xlinkHref={useCaseUtils.getUseCaseIcon(ucIdentifier)}
                    href={useCaseUtils.getUseCaseIcon(ucIdentifier)}
                  />
                </svg>
              )}
              <span>{useCaseUtils.getUseCaseLabel(ucIdentifier)}</span>
            </button>
          );
        });

      footerElement = (
        <div className="atom-info__footer">
          {this.props.showAdHocRequestField && (
            <ChatTextfield
              placeholder="Message (optional)"
              allowEmptySubmit={true}
              showPersonas={true}
              submitButtonLabel="Ask&#160;to&#160;Chat"
              onSubmit={({ value, selectedPersona }) =>
                this.sendAdHocRequest(value, selectedPersona)
              }
            />
          )}
          {reactionUseCaseElements}
          {enabledUseCaseElements}
          {this.props.isInactive && (
            <div className="atom-info__footer__infolabel">
              Atom is inactive, no requests allowed
            </div>
          )}
        </div>
      );
    }

    return (
      <won-atom-info
        class={
          (this.props.className ? this.props.className : "") +
          (this.props.atomLoading && " won-is-loading ")
        }
      >
        <WonAtomHeaderBig atomUri={this.props.atomUri} />
        <WonAtomMenu atomUri={this.props.atomUri} />
        <WonAtomContent atomUri={this.props.atomUri} />
        {footerElement}
      </won-atom-info>
    );
  }

  selectUseCase(ucIdentifier) {
    this.props.routerGo("create", {
      useCase: ucIdentifier,
      useCaseGroup: undefined,
      connectionUri: undefined,
      fromAtomUri: this.props.atomUri,
      viewConnUri: undefined,
      mode: "CONNECT",
      holderUri: this.props.addHolderUri ? this.props.holderUri : undefined,
    });
  }

  sendAdHocRequest(message, persona) {
    const _atomUri = this.props.atomUri;

    if (this.props.loggedIn) {
      this.props.routerGoResetParams("connections");

      if (_atomUri) {
        this.props.connectionsConnectAdHoc(_atomUri, message, persona);
      }
    } else {
      this.props.showTermsDialog(
        Immutable.fromJS({
          acceptCallback: () => {
            this.props.hideModalDialog();
            this.props.routerGoResetParams("connections");

            if (_atomUri) {
              this.props.connectionsConnectAdHoc(_atomUri, message, persona);
            }
          },
          cancelCallback: () => {
            this.props.hideModalDialog();
          },
        })
      );
    }
  }
}

AtomInfo.propTypes = {
  atomUri: PropTypes.string,
  loggedIn: PropTypes.bool,
  isInactive: PropTypes.bool,
  showAdHocRequestField: PropTypes.bool,
  showEnabledUseCases: PropTypes.bool,
  showReactionUseCases: PropTypes.bool,
  reactionUseCasesArray: PropTypes.arrayOf(PropTypes.object),
  enabledUseCasesArray: PropTypes.arrayOf(PropTypes.object),
  atomLoading: PropTypes.bool,
  showFooter: PropTypes.bool,
  addHolderUri: PropTypes.string,
  holderUri: PropTypes.string,
  className: PropTypes.string,
  routerGo: PropTypes.func,
  routerGoResetParams: PropTypes.func,
  hideModalDialog: PropTypes.func,
  showTermsDialog: PropTypes.func,
  connectionsConnectAdHoc: PropTypes.func,
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(AtomInfo);
