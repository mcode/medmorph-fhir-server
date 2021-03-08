package org.mitre.hapifhir;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WellKnownEndpointController {

    // Well Known JSON Keys
    private static final String WELL_KNOWN_TOKEN_ENDPOINT_KEY = "token_endpoint";

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

        JSONObject wellKnownJson = new JSONObject();
        
        // TODO: refactor to not use HapiProperties -- it's a JPA starter server file
        wellKnownJson.put(WELL_KNOWN_TOKEN_ENDPOINT_KEY, "http://example.com"); //HapiProperties.getAuthServerTokenAddress());

        return wellKnownJson.toString(2);
    }
}
