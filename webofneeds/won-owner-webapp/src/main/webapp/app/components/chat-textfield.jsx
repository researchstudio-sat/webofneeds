import React, { useState } from "react";
import PropTypes from "prop-types";
import { useDispatch, useSelector } from "react-redux";
import { get, getIn } from "../utils.js";

import ElmReact from "./elm-react.jsx";
import WonLabelledHr from "./labelled-hr.jsx";
import TextareaAutosize from "react-autosize-textarea";
import { actionCreators } from "../actions/actions.js";

import { Elm } from "../../elm/PublishButton.elm";

import { getHumanReadableStringFromMessage } from "../reducers/atom-reducer/parse-message.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as messageUtils from "../redux/utils/message-utils.js";
import * as connectionUtils from "../redux/utils/connection-utils.js";
import * as useCaseUtils from "../usecase-utils.js";

import "~/style/_chattextfield.scss";
import "~/style/_textfield.scss";
import ico36_close from "~/images/won-icons/ico36_close.svg";
import ico36_plus from "~/images/won-icons/ico36_plus.svg";
import ico36_backarrow from "~/images/won-icons/ico36_backarrow.svg";
import ico36_plus_circle from "~/images/won-icons/ico36_plus_circle.svg";
import ico36_added_circle from "~/images/won-icons/ico36_added_circle.svg";
import ico36_close_circle from "~/images/won-icons/ico36_close_circle.svg";
import * as atomUtils from "../redux/utils/atom-utils";

export default function ChatTextfield({
  connection,
  className,
  placeholder,
  helpText,
  isCode,
  allowDetails,
  allowEmptySubmit,
  showPersonas,
  submitButtonLabel,
  onSubmit,
}) {
  const allMessageDetailsImm = useCaseUtils.getAllMessageDetailsImm();
  const dispatch = useDispatch();
  const [state, setState] = useState({
    message: "",
    additionalContent: new Map(),
    referencedContent: new Map(),
  });

  const connectionUri = get(connection, "uri");

  const atom = useSelector(
    generalSelectors.getOwnedAtomByConnectionUri(connectionUri)
  );
  const targetAtom = useSelector(
    generalSelectors.getAtom(get(connection, "targetAtomUri"))
  );

  const messages = get(connection, "messages");

  const selectedMessages =
    messages && messages.filter(msg => messageUtils.isMessageSelected(msg));
  const rejectableMessages =
    messages &&
    messages.filter(msg => messageUtils.isMessageRejectable(connection, msg));
  const retractableMessages =
    messages &&
    messages.filter(msg => messageUtils.isMessageRetractable(connection, msg));
  const acceptableMessages =
    messages &&
    messages.filter(msg => messageUtils.isMessageAcceptable(connection, msg));
  const proposableMessages =
    messages &&
    messages.filter(msg => messageUtils.isMessageProposable(connection, msg));
  const cancelableMessages =
    messages &&
    messages.filter(msg => messageUtils.isMessageCancelable(connection, msg));
  const claimableMessages =
    messages &&
    messages.filter(msg => messageUtils.isMessageClaimable(connection, msg));
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
  const selectedDetailIdentifier = useSelector(state =>
    getIn(state, ["view", "selectedAddMessageContent"])
  );
  const selectedDetail =
    selectedDetailIdentifier &&
    get(allMessageDetailsImm, selectedDetailIdentifier);

  const multiSelectType = get(connection, "multiSelectType");
  const showAgreementData = get(connection, "showAgreementData");
  const isChatToGroupConnection =
    atomUtils.getGroupSocket(targetAtom) === get(connection, "targetSocketUri");
  const isConnected = connectionUtils.isConnected(connection);
  const connectionHasBeenLost = !useSelector(
    generalSelectors.selectIsConnected
  );
  const showAddMessageContent = useSelector(state =>
    getIn(state, ["view", "showAddMessageContent"])
  );

  const personas = useSelector(state =>
    generalSelectors.getOwnedCondensedPersonaList(state).toJS()
  );

  function updateDetail(name, value, closeOnDelete = false) {
    const _additionalContent = state.additionalContent;

    if (!value) {
      _additionalContent.delete(name);
      setState({
        ...state,
        additionalContent: _additionalContent,
      });
      if (closeOnDelete) {
        dispatch(actionCreators.view__hideAddMessageContent());
      }
    } else {
      _additionalContent.set(name, value);
      setState({
        ...state,
        additionalContent: _additionalContent,
      });
    }
  }

  function activateMultiSelect(type) {
    dispatch(
      actionCreators.connections__setMultiSelectType({
        connectionUri: connectionUri,
        multiSelectType: type,
      })
    );
    const referencedContent = get(state.referencedContent, type);
    if (referencedContent) {
      referencedContent.forEach(msg => {
        dispatch(
          actionCreators.messages__viewState__markAsSelected({
            messageUri: get(msg, "uri"),
            connectionUri: connectionUri,
            atomUri: get(atom, "uri"),
            isSelected: true,
          })
        );
      });
    }
  }

  function getAdditionalContentKeysArray() {
    return (
      state.additionalContent &&
      state.additionalContent.keys() &&
      Array.from(state.additionalContent.keys())
    );
  }

  function getReferencedContentKeysArray() {
    return (
      state.referencedContent &&
      state.referencedContent.keys() &&
      Array.from(state.referencedContent.keys())
    );
  }

  function toggleAdditionalContentDisplay() {
    dispatch(
      actionCreators.connections__setMultiSelectType({
        connectionUri: connectionUri,
        multiSelectType: undefined,
      })
    );
    dispatch(actionCreators.view__toggleAddMessageContent());
  }

  function valid() {
    return (
      !connectionHasBeenLost &&
      (allowEmptySubmit ||
        hasAdditionalContent() ||
        hasReferencedContent() ||
        state.message.trim().length > 0)
    );
  }

  function hasAdditionalContent() {
    return state.additionalContent && state.additionalContent.size > 0;
  }

  function hasReferencedContent() {
    return state.referencedContent && state.referencedContent.size > 0;
  }

  function submit(selectedPersona) {
    const value = state.message.trim();
    const isValid = valid();
    if (isValid) {
      const payload = {
        value,
        isValid,
        additionalContent: state.additionalContent,
        referencedContent: state.referencedContent,
        selectedPersona: selectedPersona || undefined,
      };

      setState({
        message: "",
        additionalContent: new Map(),
        referencedContent: new Map(),
      });

      dispatch(
        actionCreators.connections__setMultiSelectType({
          connectionUri: connectionUri,
          multiSelectType: undefined,
        })
      );
      onSubmit(payload);
    }
  }

  function removeReferencedContent(ref = multiSelectType) {
    const _referencedContent = state.referencedContent;
    _referencedContent.delete(ref);
    setState({
      ...state,
      referencedContent: _referencedContent,
    });

    dispatch(
      actionCreators.connections__setMultiSelectType({
        connectionUri: connectionUri,
        multiSelectType: undefined,
      })
    );
  }

  function saveReferencedContent() {
    const _referencedContent = state.referencedContent;

    if (!selectedMessages || selectedMessages.size == 0) {
      _referencedContent.delete(multiSelectType);
    } else {
      _referencedContent.set(multiSelectType, selectedMessages);
    }

    setState({
      ...state,
      referencedContent: _referencedContent,
    });
    dispatch(
      actionCreators.connections__setMultiSelectType({
        connectionUri: connectionUri,
        multiSelectType: undefined,
      })
    );
  }

  function keydown(e) {
    if (e.keyCode === 13 && !e.shiftKey) {
      e.preventDefault(); // prevent a newline from being entered
      submit();
      return false;
    }
  }

  let detailDrawerElement;

  if (allowDetails && showAddMessageContent) {
    const selectDetailButtonElements =
      allMessageDetailsImm &&
      allMessageDetailsImm.size > 0 &&
      allMessageDetailsImm.toArray().map(detail => {
        if (get(detail, "component")) {
          return (
            <div
              key={get(detail, "identifier")}
              className="cts__details__grid__detail"
              onClick={() =>
                dispatch(
                  actionCreators.view__selectAddMessageContent({
                    selectedDetail: get(detail, "identifier"),
                  })
                )
              }
            >
              {get(detail, "icon") && (
                <svg className="cts__details__grid__detail__icon">
                  <use
                    xlinkHref={get(detail, "icon")}
                    href={get(detail, "icon")}
                  />
                </svg>
              )}
              {get(detail, "label") && (
                <div className="cts__details__grid__detail__label">
                  {get(detail, "label")}
                </div>
              )}
            </div>
          );
        }
      });

    let selectedMessagesElement;

    if (selectedMessages && selectedMessages.size > 0) {
      const selectedMessageArrayElement = selectedMessages
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
                  dispatch(
                    actionCreators.messages__viewState__markAsSelected({
                      messageUri: get(msg, "uri"),
                      connectionUri: connectionUri,
                      atomUri: get(atom, "uri"),
                      isSelected: false,
                    })
                  )
                }
              >
                <use xlinkHref={ico36_close} href={ico36_close} />
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
            {selectedMessages.size} Messages selected
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

    if (!selectedDetail && !multiSelectType) {
      detailsElement = (
        <div className="cts__details__grid">
          {!isChatToGroupConnection && (
            <React.Fragment>
              {isConnected && (
                <WonLabelledHr
                  className="cts__details__grid__hr"
                  label="Actions"
                />
              )}
              {showAgreementData ? (
                <React.Fragment>
                  <button
                    className="cts__details__grid__action won-button--filled red"
                    onClick={() => activateMultiSelect("accepts")}
                    disabled={!hasAcceptableMessages}
                  >
                    Accept Proposal(s)
                  </button>
                  <button
                    className="cts__details__grid__action won-button--filled red"
                    onClick={() => activateMultiSelect("rejects")}
                    disabled={!hasRejectableMessages}
                  >
                    Reject Proposal(s)
                  </button>
                </React.Fragment>
              ) : (
                <React.Fragment>
                  <button
                    className="cts__details__grid__action won-button--filled red"
                    onClick={() => activateMultiSelect("proposes")}
                    disabled={!hasProposableMessages}
                  >
                    Make Proposal
                  </button>
                  <button
                    className="cts__details__grid__action won-button--filled red"
                    onClick={() => activateMultiSelect("claims")}
                    disabled={!hasClaimableMessages}
                  >
                    Make Claim
                  </button>
                </React.Fragment>
              )}

              <button
                className="cts__details__grid__action won-button--filled red"
                onClick={() => activateMultiSelect("proposesToCancel")}
                disabled={!hasCancelableMessages}
              >
                Cancel Agreement(s)
              </button>
              <button
                className="cts__details__grid__action won-button--filled red"
                onClick={() => activateMultiSelect("retracts")}
                disabled={!hasRetractableMessages}
              >
                Retract Message(s)
              </button>
            </React.Fragment>
          )}
          {isConnected && (
            <WonLabelledHr className="cts__details__grid__hr" label="Details" />
          )}
          {selectDetailButtonElements}
        </div>
      );
    } else if (selectedDetail && !multiSelectType) {
      //we need to call toJS because otherwise non functional PickerComponents are invalid
      const selectedDetailJS = selectedDetail.toJS();

      const selectedDetailIcon = get(selectedDetail, "icon");
      const selectedDetailLabel = get(selectedDetail, "label");
      const selectedDetailIdentifier = get(selectedDetail, "identifier");
      const SelectedDetailComponent = selectedDetailJS.component;

      detailsElement = (
        <div className="cts__details__input">
          <div className="cts__details__input__header">
            <svg
              className="cts__details__input__header__back clickable"
              onClick={() =>
                dispatch(actionCreators.view__removeAddMessageContent())
              }
            >
              <use xlinkHref={ico36_backarrow} href={ico36_backarrow} />
            </svg>
            <svg className="cts__details__input__header__icon">
              <use xlinkHref={selectedDetailIcon} href={selectedDetailIcon} />
            </svg>
            <div className="cts__details__input__header__label">
              {selectedDetailLabel}
            </div>
          </div>
          {SelectedDetailComponent && (
            <SelectedDetailComponent
              className="cts__details__input__content"
              onUpdate={({ value }) =>
                updateDetail(selectedDetailIdentifier, value)
              }
              initialValue={get(
                state.additionalContent,
                selectedDetailIdentifier
              )}
              detail={selectedDetail.toJS()}
            />
          )}
        </div>
      );
    } else if (!selectedDetail && multiSelectType) {
      const getMultiSelectActionLabel = () => {
        switch (multiSelectType) {
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
      };

      detailsElement = (
        <div className="cts__details__input">
          <div className="cts__details__input__header">
            <svg
              className="cts__details__input__header__back clickable"
              onClick={() =>
                dispatch(
                  actionCreators.connections__setMultiSelectType({
                    connectionUri: connectionUri,
                    multiSelectType: undefined,
                  })
                )
              }
            >
              <use xlinkHref={ico36_backarrow} href={ico36_backarrow} />
            </svg>
            <svg className="cts__details__input__header__icon">
              <use xlinkHref={ico36_plus_circle} href={ico36_plus_circle} />
            </svg>
            <div className="cts__details__input__header__label hide-in-responsive">
              {getMultiSelectActionLabel()} ({selectedMessages.size} Messages)
            </div>
            <div className="cts__details__input__header__label show-in-responsive">
              {getMultiSelectActionLabel()}
            </div>
            <div
              className="cts__details__input__header__add"
              onClick={saveReferencedContent.bind(this)}
            >
              <svg className="cts__details__input__header__add__icon">
                <use xlinkHref={ico36_added_circle} href={ico36_added_circle} />
              </svg>
              <span className="cts__details__input__header__add__label hide-in-responsive">
                Save
              </span>
            </div>
            <div
              className="cts__details__input__header__discard"
              onClick={removeReferencedContent.bind(this)}
            >
              <svg className="cts__details__input__header__discard__icon">
                <use xlinkHref={ico36_close_circle} href={ico36_close_circle} />
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

    detailDrawerElement = <div className="cts__details">{detailsElement}</div>;
  }

  const detailDrawerToggleElement = (
    <button
      className="cts__add"
      disabled={!allowDetails}
      onClick={toggleAdditionalContentDisplay.bind(this)}
    >
      <svg className="cts__add__icon">
        {showAddMessageContent ? (
          <use xlinkHref={ico36_close} href={ico36_close} />
        ) : (
          <use xlinkHref={ico36_plus} href={ico36_plus} />
        )}
      </svg>
    </button>
  );

  let helpTextElement;

  if (helpText) {
    helpTextElement = <div className="cts__helptext">{helpText}</div>;
  }

  let addedDetailsElement;
  if (hasAdditionalContent() || hasReferencedContent()) {
    const referencedContentArrayElements =
      getReferencedContentKeysArray() &&
      getReferencedContentKeysArray().length > 0 &&
      getReferencedContentKeysArray().map((ref, index) => {
        const referencedMessages = get(state.referencedContent, ref);
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
              onClick={() => activateMultiSelect(ref)}
            >
              <use xlinkHref={ico36_plus} href={ico36_plus} />
            </svg>
            <span
              className="cts__additionalcontent__list__item__label clickable"
              onClick={() => activateMultiSelect(ref)}
            >
              {humanReadableReferenceString}
            </span>
            <svg
              className="cts__additionalcontent__list__item__discard clickable"
              onClick={() => removeReferencedContent(ref)}
            >
              <use xlinkHref={ico36_close} href={ico36_close} />
            </svg>
          </div>
        );
      });

    const additionalContentKeysArrayElements =
      getAdditionalContentKeysArray() &&
      getAdditionalContentKeysArray().length > 0 &&
      getAdditionalContentKeysArray().map((key, index) => {
        const usedDetailImm = get(allMessageDetailsImm, key);
        const usedDetail = usedDetailImm && usedDetailImm.toJS();

        const humanReadableDetail =
          usedDetail &&
          usedDetail.generateHumanReadable({
            value: get(state.additionalContent, key),
            includeLabel: true,
          });

        return (
          <div
            key={key + "-" + index}
            className="cts__additionalcontent__list__item"
          >
            <svg
              className="cts__additionalcontent__list__item__icon clickable"
              onClick={() =>
                dispatch(
                  actionCreators.view__selectAddMessageContent({
                    selectedDetail: get(usedDetailImm, "identifier"),
                  })
                )
              }
            >
              <use
                xlinkHref={get(usedDetailImm, "icon")}
                href={get(usedDetailImm, "icon")}
              />
            </svg>
            <span
              className="cts__additionalcontent__list__item__label clickable"
              onClick={() =>
                dispatch(
                  actionCreators.view__selectAddMessageContent({
                    selectedDetail: get(usedDetailImm, "identifier"),
                  })
                )
              }
            >
              {humanReadableDetail}
            </span>
            <svg
              className="cts__additionalcontent__list__item__discard clickable"
              onClick={() => updateDetail(key, undefined, true)}
            >
              <use xlinkHref={ico36_close} href={ico36_close} />
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
    <chat-textfield class={className || ""}>
      {detailDrawerElement}
      {detailDrawerToggleElement}

      <TextareaAutosize
        className={"cts__text won-txt " + (isCode && " won-txt--code ")}
        tabIndex="0"
        placeholder={placeholder}
        maxRows={4}
        onKeyDown={keydown.bind(this)}
        onChange={event =>
          setState({
            ...state,
            message: event.target.value,
          })
        }
        value={state.message}
      />
      <div className="cts__submitbutton">
        <ElmReact
          src={Elm.PublishButton}
          flags={{
            buttonEnabled: valid(),
            showPersonas: !!showPersonas,
            personas: personas,
            label: submitButtonLabel,
          }}
          onPublish={submit.bind(this)}
        />
      </div>

      {addedDetailsElement}
      {helpTextElement}
    </chat-textfield>
  );
}
ChatTextfield.propTypes = {
  connection: PropTypes.object,
  className: PropTypes.string,
  placeholder: PropTypes.string,
  helpText: PropTypes.string,
  isCode: PropTypes.bool,
  allowDetails: PropTypes.bool,
  allowEmptySubmit: PropTypes.bool,
  showPersonas: PropTypes.bool,
  submitButtonLabel: PropTypes.string,
  onSubmit: PropTypes.func,
};
