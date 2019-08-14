import React from "react";

import PropTypes from "prop-types";

export default class WonSelectPicker extends React.Component {
  constructor(props) {
    super(props);
    this.state = {};
  }

  render() {
    return <won-select-picker>TODO: IMPL</won-select-picker>;
  }
}
WonSelectPicker.propTypes = {
  initialValue: PropTypes.string,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
