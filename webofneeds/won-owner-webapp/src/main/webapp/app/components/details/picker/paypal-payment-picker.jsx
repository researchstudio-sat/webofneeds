import React from "react";

import "~/style/_paypalpaymentpicker.scss";
import PropTypes from "prop-types";

export default class WonPaypalPaymentPicker extends React.Component {
  constructor(props) {
    super(props);
    this.state = {};
  }

  render() {
    return <won-paypal-payment-picker>TODO: IMPL</won-paypal-payment-picker>;
  }
}
WonPaypalPaymentPicker.propTypes = {
  initialValue: PropTypes.string,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
