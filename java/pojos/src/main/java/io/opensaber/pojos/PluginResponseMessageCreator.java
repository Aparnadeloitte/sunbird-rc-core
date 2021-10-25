package io.opensaber.pojos;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.opensaber.pojos.attestation.Action;

import java.util.Date;

public class PluginResponseMessageCreator {
    public static PluginResponseMessage createClaimResponseMessage(String claimId, Action status, PluginRequestMessage pluginRequestMessage) {
        ObjectNode additionalData = JsonNodeFactory.instance.objectNode();
        additionalData.put("claimId", claimId);
        return PluginResponseMessage.builder()
                .sourceEntity(pluginRequestMessage.getSourceEntity())
                .sourceOSID(pluginRequestMessage.getSourceOSID())
                .attestationOSID(pluginRequestMessage.getAttestationOSID())
                .attestorPlugin(pluginRequestMessage.getAttestorPlugin())
                .additionalData(additionalData)
                .policyName(pluginRequestMessage.getPolicyName())
                .status(status.name())
                .date(new Date())
                .validUntil(new Date())
                .version("")
                .build();
    }
}
