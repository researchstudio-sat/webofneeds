import React from "react";
import PropTypes from "prop-types";
import { useSelector, useDispatch } from "react-redux";
import { get } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as atomUtils from "../redux/utils/atom-utils.js";

import "~/style/_atom-footer.scss";

const FooterType = {
  INACTIVE: 1,
  INACTIVE_OWNED: 2,
  UNKNOWN: 3,
};

export default function WonAtomFooter({ atom, className }) {
  const dispatch = useDispatch();
  const atomUri = get(atom, "uri");

  const isOwned = useSelector(generalSelectors.isAtomOwned(atomUri));

  let footerType = FooterType.UNKNOWN;

  if (atomUtils.isInactive(atom)) {
    footerType = isOwned ? FooterType.INACTIVE_OWNED : FooterType.INACTIVE;
  }

  let footerElement;

  switch (footerType) {
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
            This Atom is inactive. Others will not be able to interact with it.
          </div>
          <button
            className="won-publish-button red won-button--filled"
            onClick={() => dispatch(actionCreators.atoms__reopen(atomUri))}
          >
            Reopen
          </button>
        </React.Fragment>
      );
      break;
    case FooterType.UNKNOWN:
    default:
      footerElement = undefined;
      break;
  }

  return (
    <won-atom-footer class={className ? className : ""}>
      {footerElement}
    </won-atom-footer>
  );
}
WonAtomFooter.propTypes = {
  atom: PropTypes.object,
  className: PropTypes.string,
};
