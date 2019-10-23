import React from "react";
import { Map, Marker, TileLayer } from "react-leaflet";
import "~/style/_travelactionpicker.scss";
import PropTypes from "prop-types";
import WonTitlePicker from "./title-picker.jsx";
import {
  nominatim2draftLocation,
  reverseSearchNominatim,
  scrubSearchResults,
  searchNominatim,
} from "../../../api/nominatim-api.js";
import L from "leaflet";

import "leaflet/dist/leaflet.css";

const locationIcon = L.divIcon({
  className: "wonLocationMarkerIcon",
  html:
    "<svg class='marker__icon'><use xlink:href='#ico36_detail_location' href='#ico36_detail_location' /></svg>",
});

//TODO: SELECT BOTH LOCATIONS BY CLICKING ON THE MAP
export default class WonTravelActionPicker extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      fromSearchResults: [],
      toSearchResults: [],
      lastSearchedFor: undefined,
      fromAddedLocation: props.initialValue && props.initialValue.fromLocation,

      previousFromLocation: undefined,
      previousToLocatin: undefined,
      currentLocation: undefined,
    };
    this.doneTypingFrom = this.doneTypingFrom.bind(this);
    this.doneTypingTo = this.doneTypingTo.bind(this);
    this.update = this.update.bind(this);
  }

  componentDidMount() {
    if ("geolocation" in navigator) {
      navigator.geolocation.getCurrentPosition(
        currentLocation => {
          const geoLat = currentLocation.coords.latitude;
          const geoLng = currentLocation.coords.longitude;
          const geoZoom = 13; // TODO: use `currentLocation.coords.accuracy` to control coarseness of query / zoom-level

          reverseSearchNominatim(geoLat, geoLng, geoZoom).then(searchResult => {
            const location = nominatim2draftLocation(searchResult);
            this.setState({
              currentLocation: location,
            });
          });
        },
        err => {
          //error handler
          if (err.code === 2) {
            alert("Position is unavailable!"); //TODO toaster
          }
        },
        {
          //options
          enableHighAccuracy: true,
          timeout: 5000,
          maximumAge: 0,
        }
      );
    }
  }

  render() {
    const showCurrentLocationInFromResult =
      !this.state.fromAddedLocation && !!this.state.currentLocation;
    const showPrevFromLocationResult =
      !this.state.fromAddedLocation && !!this.state.previousFromLocation;
    const showFromResultDropDown =
      this.state.fromSearchResults.length > 0 ||
      showPrevFromLocationResult ||
      showCurrentLocationInFromResult;

    const showCurrentLocationInToResult =
      !this.state.toAddedLocation && !!this.state.currentLocation;
    const showPrevToLocationResult =
      !this.state.toAddedLocation && !!this.state.previousToLocation;
    const showToResultDropDown =
      this.state.toSearchResults.length > 0 ||
      showPrevToLocationResult ||
      showCurrentLocationInToResult;

    const fromSearchResults = this.state.fromSearchResults.map(
      (result, index) => (
        <li
          className="rp__searchresult"
          key={result.name + "-" + index}
          onClick={() => this.selectFromLocation(result)}
        >
          <svg className="rp__searchresult__icon">
            <use
              xlinkHref="#ico16_indicator_location"
              href="#ico16_indicator_location"
            />
          </svg>
          <span className="rp__searchresult__text">{result.name}</span>
        </li>
      )
    );

    const toSearchResults = this.state.toSearchResults.map((result, index) => (
      <li
        className="rp__searchresult"
        key={result.name + "-" + index}
        onClick={() => this.selectToLocation(result)}
      >
        <svg className="rp__searchresult__icon">
          <use
            xlinkHref="#ico16_indicator_location"
            href="#ico16_indicator_location"
          />
        </svg>
        <span className="rp__searchresult__text">{result.name}</span>
      </li>
    ));

    const currentLocationCoordinates =
      this.state.currentLocation &&
      this.state.currentLocation.lat &&
      this.state.currentLocation.lng
        ? [this.state.currentLocation.lat, this.state.currentLocation.lng]
        : undefined;
    const selectedFromLocationCoordinates =
      this.state.fromAddedLocation &&
      this.state.fromAddedLocation.lat &&
      this.state.fromAddedLocation.lng
        ? [this.state.fromAddedLocation.lat, this.state.fromAddedLocation.lng]
        : undefined;

    const selectedToLocationCoordinates =
      this.state.toAddedLocation &&
      this.state.toAddedLocation.lat &&
      this.state.toAddedLocation.lng
        ? [this.state.toAddedLocation.lat, this.state.toAddedLocation.lng]
        : undefined;

    let selectedLocationMarkers = [];
    if (selectedFromLocationCoordinates) {
      selectedLocationMarkers.push(
        <Marker
          key={"fromLoc-" + selectedFromLocationCoordinates}
          position={selectedFromLocationCoordinates}
          icon={locationIcon}
        />
      );
    }
    if (selectedToLocationCoordinates) {
      selectedLocationMarkers.push(
        <Marker
          key={"toLoc-" + selectedToLocationCoordinates}
          position={selectedToLocationCoordinates}
          icon={locationIcon}
        />
      );
    }

    const bounds =
      selectedToLocationCoordinates &&
      selectedFromLocationCoordinates &&
      L.latLngBounds([
        selectedToLocationCoordinates,
        selectedFromLocationCoordinates,
      ]);

    return (
      <won-travel-action-picker>
        <WonTitlePicker
          debounce={800}
          className={"rp__searchbox-from"}
          initial-value={
            this.state.fromAddedLocation && this.state.fromAddedLocation.name
          }
          onUpdate={this.doneTypingFrom}
          detail={{ placeholder: this.props.detail.placeholder.departure }}
        />
        {/*<!-- LIST OF SUGGESTED FROM-LOCATIONS -->*/}
        <ul
          className={
            "rp__searchresults " +
            (showFromResultDropDown
              ? "rp__searchresults--filled"
              : "rp__searchresults--empty")
          }
        >
          {showCurrentLocationInFromResult && (
            <li
              className="rp__searchresult"
              onClick={() =>
                this.selectFromLocation(this.state.currentLocation)
              }
            >
              <svg className="rp__searchresult__icon">
                <use
                  xlinkHref="#ico16_indicator_location"
                  href="#ico36_location_current"
                />
              </svg>
              <span className="rp__searchresult__text">
                {this.state.currentLocation.name}
              </span>
            </li>
          )}

          {showPrevFromLocationResult && (
            <li
              className="rp__searchresult"
              onClick={() =>
                this.selectFromLocation(this.state.previousFromLocation)
              }
            >
              <svg className="rp__searchresult__icon">
                {/*<!-- TODO: create and use a more appropriate icon here -->*/}
                <use
                  xlinkHref="#ico16_indicator_location"
                  href="#ico16_indicator_location"
                />
              </svg>
              <span className="rp__searchresult__text">
                {this.state.previousFromLocation.name} (previous)
              </span>
            </li>
          )}
          {fromSearchResults}
        </ul>
        <WonTitlePicker
          debounce={800}
          className={"rp__searchbox-to"}
          initial-value={
            this.state.toAddedLocation && this.state.toAddedLocation.name
          }
          onUpdate={this.doneTypingTo}
          detail={{ placeholder: this.props.detail.placeholder.destination }}
        />
        {/*<!-- LIST OF SUGGESTED TO-LOCATIONS -->*/}
        <ul
          className={
            "rp__searchresults " +
            (showToResultDropDown
              ? "rp__searchresults--filled"
              : "rp__searchresults--empty")
          }
        >
          {showCurrentLocationInToResult && (
            <li
              className="rp__searchresult"
              onClick={() => this.selectToLocation(this.state.currentLocation)}
            >
              <svg className="rp__searchresult__icon">
                <use
                  xlinkHref="#ico16_indicator_location"
                  href="#ico36_location_current"
                />
              </svg>
              <span className="rp__searchresult__text">
                {this.state.currentLocation.name}
              </span>
            </li>
          )}

          {showPrevToLocationResult && (
            <li
              className="rp__searchresult"
              onClick={() =>
                this.selectToLocation(this.state.previousToLocation)
              }
            >
              <svg className="rp__searchresult__icon">
                {/*<!-- TODO: create and use a more appropriate icon here -->*/}
                <use
                  xlinkHref="#ico16_indicator_location"
                  href="#ico16_indicator_location"
                />
              </svg>
              <span className="rp__searchresult__text">
                {this.state.previousToLocation.name} (previous)
              </span>
            </li>
          )}
          {toSearchResults}
        </ul>
        <Map
          className="rp__mapmount"
          zoom={13}
          center={
            selectedFromLocationCoordinates ||
            selectedToLocationCoordinates ||
            currentLocationCoordinates || [48.210033, 16.363449]
          }
          bounds={bounds}
        >
          <TileLayer
            attribution="&amp;copy <a href=&quot;http://osm.org/copyright&quot;>OpenStreetMap</a> contributors"
            url="https://www.matchat.org/tile/{z}/{x}/{y}.png"
          />
          {selectedLocationMarkers}
        </Map>
      </won-travel-action-picker>
    );
  }

  resetFromSearchResults() {
    this.setState({
      fromSearchResults: [],
      lastSearchedFor: undefined,
    });
  }

  resetToSearchResults() {
    this.setState({
      toSearchResults: [],
      lastSearchedFor: undefined,
    });
  }

  doneTypingFrom({ value }) {
    this.resetFromLocation(() => {
      if (value) {
        searchNominatim(value).then(fromSearchResults => {
          const parsedResults = scrubSearchResults(fromSearchResults, value);

          this.setState({
            fromSearchResults: parsedResults || [],
            lastSearchedFor: value,
          });
        });
      } else {
        this.resetFromSearchResults();
      }
    });
  }

  doneTypingTo({ value }) {
    this.resetToLocation(() => {
      if (value) {
        searchNominatim(value).then(toSearchResults => {
          const parsedResults = scrubSearchResults(toSearchResults, value);

          this.setState({
            toSearchResults: parsedResults || [],
            lastSearchedFor: value,
          });
        });
      } else {
        this.resetToSearchResults();
      }
    });
  }

  update() {
    // add some sort of validitycheck for locations?
    if (this.state.fromAddedLocation || this.state.toAddedLocation) {
      this.props.onUpdate({
        value: {
          fromLocation: this.state.fromAddedLocation,
          toLocation: this.state.toAddedLocation,
        },
      });
    } else {
      this.props.onUpdate({ value: undefined });
    }
  }

  selectFromLocation(location) {
    this.setState(
      {
        fromAddedLocation: location,
      },
      () => {
        this.resetFromSearchResults();
        this.update();
      }
    );
  }

  selectToLocation(location) {
    this.setState(
      {
        toAddedLocation: location,
      },
      () => {
        this.resetToSearchResults();
        this.update();
      }
    );
  }

  resetFromLocation(callback) {
    this.setState(
      {
        previousFromLocation: this.state.fromAddedLocation,
        fromAddedLocation: undefined,
      },
      () => {
        this.update();
        callback();
      }
    );
  }

  resetToLocation(callback) {
    this.setState(
      {
        previousToLocation: this.state.toAddedLocation,
        toAddedLocation: undefined,
      },
      () => {
        this.update();
        callback();
      }
    );
  }
}
WonTravelActionPicker.propTypes = {
  initialValue: PropTypes.object,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
