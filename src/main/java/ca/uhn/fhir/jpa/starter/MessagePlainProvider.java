package ca.uhn.fhir.jpa.starter;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.dao.DaoRegistry;
import ca.uhn.fhir.jpa.dao.IFhirResourceDao;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.MessageHeader.ResponseType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

public class MessagePlainProvider {
  DaoRegistry daoRegistry;
  FhirContext fhirContext;
  public MessagePlainProvider(DaoRegistry _daoRegistry, FhirContext _fhirContext){
    daoRegistry = _daoRegistry;
    fhirContext = _fhirContext;
  }
  @Operation(name="$process-message", manualResponse=true)
  public void processMessage(
      @OptionalParam(name="async") String async,
      @OptionalParam(name="response-url") String responseUrl,
      @ResourceParam Bundle bundleR4,
      HttpServletResponse theServletResponse
  ) {
    IParser parser = fhirContext.newJsonParser();

    // validate the bundle
    if (bundleR4.getType() != Bundle.BundleType.MESSAGE) {
      OperationOutcome oo = new OperationOutcome();
      oo.addIssue()
          .setSeverity(OperationOutcome.IssueSeverity.ERROR)
          .setDiagnostics("Bundle is not of type 'message'");
      respondWithResource(oo, theServletResponse, parser);
    }

    if (!bundleR4.hasEntry()) {
      OperationOutcome oo = new OperationOutcome();
      oo.addIssue()
          .setSeverity(OperationOutcome.IssueSeverity.ERROR)
          .setDiagnostics("Bundle has no entries, a MessageHeader is required");
      respondWithResource(oo, theServletResponse, parser);
    }

    // check that there is a message header
    Resource firstResource = bundleR4.getEntryFirstRep().getResource();
    if ( firstResource.getResourceType() != ResourceType.MessageHeader) {
      OperationOutcome oo = new OperationOutcome();
      oo.addIssue()
          .setSeverity(OperationOutcome.IssueSeverity.ERROR)
          .setDiagnostics("First resource in message bundle must be of type 'MessageHeader'");
      respondWithResource(oo, theServletResponse, parser);
    }

    MessageHeader msgHead = (MessageHeader) firstResource;

    // check messageHeader is well formed
    if (!msgHead.hasSource()) {
      OperationOutcome oo = new OperationOutcome();
      oo.addIssue()
          .setSeverity(OperationOutcome.IssueSeverity.ERROR)
          .setDiagnostics("'MessageHeader' is missing source");
      respondWithResource(oo, theServletResponse, parser);
    } else if (!msgHead.getSource().hasEndpoint()){
      OperationOutcome oo = new OperationOutcome();
      oo.addIssue()
          .setSeverity(OperationOutcome.IssueSeverity.ERROR)
          .setDiagnostics("'MessageHeader.source' is missing an endpoint");
      respondWithResource(oo, theServletResponse, parser);
    }

    // use the DAO to create resources in the fhir server
    IFhirResourceDao bundleDao = daoRegistry.getResourceDao(ResourceType.Bundle.name());
    IFhirResourceDao messageDao = daoRegistry.getResourceDao(ResourceType.MessageHeader.name());
    bundleDao.create(bundleR4);
    messageDao.create(msgHead);


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
        ResponseType responseType = doEvent(msgHead);
        Bundle bundle = buildResponseMessage(msgHead, responseType);
        String bundleString = parser.encodeResourceToString(bundle);
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
      ResponseType responseType = doEvent(msgHead);
      Bundle bundle = buildResponseMessage(msgHead, responseType);
      respondWithResource(bundle, theServletResponse, parser);
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
  private ResponseType doEvent(MessageHeader msgHead) {
    // TODO: this function should synchronously
    // complete whatever event the message has asked for
    // The "focus" list is a list of reference resources
    List<Reference> references = msgHead.getFocus();
    if (msgHead.hasEventCoding()) {
      String code = msgHead.getEventCoding().getCode();
      String system = msgHead.getEventCoding().getSystem();
    } else if (msgHead.hasEventUriType()) {
      String uri = msgHead.getEventUriType().fhirType();
    }
    return ResponseType.OK;
  }


  private void respondWithResource(IBaseResource resource, HttpServletResponse theServletResponse, IParser parser ){
    theServletResponse.setContentType("application/json");
    try {
      String data = parser.encodeResourceToString(resource);
      theServletResponse.getWriter().write(data);
      theServletResponse.getWriter().close();
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  private Bundle buildResponseMessage(MessageHeader requestHeader, ResponseType responseType) {
    String serverAddress = HapiProperties.getServerAddress();
    Bundle response = new Bundle();
    response.setType(Bundle.BundleType.MESSAGE);
    MessageHeader header = new MessageHeader();
    header.addDestination().setEndpoint(requestHeader.getSource().getEndpoint());
    header.setSource(new MessageHeader.MessageSourceComponent()
        .setEndpoint(serverAddress + "$process-message"));
    header.setResponse(new MessageHeader.MessageHeaderResponseComponent()
        .setCode(responseType));
    response.addEntry().setResource(header);
    return response;
  }


}
