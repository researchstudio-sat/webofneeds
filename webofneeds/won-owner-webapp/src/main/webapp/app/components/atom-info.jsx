import React from "react";
import PropTypes from "prop-types";
import Immutable from "immutable";
import { connect } from "react-redux";
import { get, getIn } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import WonAtomHeaderBig from "./atom-header-big.jsx";
import WonAtomHeader from "./atom-header.jsx";
import WonAtomMenu from "./atom-menu.jsx";
import WonAtomContent from "./atom-content.jsx";
import ChatTextfield from "./chat-textfield.jsx";
import * as generalSelectors from "../redux/selectors/general-selectors";
import * as atomUtils from "../redux/utils/atom-utils";
import * as connectionUtils from "../redux/utils/connection-utils";
import * as viewUtils from "../redux/utils/view-utils";
import * as processSelectors from "../redux/selectors/process-selectors";
import * as accountUtils from "../redux/utils/account-utils";
import * as useCaseUtils from "../usecase-utils.js";
import vocab from "../service/vocab.js";

import "~/style/_atom-info.scss";

const mapStateToProps = (state, ownProps) => {
  const ownedAtoms = generalSelectors.getOwnedAtoms(state);
  const atom = getIn(state, ["atoms", ownProps.atomUri]);

  const isOwned = generalSelectors.isAtomOwned(state, ownProps.atomUri);

  const showEnabledUseCases =
    atomUtils.isConnectible(atom) &&
    isOwned &&
    atomUtils.hasEnabledUseCases(atom);
  const showReactionUseCases =
    atomUtils.isConnectible(atom) &&
    !isOwned &&
    atomUtils.hasReactionUseCases(atom);

  const reactionUseCases = atomUtils.getReactionUseCases(atom);
  const enabledUseCases = atomUtils.getEnabledUseCases(atom);

  const ownedReactionAtoms =
    ownedAtoms &&
    ownedAtoms.filter(
      atom =>
        atomUtils.matchesDefinitions(atom, reactionUseCases) ||
        atomUtils.matchesDefinitions(atom, enabledUseCases)
    );

  const showAdHocRequestField =
    !isOwned &&
    atomUtils.isConnectible(atom) &&
    !showEnabledUseCases &&
    !showReactionUseCases;

  const viewState = get(state, "view");
  const visibleTab = viewUtils.getVisibleTabByAtomUri(
    viewState,
    ownProps.atomUri
  );

  const atomLoading =
    !atom || processSelectors.isAtomLoading(state, ownProps.atomUri);

  const holderUri = atomUtils.getHeldByUri(atom);

  const ownedChatSocketAtoms =
    ownedAtoms && ownedAtoms.filter(atom => atomUtils.hasChatSocket(atom));

  return {
    className: ownProps.className,
    atomUri: ownProps.atomUri,
    atom: atom,
    defaultTab: ownProps.defaultTab,
    loggedIn: accountUtils.isLoggedIn(get(state, "account")),
    isOwned: isOwned,
    showAdHocRequestField,
    reactionUseCasesArray: showReactionUseCases
      ? reactionUseCases.toArray()
      : [],
    enabledUseCasesArray: showEnabledUseCases ? enabledUseCases.toArray() : [],
    ownedReactionAtomsArray: ownedReactionAtoms
      ? ownedReactionAtoms.toArray()
      : [],
    atomLoading,
    showFooter:
      !atomLoading &&
      visibleTab === "DETAIL" &&
      (showEnabledUseCases ||
        showReactionUseCases ||
        showAdHocRequestField ||
        atomUtils.isInactive(atom)),
    addHolderUri: showEnabledUseCases ? holderUri : undefined,
    holderUri,
    ownedChatSocketAtoms,
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
    showModalDialog: payload => {
      dispatch(actionCreators.view__showModalDialog(payload));
    },
    showTermsDialog: payload => {
      dispatch(actionCreators.view__showTermsDialog(payload));
    },
    connectionsConnectAdHoc: (targetSocketUri, message, personaUri) => {
      dispatch(
        actionCreators.connections__connectAdHoc(
          targetSocketUri,
          message,
          personaUri
        )
      );
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
    sendChatMessage: (
      trimmedMsg,
      additionalContent,
      referencedContent,
      senderSocketUri,
      targetSocketUri,
      connectionUri,
      isTTL
    ) => {
      dispatch(
        actionCreators.connections__sendChatMessage(
          trimmedMsg,
          additionalContent,
          referencedContent,
          senderSocketUri,
          targetSocketUri,
          connectionUri,
          isTTL
        )
      );
    },
    connectSockets: (senderSocketUri, targetSocketUri, message) => {
      dispatch(
        actionCreators.atoms__connectSockets(
          senderSocketUri,
          targetSocketUri,
          message
        )
      );
    },
    atomReopen: atomUri => dispatch(actionCreators.atoms__reopen(atomUri)),
  };
};

class AtomInfo extends React.Component {
  render() {
    let footerElement;

    if (this.props.showFooter) {
      if (atomUtils.isInactive(this.props.atom)) {
        if (this.props.isOwned) {
          footerElement = (
            <div className="atom-info__footer">
              <div className="atom-info__footer__infolabel">
                This Atom is inactive. Others will not be able to interact with
                it.
              </div>
              <button
                className="won-publish-button red won-button--filled"
                onClick={() => this.props.atomReopen(this.props.atomUri)}
              >
                Reopen
              </button>
            </div>
          );
        } else {
          footerElement = (
            <div className="atom-info__footer">
              <div className="atom-info__footer__infolabel">
                Atom is inactive, no requests allowed
              </div>
            </div>
          );
        }
      } else {
        if (this.props.showAdHocRequestField) {
          footerElement = (
            <div className="atom-info__footer">
              {atomUtils.getChatSocket(this.props.atom) && (
                <ChatTextfield
                  placeholder="Message (optional)"
                  allowEmptySubmit={true}
                  showPersonas={true}
                  submitButtonLabel="Ask&#160;to&#160;Chat"
                  onSubmit={({ value, selectedPersona }) =>
                    this.sendAdHocRequest(
                      value,
                      atomUtils.getChatSocket(this.props.atom),
                      selectedPersona && selectedPersona.personaId
                    )
                  }
                />
              )}
              {atomUtils.getGroupSocket(this.props.atom) && (
                <ChatTextfield
                  placeholder="Message (optional)"
                  allowEmptySubmit={true}
                  showPersonas={true}
                  submitButtonLabel="Join&#160;Group"
                  onSubmit={({ value, selectedPersona }) =>
                    this.sendAdHocRequest(
                      value,
                      atomUtils.getGroupSocket(this.props.atom),
                      selectedPersona && selectedPersona.personaId
                    )
                  }
                />
              )}
            </div>
          );
        } else {
          const reactionUseCaseElements = this.props.reactionUseCasesArray.map(
            (useCase, index) => this.getUseCaseTypeButton(useCase, index)
          );

          const enabledUseCaseElements = this.props.enabledUseCasesArray.map(
            (useCase, index) => this.getUseCaseTypeButton(useCase, index)
          );

          footerElement = (
            <div className="atom-info__footer">
              {reactionUseCaseElements}
              {enabledUseCaseElements}
            </div>
          );
        }
      }
    }

    return (
      <won-atom-info
        class={
          (this.props.className ? this.props.className : "") +
          (this.props.atomLoading ? " won-is-loading " : "")
        }
      >
        <WonAtomHeaderBig atomUri={this.props.atomUri} />
        <WonAtomMenu
          atomUri={this.props.atomUri}
          defaultTab={this.props.defaultTab}
        />
        <WonAtomContent
          atomUri={this.props.atomUri}
          defaultTab={this.props.defaultTab}
        />
        {footerElement}
      </won-atom-info>
    );
  }

  getUseCaseTypeButton(useCase, index) {
    const ucIdentifier = get(useCase, "identifier");
    const ucSenderSocketType = get(useCase, "senderSocketType");
    const ucTargetSocketType = get(useCase, "targetSocketType");

    const atomElements = this.props.ownedReactionAtomsArray
      .filter(reactionAtom =>
        atomUtils.matchesDefinition(reactionAtom, useCase)
      )
      .map((reactionAtom, index) => {
        return (
          <WonAtomHeader
            key={get(reactionAtom, "uri") + "-" + index}
            atomUri={get(reactionAtom, "uri")}
            hideTimestamp={true}
            onClick={() => {
              this.connectAtomSockets(
                reactionAtom,
                ucSenderSocketType,
                ucTargetSocketType
              );
            }}
          />
        );
      });

    return (
      <React.Fragment>
        <div className="atom-info__footer__header">
          Connect {ucSenderSocketType} with {ucTargetSocketType}:
        </div>
        <div className="atom-info__footer__matches">
          <div className="atom-info__footer__matches__header">
            These existing Atoms you own would match:
          </div>
          <div className="atom-info__footer__matches__list">
            {atomElements}
            <div
              key={ucIdentifier + "-" + index}
              className="atom-info__footer__adhocbutton"
              onClick={() =>
                this.selectUseCase(
                  ucIdentifier,
                  ucSenderSocketType,
                  ucTargetSocketType
                )
              }
            >
              <div className="atom-info__footer__adhocbutton__icon">
                {useCaseUtils.getUseCaseIcon(ucIdentifier) ? (
                  <svg className="atom-info__footer__adhocbutton__icon__svg">
                    <use
                      xlinkHref={useCaseUtils.getUseCaseIcon(ucIdentifier)}
                      href={useCaseUtils.getUseCaseIcon(ucIdentifier)}
                    />
                  </svg>
                ) : (
                  <svg className="atom-info__footer__adhocbutton__icon__svg">
                    <use xlinkHref="ico36_plus" href="ico36_plus" />
                  </svg>
                )}
              </div>
              <div className="atom-info__footer__adhocbutton__right">
                <div className="atom-info__footer__adhocbutton__right__topline">
                  <div className="atom-info__footer__adhocbutton__right__topline__notitle">
                    + Connect with new Atom
                  </div>
                </div>
                <div className="atom-info__footer__adhocbutton__right__subtitle">
                  <span className="atom-info__footer__adhocbutton__right__subtitle__type">
                    <span>{useCaseUtils.getUseCaseLabel(ucIdentifier)}</span>
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </React.Fragment>
    );
  }

  selectUseCase(ucIdentifier, ucSenderSocketType, ucTargetSocketType) {
    this.props.routerGo("create", {
      useCase: ucIdentifier,
      useCaseGroup: undefined,
      connectionUri: undefined,
      fromAtomUri: this.props.atomUri,
      senderSocketType: ucSenderSocketType,
      targetSocketType: ucTargetSocketType,
      viewConnUri: undefined,
      mode: "CONNECT",
      holderUri: this.props.addHolderUri ? this.props.holderUri : undefined,
    });
  }

  connectAtomSockets(
    reactionAtom,
    senderSocketType,
    targetSocketType,
    message = ""
  ) {
    const reactionAtomUri = get(reactionAtom, "uri");
    const senderSocketUri = atomUtils.getSocketUri(
      reactionAtom,
      senderSocketType
    );
    const targetSocketUri = atomUtils.getSocketUri(
      this.props.atom,
      targetSocketType
    );

    const existingConnections = get(reactionAtom, "connections")
      .filter(conn => get(conn, "targetAtomUri") === this.props.atomUri)
      .filter(conn => get(conn, "targetSocketUri") === targetSocketUri)
      .filter(conn => get(conn, "socketUri") === senderSocketUri);

    console.debug("ExistingConnections: ", existingConnections);

    if (this.props.isOwned) {
      //TODO: SERVER SIDE CONNECT OF TWO ATOMS THAT ARE OWNED
    } else {
      const dialogText = "Connect with this Atom?";

      const payload = {
        caption: "Connect",
        text: dialogText,
        buttons: [
          {
            caption: "Yes",
            callback: () => {
              this.props.connect(
                reactionAtomUri,
                undefined,
                this.props.atomUri,
                message,
                senderSocketType,
                targetSocketType
              );
              this.props.hideModalDialog();

              if (senderSocketType === vocab.CHAT.ChatSocketCompacted) {
                this.props.routerGoResetParams("connections");
              }
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

  sendAdHocRequest(message, targetSocketUri, personaUri) {
    const _atomUri = this.props.atomUri;

    if (this.props.loggedIn) {
      if (_atomUri) {
        const personaAtom = get(this.props.ownedChatSocketAtoms, personaUri);

        if (personaAtom) {
          const targetSocketType =
            targetSocketUri === atomUtils.getChatSocket(this.props.atom)
              ? vocab.CHAT.ChatSocketCompacted
              : vocab.GROUP.GroupSocketCompacted;

          // if the personaAtom already contains a chatSocket we will just use the persona as the Atom that connects
          const personaConnections = get(personaAtom, "connections")
            .filter(conn => get(conn, "targetAtomUri") === _atomUri)
            .filter(conn => get(conn, "targetSocketUri") === targetSocketUri)
            .filter(
              conn =>
                get(conn, "socketUri") === atomUtils.getChatSocket(personaAtom)
            );

          if (personaConnections.size == 0) {
            this.props.connect(
              personaUri,
              undefined,
              _atomUri,
              message,
              vocab.CHAT.ChatSocketCompacted,
              targetSocketType
            );
            this.props.routerGoResetParams("connections");
          } else if (personaConnections.size == 1) {
            const personaConnection = personaConnections.first();
            const personaConnectionUri = get(personaConnection, "uri");

            if (
              connectionUtils.isSuggested(personaConnection) ||
              connectionUtils.isClosed(personaConnection)
            ) {
              this.props.connectSockets(
                get(personaConnection, "socketUri"),
                get(personaConnection, "targetSocketUri"),
                message
              );
            } else if (connectionUtils.isRequestSent(personaConnection)) {
              // Just go to the connection without sending another request
              /*
              //Send another Request with a new message if there is a message present
              this.props.connect(
                personaUri,
                personaConnectionUri,
                _atomUri,
                message,
                vocab.CHAT.ChatSocketCompacted,
                targetSocketType
              );
              */
            } else if (connectionUtils.isRequestReceived(personaConnection)) {
              const senderSocketUri = get(personaConnection, "socketUri");
              const targetSocketUri = get(personaConnection, "targetSocketUri");
              this.props.connectSockets(
                senderSocketUri,
                targetSocketUri,
                message
              );
            } else if (connectionUtils.isConnected(personaConnection)) {
              const senderSocketUri = get(personaConnection, "socketUri");
              const targetSocketUri = get(personaConnection, "targetSocketUri");
              this.props.sendChatMessage(
                message,
                undefined,
                undefined,
                senderSocketUri,
                targetSocketUri,
                personaConnectionUri,
                false
              );
            }

            this.props.routerGo("connections", {
              connectionUri: personaConnectionUri,
            });
          } else {
            console.error(
              "more than one connection stored between two atoms that use the same exact sockets",
              personaAtom,
              targetSocketUri
            );
          }
        } else {
          this.props.routerGoResetParams("connections");

          this.props.connectionsConnectAdHoc(
            targetSocketUri,
            message,
            personaUri
          );
        }
      }
    } else {
      this.props.showTermsDialog(
        Immutable.fromJS({
          acceptCallback: () => {
            this.props.hideModalDialog();
            this.props.routerGoResetParams("connections");

            this.props.connectionsConnectAdHoc(
              targetSocketUri,
              message,
              personaUri
            );
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
  atom: PropTypes.object,
  defaultTab: PropTypes.string,
  loggedIn: PropTypes.bool,
  isOwned: PropTypes.bool,
  showAdHocRequestField: PropTypes.bool,
  reactionUseCasesArray: PropTypes.arrayOf(PropTypes.object),
  enabledUseCasesArray: PropTypes.arrayOf(PropTypes.object),
  ownedReactionAtomsArray: PropTypes.arrayOf(PropTypes.object),
  atomLoading: PropTypes.bool,
  showFooter: PropTypes.bool,
  addHolderUri: PropTypes.string,
  holderUri: PropTypes.string,
  className: PropTypes.string,
  routerGo: PropTypes.func,
  routerGoResetParams: PropTypes.func,
  hideModalDialog: PropTypes.func,
  showModalDialog: PropTypes.func,
  showTermsDialog: PropTypes.func,
  connectionsConnectAdHoc: PropTypes.func,
  ownedChatSocketAtoms: PropTypes.object,
  connect: PropTypes.func,
  connectSockets: PropTypes.func,
  sendChatMessage: PropTypes.func,
  atomReopen: PropTypes.func,
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(AtomInfo);
