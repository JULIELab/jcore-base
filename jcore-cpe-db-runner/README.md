# JCoRe CPE Database Runner

The JCoRe CPE DB Runner is used for reading documents from a PostgreSQL database that has tables created by the [JeDIS](https://github.com/JULIELab/jedis) project. The runner has `StatusCallbackListener` that tracks the processing and logs processing status changes of documents into the database subset table which is read. It sets documents to have finished processing or to have errors.

Thus, this component is not a CollectionReader, AnalysisEngine or Consumer but a meta-component that runs a whole CPE (Collection Processing Engine) given by its descriptor. This is typically done by calling the main class `de.julielab.jcore.cpe.DBCPERunner` from the command line where all component dependencies must reside on the classpath. Thus, this project is most commonly used as a dependency in pipeline projects.
A prerequisite for CPEs to be run with this runner is that they need to use a CollectionReader that extends the [jcore-db-reader](https://github.com/JULIELab/jcore-base/tree/master/jcore-db-reader). The JeDIS configuration will be taken from the reader to establish a database connection for status updates to the subset table.

## Running CPEs

To run a CPE file call the runner like this
```
export CLASSPATH=<component JARs and classes>
java de.julielab.jcore.cpe.DBCPERunner <parameters>
```
    
The following parameters are possible

| parameter name | description | mandatory |
|----------------|-------------|-----------|
| d | Path to the descriptor file | true |
| n | The maximum number of documents to process by the started process. | false |
| t | The number of threads to use for processing. This overrides the respective setting in the CPE. The user needs to take care that the CAS pool is defined large enough in the CPE descriptor or with the `a` parameter. | false |
| a | The size of the CAS pool to use. The pool should be three to four times the number of processing threads, depending on the number of components in the CPE and how quickly the reader can populate CAS instances. A lack of CAS objects might cause a few threads being stuck on a complicated or large document and the other threads running dry on CASes, lowering throughput. |
| b | Sets the CPE batch size. | false | 
