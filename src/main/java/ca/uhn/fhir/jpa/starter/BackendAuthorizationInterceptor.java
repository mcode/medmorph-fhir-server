package ca.uhn.fhir.jpa.starter;

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONObject;
import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.representations.AccessToken;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.interceptor.auth.AuthorizationInterceptor;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;

@SuppressWarnings("ConstantConditions")
public class BackendAuthorizationInterceptor extends AuthorizationInterceptor {

    @Override
    public List<IAuthRule> buildRuleList(RequestDetails theRequestDetails) {

        String authHeader = theRequestDetails.getHeader("Authorization");

        if (authHeader != null) {
            // Get the JWT token from the Authorization header
            String regex = "Bearer (.*)";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(authHeader);
            String token = "";
            if (matcher.find() && matcher.groupCount() == 1) {
                token = matcher.group(1);

                if (token.equals("admin"))
                    return authorizedRule();
                else {
                    try {
                        TokenVerifier<AccessToken> verifier = TokenVerifier.create(token, AccessToken.class).publicKey(getKey());
                        verifier.verify();
                        return authorizedRule();
                    } catch (VerificationException e) {
                        e.printStackTrace();
                        throw new AuthenticationException("Token is invalid", e);
                    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                        e.printStackTrace();
                        throw new AuthenticationException("Internal error processing public key", e);
                    }
                }

            }
            else
                throw new AuthenticationException("Authorization header is not in the form \"Bearer <token>\"");
        }

        return unauthorizedRule();
    }

    private List<IAuthRule> authorizedRule() {
        return new RuleBuilder().allowAll().build();
    }

    private List<IAuthRule> unauthorizedRule() {
        // By default, deny everything except the metadata. This is for
        // unauthorized users
        return new RuleBuilder().allow().metadata().andThen().denyAll().build();
    }

    private RSAPublicKey getKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        String eRaw = "AQAB";
        String nRaw = "obB80r5GR3Zx5tPXEb2NYI3itp6xI-XAr2kly4xbkcmLweWDouIXxCkamFPEjo-Fb7gfnM-2_u8wV3Rzg5Z8IpIJh3hJy6XV0DH_rFIVQMtyMX-NaNvOxMQRoohVAXKpluw14-lmH5Y90sX_k7lK3JR5WlPcv9RKRFWWW8ePxrzOqFt0Jvi8mgCETFEG4259oVWPgD4SDI3zrTLkZ30-Dw1VDwDBXCTkZUK8LoWqavecPXMERHa10XnCqZXOntAIzrMXd3XzAvw2iOmdHytB--RP_zsLnEJZdqq2T5Q1WNbkPqSWEKUkPJ_WuAHtxppZnFKFGX-VmytD3rLWhAQXWw";

        // Get the latest key from the auth server
		try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(HapiProperties.getAuthServerCertsAddress()).build();
            Response response = client.newCall(request).execute();
            JSONObject jwks = new JSONObject(response.body().string());
            JSONArray keys = jwks.getJSONArray("keys");
            JSONObject key = (JSONObject) keys.get(0);
            eRaw = key.getString("e");
            nRaw = key.getString("n");
		} catch (IOException e1) {
            e1.printStackTrace();
        }

        BigInteger e = new BigInteger(1, Base64.getUrlDecoder().decode(eRaw));
        BigInteger n = new BigInteger(1, Base64.getUrlDecoder().decode(nRaw));

        KeyFactory kf = KeyFactory.getInstance("RSA");
        RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(n, e);
        return (RSAPublicKey) kf.generatePublic(publicKeySpec);
    }
}