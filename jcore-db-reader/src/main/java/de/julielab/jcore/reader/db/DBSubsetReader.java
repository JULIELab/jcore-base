package de.julielab.jcore.reader.db;

import org.apache.uima.UimaContext;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;

public abstract class DBSubsetReader extends DBReaderBase {
    /**
     * Boolean parameter. Indicates whether the read subset table is to be reset
     * before reading.
     */
    public static final String PARAM_RESET_TABLE = "ResetTable";
    private final static Logger log = LoggerFactory.getLogger(DBSubsetReader.class);
    @ConfigurationParameter(name = PARAM_RESET_TABLE, defaultValue = "false", mandatory = false)
    protected Boolean resetTable;
    @ConfigurationParameter(name = PARAM_TIMESTAMP, mandatory = false)
    protected String timestamp;
    protected String hostName;
    protected String pid;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);

        hostName = getHostName();
        pid = getPID();

        timestamp = (String) getConfigParameterValue(PARAM_TIMESTAMP);
        resetTable = (Boolean) getConfigParameterValue(PARAM_RESET_TABLE);
        if (resetTable == null)
            resetTable = false;

        logConfigurationState();
    }

    private void logConfigurationState() {
        log.info("Subset table {} will be reset upon pipeline start: {}", tableName, resetTable);
    }

    private String getPID() {
        String id = ManagementFactory.getRuntimeMXBean().getName();
        return id.substring(0, id.indexOf('@'));
    }

    private String getHostName() {
        InetAddress address;
        String hostName;
        try {
            address = InetAddress.getLocalHost();
            hostName = address.getHostName();
        } catch (UnknownHostException e) {
            throw new IllegalStateException(e);
        }
        return hostName;
    }
}
