import React from "react";
import PropTypes from "prop-types";
import { useSelector } from "react-redux";
import { get } from "../utils.js";
import WonAtomHeaderBig from "./atom-header-big.jsx";
import WonAtomMenu from "./atom-menu.jsx";
import WonAtomFooter from "./atom-footer.jsx";
import WonAtomContent from "./atom-content.jsx";
import * as generalSelectors from "../redux/selectors/general-selectors";
import * as atomUtils from "../redux/utils/atom-utils";
import * as processUtils from "../redux/utils/process-utils";

import "~/style/_atom-info.scss";

export default function WonAtomInfo({ atom, className, defaultTab }) {
  const atomUri = get(atom, "uri");
  const isOwned = useSelector(generalSelectors.isAtomOwned(atomUri));
  const processState = useSelector(generalSelectors.getProcessState);
  const atomLoading =
    !atom || processUtils.isAtomLoading(processState, atomUri);

  const showFooter =
    !atomLoading &&
    (atomUtils.isInactive(atom) ||
      (isOwned && atomUtils.hasEnabledUseCases(atom)) ||
      (!isOwned && atomUtils.hasReactionUseCases(atom)) ||
      (!isOwned &&
        (atomUtils.hasGroupSocket(atom) || atomUtils.hasChatSocket(atom))));

  return (
    <won-atom-info
      class={
        (className ? className : "") + (atomLoading ? " won-is-loading " : "")
      }
    >
      <WonAtomHeaderBig atom={atom} />
      <WonAtomMenu atom={atom} defaultTab={defaultTab} />
      <WonAtomContent atom={atom} defaultTab={defaultTab} />
      {showFooter ? <WonAtomFooter atom={atom} /> : undefined}
    </won-atom-info>
  );
}
WonAtomInfo.propTypes = {
  atom: PropTypes.object,
  defaultTab: PropTypes.string,
  className: PropTypes.string,
};
