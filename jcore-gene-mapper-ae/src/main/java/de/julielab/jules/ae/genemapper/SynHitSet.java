package de.julielab.jules.ae.genemapper;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

public class SynHitSet extends TreeSet<SynHit> {
	
	public SynHitSet() {
		super(new Comparator<SynHit>() {
			public int compare(SynHit s1, SynHit s2) {
				return s1.getId().compareTo(s2.getId());
			}}
		);
	}

	public SynHitSet(Collection<SynHit> synHits) {
		super(synHits);
	}

	private static final long serialVersionUID = -3948116045932952171L;
	
	public boolean retainAll(Set<SynHit> s) {
		boolean hasChanged = false;
		
		for (Iterator<SynHit> it = this.iterator(); it.hasNext();) {
			if (!s.contains(it.next())) {
				it.remove();
				hasChanged = true;
			}
		}
		
		return hasChanged;
	}

	public boolean containsAny(Collection<SynHit> s) {
		boolean containsAny = false;
		
		for (Iterator<SynHit> it = s.iterator(); it.hasNext();) {
			if (this.contains(it.next())) {
				containsAny = true;
				break;
			}
		}

		return containsAny;
	}

}
