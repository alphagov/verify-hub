package uk.gov.ida.integrationtest.hub.samlproxy.apprule.support;

import com.google.common.base.Throwables;
import helpers.ResourceHelpers;
import org.apache.commons.io.FileUtils;

import java.io.IOException;

public class MetadataHelper {
    public static final String METADATA_CONTENT = readMetadata();

    public static String readMetadata() {
        try {
            return FileUtils.readFileToString(ResourceHelpers.resourceFile("metadata"));
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }
}
