package de.julielab.jules.ae.genemapper.svm;

import java.io.Serializable;

/**
 * <pre>
 * options:
-s svm_type : set type of SVM (default 0)
	0 -- C-SVC
	1 -- nu-SVC
	2 -- one-class SVM
	3 -- epsilon-SVR
	4 -- nu-SVR
-t kernel_type : set type of kernel function (default 2)
	0 -- linear: u'*v
	1 -- polynomial: (gamma*u'*v + coef0)^degree
	2 -- radial basis function: exp(-gamma*|u-v|^2)
	3 -- sigmoid: tanh(gamma*u'*v + coef0)
-d degree : set degree in kernel function (default 3)
-g gamma : set gamma in kernel function (default 1/num_features)
-r coef0 : set coef0 in kernel function (default 0)
-c cost : set the parameter C of C-SVC, epsilon-SVR, and nu-SVR (default 1)
-n nu : set the parameter nu of nu-SVC, one-class SVM, and nu-SVR (default 0.5)
-p epsilon : set the epsilon in loss function of epsilon-SVR (default 0.1)
-m cachesize : set cache memory size in MB (default 100)
-e epsilon : set tolerance of termination criterion (default 0.001)
-h shrinking: whether to use the shrinking heuristics, 0 or 1 (default 1)
-b probability_estimates: whether to train a SVC or SVR model for probability estimates, 0 or 1 (default 0)
-wi weight: set the parameter C of class i to weight*C, for C-SVC (default 1)

The k in the -g option means the number of attributes in the input data.
 * </pre>
 * 
 * @author faessler
 *
 */
public class SVMTrainOptions implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1500754681335475768L;
	public boolean rangeScaleFeatures;
	public boolean centerFeatures;
	public boolean standardizeFeatures;
	public boolean copyData;
	/**
	 * <pre>
	 * svm_type : set type of SVM (default 0)
	0 -- C-SVC
	1 -- nu-SVC
	2 -- one-class SVM
	3 -- epsilon-SVR
	4 -- nu-SVR
	 * </pre>
	 */
	public int svmType;
	public double C;
	/**
	 * <pre>
	 * kernel_type : set type of kernel function (default 2)
	0 -- linear: u'*v
	1 -- polynomial: (gamma*u'*v + coef0)^degree
	2 -- radial basis function: exp(-gamma*|u-v|^2)
	3 -- sigmoid: tanh(gamma*u'*v + coef0)
	 * </pre>
	 */
	public int kernelType;
	public double svmGamma;
	public double coef0;
	public int svmDegree;
	public double cacheSize = 512;
	public double eps = 0.001;
	public boolean shrinking;
	public boolean propability;
}
