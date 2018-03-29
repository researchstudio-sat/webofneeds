package won.protocol.agreement.effect;

public enum ProposalType {
	PROPOSES, CANCELS, PROPOSES_AND_CANCELS, 
	// only needed so we can return a type for a proposal that does neither (which is unexpected)
	NONE;
}
