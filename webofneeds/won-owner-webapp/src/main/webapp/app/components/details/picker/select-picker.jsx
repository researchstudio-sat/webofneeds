import React, { useEffect, useState } from "react";
import Immutable from "immutable";

import PropTypes from "prop-types";

import "~/style/_selectpicker.scss";

export default function WonSelectPicker({ initialValue, detail, onUpdate }) {
  const [selectedValuesImm, setSelectedValues] = useState(
    Immutable.fromJS(initialValue || [])
  );

  const update = event => {
    if (detail.multiSelect) {
      if (selectedValuesImm.includes(event.target.value)) {
        setSelectedValues(
          selectedValuesImm.filter(elem => elem !== event.target.value)
        );
      } else if (event.target.checked) {
        setSelectedValues(selectedValuesImm.push(event.target.value));
      }
    } else if (event.target.type === "radio") {
      setSelectedValues(Immutable.fromJS([event.target.value]));
    }
  };

  useEffect(
    () => {
      onUpdate({
        value:
          selectedValuesImm && selectedValuesImm.size > 0
            ? selectedValuesImm.toJS()
            : undefined,
      });
    },
    [selectedValuesImm]
  );

  let optionElements = [];
  if (detail && detail.options && detail.options.length > 0) {
    detail.options.map((option, index) =>
      optionElements.push(
        <label
          key={option.label + "-" + index}
          className="selectp__input__inner"
        >
          <input
            className="selectp__input__inner__select"
            type={detail && detail.multiSelect ? "checkbox" : "radio"}
            value={option.value}
            defaultChecked={selectedValuesImm.includes(option.value)}
            name={detail.identifier}
            onChange={update}
          />
          {option.label}
        </label>
      )
    );
  }

  return <won-select-picker>{optionElements}</won-select-picker>;
}

WonSelectPicker.propTypes = {
  initialValue: PropTypes.array,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
