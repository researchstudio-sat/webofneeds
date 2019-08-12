/**
 * Created by fsuda on 21.08.2017.
 */

import React from "react";

import "~/style/_trig.scss";
import PropTypes from "prop-types";

export default class WonLabelledHr extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      prefixesExpanded: false,
    };
  }

  togglePrefixes() {
    this.setState({ prefixesExpanded: !this.state.prefixesExpanded });
  }

  render() {
    const trigBody = undefined; //TODO: IMPL FETCH OF TRIGBODY
    const trigPrefixes = undefined; //TODO: IMPL FETCH OF TRIGPREFIXES

    const trigBodyElement = trigBody ? (
      <div onClick={() => this.togglePrefixes()} className="trig">
        {/*no spaces or newlines within the code-tag, because it is preformatted*/}
        <code className="trig__prefixes">
          {this.state.prefixesExpanded ? trigPrefixes : "@prefix ..."}
        </code>
        <code className="trig__contentgraph">{trigBody}</code>
      </div>
    ) : (
      undefined
    );

    return (
      <won-trig>
        TODO!
        {trigBodyElement}
      </won-trig>
    );
  }

  // OLD ANGULAR CODE:
  // constructor(/* arguments = dependency injections */) {
  //   attach(this, serviceDependencies, arguments);
  //
  //   this.$scope.$watch("self.trig", (newTrig, prevTrig) =>
  //     this.updatedTrig(newTrig, prevTrig)
  //   );
  //   this.$scope.$watch("self.jsonld", (newJsonld, prevJsonld) =>
  //     this.updatedJsonld(newJsonld, prevJsonld)
  //   );
  // }
  // updatedTrig(newTrig, prevTrig) {
  //   // generate new trig body and prefixes, if the trig-input has changed or it hasn't been generated before
  //   if (newTrig && (newTrig != prevTrig || !this.trigBody)) {
  //     this.setTrig(newTrig);
  //   }
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
WonLabelledHr.propTypes = {
  trig: PropTypes.string,
  jsonld: PropTypes.object,
};
