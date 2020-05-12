# JCoRe Descriptor Creator

This tool employs UIMAfit to create component descriptors. Thus, the relevant components must be annotated with UIMAfit annotations to take full advantage of the automatic descriptor generation.

## Running the tool

To run this tool, it must be on the classpath of the component project for which the descriptor should be built. for this, the tool can be used as a Maven dependency. The `jcore-parent` POM sets appropriate values in its `dependencyManagement` section.
If the tool is a project in the same Eclipse workspace as the component in question, a run configuration for the `DependencyCreator` can be used that sets the component project root as its working directory.
Descriptors will be created in `src/main/resources` in subdirectories matching the package name of the component class.

## Import Notes

### Descriptor settings

The created descriptors will be a good start but not perfect. You should check

- Parameters and their default values
- The description
- The capabilities
- The type system

The last point is crucial: UIMAfit will define the whole type system right within the descriptor which mustn't stay this way! Use imports to the JCoRe type system parts actually used by the component. Never define a type in a component that is also used by other components! Add it to the type system which is distributed via the Maven dependency mechanism, avoiding conflicts.

### Multiple descriptor creation

The tool will scan the classpath for all component classes, i.e. also Maven dependencies. At times, this will cause multiple descriptors being created because multiple component classes reside on the classpath. Then, you must manually check which is the right one. That will often be clear from the package name. You will potentially have to remove an index number from the descriptor file name.
