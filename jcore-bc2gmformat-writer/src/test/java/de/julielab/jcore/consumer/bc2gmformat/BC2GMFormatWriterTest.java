
package de.julielab.jcore.consumer.bc2gmformat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


import org.apache.uima.fit.factory.UimaContextFactory;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.TreeMap;


/**
 * Unit tests for jcore-bc2gmformat-writer.
 *
 */
public class BC2GMFormatWriterTest{
    @Test
    public void testBuildMap() throws Exception {
        BC2GMFormatWriter writer = new BC2GMFormatWriter();
        writer.initialize(UimaContextFactory.createUimaContext(BC2GMFormatWriter.PARAM_GENE_FILE, "dummy1",
                BC2GMFormatWriter.PARAM_OUTPUT_DIR, "src/test/resources", BC2GMFormatWriter.PARAM_SENTENCES_FILE, "dummy3"));
        Method m = writer.getClass().getDeclaredMethod("buildWSMap", String.class);
        m.setAccessible(true);
        // we will build a map that knows for each position in the string how many whitespaces there have been until there
        String string = "This is a test string.";
        @SuppressWarnings("unchecked")
        TreeMap<Integer, Integer> wsMap = (TreeMap<Integer, Integer>) m.invoke(writer, string);
        assertNotNull(wsMap);
        assertEquals(Integer.valueOf(0), wsMap.floorEntry(1).getValue());
        assertEquals(Integer.valueOf(0), wsMap.floorEntry(4).getValue());
        assertEquals(Integer.valueOf(2), wsMap.floorEntry(8).getValue());
        assertEquals(Integer.valueOf(2), wsMap.floorEntry(9).getValue());
        assertEquals(Integer.valueOf(2), wsMap.floorEntry(9).getValue());
        assertEquals(Integer.valueOf(4), wsMap.floorEntry(15).getValue());
    }
}
