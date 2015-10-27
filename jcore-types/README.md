# JCoRe Types
Annotation system with an extensive semantic layer

This is the JCoRe UIMA type system. It offers a broad range of types to use in the context of various text analytics tasks such as basic annotations levels like sentences, tokens, abbreviation, parts-of-speech etc. as well as semantic types for the biomedical domain and document structure types like title, abstract, paragraph etc.

The usage of the type system in a runtime environment is first to create the actual java classes from the type system by using "jcasgen" on the type system descriptors. You may use the type system descriptor "julie-all-types.xml" which should always import all other descriptors to facilitate the process. Then, the project is built via "mvn package". The resulting JAR is to be put on the CLASSPATH of your application and the type system descriptors are referenced by name.

For more detailed information about the type system, please refer to the following sources:

Udo Hahn, Ekaterina Buyko, Katrin Tomanek, Scott Piao, Yoshimasa Tsuruoka, John McNaught, Sophia Ananiadou. An UIMA Annotation Type System for a Generic Text Mining Architecture. UIMA-Workshop, GLDV Conference, April 2007.

Udo Hahn, Ekaterina Buyko, Katrin Tomanek, Scott Piao, John McNaught, Yoshimasa Tsuruoka, Sophia Ananiadou. An Annotation Type System for a Data-Driven NLP Pipeline. The Linguistic Annotation Workshop (LAW) of ACL 2007 to be held in Prague, Czech Republic, June 28-29, 2007.
