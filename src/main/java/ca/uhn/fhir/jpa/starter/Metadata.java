package ca.uhn.fhir.jpa.starter;

import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hl7.fhir.r4.hapi.rest.server.ServerCapabilityStatementProvider;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestSecurityComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementSoftwareComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.RestfulCapabilityMode;

import ca.uhn.fhir.rest.api.server.RequestDetails;

public class Metadata extends ServerCapabilityStatementProvider {

    @Override
    public CapabilityStatement getServerConformance(HttpServletRequest request, RequestDetails requestDetails) {
        CapabilityStatement c = super.getServerConformance(request, requestDetails);
        c.setTitle("MedMorph EHR");
        c.setExperimental(true);
        c.setPublisher("MITRE");
        c.addImplementationGuide("https://build.fhir.org/ig/HL7/fhir-medmorph/index.html");

        CapabilityStatementSoftwareComponent software = new CapabilityStatementSoftwareComponent();
        software.setName("https://github.com/mcode/medmorph-ehr");
        c.setSoftware(software);

        CapabilityStatementRestSecurityComponent securityComponent = new CapabilityStatementRestSecurityComponent();
        Extension oauthExtension = new Extension();
        Extension tokenEndpointUri = new Extension("token", new UriType(HapiProperties.getAuthServerTokenAddress()));
        oauthExtension.setUrl("http://fhir-registry.smarthealthit.org/StructureDefinition/oauth-uris");
        oauthExtension.setExtension(Collections.singletonList(tokenEndpointUri));
        securityComponent.addExtension(oauthExtension);

        // Get the CapabilityStatementRestComponent for the server if one exists
        List<CapabilityStatementRestComponent> restComponents = c.getRest();
        CapabilityStatementRestComponent rest = null;
        for (CapabilityStatementRestComponent rc : restComponents) {
            if (rc.getMode().equals(RestfulCapabilityMode.SERVER)) {
                rest = rc;
            }
        }

        if (rest == null) {
            // Create new rest component
            rest = new CapabilityStatementRestComponent();
            rest.setMode(RestfulCapabilityMode.SERVER);
            rest.setSecurity(securityComponent);
            c.addRest(rest);
        } else
            rest.setSecurity(securityComponent);

        return c;
    }
    
}
