import React from "react";
import { useSelector } from "react-redux";
import { get, getIn } from "../utils.js";

import "~/style/_modal-dialog.scss";

export default function WonModalDialog() {
  const modalDialog = useSelector(state =>
    getIn(state, ["view", "modalDialog"])
  );
  const modalDialogCaption = get(modalDialog, "caption");
  const modalDialogText = get(modalDialog, "text");
  const modalDialogButtons = get(modalDialog, "buttons") || [];
  const showTerms = get(modalDialog, "showTerms");

  return (
    <won-modal-dialog>
      <div className="md__dialog">
        {showTerms ? (
          <React.Fragment>
            <div className="md__dialog__header">
              <span className="md__dialog__header__caption">
                Important Note
              </span>
            </div>
            <div className="md__dialog__content">
              <span className="md__dialog__content__text">
                This action requires an account. If you want to proceed, we will
                create an anonymous account for you.
                <br />
                <br />
                {"By clicking 'Yes', you accept the "}
                <a
                  target="_blank"
                  rel="noopener noreferrer"
                  href="#!/about?aboutSection=aboutTermsOfService"
                >
                  Terms Of Service(ToS)
                </a>
                {
                  " and anonymous account will be created. Clicking 'No' will just cancel the action."
                }
              </span>
            </div>
          </React.Fragment>
        ) : (
          <React.Fragment>
            <div className="md__dialog__header">
              <span className="md__dialog__header__caption">
                {modalDialogCaption}
              </span>
            </div>
            <div className="md__dialog__content">
              <span className="md__dialog__content__text">
                {modalDialogText}
              </span>
            </div>
          </React.Fragment>
        )}
        <div
          className={
            "md__dialog__footer " +
            (modalDialogButtons.size > 2
              ? " md__dialog__footer--row"
              : " md__dialog__footer--column")
          }
        >
          {modalDialogButtons.map((button, index) => (
            <button
              key={get(button, "caption") + "-" + index}
              className={"won-button--filled secondary"}
              onClick={get(button, "callback")}
            >
              <span>{get(button, "caption")}</span>
            </button>
          ))}
        </div>
      </div>
    </won-modal-dialog>
  );
}
