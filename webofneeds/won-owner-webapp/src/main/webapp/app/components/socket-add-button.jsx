import React from "react";
import PropTypes from "prop-types";

import * as wonLabelUtils from "../won-label-utils";

import "~/style/_socket-add-button.scss";
import ico36_plus from "~/images/won-icons/ico36_plus.svg";

export default function WonSocketAddButton({
  senderReactions,
  /*targetSocketType,*/
  isAtomOwned,
  onClick,
}) {
  function generateReactionLabel() {
    return isAtomOwned
      ? `Add ${
          senderReactions
            ? wonLabelUtils.getSocketItemLabels(senderReactions.keys())
            : "Chat"
        }`
      : `Connect ${
          senderReactions
            ? wonLabelUtils.getSocketItemLabels(senderReactions.keys())
            : "Chat"
        }`;
  }

  return (
    <won-socket-add-button class="clickable" onClick={onClick}>
      <svg className="socketadd__icon" title="Create a new post">
        <use xlinkHref={ico36_plus} href={ico36_plus} />
      </svg>
      <span className="socketadd__label">{generateReactionLabel()}</span>
    </won-socket-add-button>
  );
}

WonSocketAddButton.propTypes = {
  senderReactions: PropTypes.object.isRequired,
  targetSocketType: PropTypes.string,
  isAtomOwned: PropTypes.bool.isRequired,
  onClick: PropTypes.func.isRequired,
};
