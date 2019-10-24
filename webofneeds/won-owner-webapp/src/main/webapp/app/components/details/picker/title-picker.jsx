import React from "react";

import "~/style/_titlepicker.scss";
import PropTypes from "prop-types";

export default function WonTitlePicker(props) {
  const reset = () => {
    props.onUpdate({ value: "" });
    if (props.onReset) {
      props.onReset();
    }
  };
  const update = event => {
    props.onUpdate({ value: event.target.value });
  };

  return (
    <won-title-picker class={props.className ? props.className : ""}>
      <div className="titlep__input">
        {props.initialValue && (
          <svg className="titlep__input__icon clickable" onClick={reset}>
            <use xlinkHref="#ico36_close" href="#ico36_close" />
          </svg>
        )}
        <input
          type="text"
          className="titlep__input__inner"
          placeholder={props.detail && props.detail.placeholder}
          value={props.initialValue === undefined ? "" : props.initialValue}
          onChange={update}
        />
      </div>
    </won-title-picker>
  );
}

WonTitlePicker.propTypes = {
  className: PropTypes.string,
  initialValue: PropTypes.string,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
  onReset: PropTypes.func,
};
