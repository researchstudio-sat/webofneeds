/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import PropTypes from "prop-types";
import { useSelector } from "react-redux";
import { get, getIn } from "../../utils.js";

import "~/style/_atom-content-message.scss";
import WonAtomMenu from "../atom-menu.jsx";
import WonAtomContent from "../atom-content.jsx";

export default function WonAtomContentMessage({ atom }) {
  const atomLoading = useSelector(
    state =>
      !atom || getIn(state, ["process", "atoms", get(atom, "uri"), "loading"])
  );

  return (
    <won-atom-content-message class={atomLoading ? "won-is-loading" : ""}>
      <div className="won-cm__center">
        <div className="won-cm__center__bubble">
          <WonAtomMenu atom={atom} />
          <WonAtomContent atom={atom} />
        </div>
      </div>
    </won-atom-content-message>
  );
}

WonAtomContentMessage.propTypes = {
  atom: PropTypes.object,
  atomLoading: PropTypes.bool,
};
