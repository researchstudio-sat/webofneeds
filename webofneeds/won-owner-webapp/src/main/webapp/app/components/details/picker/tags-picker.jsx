import React from "react";
import TagsInput from "react-tagsinput";
import AutosizeInput from "react-input-autosize";
import "~/style/_tagspicker.scss";
import PropTypes from "prop-types";

export default class WonTagsPicker extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      tags: props.initialValue || [],
    };
  }

  render() {
    return (
      <won-tags-picker>
        <TagsInput
          value={this.state.tags}
          onChange={this.changeTags.bind(this)}
          onlyUnique={true}
          pasteSplit={this.pasteSplit}
          renderInput={this.autosizingRenderInput}
          addKeys={[13, 188]}
        />
      </won-tags-picker>
    );
  }

  changeTags(tags) {
    this.setState({ tags: tags }, () => {
      this.onUpdate({
        value: this.state.tags && this.state.tags.length > 0 ? tags : undefined,
      });
    });
  }
  pasteSplit(data) {
    return data.split(",").map(d => d.trim());
  }
  autosizingRenderInput({ ...props }) {
    let { onChange, value, ...other } = props;
    return (
      <AutosizeInput
        className="react-tagsinput-input-autogrow"
        type="text"
        onChange={onChange}
        value={value}
        {...other}
      />
    );
  }
}
WonTagsPicker.propTypes = {
  initialValue: PropTypes.arrayOf(PropTypes.string),
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
