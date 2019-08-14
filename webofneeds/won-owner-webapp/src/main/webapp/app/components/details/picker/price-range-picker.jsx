import React from "react";

import "~/style/_pricerangepicker.scss";
import PropTypes from "prop-types";

export default class WonPriceRangePicker extends React.Component {
  constructor(props) {
    super(props);
    this.state = {};
  }

  render() {
    return <won-price-range-picker>TODO: IMPL</won-price-range-picker>;
  }
}
WonPriceRangePicker.propTypes = {
  initialValue: PropTypes.string,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
