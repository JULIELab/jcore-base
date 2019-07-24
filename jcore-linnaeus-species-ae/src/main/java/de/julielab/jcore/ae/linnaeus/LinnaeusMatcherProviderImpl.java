package de.julielab.jcore.ae.linnaeus;

import martin.common.ArgParser;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.LoggerFactory;
import uk.ac.man.entitytagger.EntityTagger;
import uk.ac.man.entitytagger.matching.Matcher;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Logger;

public class LinnaeusMatcherProviderImpl implements LinnaeusMatcherProvider {
    private final static org.slf4j.Logger log = LoggerFactory.getLogger(LinnaeusMatcherProviderImpl.class);
    private Matcher matcher;

    @Override
    public Matcher getMatcher() {
        return matcher;
    }

    @Override
    public void load(DataResource aData) throws ResourceInitializationException {
        final URI uri = aData.getUri();
        String configFile;
        if (aData.getUrl() != null) {
            try {
                log.info("Loading LINNAUS configuration from file {}", aData.getUrl());
                configFile = new File(aData.getUrl().toURI()).getAbsolutePath();
            } catch (URISyntaxException e) {
                throw new ResourceInitializationException(e);
            }
        } else if (getClass().getResource(uri.toString()) != null) {
            log.info("Loading LINNAEUS configuration as classpath resource from {}", uri);
            configFile = "internal:" + uri.toString();
        }
        else
            throw new ResourceInitializationException(new IllegalArgumentException("Could not find the LINNAEUS configuration as a file or a classpath resource at " + uri.toString()));

        ArgParser ap = new ArgParser(new String[]{"--properties", configFile});

        this.matcher = EntityTagger.getMatcher(ap, Logger.getLogger(Logger.GLOBAL_LOGGER_NAME));
    }
}
