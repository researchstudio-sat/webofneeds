import React from "react";

import "~/style/_reviewpicker.scss";
import PropTypes from "prop-types";

export default class WonReviewPicker extends React.Component {
  constructor(props) {
    super(props);
    this.state = {};
  }

  render() {
    return <won-review-picker>TODO: IMPL</won-review-picker>;
  }
}
WonReviewPicker.propTypes = {
  initialValue: PropTypes.string,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
