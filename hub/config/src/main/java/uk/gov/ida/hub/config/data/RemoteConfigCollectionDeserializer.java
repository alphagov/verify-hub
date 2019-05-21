package uk.gov.ida.hub.config.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import uk.gov.ida.hub.config.domain.remoteconfig.RemoteCertificateConfig;
import uk.gov.ida.hub.config.domain.remoteconfig.RemoteConfigCollection;
import uk.gov.ida.hub.config.domain.remoteconfig.RemoteConnectedServiceConfig;
import uk.gov.ida.hub.config.domain.remoteconfig.RemoteMatchingServiceConfig;
import uk.gov.ida.hub.config.domain.remoteconfig.RemoteServiceProviderConfig;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RemoteConfigCollectionDeserializer extends StdDeserializer<RemoteConfigCollection> {

    public RemoteConfigCollectionDeserializer() {
        this(null);
    }

    public RemoteConfigCollectionDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public RemoteConfigCollection deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {

        Map<String, RemoteCertificateConfig> signingCertificates;
        Map<String, RemoteCertificateConfig> encryptionCertificates;
        ObjectMapper om = new ObjectMapper();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        om.setDateFormat(df);
        JsonNode node = jp.getCodec().readTree(jp);
        Date publishedAt = om.readValue(node.get("published_at").toString(), Date.class);

        Map<String, RemoteConnectedServiceConfig> connectedServices = new HashMap<>();
        Map<String, RemoteMatchingServiceConfig> msas = new HashMap<>();
        Map<Integer, RemoteServiceProviderConfig> sps = new HashMap<>();

        // Process MSAs
        MSAandSPTranslator[] msasToTranslate = om.readValue(node.get("matching_service_adapters").toString(), MSAandSPTranslator[].class);
        for(MSAandSPTranslator msa : msasToTranslate) {
            msas.put(msa.entityId, msa.toRemoteMatchingServiceConfig());
        }

        // Process Service Providers
        MSAandSPTranslator[] spsToTranslate = om.readValue(node.get("service_providers").toString(), MSAandSPTranslator[].class);
        for(MSAandSPTranslator sp : spsToTranslate) {
            sps.put(sp.id, sp.toRemoteServiceProviderConfig());
        }

        RemoteConnectedServiceConfigTranslator[] connectedServicesToTranslate =
                om.readValue(node.get("connected_services").toString(), RemoteConnectedServiceConfigTranslator[].class);
        for(RemoteConnectedServiceConfigTranslator service :  connectedServicesToTranslate) {
            connectedServices.put(service.entityId, service.toRemoteConnectedServiceConfigTranslator(msas, sps));
        }

        return new RemoteConfigCollection(publishedAt, connectedServices, msas, new ArrayList<>(sps.values()));
    }

    private static class RemoteConnectedServiceConfigTranslator {

        @JsonProperty("entity_id")
        protected String entityId;

        @JsonProperty("service_provider")
        protected int serviceProviderConfigId;

        @JsonProperty("matching_service_adapter")
        protected String matchingServiceConfigId;

        @SuppressWarnings("unused")
        protected RemoteConnectedServiceConfigTranslator() {
        }

        public RemoteConnectedServiceConfig toRemoteConnectedServiceConfigTranslator(Map<String, RemoteMatchingServiceConfig> matchingServiceAdapters,
                                                                                     Map<Integer, RemoteServiceProviderConfig>serviceProviders) {
            return new RemoteConnectedServiceConfig(this.entityId,
                    serviceProviders.get(this.serviceProviderConfigId), matchingServiceAdapters.get(this.matchingServiceConfigId));
        }

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class MSAandSPTranslator {

        @JsonProperty("id")
        private int id;
        @JsonProperty("entity_id")
        private String entityId;
        @JsonProperty("name")
        private String name;
        @JsonProperty("encryption_certificate")
        private RemoteCertificateConfig encryptionCertificate;
        @JsonProperty("signing_certificates")
        private List<RemoteCertificateConfig> signingCertificates;

        /**
         * Unused
         */
        private MSAandSPTranslator() {

        }

        public RemoteMatchingServiceConfig toRemoteMatchingServiceConfig() {
            return new RemoteMatchingServiceConfig(this.name, this.entityId, this.encryptionCertificate, this.signingCertificates);
        }

        public RemoteServiceProviderConfig toRemoteServiceProviderConfig() {
            return new RemoteServiceProviderConfig(this.name, this.encryptionCertificate, this.signingCertificates);
        }

    }

}
