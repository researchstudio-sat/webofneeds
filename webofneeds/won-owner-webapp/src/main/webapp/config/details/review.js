/**
 * Created by fsuda on 03.10.2018.
 */
import { generateIdString } from "../../app/utils.js";
import won from "../../app/won-es6.js";

/*Detail based on https://schema.org/Review*/
export const review = {
  identifier: "review",
  label: "Review",
  icon: "#ico36_detail_title" /*TODO: REVIEW/RATING ICON*/,
  component: "won-review-picker",
  viewerComponent: "won-review-viewer",
  placeholder: "Review Text (optional)",
  rating: [
    { value: "1", label: "1 Star" },
    { value: "2", label: "2 Stars" },
    { value: "3", label: "3 Stars", default: true },
    { value: "4", label: "4 Stars" },
    { value: "5", label: "5 Stars" }, //TODO: change value to numbers
  ],
  messageEnabled: true,
  parseToRDF: function({ value, identifier, contentUri }) {
    if (!value || !value.rating) {
      return { "s:review": undefined };
    }

    const randomString = generateIdString(10);

    return {
      "s:review": {
        "@type": "s:Review",
        "@id":
          contentUri && identifier
            ? contentUri + "/" + identifier + "/" + randomString
            : undefined,
        "s:about":
          "TODO: here should be the identityUri that the review refers to", //TODO: use identity uri
        "s:author":
          "TODO:  here should be the identityUri of the review-author", //TODO: use identity uri
        "s:reviewRating": {
          "@type": "s:Rating",
          "@id":
            contentUri && identifier
              ? contentUri +
                "/" +
                identifier +
                "/" +
                randomString +
                "/" +
                generateIdString(10)
              : undefined,
          "s:bestRating": [{ "@value": 5, "@type": "xsd:int" }], //not necessary but possible
          "s:ratingValue": [{ "@value": value.rating, "@type": "xsd:int" }],
          "s:worstRating": [{ "@value": 1, "@type": "xsd:int" }], //not necessary but possible
        },
        "s:description": value.text,
      },
    };
  },
  parseFromRDF: function(jsonLDImm) {
    const text = won.parseFrom(
      jsonLDImm,
      ["s:review", "s:description"],
      "xsd:string"
    );

    const rating = won.parseFrom(
      jsonLDImm,
      ["s:review", "s:reviewRating", "s:ratingValue"],
      "xsd:string"
    );

    //TODO: EXTRACT AUTHOR AND ABOUT URI

    if (!rating) {
      return undefined;
    } else {
      return { text: text, rating: rating };
    }
  },
  generateHumanReadable: function({ value, includeLabel }) {
    if (value) {
      //TODO: EXTRACT AUTHOR AND ABOUT URI
      const text = value.text;

      let ratingLabel = undefined;

      this.rating &&
        this.rating.forEach(rating => {
          if (rating.value === value.rating) {
            ratingLabel = rating.label;
          }
        });
      ratingLabel = ratingLabel || value.rating;

      return (
        (includeLabel ? this.label + ": " + ratingLabel : ratingLabel) +
        (text ? " - " + text : "")
      );
    }
    return undefined;
  },
};
