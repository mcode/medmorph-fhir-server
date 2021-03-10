package org.mitre.hapifhir;

import org.json.JSONObject;

public class WellknownEndpointHelper {

    // Well Known JSON Keys
    private static final String WELL_KNOWN_TOKEN_ENDPOINT_KEY = "token_endpoint";
	
	public static String getWellKnownJson(String tokenEndpointUrl) {
        JSONObject wellKnownJson = new JSONObject();

        wellKnownJson.put(WELL_KNOWN_TOKEN_ENDPOINT_KEY, tokenEndpointUrl);

        return wellKnownJson.toString(2);
	}
	
}
