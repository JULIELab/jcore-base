package de.julielab.jcore.reader.xmi;

import de.julielab.jcore.reader.db.DBMultiplierReader;
import de.julielab.jcore.reader.db.SubsetReaderConstants;
import de.julielab.jcore.reader.db.TableReaderConstants;
import de.julielab.xmlData.config.FieldConfig;
import de.julielab.xmlData.dataBase.DataBaseConnector;
import org.apache.uima.UimaContext;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@ResourceMetaData(name = "XMI Database Multiplier Reader", description = "This is a convenience extension of the " +
        "DBMultiplierReader that is to be used for reading of XMI data that is segmented and stored into multiple " +
        "tables. The table schema")
public class XmiDBMultiplierReader extends DBMultiplierReader {

    public static final String PARAM_DO_GZIP = "DoGzip";
    @ConfigurationParameter(name = PARAM_DO_GZIP, description = "If set to true, the XMI schema will specify the XMI " +
            "column as to be compressed. Then, the XMI data will be stored in GZIP format in the database table.",
            mandatory = false, defaultValue = "false")
    private Boolean doGzip;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        doGzip = Optional.ofNullable((Boolean) context.getConfigParameterValue(PARAM_DO_GZIP)).orElse(false);
//        String[] additionalTables = (String[]) context.getConfigParameterValue(SubsetReaderConstants.PARAM_ANNOS_TO_STORE);
//        List<Map<String, String>> activeTableSchemaPrimaryKey;
//        if (additionalTables != null) {
//            // As a convenience and to avoid mistakes, we want to automatically fill the annotation table schemas into the
//            // additionalTableSchemas array. For this, we need to get a local DBC instance to get the correct field definition.
//            String costosysConfigPath = (String) context.getConfigParameterValue(TableReaderConstants.PARAM_COSTOSYS_CONFIG_NAME);
//            try {
//                dbc = new DataBaseConnector(costosysConfigPath);
//                activeTableSchemaPrimaryKey = dbc.getActiveTableFieldConfiguration().getPrimaryKeyFields().collect(Collectors.toList());
//                String xmiAnnotationSchemaName = dbc.addXmiAnnotationFieldConfiguration(activeTableSchemaPrimaryKey, doGzip).getName();
//                String[] additionalTableSchemas = new String[additionalTables.length];
//                Arrays.fill(additionalTableSchemas, xmiAnnotationSchemaName);
//                // We need to set the value to the meta data of the component because at the end of the method, we call
//                // super.initialize() which will then read the table schemas as parameters from the meta data
//                setConfigParameterValue(SubsetReaderConstants.PARAM_ADDITIONAL_TABLE_SCHEMAS, additionalTableSchemas);
//            } catch (FileNotFoundException e) {
//                throw new ResourceInitializationException(e);
//            }
//        }
        super.initialize(context);
//        activeTableSchemaPrimaryKey = dbc.getActiveTableFieldConfiguration().getPrimaryKeyFields().collect(Collectors.toList());
        // The data table that is read must be an XMI base document table.
//        schemas[0] = dbc.addXmiTextFieldConfiguration(activeTableSchemaPrimaryKey, doGzip).getName();
        schemas[0] = dbc.getActiveTableSchema();
    }
}
