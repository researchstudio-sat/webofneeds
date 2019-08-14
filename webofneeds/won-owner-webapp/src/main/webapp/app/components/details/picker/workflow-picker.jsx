import React from "react";

import "~/style/_workflowpicker.scss";
import PropTypes from "prop-types";

export default class WonWorkflowPicker extends React.Component {
  constructor(props) {
    super(props);
    this.state = {};
  }

  render() {
    return <won-workflow-picker>TODO: IMPL</won-workflow-picker>;
  }
}
WonWorkflowPicker.propTypes = {
  initialValue: PropTypes.string,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
