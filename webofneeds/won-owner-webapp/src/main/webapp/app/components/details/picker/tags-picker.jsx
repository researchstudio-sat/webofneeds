import React from "react";

import "~/style/_tagspicker.scss";
import PropTypes from "prop-types";

export default class WonTagsPicker extends React.Component {
  constructor(props) {
    super(props);
    this.state = {};
  }

  render() {
    return <won-tags-picker>TODO: IMPL</won-tags-picker>;
  }
}
WonTagsPicker.propTypes = {
  initialValue: PropTypes.string,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
