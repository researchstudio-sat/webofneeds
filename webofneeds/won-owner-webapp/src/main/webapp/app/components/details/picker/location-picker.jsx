import React from "react";
import { Map, Marker, TileLayer } from "react-leaflet";
import "~/style/_locationpicker.scss";
import PropTypes from "prop-types";
import WonTitlePicker from "./title-picker.jsx";
import {
  nominatim2draftLocation,
  reverseSearchNominatim,
  scrubSearchResults,
  searchNominatim,
} from "../../../api/nominatim-api.js";
import L from "leaflet";

import _ from "lodash";

import "leaflet/dist/leaflet.css";
import ico16_indicator_location from "~/images/won-icons/ico16_indicator_location.svg";
import ico36_location_current from "~/images/won-icons/ico36_location_current.svg";
import ico_loading_anim from "~/images/won-icons/ico_loading_anim.svg";

const locationIcon = L.divIcon({
  className: "wonLocationMarkerIcon",
  html:
    "<svg class='marker__icon'><use xlink:href='~/images/won-icons/ico36_detail_location.svg' href='~/images/won-icons/ico36_detail_location.svg' /></svg>",
});

//TODO: SELECT LOCATION BY CLICKING ON THE MAP
export default class WonLocationPicker extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      searchResults: [],
      lastSearchedFor: undefined,
      pickedLocation: props.initialValue,
      previousLocation: undefined,
      alternativeName: "",
      currentLocation: undefined,
      searchTerm: (props.initialValue && props.initialValue.name) || "",
      searchState: "IDLE",
    };
    this.doneTyping = this.doneTyping.bind(this);
    this.setAlternativeName = this.setAlternativeName.bind(this);
    this.update = this.update.bind(this);

    this.startSearch = _.debounce(value => {
      this.setState({
        searchState: "RUNNING",
      });
      searchNominatim(value).then(searchResults => {
        const parsedResults = scrubSearchResults(searchResults, value);

        this.setState({
          searchResults: parsedResults || [],
          lastSearchedFor: value,
          searchState: "DONE",
        });
      });
    }, 700);
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
    const showCurrentLocationResult =
      !this.state.pickedLocation && !!this.state.currentLocation;
    const showPrevLocationResult =
      !this.state.pickedLocation && !!this.state.previousLocation;

    let searchNotice;
    if (
      this.state.searchTerm != "" &&
      this.state.searchTerm != this.state.lastSearchedFor
    ) {
      searchNotice = (
        <div className="lp__loadingspinner">
          <svg className="hspinner">
            <use xlinkHref={ico_loading_anim} href={ico_loading_anim} />
          </svg>
        </div>
      );
    } else if (
      this.state.searchState == "DONE" &&
      this.state.searchResults.length == 0
    ) {
      searchNotice = <div className="lp__noresults">No results found</div>;
    }

    const showResultDropDown =
      this.state.searchResults.length > 0 ||
      showPrevLocationResult ||
      showCurrentLocationResult ||
      searchNotice;

    const searchResults = this.state.searchResults.map((result, index) => (
      <li
        className="lp__searchresult"
        key={result.name + "-" + index}
        onClick={() => this.selectLocation(result)}
      >
        <svg className="lp__searchresult__icon">
          <use
            xlinkHref={ico16_indicator_location}
            href={ico16_indicator_location}
          />
        </svg>
        <span className="lp__searchresult__text">{result.name}</span>
      </li>
    ));

    const currentLocationCoordinates =
      this.state.currentLocation &&
      this.state.currentLocation.lat &&
      this.state.currentLocation.lng
        ? [this.state.currentLocation.lat, this.state.currentLocation.lng]
        : undefined;
    const selectedLocationCoordinates =
      this.state.pickedLocation &&
      this.state.pickedLocation.lat &&
      this.state.pickedLocation.lng
        ? [this.state.pickedLocation.lat, this.state.pickedLocation.lng]
        : undefined;
    let selectedLocationMarker;

    if (selectedLocationCoordinates) {
      selectedLocationMarker = (
        <Marker
          position={selectedLocationCoordinates}
          icon={locationIcon}
          onClick={() => false}
        />
      );
    }

    return (
      <won-location-picker>
        <WonTitlePicker
          className={"lp__searchbox"}
          initialValue={this.state.searchTerm}
          onUpdate={this.doneTyping}
          onReset={this.resetLocation.bind(this)}
          detail={{ placeholder: this.props.detail.placeholder }}
        />
        {/*<!-- LIST OF SUGGESTED LOCATIONS -->*/}
        <ul
          className={
            "lp__searchresults " +
            (showResultDropDown
              ? "lp__searchresults--filled"
              : "lp__searchresults--empty")
          }
        >
          {searchNotice}
          {showCurrentLocationResult && (
            <li
              className="lp__searchresult"
              onClick={() => this.selectLocation(this.state.currentLocation)}
            >
              <svg className="lp__searchresult__icon">
                <use
                  xlinkHref={ico16_indicator_location}
                  href={ico36_location_current}
                />
              </svg>
              <span className="lp__searchresult__text">
                {this.state.currentLocation.name}
              </span>
            </li>
          )}

          {showPrevLocationResult && (
            <li
              className="lp__searchresult"
              onClick={() => this.selectLocation(this.state.previousLocation)}
            >
              <svg className="lp__searchresult__icon">
                {/*<!-- TODO: create and use a more appropriate icon here -->*/}
                <use
                  xlinkHref={ico16_indicator_location}
                  href={ico16_indicator_location}
                />
              </svg>
              <span className="lp__searchresult__text">
                {this.state.previousLocation.name} (previous)
              </span>
            </li>
          )}
          {searchResults}
        </ul>
        <Map
          className="lp__mapmount"
          zoom={13}
          onClick={e => this.mapClicked(e)}
          center={
            selectedLocationCoordinates ||
            currentLocationCoordinates || [48.210033, 16.363449]
          }
        >
          <TileLayer
            attribution="&amp;copy <a href=&quot;http://osm.org/copyright&quot;>OpenStreetMap</a> contributors"
            url="https://www.matchat.org/tile/{z}/{x}/{y}.png"
          />
          {selectedLocationMarker}
        </Map>
        {!!this.state.pickedLocation && (
          <WonTitlePicker
            className={"lp__addressoverride"}
            initialValue={this.state.alternativeName}
            onUpdate={this.setAlternativeName}
            detail={
              this.props.detail && this.props.detail.overrideAddressDetail
            }
          />
        )}
      </won-location-picker>
    );
  }

  resetSearchResults() {
    this.setState({
      searchResults: [],
      lastSearchedFor: undefined,
      searchState: "IDLE",
    });
  }

  doneTyping({ value }) {
    this.setState({ searchTerm: value }, () => {
      if (value) {
        this.startSearch(value);
      } else {
        this.resetSearchResults();
      }
    });
  }

  update() {
    if (this.state.pickedLocation) {
      const _pickedLocation = this.state.pickedLocation;

      if (
        this.state.alternativeName &&
        this.state.alternativeName.trim().length > 0
      ) {
        _pickedLocation.name = this.state.alternativeName.trim();
      }

      this.props.onUpdate({ value: _pickedLocation });
    } else {
      this.props.onUpdate({ value: undefined });
    }
  }

  setAlternativeName({ value }) {
    this.setState(
      {
        alternativeName: value,
      },
      this.update
    );
  }

  selectLocation(location) {
    console.log(location);
    this.setState(
      {
        pickedLocation: location,
        alternativeName: "",
        searchTerm: location.name,
      },
      () => {
        this.resetSearchResults();
        this.update();
      }
    );
  }

  mapClicked({ latlng }) {
    this.selectLocation({
      ...latlng,
      name: `${latlng.lat}, ${latlng.lng}`,
      nwCorner: latlng,
      seCorner: latlng,
    });
  }

  resetLocation(callback) {
    this.setState(
      {
        previousLocation: this.state.pickedLocation,
        pickedLocation: undefined,
        alternativeName: "",
      },
      () => {
        this.update();
        if (callback) {
          callback();
        }
      }
    );
  }
}
WonLocationPicker.propTypes = {
  initialValue: PropTypes.object,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
