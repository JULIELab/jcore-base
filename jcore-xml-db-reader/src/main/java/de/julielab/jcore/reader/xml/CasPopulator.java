package de.julielab.jcore.reader.xml;

import de.julielab.jcore.reader.xmlmapper.mapper.XMLMapper;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import de.julielab.xmlData.dataBase.DataBaseConnector;
public class CasPopulator {
private final static Logger LOGGER = LoggerFactory.getLogger(CasPopulator.class);
    protected static final byte[] comma = ",".getBytes();

    private final DataBaseConnector dbc;
    private final XMLMapper xmlMapper;
    private Row2CasMapper row2CasMapper;
    private String[] rowMappingArray;
    private BiConsumer<byte[][], JCas> dbProcessingMetaDataSetter;

    public CasPopulator(DataBaseConnector dbc, XMLMapper xmlMapper, Row2CasMapper row2CasMapper, String[] rowMappingArray) {
        this.dbc = dbc;
        this.xmlMapper = xmlMapper;
        this.row2CasMapper = row2CasMapper;
        this.rowMappingArray = rowMappingArray;
    }

    public void populateCas(JCas jcas, byte[][] arrayArray, BiConsumer<byte[][], JCas> dbProcessingMetaDataSetter) throws CasPopulationException {
        List<Integer> pkIndices = dbc.getPrimaryKeyIndices();

        // get index of xmlData;
        // assumes that only one byte[] in arrayArray contains this data
        // and that this byte[] is at the only index position that holds no
        // primary key
        List<Integer> allIndices = new ArrayList<Integer>();
        for (int i = 0; i < arrayArray.length; i++) {
            allIndices.add(i);
        }
        List<Integer> xmlIndices = new ArrayList<>(allIndices);
        for (Integer pkIndex : pkIndices)
            xmlIndices.remove(pkIndex);
        int xmlIndex = xmlIndices.get(0);

        ArrayList<byte[]> primaryKey = new ArrayList<byte[]>();
        int lengthIdentifier = pkIndices.size() - 1;
        for (Integer index : pkIndices) {
            byte[] pkElementValue = arrayArray[index];
            primaryKey.add(pkElementValue);
            lengthIdentifier = lengthIdentifier + pkElementValue.length;
        }

        // build byte[] identifier out of primary key values, separated by
        // comma;
        // this identifier is used for method parse() in XMLMapper
        byte[] identifier = new byte[lengthIdentifier];
        int currentPosition = 0;
        for (int j = 0; j < primaryKey.size(); j++) {
            System.arraycopy(primaryKey.get(j), 0, identifier, currentPosition, primaryKey.get(j).length);
            currentPosition = currentPosition + primaryKey.get(j).length;
            if (j < primaryKey.size() - 1) {
                System.arraycopy(comma, 0, identifier, currentPosition, 1);
                currentPosition = currentPosition + 1;
            }
        }

        if (LOGGER.isDebugEnabled())
            LOGGER.debug("getNext(CAS), primaryKeyValue = {}", new String(identifier));
        try {
            xmlMapper.parse(arrayArray[xmlIndex], identifier, jcas);
            // Are there additional rows besides the primary key columns and the
            // document XML?
            if (arrayArray.length > (pkIndices.size() + 1)) {
                if (null == row2CasMapper || row2CasMapper.getRowMapping().size() < (xmlIndices.size() - 1)) {
                    throw new NullPointerException("There are elements in the returned array that are not"
                            + " mapped to UIMA type classes via the row mapping. Row mapping: " +
                            (row2CasMapper == null ? Arrays.toString(rowMappingArray) : row2CasMapper.getRowMapping()));
                } else {
                    row2CasMapper.mapRowToType(arrayArray, jcas);
                }
            }
            dbProcessingMetaDataSetter.accept(arrayArray, jcas);
        } catch (Exception e) {
            LOGGER.error("getNext(CAS), primaryKeyValue = " + new String(identifier), e);
            throw new CasPopulationException(e);
        } catch (Throwable e) {
            throw new CasPopulationException(e);
        }
    }
}
