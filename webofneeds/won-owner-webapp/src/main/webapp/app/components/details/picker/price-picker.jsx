import React from "react";

import "~/style/_pricepicker.scss";
import PropTypes from "prop-types";

export default class WonPricePicker extends React.Component {
  constructor(props) {
    super(props);
    this.state = {};
  }

  render() {
    return <won-price-picker>TODO: IMPL</won-price-picker>;
  }
}
WonPricePicker.propTypes = {
  initialValue: PropTypes.string,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
