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
package de.julielab.jcore.ae.jsbd;

import java.util.TreeSet;

public class AbbreviationsBiomed {

	public TreeSet<String> abbr;

	public AbbreviationsBiomed() {
		init();
	}

	private void init() {
		abbr = new TreeSet<String>();

		abbr.add("cv.");
		abbr.add("(approx.");
		abbr.add("approx.");
		abbr.add("Dr.");
		abbr.add("(e.g.");
		abbr.add("e.g.");
		abbr.add("(i.e.");
		abbr.add("i.e.");
		abbr.add("sp.");
		abbr.add("spp.");
		abbr.add("pmol.");
		abbr.add("Biol.");
		abbr.add("Biosci.");
		abbr.add("Biotech.");
		abbr.add("Rev.");
		abbr.add("Heynh.");
		abbr.add("vs.");
		abbr.add("subsp.");
		abbr.add("Ltd.");
		abbr.add("etc.");
		abbr.add("mol.");
		abbr.add("viz.");
		abbr.add("St.");
		abbr.add("wt.");
		abbr.add("ca.");
		abbr.add("s.c.");
		abbr.add("i.v.");
		abbr.add("Molec.");
		abbr.add("Ed.");
		abbr.add("Eds.");

	}

	public TreeSet<String> getSet() {
		return abbr;
	}
}
