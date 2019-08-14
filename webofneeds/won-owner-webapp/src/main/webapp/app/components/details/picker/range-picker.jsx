import React from "react";

import "~/style/_rangepicker.scss";
import PropTypes from "prop-types";

export default class WonRangePicker extends React.Component {
  constructor(props) {
    super(props);
    this.state = {};
  }

  render() {
    return <won-range-picker>TODO: IMPL</won-range-picker>;
  }
}
WonRangePicker.propTypes = {
  initialValue: PropTypes.string,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
