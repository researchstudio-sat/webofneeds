/**
 * Created by fsuda on 21.08.2017.
 */

import React from "react";

import "~/style/_trig.scss";
import PropTypes from "prop-types";
import { trigPrefixesAndBody } from "../utils";

export default class WonTrig extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      prefixesExpanded: false,
      trigPrefixes: undefined,
      trigBody: undefined,
    };
    this.togglePrefixes = this.togglePrefixes.bind(this);
  }

  togglePrefixes() {
    this.setState({ prefixesExpanded: !this.state.prefixesExpanded });
  }

  static getDerivedStateFromProps(props, state) {
    if (props.jsonld) {
      console.debug("jsonld: ", props.jsonld);
      /*const trigString = await won.jsonLdToTrig(props.jsonld.toJS());
      const { trigPrefixes, trigBody } = trigPrefixesAndBody(trigString);
      //TODO: FIGURE OUT HOW TO DO THAT
      */
      return {
        prefixesExpanded: state.prefixesExpanded,
        trigPrefixes: "TODO FROM JSONLD",
        trigBody: "TODO FROM JSONLD",
      };
    } else {
      const { trigPrefixes, trigBody } = trigPrefixesAndBody(props.trig);

      return {
        prefixesExpanded: state.prefixesExpanded,
        trigPrefixes: trigPrefixes,
        trigBody: trigBody,
      };
    }
  }

  render() {
    const trigBodyElement = this.state.trigBody ? (
      <div
        onClick={!this.state.prefixesExpanded ? this.togglePrefixes : undefined}
        className={
          "trig " + (!this.state.prefixesExpanded ? " clickable " : "")
        }
      >
        {/*no spaces or newlines within the code-tag, because it is preformatted*/}
        <code className="trig__prefixes">
          {this.state.prefixesExpanded
            ? this.state.trigPrefixes
            : "@prefix ..."}
        </code>
        <code className="trig__contentgraph">{this.state.trigBody}</code>
      </div>
    ) : (
      undefined
    );

    return <won-trig>{trigBodyElement}</won-trig>;
  }

  // OLD ANGULAR CODE:
  // constructor(/* arguments = dependency injections */) {
  //   this.$scope.$watch("self.jsonld", (newJsonld, prevJsonld) =>
  //     this.updatedJsonld(newJsonld, prevJsonld)
  //   );
  // }
  // async updatedJsonld(newJsonld, prevJsonld) {
  //   // generate new trig body and prefixes, if the json-ld has changed or it hasn't been generated before
  //   if (newJsonld && (newJsonld != prevJsonld || !this.trigBody)) {
  //     const trigString = await won.jsonLdToTrig(newJsonld.toJS());
  //     this.setTrig(trigString);
  //   }
  // }
  // setTrig(trigString) {
  //   const { trigPrefixes, trigBody } = trigPrefixesAndBody(trigString);
  //   this.trigPrefixes = trigPrefixes;
  //   this.trigBody = trigBody;
  // }
}
WonTrig.propTypes = {
  trig: PropTypes.string,
  jsonld: PropTypes.object,
};
