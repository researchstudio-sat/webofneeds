# Reputation System Design

## Rating Format

Since we want to support interactions between Needs from arbitrary domains we do not want to restrict the rating format upfront. 
For instance there could be ratings with single dimension or multiple dimensions, discrete or floating number scales or also including text descriptions depending on use case. Rating scheme and ontology should be negotiable between the partners of a transaction. This means that there will exists (and dynamically change) a vast amount of different rating formats in the Web of Needs Network that have to be handled in different ways. 

Examples for rating ontologies that can be used:

## Reputation Profiles

We intend to have reputation profiles in our system that are independet of the Needs (public keys) or the Need Owners (public keys). The user should be able to use different reputation profile pseudonyms with different owners or for different purposes. 
=> TODO: WHY?

## Architecture Options

One of the major design choices of the reputation systems architecture is who should be able to access transaction data which is the basis for calculating a rating or reputation. We see two alternatives: either making rating data public and accessable for everybody or making rating data private and only accessible for the owner and other partners and services which are trusted. 

### Public Rating Data

### Private Rating Data 

## Counter-Measures for Attacks and Manipulation
