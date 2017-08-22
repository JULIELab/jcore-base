All required resources - i.e. Linnaeus configuration, dictionary, frequency
statistics etc. - are packaged in a project of their own. This is done
because there are multiple, different dictionaries available, e.g. for pure
species name mentions or additionally for species hints (like "patient" hints
for a human).
Each such resource project brings with it a pre-configured analysis engine
descriptor that uses the resource of the respective project. Since the
resources are stored in distinct packages, different configurations may
be used in the same pipeline without resource collision problems.
However, this would cause a lot of duplicate annotations since there is no
merging algorithm.