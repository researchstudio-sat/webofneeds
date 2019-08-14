import React from "react";

import "~/style/_descriptionpicker.scss";
import PropTypes from "prop-types";

export default class WonDescriptionPicker extends React.Component {
  constructor(props) {
    super(props);
    this.state = {};
  }

  render() {
    return <won-description-picker>TODO: IMPL</won-description-picker>;
  }
}
WonDescriptionPicker.propTypes = {
  initialValue: PropTypes.string,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
