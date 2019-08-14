import React from "react";

import "~/style/_filepicker.scss";
import PropTypes from "prop-types";

export default class WonFilePicker extends React.Component {
  constructor(props) {
    super(props);
    this.state = {};
  }

  render() {
    return <won-file-picker>TODO: IMPL</won-file-picker>;
  }
}
WonFilePicker.propTypes = {
  initialValue: PropTypes.string,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
