# Reputation System Design

## Transactions 

Needs can have an interaction with each other and at some point engage in a transaction. For instance this could be initiated by an agreement between the Needs. Ratings could be applied to these transactions if the Need owners agree on that. So after the transaction completed and a rating process was agreed on before, the ratings by the participants can be submitted. The system has to make sure that the submited ratings are counted for all participants even though the rating turns out to be bad for one or both sides. 

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

### Public Rating Data Approach

One idea is to make rating data public to everyone in the network. For every transaction the interacting Need owners would decide and agree on what rating data about the transaction they would like to make public after the transaction has been completed and the ratings submitted. The public rating data of a transaction could be created by a trusted service mentioned earlier to certify transactions. There would not only be different rating formats but also be different amounts of rating data available for all the connections/transactions in the network. 

Note that if the participants of a transaction would decide on making no rating data public they could probably also not trust the counterpart for the current transaction since the incentive (fear of bad rating) for correct behavior is missing. This means the amount of agreed rating data to make public for this transaction determines the trust in the transaction partners.

Benefits:

* Reputation of a potential transaction partner can be computed by everybody and verified by everybody
* This means rating or reputation services would have only reduced power compared to a system where only selected services can do this calculation
* Simple system architecture: there is basically no protocol for rating service interaction since rating data can just be crawled by everyone
* Reputation can be computed by arbitrary and possibly competing algorithms (including algorithms that take the whole transaction/rating graph into account, e.g. PageRank)

Drawbacks/Risks:

* Anonymity: reputation profiles would accumulate transaction/rating information over time and therefore get traceable and more linkable to the real users behind it. To reduce this danger either the amount of publicly available transaction/rating data would have to be reduced or the reputation profiles would have to be dumped at some time when they accomulated too much information. Both options however would reduce the trust or confidence in reputation profiles and self-defeating its purpose in a way.

=> Therefore in a system where anonymity is important we discard this approach

### Simple Rating Service Approach

Another approach is that rating service providers store the reputation based on transaction data for the users. Every participant could use its own reputation provider. Before transaction it has to be checked that participants accept the reputation provider of their counterpart. If that is the case they can query the reputation (if its not already included in the need description) of their counterpart. If the reputation is sufficient they can start a transaction and afterwards provide ratings to the reputation provider of their counterpart to update their counterparts rating profile.

Benefits:

* Simple solution 
* Rating provider shows just enough reputation data to other users that is needed for a certain transaction
* Rating service providers (and transactions counterpart provider) can be choosen by participants

Drawbacks/Risks:

* Reputation providers can become powerful since they provide central services
* Reputation providers have to be trusted to handle reputation data correctly (not pass/sell user data without agreement of users, count all ratings according to a transparent algorithm, not drop some ratings cause they might be bad for a user, etc.)
* In case of change of reputation provider all transaction data has to be transfered (trustful cooperation between providers)
* Incompatibility of which reputation providers are accepted by participants of a potential transaction

### Privacy-enhanced Rating Service Approach

