import React from "react";

import "~/style/_datetimepicker.scss";
import PropTypes from "prop-types";

export default class WonDatetimePicker extends React.Component {
  constructor(props) {
    super(props);
    this.state = {};
  }

  render() {
    return <won-datetime-picker>TODO: IMPL</won-datetime-picker>;
  }
}
WonDatetimePicker.propTypes = {
  initialValue: PropTypes.string,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
