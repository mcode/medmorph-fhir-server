package ca.uhn.fhir.jpa.starter.wellknown;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import ca.uhn.fhir.jpa.starter.HapiProperties;

@RestController
public class WellKnownEndpointController {

    // Well Known JSON Keys
    private static final String WELL_KNOWN_TOKEN_ENDPOINT_KEY = "token_endpoint";
    private static final String RESPONSE_TYPES_SUPPORTED_KEY = "response_types_supported";
    private static final String SCOPES_SUPPORTED_KEY = "scopes_supported";

    @PostConstruct
    protected void postConstruct() {
        System.out.println("Well Known controller added.");
    }

    /**
     * Get request to support well-known endpoints for authorization metadata. See
     * http://www.hl7.org/fhir/smart-app-launch/conformance/index.html#using-well-known
     *
     * @return String representing json object of metadata returned at this url
     * @throws IOException when the request fails
     */
    @GetMapping(path = "/smart-configuration", produces = {"application/json"})
    public String getWellKnownJson(HttpServletRequest theRequest) {

        JSONArray scopesSupported = new JSONArray();
        scopesSupported.put("system/*.read");
        scopesSupported.put("offline_access");

        JSONArray responseTypesSupported = new JSONArray();
        responseTypesSupported.put("token");

        JSONObject wellKnownJson = new JSONObject();
        wellKnownJson.put(WELL_KNOWN_TOKEN_ENDPOINT_KEY, HapiProperties.getAuthServerTokenAddress());
        wellKnownJson.put(RESPONSE_TYPES_SUPPORTED_KEY, responseTypesSupported);
        wellKnownJson.put(SCOPES_SUPPORTED_KEY, scopesSupported);

        return wellKnownJson.toString(2);
    }
}
