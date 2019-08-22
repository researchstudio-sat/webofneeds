import React from "react";
import { Map, TileLayer } from "react-leaflet";
import "~/style/_locationpicker.scss";
import PropTypes from "prop-types";
import WonTitlePicker from "./title-picker";

import "leaflet/dist/leaflet.css";
export default class WonLocationPicker extends React.Component {
  constructor(props) {
    super(props);
    this.state = {};
  }

  render() {
    return (
      <won-location-picker>
        <WonTitlePicker
          className={"lp__searchbox"}
          initial-value=""
          onUpdate={this.doneTyping.bind(this)}
          detail={{ placeholder: this.props.detail.placeholder }}
        />
        <Map className="lp__mapmount" zoom={13} center={[48.210033, 16.363449]}>
          <TileLayer
            attribution="&amp;copy <a href=&quot;http://osm.org/copyright&quot;>OpenStreetMap</a> contributors"
            url="https://www.matchat.org/tile/{z}/{x}/{y}.png"
          />
          {/*{locationMarkers}*/}
          {/*{currentLocationMarker}*/}
        </Map>
      </won-location-picker>
    );
  }

  doneTyping({ value }) {
    const text = value;

    console.debug("doneTyping: ", value, "text:", text);
    /*this.$scope.$apply(() => {
      this.resetLocation();
    });

    if (!text) {
      this.$scope.$apply(() => {
        this.resetSearchResults();
      });
    } else {
      // TODO: sort results by distance/relevance/???
      // TODO: limit amount of shown results
      searchNominatim(text).then(searchResults => {
        const parsedResults = scrubSearchResults(searchResults, text);
        this.$scope.$apply(() => {
          this.searchResults = parsedResults;
          //this.lastSearchedFor = { name: text };
          this.lastSearchedFor = text;
        });
        this.placeMarkers(Object.values(parsedResults));
      });
    }*/
  }
}
WonLocationPicker.propTypes = {
  initialValue: PropTypes.string,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
