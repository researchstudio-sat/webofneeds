/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import PropTypes from "prop-types";
import { connect } from "react-redux";
import { getIn } from "../../utils.js";

import "~/style/_atom-content-message.scss";
import WonAtomMenu from "../atom-menu.jsx";
import WonAtomContent from "../atom-content.jsx";

const mapStateToProps = (state, ownProps) => {
  const atom = ownProps.atomUri && state.getIn(["atoms", ownProps.atomUri]);

  return {
    atomUri: ownProps.atomUri,
    atom,
    atomLoading:
      !atom || getIn(state, ["process", "atoms", atom.get("uri"), "loading"]),
  };
};

const AtomContentMessage = props => {
  return (
    <won-atom-content-message class={props.atomLoading ? "won-is-loading" : ""}>
      <div className="won-cm__center">
        <div className="won-cm__center__bubble">
          <WonAtomMenu atom={props.atom} />
          <WonAtomContent atom={props.atom} />
        </div>
      </div>
    </won-atom-content-message>
  );
};

AtomContentMessage.propTypes = {
  atomUri: PropTypes.string.isRequired,
  atom: PropTypes.object,
  atomLoading: PropTypes.bool,
};

export default connect(mapStateToProps)(AtomContentMessage);
