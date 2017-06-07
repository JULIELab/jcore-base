# JCoRe XMI Writer

**Descriptor Path**:
```
de.julielab.jcore.consumer.xmi.desc.jcore-xmi-writer
```
### Objective
JULIE Lab CasToXmiConsumer is a consumer that writes the complete CAS to XMI. It is a complex wrapper around the UIMA inherent XmiCasSerializer, which is used to write out a CAS in an XML Metadata Interchange (XMI) format. This wrapper allows single or multiple files to be compressed into zip files.

### Requirements and Dependencies
The input of a CC is done via annotation objects. The classes corresponding to these objects are part of the [JCoRe Type System](https://github.com/JULIELab/jcore-base/tree/master/jcore-types). The output should be a valid XMI-file.

### Using the CC - Descriptor Configuration
In UIMA, each component is configured by a descriptor in XML. Such a preconfigured descriptor is available under `src/main/resources/de/julielab/jcore/consumer/xmi/desc` but it can be further edited if so desired; see [UIMA SDK User's Guide](https://uima.apache.org/downloads/releaseDocs/2.1.0-incubating/docs/html/tools/tools.html#ugr.tools.cde) for further information.

**1. Parameters**

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| outputDir | String | yes | yes | Path to an output directory |
| createBatchSubdirs| Boolean | no | no | If subdirectories should be created, default value is false |
| fileNameTypeName | String | yes | no | The name of the file name type |
| fileNameFeatureName| String | no | no | The name of the file name feature |
| compressSingle| Boolean | no | no | If the Xmi's should be compressed in one batch, default false |
| compress | Boolean | no | no | Only plays a role if compresssSingle is false. Decides whether the Xmi should be compressed with gzip (multiple files compression), default false |



**2. Predefined Settings**

| Parameter Name | Parameter Syntax | Example |
|----------------|------------------|---------|
| outputDir | Valid Path to an output directory | `data/XMIOutputData` |
| createBatchSubdirs | Boolean | `true` |
| fileNameTypeName | A valid name of the file name type| `de.julielab.jcore.types.Header` |
| fileNameFeatureName | A valid name of the file name feature | `source` |
| compressSingle | Boolean | `false` |
| compress | Boolean | `false` |



**3. Capabilities**

| Type | Input | Output |
|------|:-----:|:------:|
| de.julielab.jcore.types.Header | `+` |  |


### Reference
