import React from "react";

import "~/style/_travelactionpicker.scss";
import PropTypes from "prop-types";

import "leaflet/dist/leaflet.css";

export default class WonTravelActionPicker extends React.Component {
  constructor(props) {
    super(props);
    this.state = {};
  }

  render() {
    return <won-travel-action-picker>TODO: IMPL</won-travel-action-picker>;
  }
}
WonTravelActionPicker.propTypes = {
  initialValue: PropTypes.object,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
