# Reputation System Design

## Reputation Profiles

We intend to have reputation profiles in our system that are independent of the Atoms (public keys) or the Atom Owners (public keys). The user should be able to use different reputation profile pseudonyms with different owners or for different purposes. 

## Transactions 

Atoms can have an interaction with each other and at some point engage in a transaction. For instance this could be initiated by an agreement between the Atoms. Ratings could be applied to these transactions if the Atom owners agree on that. So after the transaction is completed and a rating process was agreed on before, the ratings by the participants can be submitted. The system has to make sure that the submitted ratings are counted for all participants even though the rating turns out to be bad for one or both sides. 

## Certify Transactions

To avoid fake transactions that would enable fake ratings there should be ways of validating a transaction. This could be done by trusted third party services that could have a look into (some data of) the connection between the Atoms and check if the transaction is a valid one. For instance it could verify that there was a real transfer of money between the two partners. The trusted service could then create a certificate about the validity of the transaction. The trusted party could be paid for its service. 

Possible certifiable parameters of transactions:
* Date
* Amount of money exchanged
* Domain or context
* Certification authority
* Certification fee
* Other certificates (payment, postal, insurance, etc.)

In addition to the transaction certification parameters for the ratings additional parameters are added:
* Rating
* Reputation profiles/pseudonyms of participants

## Rating Format and Reputation Representation 

Since we want to support interactions between Atoms from arbitrary domains we do not want to restrict the rating format upfront. 
For instance, there could be ratings with single dimension or multiple dimensions, discrete or floating number scales or also including text descriptions depending on use case. Also reputation that is based on these ratings are bound to the context or domain the transactions happened in.

Rating scheme and ontology should be negotiable between the partners of a transaction. Alternatively or in combination with that we could use general purpose context-aware reputation ontologies. This means that there might be a vast amount of different and dynamically changing rating formats in the Web of Needs network that have to be handled in different ways. 

Examples for rating/reputation ontologies that could be used:

* [Alnemr, R., Schnjakin, M., & Meinel, C. (2011, November). Towards context-aware service-oriented semantic reputation framework. In Trust, Security and Privacy in Computing and Communications (TrustCom), 2011 IEEE 10th International Conference on (pp. 362-372). IEEE.](https://www.researchgate.net/profile/Rehab_Alnemr/publication/254017612_Towards_Context-Aware_Service-Oriented_Semantic_Reputation_Framework/links/54bd10330cf218da9390ec02.pdf)

## Reputation Computation

There are different ways of calculating an aggregated reputation "score" out of all relevant transaction ratings of a certain profile. Many of them (e.g. summation, average, weighted models, decay of ratings over time, ect.) are described in:
* [Jøsang, A., Ismail, R., & Boyd, C. (2007). A survey of trust and reputation systems for online service provision. Decision support systems, 43(2), 618-644.](http://eprints.qut.edu.au/7280/1/7280.pdf) 


For this architecture document the actual computation of the reputation is not a primary focus since we imagine that in this decentralized system different reputation providers will develop different calculation methods. However, the architecture does have some influence on what can be computed and what cannot (e.g. if all rating data is public the whole graph of ratings of all profiles can be used, in case the rating data is not public and there is not only one centralised reputation provider only a local view on single profiles can be used for the calculation). 

## Architecture Options

A major design choice of the reputation systems' architecture is to define who should be able to access the transaction data that is the basis for calculating a rating or reputation. Furthermore the system could be centralized or decentralized. 
We will start describing an approach making rating data public and accessable for everybody. Subsequently, we will descibe different approaches with more or less privacy for rating data (centralized and dezentralized). 

### Public Rating Data Approach

One idea is to make rating data public to everyone in the network. For every transaction the interacting Atom owners would decide and agree on what rating data about the transaction they would like to make public after the transaction has been completed and the ratings submitted. The public rating data of a transaction could be created by a trusted service mentioned earlier to certify transactions. There would not only be different rating formats but also be different amounts of rating data available for all the connections/transactions in the network. 

Note that if the participants of a transaction decided publish no rating data at all, they could probably also not trust the counterpart for the current transaction since the incentive (fear of bad rating) for correct behavior would be missing. This means the amount of agreed rating data to make public for this transaction determines the trust in the transaction partners.

Benefits:

* Reputation of a potential transaction partner can be computed by everybody and verified by everybody
* This means rating or reputation services would have only reduced power compared to a system in which only selected services can do this calculation
* Simple system architecture: there is basically no protocol for rating service interaction since rating data can just be crawled by everyone
* Reputation can be computed by arbitrary and possibly competing algorithms (including algorithms that take the whole transaction/rating graph into account, e.g. PageRank)
* Having a global view on the rating graph means that there are more advanced approaches available to detect and react to attacks on the reputation system (e.g. fake ratings, discrimination, unfair ratings)

Drawbacks/Risks:

* Anonymity: reputation profiles would accumulate transaction/rating information over time and therefore get traceable and more linkable to the real users behind it. To reduce this effect either the amount of publicly available transaction/rating data would have to be reduced or the reputation profiles would have to be used only for a limited number of transactions. Both options however would reduce the trust or confidence in reputation profiles and self-defeating its purpose in a way.

=> If transaction history is not privacy sensitive in a certain domain this approach can be powerful and relatively easy to implement (especially since crawling of data has already be implemented for matching). However in a system/domain in which anonymity of the transaction history is important, this approach cannot be used.

### Centralized Reputation Service Approach

Instead of detailed public rating and transaction data there could be one centralized reputation service which manages the ratings and reputations for everyone. This service holds the rating data of the whole transaction graph of the whole network. Users can query the service for an aggregated reputation score of their potential counterpart in a transaction. If they decide to interact with the counterpart, they can afterwards provide ratings to the reputation service to update their counterparts rating profile. 

Benefits:

* Simple solution 
* Reputation provider shows just enough reputation data to other users that is needed for a certain transaction
* Control of profiles can be applied (e.g. can demand an ID of users before creating a reputation profile)

Drawbacks/Risks:

* A centralized service defeats purpose of decentralized WoN system architecture 
* A reputation service is extremly powerful
* A reputation service collects all transaction data of reputation pseudonyms
* A reputation service has to be trusted by everyone to handle reputation data correctly (not pass/sell user data without agreement of users, count all ratings according to a transparent algorithm, not drop some ratings cause they might be bad for a user, etc.)

=> Centralized Reputation Service Approach (in general) not useful for decentralized infrastructure

### Simple Decentralized Reputation Service Approach

Another approach is that reputation service providers store the reputation based on transaction data/ratings for the users. Every participant could use its own reputation provider. Before a transaction it has to be checked that participants accept the reputation provider of their counterpart. If that is the case they can query the reputation (if its not already included in the atom description) of their counterpart. If the reputation is sufficient they can start a transaction and afterwards provide ratings to the reputation provider of their counterpart to update their counterpart's rating profile.

Benefits:

* Simple solution 
* Reputation provider shows just enough reputation data to other users that is needed for a certain transaction
* Reputation service providers (and transactions counterpart provider) can be chosen by participants
* Control of profiles can be applied (e.g. can demand an ID of a users before creating a reputation profile)

Drawbacks/Risks:

* Reputation providers can become powerful since they provide central services
* Reputation provider collects all of a users reputation pseudonym's transaction data
* Reputation providers have to be trusted to handle reputation data correctly (not pass/sell user data without agreement of users, count all ratings according to a transparent algorithm, not drop some ratings cause they might be bad for a user, etc.)
* In case of change of reputation provider all transaction data has to be transferred (trustful cooperation between providers)
* Incompatibility of which reputation providers are accepted by participants of a potential transaction

=> Simple Decentralized Reputation Service Approach could be a starting point of implementing a first reputation concept but would also need some rework if its later changed to more advanced concepts like a privacy-enhancement (see next). 

### Privacy-enhanced Decentralized Reputation Service Approach

The problem with the previous approach is mostly that reputation service providers collect too much private user data and therefore have too much power. They must be trusted but cannot be effectively controlled from paying and interacting users. 

A rather advanced option to the privacy problem would be to let every user store and manage their own reputation profiles. The detailed transaction and the complete rating history ideally would not be revealed to anybody (not even the reputation services), just the aggregated rating computed based on it. Two options of implementing this with different complexity are described:

1) If the new aggregated reputation score could be computed by the old aggregated reputation score and the new transaction rating (e.g. if the aggregation value is just a counter of the positive and negative ratings) it could be passed to (possibly changing) reputation providers that update the aggregated reputation score sign it and pass it back to the user. This however is only possible if reputation scores can be computed incrementally and the complete rating history is not needed for every computation. These are simple general purpose reputation scores that would not be applicable for every domain or use case.
This Option could also be combined with reputation tokens like for example described in: [Pham, A., Dacosta, I., Jacot-Guillarmod, B., Huguenin, K., Hajar, T., Tramèr, F., ... & Hubaux, J. P. (2017). PrivateRide: A Privacy-Enhanced Ride-Hailing Service. Proceedings on Privacy Enhancing Technologies, 2017(2), 38-56.](https://www.degruyter.com/downloadpdf/j/popets.2017.2017.issue-2/popets-2017-0015/popets-2017-0015.pdf). This way the reputation provider cannot even track and link transactions to certain profiles and build up a transaction history. 

2) The rating services would be used to update and sign a transaction/rating history when a new transaction with a rating is available. The user would prove (zero knowledge proof) to the rating service that she has a valid signed transaction history and the service should blind sign (blind signature) the old history including the new transaction rating. The user could then use the newly signed rating history to compute his updated agreegated rating himself and publish it. A user who computes his aggregated rating himself would have to prove to other users that he used his signed and up-to-date transaction rating history and a certain rating aggregation algorithm for that purpose without revealing the details of this history to other users (zkSNARKS).

Benefits:
* privacy and trust of reputation profiles (towards other users and reputation services, basically invalidate all drawback arguments from the Simple Reputation Service Approach)

Drawbacks/Risks:
* Advanced complex concepts that have not been adopted widely yet (Option 2: e.g. zkSNARKS)
* Not yet clear how exactly this would be implemented (Option 2)

=> Option 1 could be implemented if some more privacy is required than in the "Simple Decentralized Reputation Service Approach" but is not a general purpose solution. Option 2 is quite advanced and unclear to implement currently
