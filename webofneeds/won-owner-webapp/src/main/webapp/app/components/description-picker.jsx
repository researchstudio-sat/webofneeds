import * as React from "react";
import { Component } from "react";
import * as ReactDOM from "react-dom";
import * as angular from "angular";
import TextareaUnstyled from "react-autosize-textarea";
import styled from "styled-components";

const Textarea = styled(TextareaUnstyled)`
  resize: none;
  box-sizing: content-box;
`;

const ResetButtonSvg = styled.svg`
  position: absolute;
  top: 0.47rem;
  right: 0.5rem;
  width: 2.25rem;
  height: 2.25rem;
  z-index: 1;
`;

const Wrapper = styled.div`
  position: relative;
`;

function ResetButton({ onClick }) {
  return (
    <ResetButtonSvg
      onClick={onClick}
      className="lp__searchbox__icon clickable ng-scope"
      style={{ "--local-primary": "var(--won-primary-color)" }}
    >
      <use xlinkHref="#ico36_close" />
    </ResetButtonSvg>
  );
}

export default class DescriptionPicker extends Component {
  constructor(props) {
    super(props);
    this.state = { value: props.value };
  }

  static getDerivedStateFromProps({ value }) {
    return {
      value,
    };
  }

  updateText = e => {
    const newValue = e.target.value;
    this.setState({
      value: newValue,
    });
    this.props.changeHandler({ description: newValue });
  };

  resetText = () => {
    this.setState({
      value: "",
    });
    this.props.changeHandler({ description: "" });
  };

  render() {
    return (
      <Wrapper className="cis__description">
        {this.state.value != "" && <ResetButton onClick={this.resetText} />}
        <Textarea
          className="cis__description__text won-txt"
          onChange={this.updateText}
          value={this.state.value}
          maxRows={4}
        />
      </Wrapper>
    );
  }
}

// Needed to bind to angular
const directiveName = angular
  .module("won.owner.components.descriptionPicker", [])
  .directive("wonDescriptionPicker", () => {
    return {
      scope: {
        initialDescription: "=",
        onDescriptionUpdated: "&",
      },
      link: function(scope, el) {
        scope.$watch("initialDescription", newValue => {
          ReactDOM.render(
            <DescriptionPicker
              value={newValue}
              changeHandler={scope["onDescriptionUpdated"]}
            />,
            el[0]
          );
        });
      },
    };
  }).name;

export { directiveName as name };
