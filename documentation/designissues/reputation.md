# Reputation System Design

## Transactions 

Needs can have an interaction with each other and at some point engage in a transaction. For instance this could be initiated by an agreement between the Needs. Ratings could be applied to these transactions if the Need owners agree on that. 

## Certify Transactions

To avoid fake transactions in order to avoid fake ratings there should be ways of validating a transaction. This could be done by trusted third party services that could have a look into (some data of) the connection between the Needs and check if the transaction is a valid one. For instance it could verify that there was a real transfer of money between the two partners. The trusted service could then create a certificate about the validity of the transaction, e.g. stating the amount, the date, domain of the transaction. The trusted party could be paid for its service. 

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

One idea is to make rating data public to everyone in the network. 

### Private Rating Data 

## Counter-Measures for Attacks and Manipulation
