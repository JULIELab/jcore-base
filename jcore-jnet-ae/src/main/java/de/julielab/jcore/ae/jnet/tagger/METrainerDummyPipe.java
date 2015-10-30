/** 
 * METrainerDummyPipe.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0 
 *
 * Author: tomanek
 * 
 * Current version: 1.0 	
 * Since version:   1.0
 *
 * Creation date: Dec 10, 2007 
 * 
 * this is a dummy pipe that actually doesn't do anything but 
 * is needed when sequence features are converted to token features
 **/

package de.julielab.jcore.ae.jnet.tagger;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Alphabet;
import cc.mallet.types.Instance;

public class METrainerDummyPipe extends Pipe {

	private static final long serialVersionUID = 1L;

	public METrainerDummyPipe(final Alphabet data, final Alphabet label) {

		super.setDataAlphabet(data);
		super.setTargetAlphabet(label);
	}

	@Override
	public Instance pipe(final Instance inst) {
		return inst;
	}

}
