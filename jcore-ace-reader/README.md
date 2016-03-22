# JCoRe ACE Collection Reader
A Collection Reader that converts ACE-XML ([Automatic Content Extraction](https://www.ldc.upenn.edu/collaborations/past-projects/ace)) files to CAS objects.

### Objective
The JULIE Lab ACE Reader is a UIMA Collection Reader (CR). It reads the English section of the ACE 2005 Multilingual Training Corpus data, which is given as XML files, and converts it to types defined in the UIMA type system that we provide as well.

### Requirements and Dependencies
The input files for the JULIE Lab ACE Reader can be purchased at the [Linguistic Data Consortium (LDC)](http://www.ldc.upenn.edu/). The output of the CR is in the form of annotation objects. The classes corresponding to these objects are part of our [JCoRe Type System](https://github.com/JULIELab/jcore-base/tree/master/jcore-types).

### Using the CR - Descriptor Configuration
In UIMA, each component is configured by a descriptor in XML. Such a preconfigured descriptor is available under `src/main/resources/de/julielab/jcore/reader/ace/desc` but it can be further edited if so desired; see [UIMA SDK User's Guide](https://uima.apache.org/downloads/releaseDocs/2.1.0-incubating/docs/html/tools/tools.html#ugr.tools.cde) for further information.

**1. Parameters**

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| inputDirectory | String | yes | no | Path to ACE files |
| generateJcoreTypes| Boolean | no | no | Determines if JULIE Lab Types (jcore-semantics-ace-types.xml) should be generated in addition to types from jcore-ace- types.xml |

**2. Predefined Settings**

| Parameter Name | Parameter Syntax | Example |
|----------------|------------------|---------|
| inputDirectory | valid Path to the ACE files | `data/ACEData` |
| generateJcoreTypes| boolean Variable | `true` |

**3. Capabilities**

| Type | Input | Output |
|------|:-----:|:------:|
| de.julielab.jcore.types.ArgumentMention |  | `+` |
| de.julielab.jcore.types.ace.EntityMention |  | `+` |
| de.julielab.jcore.types.ace.EventMention |  | `+` |
| de.julielab.jcore.types.ace.EventMentionArgument |  | `+` |
| de.julielab.jcore.types.ace.RelationMention |  | `+` |
| de.julielab.jcore.types.ace.RelationMentionArgument |  | `+` |
| de.julielab.jcore.types.ace.Timex2 |  | `+` |
| de.julielab.jcore.types.ace.Timex2Mention |  | `+` |
| de.julielab.jcore.types.ace.Value |  | `+` |
| de.julielab.jcore.types.ace.ValueMention |  | `+` |

So if you want to use this CR out-of-the-box (as a Maven Dependency in another project or as Component in a CPE) make sure to either put the data in the predefined inputDirectory or change this parameter to your liking.

### Reference
George Doddington, Alexis Mitchell, Mark Przybocki, Lance A. Ramshaw, Stephanie Strassel, and Ralph M. Weischedel. 2004. The Automatic Content Extraction ACE Program: Tasks, data & evaluation. In *LREC 2004 –Proceedings of the 4th International Conference on Language Resources and Evaluation. In Memory of Antonio Zampolli. Lisbon, Portugal, 24-30 May, 2004,* volume 3, pages 837–840.
