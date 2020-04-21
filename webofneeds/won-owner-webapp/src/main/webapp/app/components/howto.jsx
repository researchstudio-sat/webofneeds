import React from "react";
import PropTypes from "prop-types";
import Immutable from "immutable";
import { connect } from "react-redux";
import { actionCreators } from "../actions/actions.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import { getIn } from "../utils.js";

import WonLabelledHr from "./labelled-hr.jsx";

import "~/style/_howto.scss";
import ico36_description from "~/images/won-icons/ico36_description.svg";
import ico36_match from "~/images/won-icons/ico36_match.svg";
import ico36_incoming from "~/images/won-icons/ico36_incoming.svg";
import ico36_message from "~/images/won-icons/ico36_message.svg";
import ico36_location_current from "~/images/won-icons/ico36_location_current.svg";
import ico36_backarrow from "~/images/won-icons/ico36_backarrow.svg";
import ico36_uc_question from "~/images/won-icons/ico36_uc_question.svg";
import { Link, withRouter } from "react-router-dom";

const howItWorksSteps = [
  {
    svgSrc: ico36_description,
    title: "Post your atom anonymously",
    text:
      "Atoms can be very personal, so privacy is important. You don't have to reveal your identity here.",
  },
  {
    svgSrc: ico36_match,
    title: "Get matches",
    text:
      "Based on the" +
      " information you provide, we will try to connect you with others",
  },
  {
    svgSrc: ico36_incoming,
    title: "Request contact – or be contacted",
    text:
      "If you're interested," +
      " make a contact request - or get one if your counterpart is faster than you",
  },
  {
    svgSrc: ico36_message,
    title: "Interact and exchange",
    text:
      "You found someone" +
      " who has what you need, wants to meet or change something in your common environment? Go chat with them! ",
  },
];

const mapStateToProps = (state, ownProps) => {
  return {
    className: ownProps.className,
    appTitle: getIn(state, ["config", "theme", "title"]),
    isLocationAccessDenied: generalSelectors.isLocationAccessDenied(state),
  };
};

const mapDispatchToProps = dispatch => {
  return {
    locationAccessDenied: () => {
      dispatch(actionCreators.view__locationAccessDenied());
    },
    updateCurrentLocation: locImm => {
      dispatch(actionCreators.view__updateCurrentLocation(locImm));
    },
  };
};

class WonHowTo extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      selectedHowItWorksStep: 0,
    };

    this.viewWhatsAround = this.viewWhatsAround.bind(this);
    this.viewWhatsNew = this.viewWhatsNew.bind(this);
  }

  render() {
    return (
      <won-how-to class={this.props.className ? this.props.className : ""}>
        <h1 className="howto__title">How it works</h1>
        <h3 className="howto__subtitle">
          {"in " + howItWorksSteps.length + " Steps"}
        </h3>
        <div className="howto__steps">
          <div
            className="howto__steps__process"
            style={{ "--howToColCount": howItWorksSteps.length }}
          >
            {howItWorksSteps.map((item, index) => (
              <svg
                key={index}
                className={
                  "howto__steps__process__icon " +
                  (index == this.state.selectedHowItWorksStep
                    ? " howto__steps__process__icon--selected "
                    : "")
                }
                onClick={() => this.setSelectedHowItWorksStep(index)}
              >
                <use
                  xlinkHref={this.getSvgIconFromItem(item)}
                  href={this.getSvgIconFromItem(item)}
                />
              </svg>
            ))}
            {howItWorksSteps.map((item, index) => (
              <div
                key={index}
                className={
                  "howto__steps__process__stepcount " +
                  (index == this.state.selectedHowItWorksStep
                    ? " howto__steps__process__stepcount--selected "
                    : "")
                }
                onClick={() => this.setSelectedHowItWorksStep(index)}
              >
                {index + 1}
              </div>
            ))}
            <div className="howto__steps__process__stepline" />
          </div>
          <svg
            className={
              "howto__steps__button howto__steps__button--prev " +
              (this.state.selectedHowItWorksStep <= 0
                ? " howto__steps__button--invisible "
                : "")
            }
            onClick={() =>
              this.setSelectedHowItWorksStep(
                this.state.selectedHowItWorksStep - 1
              )
            }
          >
            <use xlinkHref={ico36_backarrow} href={ico36_backarrow} />
          </svg>
          <div className="howto__steps__detail">
            <div className="howto__detail__title">
              {howItWorksSteps[this.state.selectedHowItWorksStep].title}
            </div>
            <div className="howto__steps__detail__text">
              {howItWorksSteps[this.state.selectedHowItWorksStep].text}
            </div>
          </div>
          <svg
            className={
              "howto__steps__button howto__steps__button--next " +
              (this.state.selectedHowItWorksStep >= howItWorksSteps.length - 1
                ? " howto__steps__button--invisible "
                : "")
            }
            onClick={() =>
              this.setSelectedHowItWorksStep(
                this.state.selectedHowItWorksStep + 1
              )
            }
          >
            <use xlinkHref={ico36_backarrow} href={ico36_backarrow} />
          </svg>
        </div>
        <h2 className="howto__title">Ready to start?</h2>
        <h3 className="howto__subtitle">
          {"Post your atom or offer and let " +
            this.props.appTitle +
            " do the rest"}
        </h3>
        <div className="howto__createx">
          <button
            className="won-button--filled red howto__createx__button"
            onClick={this.viewWhatsAround}
          >
            <svg className="won-button-icon">
              <use
                xlinkHref={ico36_location_current}
                href={ico36_location_current}
              />
            </svg>
            <span>{"What's in your Area?"}</span>
          </button>
          <button
            className="won-button--filled red howto__createx__button"
            onClick={this.viewWhatsNew}
          >
            <span>{"What's new?"}</span>
          </button>
          <WonLabelledHr
            className="labelledHr howto__createx__labelledhr"
            label="Or"
          />
          <Link
            to="/create?useCase=persona"
            className="won-button--filled red howto__createx__spanbutton"
          >
            <span>Create your Persona!</span>
          </Link>
          <WonLabelledHr
            className="labelledHr howto__createx__labelledhr"
            label="Or"
          />
          <Link
            to="/create"
            className="won-button--filled red howto__createx__spanbutton"
          >
            <span>Post something now!</span>
          </Link>
        </div>
      </won-how-to>
    );
  }

  setSelectedHowItWorksStep(index) {
    this.setState({
      selectedHowItWorksStep: index,
    });
  }

  getSvgIconFromItem(item) {
    return item.svgSrc ? item.svgSrc : ico36_uc_question;
  }

  viewWhatsAround() {
    this.viewWhatsX(() => {
      this.props.history.push("/map");
    });
  }

  viewWhatsNew() {
    this.viewWhatsX(() => {
      this.props.history.push("/overview");
    });
  }

  viewWhatsX(callback) {
    if (this.props.isLocationAccessDenied) {
      callback();
    } else if ("geolocation" in navigator) {
      navigator.geolocation.getCurrentPosition(
        currentLocation => {
          const lat = currentLocation.coords.latitude;
          const lng = currentLocation.coords.longitude;

          this.props.updateCurrentLocation(
            Immutable.fromJS({ location: { lat, lng } })
          );
          callback();
        },
        error => {
          //error handler
          console.error(
            "Could not retrieve geolocation due to error: ",
            error.code,
            ", continuing map initialization without currentLocation. fullerror:",
            error
          );
          this.props.locationAccessDenied();
          callback();
        },
        {
          //options
          enableHighAccuracy: true,
          maximumAge: 30 * 60 * 1000, //use if cache is not older than 30min
        }
      );
    } else {
      console.error("location could not be retrieved");
      this.props.locationAccessDenied();
      callback();
    }
  }
}
WonHowTo.propTypes = {
  className: PropTypes.string,
  appTitle: PropTypes.string,
  isLocationAccessDenied: PropTypes.bool,
  updateCurrentLocation: PropTypes.func,
  locationAccessDenied: PropTypes.func,
  history: PropTypes.object,
};

export default withRouter(
  connect(
    mapStateToProps,
    mapDispatchToProps
  )(WonHowTo)
);
