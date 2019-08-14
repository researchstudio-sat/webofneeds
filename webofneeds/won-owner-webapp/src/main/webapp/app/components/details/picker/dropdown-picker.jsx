import React from "react";

import "~/style/_dropdownpicker.scss";
import PropTypes from "prop-types";

export default class WonDropdownPicker extends React.Component {
  constructor(props) {
    super(props);
    this.state = {};
  }

  render() {
    return <won-dropdown-picker>TODO: IMPL</won-dropdown-picker>;
  }
}
WonDropdownPicker.propTypes = {
  initialValue: PropTypes.string,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
