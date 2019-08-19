import React from "react";
import PropTypes from "prop-types";
import { connect } from "react-redux";
import { get, getIn } from "../utils.js";

import ElmReact from "./elm-react.jsx";
import WonLabelledHr from "./labelled-hr.jsx";
import TextareaAutosize from "react-autosize-textarea";
import { actionCreators } from "../actions/actions.js";

import { Elm } from "../../elm/PublishButton.elm";

import { getMessagesByConnectionUri } from "../redux/selectors/message-selectors.js";
import { getHumanReadableStringFromMessage } from "../reducers/atom-reducer/parse-message.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as messageUtils from "../redux/utils/message-utils.js";
import * as connectionSelectors from "../redux/selectors/connection-selectors.js";
import * as connectionUtils from "../redux/utils/connection-utils.js";
import * as accountUtils from "../redux/utils/account-utils.js";
import * as useCaseUtils from "../usecase-utils.js";

import "~/style/_chattextfield.scss";
import "~/style/_textfield.scss";

const allMessageDetails = useCaseUtils.getAllMessageDetails();

const mapStateToProps = (state, ownProps) => {
  const atom =
    ownProps.connectionUri &&
    generalSelectors.getOwnedAtomByConnectionUri(state, ownProps.connectionUri);
  const connection = getIn(atom, ["connections", ownProps.connectionUri]);

  const messages = getMessagesByConnectionUri(state, ownProps.connectionUri);

  const selectedMessages =
    messages && messages.filter(msg => messageUtils.isMessageSelected(msg));
  const rejectableMessages =
    messages && messages.filter(msg => messageUtils.isMessageRejectable(msg));
  const retractableMessages =
    messages && messages.filter(msg => messageUtils.isMessageRetractable(msg));
  const acceptableMessages =
    messages && messages.filter(msg => messageUtils.isMessageAcceptable(msg));
  const proposableMessages =
    messages && messages.filter(msg => messageUtils.isMessageProposable(msg));
  const cancelableMessages =
    messages && messages.filter(msg => messageUtils.isMessageCancelable(msg));
  const claimableMessages =
    messages && messages.filter(msg => messageUtils.isMessageClaimable(msg));

  const hasRejectableMessages =
    rejectableMessages && rejectableMessages.size > 0;
  const hasRetractableMessages =
    retractableMessages && retractableMessages.size > 0;

  const hasAcceptableMessages =
    acceptableMessages && acceptableMessages.size > 0;
  const hasProposableMessages =
    proposableMessages && proposableMessages.size > 0;
  const hasCancelableMessages =
    cancelableMessages && cancelableMessages.size > 0;
  const hasClaimableMessages = claimableMessages && claimableMessages.size > 0;

  const selectedDetailIdentifier = getIn(state, [
    "view",
    "selectedAddMessageContent",
  ]);
  const selectedDetail =
    allMessageDetails &&
    selectedDetailIdentifier &&
    allMessageDetails[selectedDetailIdentifier];
  return {
    className: ownProps.className,
    connectionUri: ownProps.connectionUri,
    placeholder: ownProps.placeholder,
    helpText: ownProps.helpText,
    isCode: ownProps.isCode,
    allowDetails: ownProps.allowDetails,
    allowEmptySubmit: ownProps.allowEmptySubmit,
    showPersonas: ownProps.showPersonas,
    showPersonasSelection: ownProps.showPersonas || false,
    atom,
    multiSelectType: get(connection, "multiSelectType"),
    showAgreementData: get(connection, "showAgreementData"),
    isChatToGroupConnection: connectionSelectors.isChatToGroupConnection(
      get(state, "atoms"),
      connection
    ),
    isConnected: connectionUtils.isConnected(connection),
    selectedMessages: selectedMessages,
    hasClaimableMessages,
    hasProposableMessages,
    hasCancelableMessages,
    hasAcceptableMessages,
    hasRetractableMessages,
    hasRejectableMessages,
    connectionHasBeenLost:
      getIn(state, ["messages", "reconnecting"]) ||
      getIn(state, ["messages", "lostConnection"]),
    showAddMessageContent: getIn(state, ["view", "showAddMessageContent"]),
    selectedDetail,
    selectedDetailComponent: selectedDetail && selectedDetail.component,
    isLoggedIn: accountUtils.isLoggedIn(get(state, "account")),
    personas: generalSelectors.getOwnedCondensedPersonaList(state).toJS(),
    submitButtonLabel: ownProps.submitButtonLabel,
    onSubmit: ownProps.onSubmit,
  };
};

const mapDispatchToProps = dispatch => {
  return {
    viewRemoveAddMessageContent: () => {
      dispatch(actionCreators.view__removeAddMessageContent());
    },
    setMultiSelectType: (connectionUri, type) => {
      dispatch(
        actionCreators.connections__setMultiSelectType({
          connectionUri: connectionUri,
          multiSelectType: type,
        })
      );
    },
    selectDetail: detail => {
      dispatch(
        actionCreators.view__selectAddMessageContent({
          selectedDetail: detail.identifier,
        })
      );
    },
    toggleAddMessageContent: () => {
      dispatch(actionCreators.view__toggleAddMessageContent());
    },
    hideAddMessageContent: () => {
      dispatch(actionCreators.view__hideAddMessageContent());
    },
    markMessageAsSelected: (msgUri, connUri, atomUri) => {
      dispatch(
        actionCreators.messages__viewState__markAsSelected({
          messageUri: msgUri,
          connectionUri: connUri,
          atomUri: atomUri,
          isSelected: true,
        })
      );
    },
    markMessageAsUnselected: (msgUri, connUri, atomUri) => {
      dispatch(
        actionCreators.messages__viewState__markAsSelected({
          messageUri: msgUri,
          connectionUri: connUri,
          atomUri: atomUri,
          isSelected: false,
        })
      );
    },
  };
};

class ChatTextfield extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      message: "",
      additionalContent: new Map(),
      referencedContent: new Map(),
    };
  }

  render() {
    let detailDrawerElement;

    if (this.props.allowDetails && this.props.showAddMessageContent) {
      const selectDetailButtonElements =
        allMessageDetails &&
        allMessageDetails.length > 0 &&
        allMessageDetails.map(detail => {
          if (detail.component) {
            return (
              <div
                key={detail.identifier}
                className="cts__details__grid__detail"
                onClick={() => this.props.selectDetail(detail)}
              >
                {detail.icon && (
                  <svg className="cts__details__grid__detail__icon">
                    <use xlinkHref={detail.icon} href={detail.icon} />
                  </svg>
                )}
                {detail.label && (
                  <div className="cts__details__grid__detail__label">
                    {detail.label}
                  </div>
                )}
              </div>
            );
          }
        });

      let selectedMessagesElement;

      if (this.props.selectedMessages && this.props.selectedMessages.size > 0) {
        const selectedMessageArrayElement = this.props.selectedMessages
          .toArray()
          .map((msg, index) => {
            return (
              <div
                key={msg.uri + "-" + index}
                className="cts__details__input__refcontent__message"
              >
                <div className="cts__details__input__refcontent__message__label">
                  {getHumanReadableStringFromMessage(msg) ||
                    "«Message does not have text»"}
                </div>
                <svg
                  className="cts__details__input__refcontent__message__discard clickable"
                  onClick={() =>
                    this.props.markMessageAsUnselected(
                      get(msg, "uri"),
                      this.props.connectionUri,
                      get(this.props.atom, "uri")
                    )
                  }
                >
                  <use xlinkHref="#ico36_close" href="#ico36_close" />
                </svg>
              </div>
            );
          });

        selectedMessagesElement = (
          <React.Fragment>
            <div className="cts__details__input__refcontent hide-in-responsive">
              {selectedMessageArrayElement}
            </div>
            <div className="cts__details__input__refcontent show-in-responsive">
              {this.props.selectedMessages.size} Messages selected
            </div>
          </React.Fragment>
        );
      } else {
        selectedMessagesElement = (
          <div className="cts__details__input__refcontent">
            Select Messages above
          </div>
        );
      }

      let detailsElement;

      if (!this.props.selectedDetail && !this.props.multiSelectType) {
        detailsElement = (
          <div className="cts__details__grid">
            {!this.props.isChatToGroupConnection && (
              <React.Fragment>
                {this.props.isConnected && (
                  <WonLabelledHr
                    className="cts__details__grid__hr"
                    label="Actions"
                  />
                )}
                {this.props.showAgreementData ? (
                  <React.Fragment>
                    <button
                      className="cts__details__grid__action won-button--filled red"
                      onClick={() => this.activateMultiSelect("accepts")}
                      disabled={!this.props.hasAcceptableMessages}
                    >
                      Accept Proposal(s)
                    </button>
                    <button
                      className="cts__details__grid__action won-button--filled red"
                      onClick={() => this.activateMultiSelect("rejects")}
                      disabled={!this.props.hasRejectableMessages}
                    >
                      Reject Proposal(s)
                    </button>
                  </React.Fragment>
                ) : (
                  <React.Fragment>
                    <button
                      className="cts__details__grid__action won-button--filled red"
                      onClick={() => this.activateMultiSelect("proposes")}
                      disabled={!this.props.hasProposableMessages}
                    >
                      Make Proposal
                    </button>
                    <button
                      className="cts__details__grid__action won-button--filled red"
                      onClick={() => this.activateMultiSelect("claims")}
                      disabled={!this.props.hasClaimableMessages}
                    >
                      Make Claim
                    </button>
                  </React.Fragment>
                )}

                <button
                  className="cts__details__grid__action won-button--filled red"
                  onClick={() => this.activateMultiSelect("proposesToCancel")}
                  disabled={!this.props.hasCancelableMessages}
                >
                  Cancel Agreement(s)
                </button>
                <button
                  className="cts__details__grid__action won-button--filled red"
                  onClick={() => this.activateMultiSelect("retracts")}
                  disabled={!this.props.hasRetractableMessages}
                >
                  Retract Message(s)
                </button>
              </React.Fragment>
            )}
            {this.props.isConnected && (
              <WonLabelledHr
                className="cts__details__grid__hr"
                label="Details"
              />
            )}
            {selectDetailButtonElements}
          </div>
        );
      } else if (this.props.selectedDetail && !this.props.multiSelectType) {
        detailsElement = (
          <div className="cts__details__input">
            <div className="cts__details__input__header">
              <svg
                className="cts__details__input__header__back clickable"
                onClick={this.props.viewRemoveAddMessageContent}
              >
                <use xlinkHref="#ico36_backarrow" href="#ico36_backarrow" />
              </svg>
              <svg className="cts__details__input__header__icon">
                <use
                  xlinkHref={this.props.selectedDetail.icon}
                  href={this.props.selectedDetail.icon}
                />
              </svg>
              <div className="cts__details__input__header__label">
                {this.props.selectedDetail.label}
              </div>
            </div>
            {this.props.selectedDetailComponent && (
              <this.props.selectedDetailComponent
                className="cts__details__input__content"
                onUpdate={({ value }) =>
                  this.updateDetail(this.props.selectedDetail.identifier, value)
                }
                initialValue={get(
                  this.state.additionalContent,
                  this.props.selectedDetail.identifier
                )}
                detail={this.props.selectedDetail}
              />
            )}
          </div>
        );
      } else if (!this.props.selectedDetail && this.props.multiSelectType) {
        detailsElement = (
          <div className="cts__details__input">
            <div className="cts__details__input__header">
              <svg
                className="cts__details__input__header__back clickable"
                onClick={() =>
                  this.props.setMultiSelectType(
                    this.props.connectionUri,
                    undefined
                  )
                }
              >
                <use xlinkHref="#ico36_backarrow" href="#ico36_backarrow" />
              </svg>
              <svg className="cts__details__input__header__icon">
                <use xlinkHref="#ico36_plus_circle" href="#ico36_plus_circle" />
              </svg>
              <div className="cts__details__input__header__label hide-in-responsive">
                {this.getMultiSelectActionLabel()} (
                {this.props.selectedMessages.size} Messages)
              </div>
              <div className="cts__details__input__header__label show-in-responsive">
                {this.getMultiSelectActionLabel()}
              </div>
              <div
                className="cts__details__input__header__add"
                onClick={this.saveReferencedContent.bind(this)}
              >
                <svg className="cts__details__input__header__add__icon">
                  <use
                    xlinkHref="#ico36_added_circle"
                    href="#ico36_added_circle"
                  />
                </svg>
                <span className="cts__details__input__header__add__label hide-in-responsive">
                  Save
                </span>
              </div>
              <div
                className="cts__details__input__header__discard"
                onClick={this.removeReferencedContent.bind(this)}
              >
                <svg className="cts__details__input__header__discard__icon">
                  <use
                    xlinkHref="#ico36_close_circle"
                    href="#ico36_close_circle"
                  />
                </svg>
                <span className="cts__details__input__header__discard__label hide-in-responsive">
                  Discard
                </span>
              </div>
            </div>
            {selectedMessagesElement}
          </div>
        );
      }

      detailDrawerElement = (
        <div className="cts__details">{detailsElement}</div>
      );
    }

    const detailDrawerToggleElement = (
      <button
        className="cts__add"
        disabled={!this.props.allowDetails}
        onClick={this.toggleAdditionalContentDisplay.bind(this)}
      >
        <svg className="cts__add__icon">
          {this.props.showAddMessageContent ? (
            <use xlinkHref="#ico36_close" href="#ico36_close" />
          ) : (
            <use xlinkHref="#ico36_plus" href="#ico36_plus" />
          )}
        </svg>
      </button>
    );

    let helpTextElement;

    if (this.props.helpText) {
      helpTextElement = (
        <div className="cts__helptext">{this.props.helpText}</div>
      );
    }

    let addedDetailsElement;
    if (this.hasAdditionalContent() || this.hasReferencedContent()) {
      const referencedContentArrayElements =
        this.getReferencedContentKeysArray() &&
        this.getReferencedContentKeysArray().length > 0 &&
        this.getReferencedContentKeysArray().map((ref, index) => {
          const referencedMessages = get(this.state.referencedContent, ref);
          const referencedMessagesSize = referencedMessages
            ? referencedMessages.size
            : 0;

          let humanReadableReferenceString = "";

          switch (ref) {
            case "rejects":
              humanReadableReferenceString = "Reject ";
              break;
            case "retracts":
              humanReadableReferenceString = "Retract ";
              break;
            case "claims":
              humanReadableReferenceString = "Claims ";
              break;
            case "proposes":
              humanReadableReferenceString = "Propose ";
              break;
            case "accepts":
              humanReadableReferenceString = "Accept ";
              break;
            case "proposesToCancel":
              humanReadableReferenceString = "Propose To Cancel ";
              break;
            default:
              return "illegal state";
          }

          humanReadableReferenceString +=
            referencedMessagesSize +
            (referencedMessagesSize > 1 ? " Messages" : " Message");

          return (
            <div
              key={ref + "-" + index}
              className="cts__additionalcontent__list__item"
            >
              <svg
                className="cts__additionalcontent__list__item__icon clickable"
                onClick={() => this.activateMultiSelect(ref)}
              >
                <use xlinkHref="#ico36_plus" href="#ico36_plus" />
              </svg>
              <span
                className="cts__additionalcontent__list__item__label clickable"
                onClick={() => this.activateMultiSelect(ref)}
              >
                {humanReadableReferenceString}
              </span>
              <svg
                className="cts__additionalcontent__list__item__discard clickable"
                onClick={() => this.removeReferencedContent(ref)}
              >
                <use xlinkHref="#ico36_close" href="#ico36_close" />
              </svg>
            </div>
          );
        });

      const additionalContentKeysArrayElements =
        this.getAdditionalContentKeysArray() &&
        this.getAdditionalContentKeysArray().length > 0 &&
        this.getAdditionalContentKeysArray().map((key, index) => {
          const usedDetail = allMessageDetails[key];

          const humanReadableDetail =
            usedDetail &&
            usedDetail.generateHumanReadable({
              value: get(this.state.additionalContent, key),
              includeLabel: true,
            });

          return (
            <div
              key={key + "-" + index}
              className="cts__additionalcontent__list__item"
            >
              <svg
                className="cts__additionalcontent__list__item__icon clickable"
                onClick={() => this.props.selectDetail(allMessageDetails[key])}
              >
                <use
                  xlinkHref={allMessageDetails[key].icon}
                  href={allMessageDetails[key].icon}
                />
              </svg>
              <span
                className="cts__additionalcontent__list__item__label clickable"
                onClick={() => this.props.selectDetail(allMessageDetails[key])}
              >
                {humanReadableDetail}
              </span>
              <svg
                className="cts__additionalcontent__list__item__discard clickable"
                onClick={() => this.updateDetail(key, undefined, true)}
              >
                <use xlinkHref="#ico36_close" href="#ico36_close" />
              </svg>
            </div>
          );
        });

      addedDetailsElement = (
        <div className="cts__additionalcontent">
          <div className="cts__additionalcontent__header">
            Additional Content to send:
          </div>
          <div className="cts__additionalcontent__list">
            {referencedContentArrayElements}
            {additionalContentKeysArrayElements}
          </div>
        </div>
      );
    }

    return (
      <chat-textfield class={this.props.className || ""}>
        {detailDrawerElement}
        {detailDrawerToggleElement}

        <TextareaAutosize
          className={
            "cts__text won-txt " + (this.props.isCode && " won-txt--code ")
          }
          tabIndex="0"
          placeholder={this.props.placeholder}
          maxRows={4}
          onKeyDown={this.keydown.bind(this)}
          onChange={event => this.setState({ message: event.target.value })}
          value={this.state.message}
        />
        <div className="cts__submitbutton">
          <ElmReact
            src={Elm.PublishButton}
            flags={{
              buttonEnabled: this.valid(),
              showPersonas: !!this.props.showPersonas,
              personas: this.props.personas,
              label: this.props.submitButtonLabel,
            }}
            onPublish={this.submit.bind(this)}
          />
        </div>

        {addedDetailsElement}
        {helpTextElement}
      </chat-textfield>
    );
  }

  getMultiSelectActionLabel() {
    if (this.props.multiSelectType) {
      switch (this.props.multiSelectType) {
        case "rejects":
          return "Reject selected";
        case "retracts":
          return "Retract selected";
        case "proposes":
          return "Propose selected";
        case "accepts":
          return "Accept selected";
        case "proposesToCancel":
          return "Propose To Cancel selected";
        default:
          return "illegal state";
      }
    }
  }

  updateDetail(name, value, closeOnDelete = false) {
    const _additionalContent = this.state.additionalContent;

    if (!value) {
      _additionalContent.delete(name);
      this.setState({ additionalContent: _additionalContent });
      if (closeOnDelete) {
        this.props.hideAddMessageContent();
      }
    } else {
      _additionalContent.set(name, value);
      this.setState({ additionalContent: _additionalContent });
    }
  }

  activateMultiSelect(type) {
    this.props.setMultiSelectType(this.props.connectionUri, type);
    const referencedContent = get(this.state.referencedContent, type);
    if (referencedContent) {
      referencedContent.forEach(msg => {
        this.props.markMessageAsSelected(
          get(msg, "uri"),
          this.props.connectionUri,
          get(this.props.atom, "uri")
        );
      });
    }
  }

  getAdditionalContentKeysArray() {
    return (
      this.state.additionalContent &&
      this.state.additionalContent.keys() &&
      Array.from(this.state.additionalContent.keys())
    );
  }

  getReferencedContentKeysArray() {
    return (
      this.state.referencedContent &&
      this.state.referencedContent.keys() &&
      Array.from(this.state.referencedContent.keys())
    );
  }

  toggleAdditionalContentDisplay() {
    this.props.setMultiSelectType(this.props.connectionUri, undefined);
    this.props.toggleAddMessageContent();
  }

  valid() {
    return (
      !this.props.connectionHasBeenLost &&
      (this.props.allowEmptySubmit ||
        this.hasAdditionalContent() ||
        this.hasReferencedContent() ||
        this.state.message.trim().length > 0)
    );
  }

  hasAdditionalContent() {
    return (
      this.state.additionalContent && this.state.additionalContent.size > 0
    );
  }

  hasReferencedContent() {
    return (
      this.state.referencedContent && this.state.referencedContent.size > 0
    );
  }

  submit(selectedPersona) {
    const value = this.state.message.trim();
    const valid = this.valid();
    if (valid) {
      const payload = {
        value,
        valid,
        additionalContent: this.state.additionalContent,
        referencedContent: this.state.referencedContent,
        selectedPersona: selectedPersona || undefined,
      };

      this.setState({
        message: "",
        additionalContent: new Map(),
        referencedContent: new Map(),
      });

      this.props.setMultiSelectType(this.props.connectionUri, undefined);
      this.props.onSubmit(payload);
    }
  }

  removeReferencedContent(ref = this.props.multiSelectType) {
    const _referencedContent = this.state.referencedContent;
    _referencedContent.delete(ref);
    this.setState({
      referencedContent: _referencedContent,
    });

    this.props.setMultiSelectType(this.props.connectionUri, undefined);
  }

  saveReferencedContent() {
    const _referencedContent = this.state.referencedContent;

    if (!this.props.selectedMessages || this.props.selectedMessages.size == 0) {
      _referencedContent.delete(this.props.multiSelectType);
    } else {
      _referencedContent.set(
        this.props.multiSelectType,
        this.props.selectedMessages
      );
    }

    this.setState({
      referencedContent: _referencedContent,
    });
    this.props.setMultiSelectType(this.props.connectionUri, undefined);
  }

  keydown(e) {
    if (e.keyCode === 13 && !e.shiftKey) {
      e.preventDefault(); // prevent a newline from being entered
      this.submit();
      return false;
    }
  }
}

ChatTextfield.propTypes = {
  className: PropTypes.string,
  connectionUri: PropTypes.string,
  placeholder: PropTypes.string,
  helpText: PropTypes.string,
  isCode: PropTypes.bool,
  allowDetails: PropTypes.bool,
  allowEmptySubmit: PropTypes.bool,
  showPersonas: PropTypes.bool,
  atom: PropTypes.object,
  multiSelectType: PropTypes.bool,
  showAgreementData: PropTypes.bool,
  isChatToGroupConnection: PropTypes.bool,
  isConnected: PropTypes.bool,
  selectedMessages: PropTypes.object,
  hasClaimableMessages: PropTypes.bool,
  hasProposableMessages: PropTypes.bool,
  hasCancelableMessages: PropTypes.bool,
  hasAcceptableMessages: PropTypes.bool,
  hasRetractableMessages: PropTypes.bool,
  hasRejectableMessages: PropTypes.bool,
  connectionHasBeenLost: PropTypes.bool,
  showAddMessageContent: PropTypes.bool,
  selectedDetail: PropTypes.object,
  selectedDetailComponent: PropTypes.object,
  isLoggedIn: PropTypes.bool,
  personas: PropTypes.arrayOf(PropTypes.object),
  submitButtonLabel: PropTypes.string,
  viewRemoveAddMessageContent: PropTypes.func,
  setMultiSelectType: PropTypes.func,
  markMessageAsSelected: PropTypes.func,
  markMessageAsUnselected: PropTypes.func,
  selectDetail: PropTypes.func,
  toggleAddMessageContent: PropTypes.func,
  hideAddMessageContent: PropTypes.func,
  onSubmit: PropTypes.func,
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(ChatTextfield);
