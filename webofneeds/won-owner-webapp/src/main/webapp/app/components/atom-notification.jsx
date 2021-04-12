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
  const atomSettings =
    accountState && accountState.getIn(["atomSettings", atomUri]);
  const isActive =
    atomSettings &&
    (get(atomSettings, "notifyMatches") ||
      get(atomSettings, "notifyRequests") ||
      get(atomSettings, "notifyConversations"));

  const [atomNotification, setAtomNotification] = useState(isActive);

  function toggleAtomNotification() {
    const updatedSetting = {
      username: atomSettings && get(atomSettings, "username"),
      email: get(accountState, "email"),
      atomUri: atomUri,
      notifyMatches: !atomNotification,
      notifyRequests: !atomNotification,
      notifyConversations: !atomNotification,
    };
    dispatch(actionCreators.account__updateAtomUserSettings(updatedSetting));
    setAtomNotification(!atomNotification);
  }

  return (
    <won-atom-notification class={className ? className : ""}>
      <svg
        className={
          atomNotification
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
