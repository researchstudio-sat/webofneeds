import React from "react";
import PropTypes from "prop-types";
import Immutable from "immutable";
import { connect } from "react-redux";
import { get, getIn } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import WonAtomHeader from "./atom-header.jsx";
import ChatTextfield from "./chat-textfield.jsx";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as connectionUtils from "../redux/utils/connection-utils.js";
import * as accountUtils from "../redux/utils/account-utils.js";
import * as useCaseUtils from "../usecase-utils.js";
import * as wonLabelUtils from "../won-label-utils.js";
import vocab from "../service/vocab.js";

import "~/style/_atom-footer.scss";

const FooterType = {
  INACTIVE: 1,
  INACTIVE_OWNED: 2,
  ENABLED: 3,
  REACTION: 4,
  ADHOC: 5,
  UNKNOWN: 6,
};

const mapStateToProps = (state, ownProps) => {
  const ownedAtoms = generalSelectors.getOwnedAtoms(state);
  const atom = getIn(state, ["atoms", ownProps.atomUri]);

  const isOwned = generalSelectors.isAtomOwned(state, ownProps.atomUri);

  const reactionUseCases = atomUtils.getReactionUseCases(atom);
  const enabledUseCases = atomUtils.getEnabledUseCases(atom);

  const ownedReactionAtoms =
    ownedAtoms &&
    ownedAtoms.filter(
      atom =>
        atomUtils.matchesDefinitions(atom, reactionUseCases) ||
        atomUtils.matchesDefinitions(atom, enabledUseCases)
    );

  let footerType = FooterType.UNKNOWN;
  let useCasesArray = [];

  if (atomUtils.isInactive(atom)) {
    footerType = isOwned ? FooterType.INACTIVE_OWNED : FooterType.INACTIVE;
  } else {
    if (isOwned && atomUtils.hasEnabledUseCases(atom)) {
      useCasesArray = atomUtils.getEnabledUseCases(atom).toArray();
      footerType = FooterType.ENABLED;
    } else if (atomUtils.hasReactionUseCases(atom)) {
      useCasesArray = atomUtils.getReactionUseCases(atom).toArray();
      footerType = FooterType.REACTION;
    } else if (
      atomUtils.hasChatSocket(atom) ||
      atomUtils.hasGroupSocket(atom)
    ) {
      footerType = FooterType.ADHOC;
    }
  }

  const ownedChatSocketAtoms =
    ownedAtoms && ownedAtoms.filter(atom => atomUtils.hasChatSocket(atom));

  return {
    className: ownProps.className,
    atomUri: ownProps.atomUri,
    atom: atom,
    loggedIn: accountUtils.isLoggedIn(get(state, "account")),
    footerType,
    useCasesArray,
    isOwned: isOwned,

    ownedReactionAtomsArray: ownedReactionAtoms
      ? ownedReactionAtoms.toArray()
      : [],
    addHolderUri:
      atomUtils.hasEnabledUseCases(atom) && footerType === FooterType.ENABLED
        ? atomUtils.getHeldByUri(atom)
        : undefined,
    ownedChatSocketAtoms,
  };
};

const mapDispatchToProps = dispatch => {
  return {
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
      senderAtomUri,
      targetAtomUri,
      senderSocketType,
      targetSocketType,
      message
    ) => {
      dispatch(
        actionCreators.atoms__connect(
          senderAtomUri,
          targetAtomUri,
          senderSocketType,
          targetSocketType,
          message
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
    connectSocketsServerSide: (senderSocketUri, targetSocketUri) => {
      dispatch(
        actionCreators.atoms__connectSocketsServerSide(
          senderSocketUri,
          targetSocketUri
        )
      );
    },
    atomReopen: atomUri => dispatch(actionCreators.atoms__reopen(atomUri)),
  };
};

class AtomInfo extends React.Component {
  render() {
    let footerElement;

    switch (this.props.footerType) {
      case FooterType.INACTIVE:
        footerElement = (
          <div className="atom-footer__infolabel">
            Atom is inactive, no requests allowed
          </div>
        );
        break;
      case FooterType.INACTIVE_OWNED:
        footerElement = (
          <React.Fragment>
            <div className="atom-footer__infolabel">
              This Atom is inactive. Others will not be able to interact with
              it.
            </div>
            <button
              className="won-publish-button red won-button--filled"
              onClick={() => this.props.atomReopen(this.props.atomUri)}
            >
              Reopen
            </button>
          </React.Fragment>
        );
        break;
      case FooterType.REACTION:
        footerElement = this.props.useCasesArray.map((useCase, index) =>
          this.getUseCaseTypeButton(useCase, index)
        );
        break;
      case FooterType.ENABLED:
        footerElement = this.props.useCasesArray.map((useCase, index) =>
          this.getUseCaseTypeButton(useCase, index)
        );
        break;
      case FooterType.ADHOC:
        footerElement = (
          <React.Fragment>
            {!!atomUtils.getChatSocket(this.props.atom) && (
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
            {!!atomUtils.getGroupSocket(this.props.atom) && (
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
          </React.Fragment>
        );
        break;
      case FooterType.UNKNOWN:
      default:
        footerElement = undefined;
        break;
    }

    return (
      <won-atom-footer class={this.props.className ? this.props.className : ""}>
        {footerElement}
      </won-atom-footer>
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
      .filter(reactionAtom => {
        // FILTER OUT ALL THE ATOMS THAT ALREADY HAVE AN EXISTING CONNECTION WITH THIS ATOM
        const senderSocketUri = atomUtils.getSocketUri(
          reactionAtom,
          ucSenderSocketType
        );
        const targetSocketUri = atomUtils.getSocketUri(
          this.props.atom,
          ucTargetSocketType
        );

        const existingConnections = get(reactionAtom, "connections")
          .filter(conn => get(conn, "targetAtomUri") === this.props.atomUri)
          .filter(conn =>
            connectionUtils.hasSocketUris(
              conn,
              senderSocketUri,
              targetSocketUri
            )
          );

        return !existingConnections || existingConnections.size == 0;
      })
      .map((reactionAtom, index) => {
        return (
          <WonAtomHeader
            key={get(reactionAtom, "uri") + "-" + index}
            atomUri={get(reactionAtom, "uri")}
            hideTimestamp={true}
            onClick={() =>
              this.connectAtomSockets(
                reactionAtom,
                atomUtils.getSocketUri(reactionAtom, ucSenderSocketType),
                atomUtils.getSocketUri(this.props.atom, ucTargetSocketType)
              )
            }
          />
        );
      });

    const type =
      this.props.footerType === FooterType.ENABLED
        ? "enabled"
        : this.props.footerType === FooterType.REACTION
          ? "reaction"
          : undefined;
    let headerText =
      getIn(wonLabelUtils.reactionLabels, [
        type,
        ucSenderSocketType,
        ucTargetSocketType,
      ]) || `Connect ${ucSenderSocketType} with ${ucTargetSocketType}`;

    return (
      <React.Fragment>
        <div className="atom-footer__header">{headerText}</div>
        <div className="atom-footer__matches">
          {!!atomElements &&
            atomElements.length > 0 && (
              <div className="atom-footer__matches__header">
                These existing Atoms you own would match
              </div>
            )}
          <div className="atom-footer__matches__list">
            {atomElements}
            <div
              key={ucIdentifier + "-" + index}
              className="atom-footer__adhocbutton"
              onClick={() =>
                this.selectUseCase(
                  ucIdentifier,
                  ucSenderSocketType,
                  ucTargetSocketType
                )
              }
            >
              <div className="atom-footer__adhocbutton__icon">
                {useCaseUtils.getUseCaseIcon(ucIdentifier) ? (
                  <svg className="atom-footer__adhocbutton__icon__svg">
                    <use
                      xlinkHref={useCaseUtils.getUseCaseIcon(ucIdentifier)}
                      href={useCaseUtils.getUseCaseIcon(ucIdentifier)}
                    />
                  </svg>
                ) : (
                  <svg className="atom-footer__adhocbutton__icon__svg">
                    <use xlinkHref="ico36_plus" href="ico36_plus" />
                  </svg>
                )}
              </div>
              <div className="atom-footer__adhocbutton__right">
                <div className="atom-footer__adhocbutton__right__topline">
                  <div className="atom-footer__adhocbutton__right__topline__notitle">
                    Create Atom
                  </div>
                </div>
                <div className="atom-footer__adhocbutton__right__subtitle">
                  <span className="atom-footer__adhocbutton__right__subtitle__type">
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
      holderUri: this.props.addHolderUri,
    });
  }

  connectAtomSockets(
    senderAtom,
    senderSocketUri,
    targetSocketUri,
    message = ""
  ) {
    const targetAtom = this.props.atom;
    const footerType = this.props.footerType;
    const dialogText = "Connect with this Atom?";

    const payload = {
      caption: "Connect",
      text: dialogText,
      buttons: [
        {
          caption: "Yes",
          callback: () => {
            if (footerType === FooterType.ENABLED) {
              this.props.connectSocketsServerSide(
                senderSocketUri,
                targetSocketUri
              );
            } else {
              this.props.connectSockets(
                senderSocketUri,
                targetSocketUri,
                message
              );
            }
            this.props.hideModalDialog();

            const senderSocketType = atomUtils.getSocketType(
              senderAtom,
              senderSocketUri
            );
            const targetSocketType = atomUtils.getSocketType(
              targetAtom,
              targetSocketUri
            );
            if (
              senderSocketType === vocab.CHAT.ChatSocketCompacted ||
              targetSocketType === vocab.CHAT.ChatSocketCompacted
            ) {
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
              _atomUri,
              vocab.CHAT.ChatSocketCompacted,
              targetSocketType,
              message
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
                _atomUri,
                vocab.CHAT.ChatSocketCompacted,
                targetSocketType,
                message
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
  loggedIn: PropTypes.bool,
  isOwned: PropTypes.bool,
  footerType: PropTypes.number,
  useCasesArray: PropTypes.arrayOf(PropTypes.object),
  ownedReactionAtomsArray: PropTypes.arrayOf(PropTypes.object),
  addHolderUri: PropTypes.string,
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
  connectSocketsServerSide: PropTypes.func,
  sendChatMessage: PropTypes.func,
  atomReopen: PropTypes.func,
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(AtomInfo);
