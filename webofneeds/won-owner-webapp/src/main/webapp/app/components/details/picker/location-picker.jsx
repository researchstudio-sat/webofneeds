import React from "react";

import "~/style/_locationpicker.scss";
import PropTypes from "prop-types";

export default class WonLocationPicker extends React.Component {
  constructor(props) {
    super(props);
    this.state = {};
  }

  render() {
    return <won-location-picker>TODO: IMPL</won-location-picker>;
  }
}
WonLocationPicker.propTypes = {
  initialValue: PropTypes.string,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
