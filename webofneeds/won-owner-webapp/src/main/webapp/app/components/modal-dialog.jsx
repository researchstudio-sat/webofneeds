import React, { useState } from "react";
import { useSelector } from "react-redux";
import { get, getIn } from "../utils.js";

import "~/style/_modal-dialog.scss";
import WonLabelledHr from "~/app/components/labelled-hr";
// import { actionCreators } from "~/app/actions/actions";

export default function WonModalDialog() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [rememberMe, setRememberMe] = useState(false);

  const modalDialog = useSelector(state =>
    getIn(state, ["view", "modalDialog"])
  );
  const modalDialogCaption = get(modalDialog, "caption");
  const modalDialogText = get(modalDialog, "text");
  const modalDialogButtons = get(modalDialog, "buttons") || [];
  const showLoggedOutDialog = get(modalDialog, "showLoggedOutDialog");

  function formKeyUp(event) {
    /*
    if (loginError) {
      // dispatch(actionCreators.view__clearLoginError());
    }*/

    if (event.keyCode === 13) {
      console.debug("Login submit");
      //TODO: IMPLEMENT ACTUAL LOGIN WITH ACCEPTCALLBACK AFTERWARDS
      // dispatch(
      //   actionCreators.account__login({
      //     email: email,
      //     password: password,
      //     rememberMe: rememberMe,
      //   })
      // );
    }
  }

  return (
    <won-modal-dialog>
      <div className="md__dialog">
        {showLoggedOutDialog ? (
          <React.Fragment>
            <div className="md__dialog__header">
              <span className="md__dialog__header__caption">
                You are currently not logged in
              </span>
            </div>
            <div className="md__dialog__content">
              <won-login-form>
                <form
                  onSubmit={e => {
                    e.preventDefault();
                    console.debug("Login submit");
                    /*dispatch(
                    actionCreators.account__login({
                      email: email,
                      password: password,
                      rememberMe: rememberMe,
                    })
                  );*/
                  }}
                  id="loginForm"
                  className="loginForm"
                >
                  <input
                    id="loginEmail"
                    placeholder="Email address"
                    value={email}
                    type="email"
                    required
                    autoFocus
                    onKeyUp={formKeyUp}
                    onChange={event => setEmail(event.target.value)}
                  />
                  <input
                    id="loginPassword"
                    placeholder="Password"
                    value={password}
                    type="password"
                    required
                    onKeyUp={formKeyUp}
                    onChange={event => setPassword(event.target.value)}
                  />
                  {email.length > 0 &&
                    password.length > 0 && (
                      <label>
                        <input
                          id="remember-me"
                          value={rememberMe}
                          onChange={event =>
                            setRememberMe(event.target.checked)
                          }
                          type="checkbox"
                        />
                        Remember me
                      </label>
                    )}
                  <button
                    className="won-button--filled secondary"
                    disabled={password === "" || email === ""}
                  >
                    Sign In
                  </button>
                </form>
              </won-login-form>
              <WonLabelledHr label="or" />
              <span className="md__dialog__content__text">
                Proceed without an Account, we will create an anonymous account
                for you.
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
            <div className="md__dialog__footer md__dialog__footer--column">
              <button
                className={"won-button--filled secondary"}
                onClick={get(modalDialog, "acceptCallback")}
              >
                <span>Yes, I accept ToS</span>
              </button>
              <button
                className={"won-button--filled secondary"}
                onClick={get(modalDialog, "cancelCallback")}
              >
                <span>No, cancel</span>
              </button>
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
          </React.Fragment>
        )}
      </div>
    </won-modal-dialog>
  );
}
