import React from "react";

import "~/style/_personpicker.scss";
import PropTypes from "prop-types";
import Immutable from "immutable";
import WonTitlePicker from "./title-picker";

export default class WonPersonPicker extends React.Component {
  constructor(props) {
    super(props);

    const personDetails = [
      { fieldname: "Title", name: "title", value: undefined },
      { fieldname: "Name", name: "name", value: undefined },
      { fieldname: "Company", name: "company", value: undefined },
      { fieldname: "Position", name: "position", value: undefined },
    ];

    let addedPerson = Immutable.Map();
    if (props.initialValue && props.initialValue.size > 0) {
      for (let [dtl, value] of props.initialValue.entries()) {
        addedPerson = addedPerson.set(dtl, value);

        let personIndex = personDetails.findIndex(
          personDetail => personDetail.name === dtl
        );
        if (personIndex !== -1) {
          personDetails[personIndex].value = value;
        }
      }
    }

    this.state = {
      addedPerson: addedPerson,
      personDetails: personDetails,
    };
  }

  render() {
    const inputFieldElements = this.state.personDetails.map((dtl, index) => (
      <div className="pp__detail" key={dtl.fieldname + "-" + index}>
        <div className="pp__detail__label">{dtl.fieldname}</div>
        <WonTitlePicker
          onUpdate={({ value }) => this.updateDetails(dtl, value)}
          detail={{}}
          initialValue={dtl.value}
        />
      </div>
    ));

    return <won-person-picker>{inputFieldElements}</won-person-picker>;
  }

  showInitialPerson() {
    this.$scope.$apply();
  }

  updateDetails(dtl, value) {
    console.debug("updateDetails: ", dtl, " value:", value);
    if (value) {
      this.setState(
        { addedPerson: this.state.addedPerson.set(dtl.name, value) },
        this.update.bind(this)
      );
    } else {
      this.setState(
        { addedPerson: this.state.addedPerson.set(dtl.name, undefined) },
        this.update.bind(this)
      );
    }
  }

  update() {
    const person = this.state.addedPerson;
    let isEmpty = true;
    // check if person is empty
    if (person) {
      //check validity
      const personArray = Array.from(person.values());
      isEmpty = personArray.every(
        dtl => dtl === undefined || dtl === "" || (dtl && dtl.size === 0)
      );
    }
    if (person && !isEmpty) {
      this.props.onUpdate({ value: person });
    } else {
      this.props.onUpdate({ value: undefined });
    }
  }
}
WonPersonPicker.propTypes = {
  initialValue: PropTypes.object,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
