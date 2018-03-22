package won.protocol.agreement.effect;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

public class Proposes extends MessageEffect {

	private Set<URI> proposes = new HashSet<>();
	private Set<URI> proposesToCancel = new HashSet<>();
	
	public Proposes(URI messageUri) {
		super(messageUri);
	}
	
	void addProposes(URI uri){
		proposes.add(uri);
	}
	
	void addProposesToCancel(URI uri) {
		proposesToCancel.add(uri);
	}

	public Set<URI> getProposes() {
		return proposes;
	}

	public Set<URI> getProposesToCancel() {
		return proposesToCancel;
	}

	@Override
	public String toString() {
		return "Proposes [proposes=" + proposes + ", proposesToCancel=" + proposesToCancel + "]";
	}
	
}
