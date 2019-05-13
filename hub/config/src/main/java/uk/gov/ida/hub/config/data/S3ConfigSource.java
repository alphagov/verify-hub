package uk.gov.ida.hub.config.data;

import com.amazonaws.services.s3.model.S3DataSource;
import uk.gov.ida.hub.config.domain.remoteconfig.RemoteConfigCollection;

public class S3ConfigSource{

    private S3DataSource s3DataSource;

    public S3ConfigSource(){

    }

    public RemoteConfigCollection getRemoteConfig(){
        return null;
    }

}
