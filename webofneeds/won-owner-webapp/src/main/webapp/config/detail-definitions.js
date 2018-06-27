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
