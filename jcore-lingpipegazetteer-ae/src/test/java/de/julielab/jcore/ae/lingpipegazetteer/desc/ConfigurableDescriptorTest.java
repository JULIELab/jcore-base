package de.julielab.jcore.ae.lingpipegazetteer.desc;

import de.julielab.jcore.ae.lingpipegazetteer.chunking.ConfigurableChunkerProviderImplAlt;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.resource.ConfigurableDataResourceSpecifier;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;
import org.apache.uima.util.InvalidXMLException;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ConfigurableDescriptorTest {
    @Test
    public void testLoadDescriptor() throws IOException, InvalidXMLException {
        // Just a small test to ensure that the descriptor is correct.
        AnalysisEngineDescription desc = AnalysisEngineFactory.createEngineDescriptionFromPath(
                "src/main/resources/de/julielab/jcore/ae/lingpipegazetteer/desc/" +
                        "jcore-lingpipe-gazetteer-ae-configurable-resource.xml");
        assertNotNull(desc);
        ExternalResourceDescription[] externalResources = desc.getResourceManagerConfiguration().getExternalResources();
        assertEquals(1, externalResources.length);
        ResourceSpecifier resourceSpecifier = externalResources[0].getResourceSpecifier();
        assertTrue(resourceSpecifier instanceof ConfigurableDataResourceSpecifier);
        ConfigurableDataResourceSpecifier configSpec = (ConfigurableDataResourceSpecifier) resourceSpecifier;
        ConfigurationParameterSettings parameterSettings = configSpec.getMetaData().getConfigurationParameterSettings();
        assertEquals(true, parameterSettings.getParameterValue(ConfigurableChunkerProviderImplAlt.PARAM_USE_APPROXIMATE_MATCHING));
        assertEquals(false, parameterSettings.getParameterValue(ConfigurableChunkerProviderImplAlt.PARAM_CASE_SENSITIVE));
        assertEquals(false, parameterSettings.getParameterValue(ConfigurableChunkerProviderImplAlt.PARAM_MAKE_VARIANTS));
        assertEquals("de/julielab/jcore/ae/lingpipegazetteer/stopwords/general_english_words", parameterSettings.getParameterValue(ConfigurableChunkerProviderImplAlt.PARAM_STOPWORD_FILE));
        assertEquals(true, parameterSettings.getParameterValue(ConfigurableChunkerProviderImplAlt.PARAM_NORMALIZE_TEXT));
        assertEquals(false, parameterSettings.getParameterValue(ConfigurableChunkerProviderImplAlt.PARAM_TRANSLITERATE_TEXT));

    }
}
