import React, { useState } from "react";
import PropTypes from "prop-types";
import Immutable from "immutable";
import { useDispatch, useSelector } from "react-redux";
import { actionCreators } from "../actions/actions.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import { get, generateLink } from "../utils.js";

import WonLabelledHr from "./labelled-hr.jsx";

import "~/style/_howto.scss";
import ico36_description from "~/images/won-icons/ico36_description.svg";
import ico36_match from "~/images/won-icons/ico36_match.svg";
import ico36_incoming from "~/images/won-icons/ico36_incoming.svg";
import ico36_message from "~/images/won-icons/ico36_message.svg";
import ico36_location_current from "~/images/won-icons/ico36_location_current.svg";
import ico36_backarrow from "~/images/won-icons/ico36_backarrow.svg";
import ico36_uc_question from "~/images/won-icons/ico36_uc_question.svg";
import { Link, useHistory } from "react-router-dom";

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

export default function WonHowTo({ className }) {
  const history = useHistory();
  const dispatch = useDispatch();
  const theme = useSelector(generalSelectors.getTheme);
  const appTitle = get(theme, "title");
  const isLocationAccessDenied = useSelector(
    generalSelectors.isLocationAccessDenied
  );
  const [selectedHowItWorksStep, setSelectedHowItWorksStep] = useState(0);

  function getSvgIconFromItem(item) {
    return item.svgSrc ? item.svgSrc : ico36_uc_question;
  }

  function viewWhatsAround() {
    viewWhatsX(() => {
      history.push("/map");
    });
  }

  function viewWhatsNew() {
    viewWhatsX(() => {
      history.push("/overview");
    });
  }

  function viewWhatsX(callback) {
    if (isLocationAccessDenied) {
      callback();
    } else if ("geolocation" in navigator) {
      navigator.geolocation.getCurrentPosition(
        currentLocation => {
          const lat = currentLocation.coords.latitude;
          const lng = currentLocation.coords.longitude;

          dispatch(
            actionCreators.view__updateCurrentLocation(
              Immutable.fromJS({ location: { lat, lng } })
            )
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
          dispatch(actionCreators.view__locationAccessDenied());
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
      dispatch(actionCreators.view__locationAccessDenied());
      callback();
    }
  }

  return (
    <won-how-to class={className ? className : ""}>
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
                (index === selectedHowItWorksStep
                  ? " howto__steps__process__icon--selected "
                  : "")
              }
              onClick={() => setSelectedHowItWorksStep(index)}
            >
              <use
                xlinkHref={getSvgIconFromItem(item)}
                href={getSvgIconFromItem(item)}
              />
            </svg>
          ))}
          {howItWorksSteps.map((item, index) => (
            <div
              key={index}
              className={
                "howto__steps__process__stepcount " +
                (index === selectedHowItWorksStep
                  ? " howto__steps__process__stepcount--selected "
                  : "")
              }
              onClick={() => setSelectedHowItWorksStep(index)}
            >
              {index + 1}
            </div>
          ))}
          <div className="howto__steps__process__stepline" />
        </div>
        <svg
          className={
            "howto__steps__button howto__steps__button--prev " +
            (selectedHowItWorksStep <= 0
              ? " howto__steps__button--invisible "
              : "")
          }
          onClick={() => setSelectedHowItWorksStep(selectedHowItWorksStep - 1)}
        >
          <use xlinkHref={ico36_backarrow} href={ico36_backarrow} />
        </svg>
        <div className="howto__steps__detail">
          <div className="howto__detail__title">
            {howItWorksSteps[selectedHowItWorksStep].title}
          </div>
          <div className="howto__steps__detail__text">
            {howItWorksSteps[selectedHowItWorksStep].text}
          </div>
        </div>
        <svg
          className={
            "howto__steps__button howto__steps__button--next " +
            (selectedHowItWorksStep >= howItWorksSteps.length - 1
              ? " howto__steps__button--invisible "
              : "")
          }
          onClick={() => setSelectedHowItWorksStep(selectedHowItWorksStep + 1)}
        >
          <use xlinkHref={ico36_backarrow} href={ico36_backarrow} />
        </svg>
      </div>
      <h2 className="howto__title">Ready to start?</h2>
      <h3 className="howto__subtitle">
        {"Post your atom or offer and let " + appTitle + " do the rest"}
      </h3>
      <div className="howto__createx">
        <button
          className="won-button--filled secondary howto__createx__button"
          onClick={viewWhatsAround}
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
          className="won-button--filled secondary howto__createx__button"
          onClick={viewWhatsNew}
        >
          <span>{"What's new?"}</span>
        </button>
        <WonLabelledHr
          className="labelledHr howto__createx__labelledhr"
          label="Or"
        />
        <Link
          to={location =>
            generateLink(location, { useCase: "persona" }, "/create", "false")
          }
          className="won-button--filled secondary howto__createx__spanbutton"
        >
          <span>Create your Persona!</span>
        </Link>
        <WonLabelledHr
          className="labelledHr howto__createx__labelledhr"
          label="Or"
        />
        <Link
          to={location => generateLink(location, {}, "/create", "false")}
          className="won-button--filled secondary howto__createx__spanbutton"
        >
          <span>Post something now!</span>
        </Link>
      </div>
    </won-how-to>
  );
}
WonHowTo.propTypes = {
  className: PropTypes.string,
};
