/**
 * Created by fsuda on 03.10.2018.
 */
import { generateIdString } from "../../app/utils.js";
import * as jsonLdUtils from "../../app/service/jsonld-utils.js";
import WonReviewViewer from "../../app/components/details/viewer/review-viewer.jsx";
import WonReviewPicker from "../../app/components/details/picker/review-picker.jsx";
import ico36_detail_title from "../../images/won-icons/ico36_detail_title.svg";

/*Detail based on https://schema.org/Review*/
export const reviewRating = {
  identifier: "reviewRating",
  label: "Rating",
  icon: ico36_detail_title /*TODO: REVIEW/RATING ICON*/,
  component: WonReviewPicker,
  viewerComponent: WonReviewViewer,
  rating: [
    //Value needs to be empty string for react to clear localState (https://github.com/facebook/react/issues/4085)
    //which isn't a problem since the bool value of an empty string is false (in js)
    { value: "", label: "Select a Rating", default: true },
    { value: 1, label: "⭐" },
    { value: 2, label: "⭐⭐" },
    { value: 3, label: "⭐⭐⭐" },
    { value: 4, label: "⭐⭐⭐⭐" },
    { value: 5, label: "⭐⭐⭐⭐⭐" },
  ],
  messageEnabled: false,
  parseToRDF: function({ value, identifier, contentUri }) {
    if (!value || !value.rating) {
      return { "s:reviewRating": undefined };
    }

    const randomString = generateIdString(10);

    return {
      /*"s:about":
          "TODO: here should be the identityUri that the review refers to", //TODO: use identity uri*/
      /*"s:author":
          "TODO:  here should be the identityUri of the review-author", //TODO: use identity uri*/
      "s:reviewRating": {
        "@type": "s:Rating",
        "@id":
          contentUri && identifier
            ? contentUri + "#" + identifier + "-" + randomString
            : undefined,
        "s:bestRating": { "@value": 5, "@type": "xsd:int" }, //not necessary but possible
        "s:ratingValue": { "@value": value.rating, "@type": "xsd:int" },
        "s:worstRating": { "@value": 1, "@type": "xsd:int" }, //not necessary but possible
      },
    };
  },
  parseFromRDF: function(jsonLDImm) {
    const rating = jsonLdUtils.parseFrom(
      jsonLDImm,
      ["s:reviewRating", "s:ratingValue"],
      "xsd:int"
    );

    //TODO: EXTRACT AUTHOR AND ABOUT URI BEST/WORST Rating

    if (!rating) {
      return undefined;
    } else {
      return { rating: rating };
    }
  },
  generateHumanReadable: function({ value, includeLabel }) {
    if (value) {
      //TODO: EXTRACT AUTHOR AND ABOUT URI BEST/WORST Rating
      let ratingLabel = undefined;

      this.rating &&
        this.rating.forEach(rating => {
          if (rating.value === value.rating) {
            ratingLabel = rating.label;
          }
        });
      ratingLabel = ratingLabel || value.rating;

      return includeLabel ? this.label + ": " + ratingLabel : ratingLabel;
    }
    return undefined;
  },
};
