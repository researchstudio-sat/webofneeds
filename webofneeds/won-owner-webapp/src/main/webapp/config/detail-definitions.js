// TODO: import detail-picker components here
// TODO: each detail picker should know it's own rdf template
// --> current goal: detail-pickers don't learn to speak RDF
// --> instead, each picker has a RDF template telling us how it would look
// --> and TODO: exposes a toRDF() method that converts whatever JSON the picker returns to RDF
// --> reasoning: we want JSON objects in the state, because RDF is harder to handle
// --> we want the need-reducer to work with arbitrary details, without required edits for each new detail
// --> so need-reducer can't be the part that knows how to convert a detail
// TODO: define RDF template *inside* the detail-pickers and link it here, so the linter doesn't complain
// --> alternatively, if we don't need a template, refer something else.

export const details = {
  description: {
    identifier: "description",
    label: "Description",
    icon: "#ico36_description_circle",
    component: "won-description-picker",
  },
  location: {
    identifier: "location",
    label: "Location",
    icon: "#ico36_location_circle",
    component: "won-location-picker",
  },
  person: {
    identifier: "person",
    label: "Person",
    icon: "#ico36_person_single_circle",
    component: "won-person-picker",
  },
  route: {
    identifier: "travelAction",
    label: "Route (From - To)",
    icon: "#ico36_location_circle",
    component: "won-route-picker",
  },
  tags: {
    identifier: "tags",
    label: "Tags",
    icon: "#ico36_tags_circle",
    component: "won-tags-picker",
  },
  ttl: {
    identifier: "ttl",
    label: "Turtle (TTL)",
    icon: "#ico36_rdf_logo_circle",
    component: "won-ttl-picker",
  },
};

export const detailList = details.map(detail => detail.identifier);
