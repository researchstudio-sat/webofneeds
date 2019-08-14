import React from "react";

import "~/style/_petrinetpicker.scss";
import PropTypes from "prop-types";

export default class WonPetrinetPicker extends React.Component {
  constructor(props) {
    super(props);
    this.state = {};
  }

  render() {
    return <won-petrinet-picker>TODO: IMPL</won-petrinet-picker>;
  }
}
WonPetrinetPicker.propTypes = {
  initialValue: PropTypes.string,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
