import React from "react";

import "~/style/_personpicker.scss";
import PropTypes from "prop-types";

export default class WonPersonPicker extends React.Component {
  constructor(props) {
    super(props);
    this.state = {};
  }

  render() {
    return <won-person-picker>TODO: IMPL</won-person-picker>;
  }
}
WonPersonPicker.propTypes = {
  initialValue: PropTypes.string,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
