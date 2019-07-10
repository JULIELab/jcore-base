package de.julielab.jcore.reader.xmi;

import de.julielab.costosys.cli.TableNotFoundException;
import de.julielab.costosys.configuration.FieldConfig;
import de.julielab.costosys.dbconnection.DataBaseConnector;
import de.julielab.costosys.dbconnection.util.CoStoSysSQLRuntimeException;
import de.julielab.costosys.dbconnection.util.TableSchemaMismatchException;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class XmiReaderUtils {
    private final static Logger log = LoggerFactory.getLogger(XmiReaderUtils.class);

    /**
     * Checks if the data table that is read directly or through a subset matches the XMI table schema.
     * @param dbc The DBC connected to the correct database.
     * @param tableName The table name parameter value given to the reader component.
     * @param xmiDocumentTableSchema The XMI document table schema as generated by {@link DataBaseConnector#addXmiTextFieldConfiguration(List, List, boolean)}
     * @param componentName The name of the reader component, needed for logging. Should be <code>getMetaData().getName()</code>, executed in the reader component class.
     * @throws ResourceInitializationException If the table is no XMI table or SQL errors happen.
     */
    public static void checkXmiTableSchema(DataBaseConnector dbc, String tableName, FieldConfig xmiDocumentTableSchema, String componentName) throws ResourceInitializationException {
        String dataTable = null;
        try {
            dataTable = dbc.getNextOrThisDataTable(tableName);
            dbc.checkTableHasSchemaColumns(dataTable, xmiDocumentTableSchema.getName());
        } catch (CoStoSysSQLRuntimeException | TableNotFoundException e) {
            throw new ResourceInitializationException(e);
        } catch (TableSchemaMismatchException e) {
            try {
                String error;
                if (dbc.isDataTable(tableName))
                    error = String.format("The table \"%s\" specified to read for the %s does not match the " +
                            "XMI text storage data schema. Either the DoGzip parameter does not match the setting that " +
                            "was used for the XMI DB Consumer or the specified table is not an XMI table.", tableName, componentName);
                else
                    error = String.format("The subset table \"%s\" specified to read for the %s " +
                            "references the data table \"%s\". This data table does not match the " +
                            "XMI text storage data schema. Either the DoGzip parameter does not match the setting that " +
                            "was used for the XMI DB Consumer or the specified table is not an XMI table.", tableName, componentName, dataTable);
                log.error(error);
                throw new ResourceInitializationException(new TableSchemaMismatchException(error, e));
            } catch (CoStoSysSQLRuntimeException e1) {
                throw new ResourceInitializationException(e1);
            }
        }
    }
}
