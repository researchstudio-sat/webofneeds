import React from "react";

import "~/style/_numberpicker.scss";
import PropTypes from "prop-types";

export default class WonNumberPicker extends React.Component {
  constructor(props) {
    super(props);
    this.state = {};
  }

  render() {
    return <won-number-picker>TODO: IMPL</won-number-picker>;
  }
}
WonNumberPicker.propTypes = {
  initialValue: PropTypes.string,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
