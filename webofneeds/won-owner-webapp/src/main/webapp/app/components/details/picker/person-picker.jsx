import React from "react";

import "~/style/_personpicker.scss";
import PropTypes from "prop-types";
import WonTitlePicker from "./title-picker";

import { Map } from "immutable";

const personDetails = [
  { fieldname: "Title", name: "title" },
  { fieldname: "Name", name: "name" },
  { fieldname: "Company", name: "company" },
  { fieldname: "Position", name: "position" },
];

export default function WonPersonPicker(props) {
  const updateDetail = (name, value) => {
    let newPerson = props.initialValue || Map();
    if (value) {
      newPerson = newPerson.set(name, value);
    } else {
      newPerson = newPerson.delete(name);
    }

    props.onUpdate({ value: newPerson.isEmpty() ? undefined : newPerson });
  };

  const inputFieldElements = personDetails.map((dtl, index) => (
    <div className="pp__detail" key={index}>
      <div className="pp__detail__label">{dtl.fieldname}</div>
      <WonTitlePicker
        onUpdate={({ value }) => updateDetail(dtl.name, value)}
        detail={{}}
        initialValue={props.initialValue && props.initialValue.get(dtl.name)}
      />
    </div>
  ));

  return <won-person-picker>{inputFieldElements}</won-person-picker>;
}

WonPersonPicker.propTypes = {
  initialValue: PropTypes.object,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
