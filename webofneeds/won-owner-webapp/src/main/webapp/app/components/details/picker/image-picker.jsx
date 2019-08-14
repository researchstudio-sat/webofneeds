import React from "react";

import "~/style/_imagepicker.scss";
import PropTypes from "prop-types";

export default class WonImagePicker extends React.Component {
  constructor(props) {
    super(props);
    this.state = {};
  }

  render() {
    return <won-image-picker>TODO: IMPL</won-image-picker>;
  }
}
WonImagePicker.propTypes = {
  initialValue: PropTypes.string,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
