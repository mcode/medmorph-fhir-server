package org.mitre.hapifhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.function.BiFunction;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;


public class ProcessMessageProvider {

  private FhirContext fhirContext;
  private BiFunction<Bundle, MessageHeader, Bundle> action;
  
  public ProcessMessageProvider(FhirContext fhirContext, 
      BiFunction<Bundle, MessageHeader, Bundle> action) {
    this.fhirContext = fhirContext;
    this.action = action;
  }
  
  
  public static OperationOutcome validateRequest(Bundle requestBundle) {
    // validate the bundle
    if (requestBundle.getType() != Bundle.BundleType.MESSAGE) {
      OperationOutcome oo = new OperationOutcome();
      oo.addIssue()
          .setSeverity(OperationOutcome.IssueSeverity.ERROR)
          .setDiagnostics("Bundle is not of type 'message'");
      return oo;
    }

    if (!requestBundle.hasEntry()) {
      OperationOutcome oo = new OperationOutcome();
      oo.addIssue()
          .setSeverity(OperationOutcome.IssueSeverity.ERROR)
          .setDiagnostics("Bundle has no entries, a MessageHeader is required");
      return oo;
    }

    // check that there is a message header
    Resource firstResource = requestBundle.getEntryFirstRep().getResource();
    if (firstResource.getResourceType() != ResourceType.MessageHeader) {
      OperationOutcome oo = new OperationOutcome();
      oo.addIssue()
          .setSeverity(OperationOutcome.IssueSeverity.ERROR)
          .setDiagnostics("First resource in message bundle must be of type 'MessageHeader'");
      return oo;
    }
    
    MessageHeader msgHead = (MessageHeader) firstResource;
    // check messageHeader is well formed
    if (!msgHead.hasSource()) {
      OperationOutcome oo = new OperationOutcome();
      oo.addIssue()
          .setSeverity(OperationOutcome.IssueSeverity.ERROR)
          .setDiagnostics("'MessageHeader' is missing source");
      return oo;
    } else if (!msgHead.getSource().hasEndpoint()) {
      OperationOutcome oo = new OperationOutcome();
      oo.addIssue()
          .setSeverity(OperationOutcome.IssueSeverity.ERROR)
          .setDiagnostics("'MessageHeader.source' is missing an endpoint");
      return oo;
    }
    
    // no errors, looks good
    return null;
  }
  
  
  @Operation(name = "$process-message", manualResponse = true)
  public void processMessage(
      @OptionalParam(name = "async") String async,
      @OptionalParam(name = "response-url") String responseUrl,
      @ResourceParam Bundle bundleR4,
      HttpServletResponse theServletResponse
  ) {
    IParser parser = fhirContext.newJsonParser();

    OperationOutcome error = validateRequest(bundleR4);
    if (error != null) {
      respondWithResource(error, theServletResponse, parser);
      return;
    }

    MessageHeader msgHead = (MessageHeader) bundleR4.getEntryFirstRep().getResource();

    if (async != null && async.equals("true")) {
      // asynchronous
      String asyncResponseUrl;
      if (responseUrl != null) {
        asyncResponseUrl = responseUrl;
      } else {
        asyncResponseUrl = msgHead.getSource().getEndpoint();
      }
      // do any async operations in the new thread
      Thread newThread = new Thread(() -> {
        Bundle response = action.apply(bundleR4, msgHead);
        String bundleString = parser.encodeResourceToString(response);
        makePost(asyncResponseUrl, bundleString);
      });
      newThread.start();
      theServletResponse.setStatus(200);
      try {
        theServletResponse.getWriter().close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    } else {
      // synchronous
      Bundle response = action.apply(bundleR4, msgHead);
      respondWithResource(response, theServletResponse, parser);
    }
  }

  private void makePost(String asyncResponseUrl, String content) {
    HttpClient httpClient = HttpClients.createDefault();
    HttpPost httpPost = new HttpPost(asyncResponseUrl);
    try {
      StringEntity entity = new StringEntity(content);
      httpPost.setEntity(entity);
      httpPost.setHeader("Content-type", "application/json");
      httpClient.execute(httpPost);
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    } catch (ClientProtocolException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void respondWithResource(IBaseResource resource,
      HttpServletResponse theServletResponse, IParser parser) {
    theServletResponse.setContentType("application/json");
    try {
      String data = parser.encodeResourceToString(resource);
      theServletResponse.getWriter().write(data);
      theServletResponse.getWriter().close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
