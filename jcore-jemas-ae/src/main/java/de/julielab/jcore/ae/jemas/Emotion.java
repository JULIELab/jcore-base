package de.julielab.jcore.ae.jemas;

public class Emotion {

	public double[] vector;

	public Emotion(double Valence, double Arousal, double Dominance) {
		vector = new double[] { Valence, Arousal, Dominance };
	}

	public double getValence() {
		return vector[0];
	}

	public double getArousal() {
		return vector[1];
	}

	public double getDominance() {
		return vector[2];
	}

	public void add(Emotion addedEmotion) {
		for (int i = 0; i < 3; i++) {
			this.vector[i] = this.vector[i] + addedEmotion.vector[i];
		}
	}

	public void multiply(double coefficient) {
		for (int i = 0; i < 3; i++) {
			this.vector[i] = this.vector[i] * coefficient;
		}
	}

	public void normalize(double param) {
		this.multiply(1 / param);
		
	}

	// public void setValence(double value) {
	// vector[0] = value;
	// }
	//
	// public void setArousal(double value) {
	// vector[1] = value;
	// }
	//
	// public void setDominance(double value) {
	// vector[2] = value;
	// }
	
	@Override
	public String toString() {
		return this.getValence() + "\t" + this.getArousal() + "\t" + this.getDominance();
	}
}
