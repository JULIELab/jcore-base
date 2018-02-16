# JCoRe BANNER AnalysisEngine

**Descriptor Path**:
```
de.julielab.jcore.ae.banner.desc.jcore-banner-ae
```

### Objective

BANNER is the gene tagger software by Leaman, R. & Gonzalez G [1]. This particular version has been copied from https://github.com/oaqa/banner and adapted for JCoRe.
To run the BANNER application for training and testing models, first perform a Maven build process by installing Maven, navigating to the jcore-banner-ae directory on the command line and type '$MAVEN_HOME/bin/mvn clean package'.
Then type 'java -cp "target/lib/*":target/classes banner.eval.BANNER'.

### Requirements and Dependencies

BANNER uses by default some PoS taggers delivered by the Dragon Toolkit. Since this toolkit is not available via Maven, a version with limited functionality has been added to JCoRe projects. Note that the MedPostagger won't work since the original Medpost dependency is also not available via Maven. The default Hepple PoS tagger, however, is contained in GATE which is used as a dependency in the JCoRe depenency's version of the Dragon Toolkit.

### Using the CR - Descriptor Configuration

**1. Parameters**

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| ConfigFile | String | true | false | A BANNER-compliant XML configuration file. |

**2. Predefined Settings**

None.

**3. Capabilities**

| Type | Input | Output |
|------|:-----:|:------:|
| de.julielab.jcore.types.Sentence | `+` | |
| de.julielab.jcore.types.Gene | | `+` |


### Reference
[1] Leaman, R. & Gonzalez G. (2008) BANNER: An executable survey of advances in biomedical named entity recognition. Pacific Symposium on Biocomputing 13:652-663(2008)
