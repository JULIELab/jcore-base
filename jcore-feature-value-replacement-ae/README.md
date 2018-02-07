# JCoRe Feature Value Replacement AE

**Descriptor Path**:
```
de.julielab.jcore.ae.fvr.desc.jcore-feature-value-replacement-ae
```

### Objective
This analysis engine simply replaces feature values of feature structures with other values as defined by an external resource, e.g. file. For instance, all NCBI Gene IDs could be mapped to Ensemble IDs.

### Requirements and Dependencies
Nothing special.

### Using the CR - Descriptor Configuration

**1. Parameters**

| Parameter Name | Parameter Type | Mandatory | Multivalued | Description |
|----------------|----------------|-----------|-------------|-------------|
| FeaturePaths | String | true | true | An array of type-featurePath pairs of the form <qualified type>=<feature path>[?defaultValue]. Each value pointed to by the feature path of the annotations of the respective type will be replaced according to the replacement map given as an external resource with key `Replacements`. If a defaultValue is specified, feature values not contained in the map will be mapped to this defaultValue. The string `null` means the null-reference. |


**2. Capabilities**
The component is independent of specific types.


### Reference
None.
