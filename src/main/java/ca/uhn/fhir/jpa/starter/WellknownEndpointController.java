package ca.uhn.fhir.jpa.starter;

import javax.servlet.http.HttpServletRequest;

import org.mitre.hapifhir.WellknownEndpointHelper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WellknownEndpointController {
    /**
     * Get request to support well-known endpoints for authorization metadata. See
     * http://www.hl7.org/fhir/smart-app-launch/conformance/index.html#using-well-known
     *
     * @return String representing json object of metadata returned at this url
     * @throws IOException when the request fails
     */
    @GetMapping(path = "/smart-configuration", produces = {"application/json"})
    public String getWellKnownJson(HttpServletRequest theRequest) {
    	String yourTokenUrl = HapiProperties.getAuthServerTokenAddress();
        String yourRegisterUrl = HapiProperties.getAuthServerRegistrationAddress();
    	
    	return WellknownEndpointHelper.getWellKnownJson(yourTokenUrl, yourRegisterUrl);
    }
}
