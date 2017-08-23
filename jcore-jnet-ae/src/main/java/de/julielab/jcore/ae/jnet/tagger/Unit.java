/**
BSD 2-Clause License

Copyright (c) 2017, JULIE Lab
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
**/
package de.julielab.jcore.ae.jnet.tagger;

import java.util.HashMap;
import java.util.Set;

public class Unit {

	public int begin;

	public int end;

	private String rep;

	private String label = null;

	private HashMap<String, String> metaInfo = null;

	private double confidence = -1;

	public Unit(final int begin, final int end, final String rep,
			final String label, final HashMap<String, String> metas) {
		this(begin, end, rep, label);
		metaInfo.putAll(metas);
	}

	public Unit(final int begin, final int end, final String rep,
			final String label) {
		this(begin, end, rep);
		this.label = label;
	}

	public Unit(final int begin, final int end, final String rep) {
		this.begin = begin;
		this.end = end;
		this.rep = rep;
		label = "";
		metaInfo = new HashMap<String, String>();
	}

	@Override
	public String toString() {
		String ret = rep + ": " + begin + "-" + end + "(" + label + ")";
		final Set<String> keySet = metaInfo.keySet();
		for (final Object key : keySet)
			ret += ", " + (String) key + ": " + metaInfo.get(key);
		return ret;
	}

	public String getMetaInfo(final String key) {
		return metaInfo.get(key);
	}

	public String getRep() {
		return rep;
	}

	public String getLabel() {
		return label;
	}

	public double getConfidence() {
		return confidence;
	}

	public void setRep(final String rep) {
		this.rep = rep;
	}

	public void setLabel(final String label) {
		this.label = label;
	}

	public void setConfidence(final double conf) {
		confidence = conf;
	}
}
