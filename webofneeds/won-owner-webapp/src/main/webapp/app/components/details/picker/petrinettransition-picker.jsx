import React from "react";

import "~/style/_petrinettransitionpicker.scss";
import PropTypes from "prop-types";

export default class WonPetrinetTransitionPicker extends React.Component {
  constructor(props) {
    super(props);
    this.state = {};
  }

  render() {
    return (
      <won-petrinettransition-picker>TODO: IMPL</won-petrinettransition-picker>
    );
  }
}
WonPetrinetTransitionPicker.propTypes = {
  initialValue: PropTypes.string,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
