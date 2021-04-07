import React, { useState } from "react";
import * as generalSelectors from "../redux/selectors/general-selectors";
import { actionCreators } from "~/app/actions/actions";
import { useDispatch, useSelector } from "react-redux";

import PropTypes from "prop-types";

import "~/style/_atom-notification.scss";
import ico16_bell from "~/images/won-icons/ico16_bell.svg";
import { get } from "../utils.js";

export default function WonAtomNotification({ atomUri, className }) {
  const dispatch = useDispatch();
  const accountState = useSelector(generalSelectors.getAccountState);
  const userSettings = accountState && get(accountState, "userSettings");
  const settings =
    userSettings &&
    userSettings.filter(element => {
      return get(element, "atomUri") === atomUri;
    });
  let setting = settings && settings.size > 0 ? settings.get(0) : undefined;
  const isActive =
    setting &&
    (get(setting, "notifyMatches") ||
      get(setting, "notifyRequests") ||
      get(setting, "notifyConversations"));

  const [atomNoticication, setAtomNoticication] = useState(isActive);

  function toggleAtomNotification() {
    const updatedSetting = {
      username: setting && get(setting, "username"),
      email: get(accountState, "email"),
      atomUri: atomUri,
      notifyMatches: !atomNoticication,
      notifyRequests: !atomNoticication,
      notifyConversations: !atomNoticication,
    };
    dispatch(actionCreators.account__updateAtomUserSettings(updatedSetting));
    setAtomNoticication(!atomNoticication);
  }

  return (
    <won-atom-notification class={className ? className : ""}>
      <svg
        className={
          atomNoticication
            ? "an__icon__small--selected clickable"
            : "an__icon__small clickable"
        }
        onClick={() => toggleAtomNotification()}
      >
        <use xlinkHref={ico16_bell} href={ico16_bell} />
      </svg>
    </won-atom-notification>
  );
}

WonAtomNotification.propTypes = {
  atomUri: PropTypes.string.isRequired,
  className: PropTypes.string,
};
