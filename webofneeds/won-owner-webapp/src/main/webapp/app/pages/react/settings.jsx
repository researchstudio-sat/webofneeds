import React, { useState } from "react";
import won from "../../won-es6";
import { useSelector } from "react-redux";
import { useHistory } from "react-router-dom";
import * as ownerApi from "../../api/owner-api.js";
import * as accountUtils from "../../redux/utils/account-utils.js";
import * as viewSelectors from "../../redux/selectors/view-selectors.js";
import * as generalSelectors from "../../redux/selectors/general-selectors.js";
import WonModalDialog from "../../components/modal-dialog.jsx";
import WonTopnav from "../../components/topnav.jsx";
import WonMenu from "../../components/menu.jsx";
import WonToasts from "../../components/toasts.jsx";
import WonSlideIn from "../../components/slide-in.jsx";
import WonFooter from "../../components/footer.jsx";

import ico16_indicator_warning from "~/images/won-icons/ico16_indicator_warning.svg";
import "~/style/_settings.scss";

const MINPW_LENGTH = 6;

const SETTING = {
  ACCOUNT: 0,
  EXPORT: 1,
};

export default function PageSettings() {
  const history = useHistory();
  const accountState = useSelector(generalSelectors.getAccountState);
  const isLoggedIn = accountUtils.isLoggedIn(accountState);
  const isEmailVerified = accountUtils.isEmailVerified(accountState);
  const showModalDialog = useSelector(viewSelectors.showModalDialog);
  const showSlideIns = useSelector(viewSelectors.showSlideIns(history));

  const [visibleSetting, setVisibleSetting] = useState(SETTING.ACCOUNT);
  const [dataEncryptionPassword, setDataEncryptionPassword] = useState();

  const [currentPassword, setCurrentPassword] = useState();
  const [newPassword, setNewPassword] = useState();
  const [newPasswordAgain, setNewPasswordAgain] = useState();
  const [passwordChanged, setPasswordChanged] = useState(false);
  const [changePasswordError, setChangePasswordError] = useState();

  const [exportStarted, setExportStarted] = useState(false);

  const generateMenuItem = (label, settingId) => {
    const className = "settings__left__item";

    const classes = [className];
    settingId === visibleSetting && classes.push(className + "--selected");

    return (
      <div
        className={classes.join(" ")}
        onClick={() => setVisibleSetting(settingId)}
      >
        <span className="settings__left__item__label">{label}</span>
      </div>
    );
  };

  const exportData = () => {
    ownerApi.exportAccount(dataEncryptionPassword);
    setExportStarted(true);
  };

  const isChangePasswordValid = () => {
    if (!currentPassword || currentPassword.length === 0) {
      return false;
    }
    if (!newPassword || newPassword.length < MINPW_LENGTH) {
      return false;
    }
    if (!newPasswordAgain || newPasswordAgain !== newPassword) {
      return false;
    }

    return true;
  };

  const changePassword = () => {
    ownerApi
      .changePassword({
        email: accountUtils.getEmail(accountState),
        newPassword: newPassword,
        oldPassword: currentPassword,
      })
      .then(() => {
        setChangePasswordError();
        setPasswordChanged(true);
      })
      .catch(error => {
        const response = error && error.response;
        if (response) {
          response.json().then(jsonResponse => {
            if (jsonResponse.code) {
              switch (jsonResponse.code) {
                case won.RESPONSECODE.PASSWORDCHANGE_USER_NOT_FOUND:
                  setChangePasswordError(
                    "Password change failed, User was not found"
                  );
                  break;
                case won.RESPONSECODE.PASSWORDCHANGE_BAD_PASSWORD:
                  setChangePasswordError(
                    "Password change failed, New Password is not valid"
                  );
                  break;
                case won.RESPONSECODE.PASSWORDCHANGE_KEYSTORE_PROBLEM:
                  setChangePasswordError(
                    "Password change failed, there was a Problem with the Keystore"
                  );
                  break;
                case won.RESPONSECODE.PASSWORDCHANGE_WRONG_OLD_PASSWORD:
                  setChangePasswordError(
                    "Password change failed, Current Password was not correct"
                  );
                  break;
                default:
                  setChangePasswordError(
                    `Password change failed, Unknown Response: ${JSON.stringify(
                      jsonResponse
                    )}`
                  );
                  break;
              }
            }
          });
        }
      });
  };

  let settingsContent;

  switch (visibleSetting) {
    case SETTING.ACCOUNT:
      settingsContent = (
        <React.Fragment>
          <div className="wsc__header">
            <span className="wsc__header__label">Account Settings</span>
          </div>
          <div className="wsc__content">
            <div className="wsc__content__description">
              Here you can change your account data.
            </div>
            <div className="wsc__content__infomsg">
              Change your password here.
            </div>
            {passwordChanged ? (
              <div className="wsc__content__infomsg--primary">
                Password Changed
              </div>
            ) : (
              <React.Fragment>
                <div className="wsc__content__passwordform">
                  <input
                    name="currentPassword"
                    placeholder="Current"
                    required
                    type="password"
                    onChange={event => setCurrentPassword(event.target.value)}
                    value={currentPassword}
                  />
                  <input
                    name="newPassword"
                    placeholder="New"
                    required
                    type="password"
                    onChange={event => setNewPassword(event.target.value)}
                    value={newPassword}
                  />

                  {newPassword &&
                    newPassword.length > 0 &&
                    newPassword.length < MINPW_LENGTH && (
                      <div className="wsc__content__passwordform__errormsg">
                        <svg className="wsc__content__passwordform__errormsg__icon">
                          <use
                            xlinkHref={ico16_indicator_warning}
                            href={ico16_indicator_warning}
                          />
                        </svg>
                        <span className="wsc__content__passwordform__errormsg__label">{`New Password too short, must be at least ${MINPW_LENGTH} Characters`}</span>
                      </div>
                    )}

                  <input
                    name="password_repeat"
                    placeholder="Repeat New"
                    required
                    type="password"
                    onChange={event => setNewPasswordAgain(event.target.value)}
                    value={newPasswordAgain}
                  />

                  {newPasswordAgain &&
                    newPasswordAgain.length > 0 &&
                    newPassword !== newPasswordAgain && (
                      <div className="wsc__content__passwordform__errormsg">
                        <svg className="wsc__content__passwordform__errormsg__icon">
                          <use
                            xlinkHref={ico16_indicator_warning}
                            href={ico16_indicator_warning}
                          />
                        </svg>
                        <span className="wsc__content__passwordform__errormsg__label">
                          New Password is not equal
                        </span>
                      </div>
                    )}
                  <button
                    className="won-button--filled secondary"
                    disabled={!isChangePasswordValid()}
                    onClick={changePassword}
                  >
                    <span>Save Changes</span>
                  </button>
                </div>
                {changePasswordError && (
                  <div className="wsc__content__infomsg--primary">
                    {changePasswordError}
                  </div>
                )}
              </React.Fragment>
            )}
          </div>
        </React.Fragment>
      );
      break;
    case SETTING.EXPORT:
      settingsContent = (
        <React.Fragment>
          <div className="wsc__header">
            <span className="wsc__header__label">Account data export</span>
          </div>
          <div className="wsc__content">
            <div className="wsc__content__description">
              Here you can export the data associated with your account,
              including posts, connections and the keys that prove you are the
              owner of your posts.
            </div>
            {exportStarted ? (
              <div className="wsc__content__infomsg--primary">
                Export Started. You will get an e-mail soon
              </div>
            ) : !isEmailVerified ? (
              <div className="wsc__content__infomsg--primary">
                You need an account with a verified email address to export your
                data
              </div>
            ) : (
              <React.Fragment>
                <div className="wsc__content__infomsg">
                  All your account data will be sent to your email address.
                </div>
                <div className="wsc__content__infomsg">
                  You can type in any password you like below and your keys will
                  be encrypted using that password.
                </div>
                <div className="wsc__content__exportform">
                  <input
                    name="encryption_password"
                    placeholder="Data encryption password"
                    required
                    type="password"
                    onChange={event =>
                      setDataEncryptionPassword(event.target.value)
                    }
                    value={dataEncryptionPassword}
                  />
                  <button
                    className="won-button--filled secondary"
                    disabled={
                      !dataEncryptionPassword ||
                      dataEncryptionPassword.length === 0
                    }
                    onClick={exportData}
                  >
                    <span>Export</span>
                  </button>
                </div>
              </React.Fragment>
            )}
          </div>
        </React.Fragment>
      );
      break;
  }

  return (
    <section className={!isLoggedIn ? "won-signed-out" : ""}>
      {showModalDialog && <WonModalDialog />}
      <WonTopnav pageTitle="Settings" />
      {isLoggedIn && <WonMenu />}
      <WonToasts />
      {showSlideIns && <WonSlideIn />}
      <aside className="settings__left">
        {generateMenuItem("Account", SETTING.ACCOUNT)}
        {generateMenuItem("Export", SETTING.EXPORT)}
      </aside>
      <main className="settings__right">
        <won-settings-content>{settingsContent}</won-settings-content>
      </main>
      <WonFooter />
    </section>
  );
}
