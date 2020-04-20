/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import { get, getIn } from "../utils.js";

import * as processUtils from "../redux/utils/process-utils.js";
import * as atomUtils from "../redux/utils/atom-utils";
import PropTypes from "prop-types";
import { connect } from "react-redux";

const mapStateToProps = (state, ownProps) => {
  const atom = getIn(state, ["atoms", ownProps.atomUri]);
  const isPersona = atomUtils.isPersona(atom);
  const image = isPersona ? atomUtils.getDefaultPersonaImage(atom) : undefined;

  const useCaseIcon = !isPersona
    ? atomUtils.getMatchedUseCaseIcon(atom)
    : undefined;
  const useCaseIconBackground = !isPersona
    ? atomUtils.getBackground(atom)
    : undefined;

  const identiconSvg = !useCaseIcon
    ? atomUtils.getIdenticonSvg(atom)
    : undefined;

  // Icons/Images of the AtomHolder
  const holderUri = atomUtils.getHeldByUri(atom);
  const holder = getIn(state, ["atoms", holderUri]);
  const holderImage = atomUtils.getDefaultPersonaImage(holder);
  const holderIdenticonSvg = atomUtils.getIdenticonSvg(holder);
  const isHolderPersona = atomUtils.isPersona(holder);
  const showHolderIdenticon =
    isHolderPersona && !holderImage && !!holderIdenticonSvg;
  const showHolderImage = isHolderPersona && holderImage;
  const showHolderIcon = !isHolderPersona;

  const holderUseCaseIcon = !isHolderPersona
    ? atomUtils.getMatchedUseCaseIcon(holder)
    : undefined;
  const holderUseCaseIconBackground = !isHolderPersona
    ? atomUtils.getBackground(holder)
    : undefined;

  const process = get(state, "process");
  return {
    className: ownProps.className,
    onClick: ownProps.onClick,
    isPersona,
    atomImage: isPersona ? atomUtils.getDefaultPersonaImage(atom) : undefined,
    atomInactive: atomUtils.isInactive(atom),
    atomFailedToLoad:
      atom && processUtils.hasAtomFailedToLoad(process, ownProps.atomUri),
    useCaseIcon,
    holderUseCaseIcon,
    useCaseIconBackground,
    showIdenticon: !image && !!identiconSvg,
    showImage: !!image,
    identiconSvg,
    image,
    holderUseCaseIconBackground,
    showHolderIcon,
    showHolderIdenticon,
    showHolderImage,
    holderImage,
    holderIdenticonSvg,
  };
};

class WonAtomIcon extends React.Component {
  render() {
    let holderIcon = undefined;
    let atomIcon = undefined;

    if (this.props.showHolderIdenticon) {
      holderIcon = (
        <img
          className="holderIcon"
          alt="Auto-generated title image for persona that holds the atom"
          src={"data:image/svg+xml;base64," + this.props.holderIdenticonSvg}
        />
      );
    } else if (this.props.showHolderImage) {
      holderIcon = (
        <img
          className="holderIcon"
          alt={get(this.props.holderImage, "name")}
          src={
            "data:" +
            get(this.props.holderImage, "encodingFormat") +
            ";base64," +
            get(this.props.holderImage, "encoding")
          }
        />
      );
    } else if (this.props.showHolderIcon) {
      const style = {
        backgroundColor: this.props.holderUseCaseIconBackground,
      };

      holderIcon = (
        <div style={style} className="holderIcon holderUseCaseIcon">
          <svg className="si__serviceatomicon">
            <use
              xlinkHref={this.props.holderUseCaseIcon}
              href={this.props.holderUseCaseIcon}
            />
          </svg>
        </div>
      );
    }

    if (this.props.showIdenticon) {
      atomIcon = (
        <img
          className="image"
          alt="Auto-generated title icon"
          src={"data:image/svg+xml;base64," + this.props.identiconSvg}
        />
      );
    } else if (this.props.showImage) {
      atomIcon = (
        <img
          className="image"
          alt={get(this.props.image, "name")}
          src={
            "data:" +
            get(this.props.image, "encodingFormat") +
            ";base64," +
            get(this.props.image, "encoding")
          }
        />
      );
    } else if (this.props.useCaseIcon) {
      const style = {
        backgroundColor: this.props.useCaseIconBackground,
      };

      atomIcon = (
        <div className="image usecaseimage" style={style}>
          <svg className="si__usecaseicon">
            <use
              xlinkHref={this.props.useCaseIcon}
              href={this.props.useCaseIcon}
            />
          </svg>
        </div>
      );
    }

    return (
      <won-atom-icon
        class={
          (this.props.className ? this.props.className : "") +
          " " +
          (this.props.isPersona ? " won-is-persona " : "") +
          (this.props.atomFailedToLoad ? " won-failed-to-load " : "") +
          (this.props.atomInactive ? " inactive " : "") +
          (this.props.onClick ? " clickable " : "")
        }
        onClick={this.props.onClick}
      >
        {atomIcon}
        {holderIcon}
      </won-atom-icon>
    );
  }
}
WonAtomIcon.propTypes = {
  atomUri: PropTypes.string.isRequired,
  className: PropTypes.string,
  onClick: PropTypes.func,
  isPersona: PropTypes.bool,
  atomImage: PropTypes.object,
  atomInactive: PropTypes.bool,
  atomFailedToLoad: PropTypes.bool,
  useCaseIcon: PropTypes.string,
  useCaseIconBackground: PropTypes.string,
  holderUseCaseIconBackground: PropTypes.string,
  showIdenticon: PropTypes.bool,
  showImage: PropTypes.bool,
  identiconSvg: PropTypes.string,
  image: PropTypes.object,
  holderUseCaseIcon: PropTypes.string,
  showHolderIcon: PropTypes.bool,
  showHolderIdenticon: PropTypes.bool,
  showHolderImage: PropTypes.bool,
  holderImage: PropTypes.object,
  holderIdenticonSvg: PropTypes.string,
};

export default connect(mapStateToProps)(WonAtomIcon);
