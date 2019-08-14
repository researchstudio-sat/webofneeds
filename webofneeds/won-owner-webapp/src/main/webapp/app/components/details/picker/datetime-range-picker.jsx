import React from "react";

import "~/style/_datetimepicker.scss";
import PropTypes from "prop-types";

export default class WonDatetimeRangePicker extends React.Component {
  constructor(props) {
    super(props);
    this.state = {};
  }

  render() {
    return <won-datetime-range-picker>TODO: IMPL</won-datetime-range-picker>;
  }
}
WonDatetimeRangePicker.propTypes = {
  initialValue: PropTypes.string,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
