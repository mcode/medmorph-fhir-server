package ca.uhn.fhir.jpa.starter;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.interceptor.api.IInterceptorBroadcaster;
import ca.uhn.fhir.interceptor.api.IInterceptorService;
import ca.uhn.fhir.jpa.binstore.BinaryStorageInterceptor;
import ca.uhn.fhir.jpa.bulk.BulkDataExportProvider;
import ca.uhn.fhir.jpa.dao.DaoConfig;
import ca.uhn.fhir.jpa.dao.DaoRegistry;
import ca.uhn.fhir.jpa.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.interceptor.CascadingDeleteInterceptor;
import ca.uhn.fhir.jpa.provider.GraphQLProvider;
import ca.uhn.fhir.jpa.provider.SubscriptionTriggeringProvider;
import ca.uhn.fhir.jpa.provider.TerminologyUploaderProvider;
import ca.uhn.fhir.jpa.provider.r4.JpaSystemProviderR4;
import ca.uhn.fhir.jpa.provider.r5.JpaConformanceProviderR5;
import ca.uhn.fhir.jpa.provider.r5.JpaSystemProviderR5;
import ca.uhn.fhir.jpa.search.DatabaseBackedPagingProvider;
import ca.uhn.fhir.jpa.subscription.SubscriptionInterceptorLoader;
import ca.uhn.fhir.jpa.subscription.module.interceptor.SubscriptionDebugLogInterceptor;
import ca.uhn.fhir.jpa.util.ResourceProviderFactory;
import ca.uhn.fhir.narrative.DefaultThymeleafNarrativeGenerator;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.HardcodedServerAddressStrategy;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.interceptor.CorsInterceptor;
import ca.uhn.fhir.rest.server.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.server.interceptor.RequestValidatingInterceptor;
import ca.uhn.fhir.rest.server.interceptor.ResponseHighlighterInterceptor;
import ca.uhn.fhir.rest.server.interceptor.ResponseValidatingInterceptor;
import ca.uhn.fhir.validation.IValidatorModule;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import java.util.HashSet;
import java.util.TreeSet;
import javax.servlet.http.HttpServletRequest;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.MessageHeader.ResponseType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementSoftwareComponent;
import org.mitre.hapifhir.BackendAuthorizationInterceptor;
import org.mitre.hapifhir.SMARTServerCapabilityStatementProvider;
import org.mitre.hapifhir.MedMorphToCIBMTR;
import org.mitre.hapifhir.ProcessMessageProvider;
import org.mitre.hapifhir.SubscriptionInterceptor;
import org.mitre.hapifhir.client.BearerAuthServerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.mitre.hapifhir.TopicListInterceptor;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;

import javax.servlet.ServletException;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

public class JpaRestfulServer extends RestfulServer {

  private final Logger logger = LoggerFactory.getLogger(JpaRestfulServer.class.getName());
  
  private static final long serialVersionUID = 1L;

  @Override
  protected void initialize() throws ServletException {
    super.initialize();

    /*
     * Create a FhirContext object that uses the version of FHIR specified in the
     * properties file.
     */
    ApplicationContext appCtx = (ApplicationContext) getServletContext()
        .getAttribute("org.springframework.web.context.WebApplicationContext.ROOT");
    // Customize supported resource types
    Set<String> supportedResourceTypes = HapiProperties.getSupportedResourceTypes();

    if (!supportedResourceTypes.isEmpty() && !supportedResourceTypes.contains("SearchParameter")) {
      supportedResourceTypes.add("SearchParameter");
    }

    if (!supportedResourceTypes.isEmpty()) {
      DaoRegistry daoRegistry = appCtx.getBean(DaoRegistry.class);
      daoRegistry.setSupportedResourceTypes(supportedResourceTypes);
    }

    /*
     * ResourceProviders are fetched from the Spring context
     */
    FhirVersionEnum fhirVersion = HapiProperties.getFhirVersion();
    ResourceProviderFactory resourceProviders;
    Object systemProvider;
    if (fhirVersion == FhirVersionEnum.R4) {
      resourceProviders = appCtx.getBean("myResourceProvidersR4", ResourceProviderFactory.class);
      systemProvider = appCtx.getBean("mySystemProviderR4", JpaSystemProviderR4.class);
    } else if (fhirVersion == FhirVersionEnum.R5) {
      resourceProviders = appCtx.getBean("myResourceProvidersR5", ResourceProviderFactory.class);
      systemProvider = appCtx.getBean("mySystemProviderR5", JpaSystemProviderR5.class);
    } else {
      throw new IllegalStateException();
    }

    setFhirContext(appCtx.getBean(FhirContext.class));
    String cibmtrUrl = System.getenv("CIBMTR_URL");
    MedMorphToCIBMTR medmorphToCIBMTR = new MedMorphToCIBMTR(cibmtrUrl);
    logger.info("CIBMTR translator started and pointing to " + cibmtrUrl);
    ProcessMessageProvider pmp = new ProcessMessageProvider(this.getFhirContext(), (messageContext) -> {
      Bundle bundle = messageContext.bundle;
      logger.info("Received message at $process-message endpoint -- bundle id: " + bundle.getId());
      MessageHeader messageHeader = messageContext.messageHeader;
      HttpServletRequest request = messageContext.request;

      // Pass report to CIBMTR translator
      String authToken = request.getHeader("Authorization");
      OperationOutcome operationOutcome = medmorphToCIBMTR.convert(bundle, messageHeader, authToken);
    
      // NOTE: this line is the reason the provider doesn't do this itself
      // -- it doesn't know its own address (HapiProperties is JPA server only)
      String serverAddress = HapiProperties.getServerAddress();
      Bundle response = new Bundle();
      response.setType(Bundle.BundleType.MESSAGE);
      MessageHeader header = new MessageHeader();
      header.addDestination().setEndpoint(messageHeader.getSource().getEndpoint());
      header.setSource(new MessageHeader.MessageSourceComponent()
          .setEndpoint(serverAddress + "/$process-message"));

      // TODO: expand errors to distinguish transient vs fatal error
      ResponseType responseTypeCode = operationOutcome == null ? ResponseType.OK : ResponseType.FATALERROR;
      logger.info("Returning response from $process-message, bundle id: " + bundle.getId() + " -- response code: " + responseTypeCode);
      header.setResponse(new MessageHeader.MessageHeaderResponseComponent()
          .setCode(responseTypeCode));
      response.addEntry().setResource(header);
      if (operationOutcome != null) response.addEntry().setResource(operationOutcome);
      return response;
    });

    registerProvider(pmp);

    registerProviders(resourceProviders.createProviders());
    registerProvider(systemProvider);

    String title = "MedMorph FHIR Server";
    try {
        String envTitle = System.getenv("SERVER_TITLE");
        if (envTitle != null) title = envTitle;
    } catch (NullPointerException | SecurityException e) {
    	System.err.println("Error getting server title: ");
        e.printStackTrace();
    }

    /*
     * The conformance provider exports the supported resources, search parameters,
     * etc for this server. The JPA version adds resourceProviders counts to the
     * exported statement, so it is a nice addition.
     *
     * You can also create your own subclass of the conformance provider if you need
     * to provide further customization of your server's CapabilityStatement
     */
    if (fhirVersion == FhirVersionEnum.R4) {
      SMARTServerCapabilityStatementProvider smartCSProvider =
    			new SMARTServerCapabilityStatementProvider(HapiProperties.getAuthServerTokenAddress(), HapiProperties.getAuthServerRegistrationAddress());

      final String serverTitle = title;
      
      smartCSProvider
      	.with(c -> c.setTitle(serverTitle))
      	.with(c -> c.setExperimental(true))
      	.with(c -> c.setPublisher("MITRE"))
      	.with(c -> c.addImplementationGuide("https://build.fhir.org/ig/HL7/fhir-medmorph/index.html"))
      	.with(c -> {
          CapabilityStatementSoftwareComponent software = new CapabilityStatementSoftwareComponent();
          software.setName("https://github.com/mcode/medmorph-fhir-server");
          c.setSoftware(software);
      	});


      setServerConformanceProvider(smartCSProvider);
    } else if (fhirVersion == FhirVersionEnum.R5) {
      IFhirSystemDao<org.hl7.fhir.r5.model.Bundle, org.hl7.fhir.r5.model.Meta> systemDao = appCtx
          .getBean("mySystemDaoR5", IFhirSystemDao.class);
      JpaConformanceProviderR5 confProvider = new JpaConformanceProviderR5(this, systemDao,
          appCtx.getBean(DaoConfig.class));
      confProvider.setImplementationDescription("HAPI FHIR R5 Server");
      setServerConformanceProvider(confProvider);
    } else {
      throw new IllegalStateException();
    }

    /*
     * ETag Support
     */
    setETagSupport(HapiProperties.getEtagSupport());

    /*
     * This server tries to dynamically generate narratives
     */
    FhirContext ctx = getFhirContext();
    ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());

    /*
     * Default to JSON and pretty printing
     */
    setDefaultPrettyPrint(HapiProperties.getDefaultPrettyPrint());

    /*
     * Default encoding
     */
    setDefaultResponseEncoding(HapiProperties.getDefaultEncoding());

    /*
     * This configures the server to page search results to and from the database,
     * instead of only paging them to memory. This may mean a performance hit when
     * performing searches that return lots of results, but makes the server much
     * more scalable.
     */
    setPagingProvider(appCtx.getBean(DatabaseBackedPagingProvider.class));

    /*
     * This interceptor formats the output using nice colourful HTML output when the
     * request is detected to come from a browser.
     */
    ResponseHighlighterInterceptor responseHighlighterInterceptor = new ResponseHighlighterInterceptor();
    ;
    this.registerInterceptor(responseHighlighterInterceptor);

    /*
     * Add some logging for each request
     */
    LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
    loggingInterceptor.setLoggerName(HapiProperties.getLoggerName());
    loggingInterceptor.setMessageFormat(HapiProperties.getLoggerFormat());
    loggingInterceptor.setErrorMessageFormat(HapiProperties.getLoggerErrorFormat());
    loggingInterceptor.setLogExceptions(HapiProperties.getLoggerLogExceptions());
    this.registerInterceptor(loggingInterceptor);

    /*
     * Add Authorization interceptor
     */
    String requireAuth = System.getenv("REQUIRE_AUTH");
    if (requireAuth == null || !requireAuth.equals("false")) {
      BackendAuthorizationInterceptor authorizationInterceptor = new BackendAuthorizationInterceptor(HapiProperties.getAuthServerCertsAddress());
      this.registerInterceptor(authorizationInterceptor);
    }

    /*
     * Add Backport Subscription interceptor
     */
    IGenericClient client = this.getFhirContext().newRestfulGenericClient(HapiProperties.getServerAddress());
    BearerAuthServerClient serverClient = new BearerAuthServerClient(System.getenv("ADMIN_TOKEN"), client);
    SubscriptionInterceptor subscriptionInterceptor = new SubscriptionInterceptor(HapiProperties.getServerAddress(), this.getFhirContext(), serverClient, MedmorphSubscriptionTopics.getAllTopics());
    this.registerInterceptor(subscriptionInterceptor);

    /*
     * Add Topic List interceptor
     */
    TopicListInterceptor topicListInterceptor = new TopicListInterceptor(this.getFhirContext(), MedmorphSubscriptionTopics.getAllTopics());
    this.registerInterceptor(topicListInterceptor);

    /*
     * If you are hosting this server at a specific DNS name, the server will try to
     * figure out the FHIR base URL based on what the web container tells it, but
     * this doesn't always work. If you are setting links in your search bundles
     * that just refer to "localhost", you might want to use a server address
     * strategy:
     */
    String serverAddress = HapiProperties.getServerAddress();
    if (serverAddress != null && serverAddress.length() > 0) {
      setServerAddressStrategy(new HardcodedServerAddressStrategy(serverAddress));
    }

    /*
     * If you are using DSTU3+, you may want to add a terminology uploader, which
     * allows uploading of external terminologies such as Snomed CT. Note that this
     * uploader does not have any security attached (any anonymous user may use it
     * by default) so it is a potential security vulnerability. Consider using an
     * AuthorizationInterceptor with this feature.
     */
    if (false) { // <-- DISABLED RIGHT NOW
      registerProvider(appCtx.getBean(TerminologyUploaderProvider.class));
    }

    // If you want to enable the $trigger-subscription operation to allow
    // manual triggering of a subscription delivery, enable this provider
    if (false) { // <-- DISABLED RIGHT NOW
      SubscriptionTriggeringProvider retriggeringProvider = appCtx.getBean(SubscriptionTriggeringProvider.class);
      registerProvider(retriggeringProvider);
    }

    // Define your CORS configuration. This is an example
    // showing a typical setup. You should customize this
    // to your specific needs
    if (HapiProperties.getCorsEnabled()) {
      CorsConfiguration config = new CorsConfiguration();
      config.addAllowedHeader(HttpHeaders.ORIGIN);
      config.addAllowedHeader(HttpHeaders.ACCEPT);
      config.addAllowedHeader(HttpHeaders.CONTENT_TYPE);
      config.addAllowedHeader(HttpHeaders.AUTHORIZATION);
      config.addAllowedHeader(HttpHeaders.CACHE_CONTROL);
      config.addAllowedHeader("x-fhir-starter");
      config.addAllowedHeader("X-Requested-With");
      config.addAllowedHeader("Prefer");
      String allAllowedCORSOrigins = HapiProperties.getCorsAllowedOrigin();
      Arrays.stream(allAllowedCORSOrigins.split(",")).forEach(o -> {
        config.addAllowedOrigin(o);
      });
      config.addAllowedOrigin(HapiProperties.getCorsAllowedOrigin());

      config.addExposedHeader("Location");
      config.addExposedHeader("Content-Location");
      config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));
      config.setAllowCredentials(HapiProperties.getCorsAllowedCredentials());

      // Create the interceptor and register it
      CorsInterceptor interceptor = new CorsInterceptor(config);
      registerInterceptor(interceptor);
    }

    // If subscriptions are enabled, we want to register the interceptor that
    // will activate them and match results against them
    if (HapiProperties.getSubscriptionWebsocketEnabled() || HapiProperties.getSubscriptionEmailEnabled()
        || HapiProperties.getSubscriptionRestHookEnabled()) {
      // Loads subscription interceptors (SubscriptionActivatingInterceptor,
      // SubscriptionMatcherInterceptor)
      // with activation of scheduled subscription
      SubscriptionInterceptorLoader subscriptionInterceptorLoader = appCtx.getBean(SubscriptionInterceptorLoader.class);
      subscriptionInterceptorLoader.registerInterceptors();

      // Subscription debug logging
      IInterceptorService interceptorService = appCtx.getBean(IInterceptorService.class);
      interceptorService.registerInterceptor(new SubscriptionDebugLogInterceptor());
    }

    // Cascading deletes
    DaoRegistry daoRegistry = appCtx.getBean(DaoRegistry.class);
    IInterceptorBroadcaster interceptorBroadcaster = appCtx.getBean(IInterceptorBroadcaster.class);
    if (HapiProperties.getAllowCascadingDeletes()) {
      CascadingDeleteInterceptor cascadingDeleteInterceptor = new CascadingDeleteInterceptor(daoRegistry,
          interceptorBroadcaster);
      getInterceptorService().registerInterceptor(cascadingDeleteInterceptor);
    }

    // Binary Storage
    if (HapiProperties.isBinaryStorageEnabled()) {
      BinaryStorageInterceptor binaryStorageInterceptor = appCtx.getBean(BinaryStorageInterceptor.class);
      getInterceptorService().registerInterceptor(binaryStorageInterceptor);
    }

    // Validation
    IValidatorModule validatorModule;
    switch (fhirVersion) {
    case R4:
      validatorModule = appCtx.getBean("myInstanceValidatorR4", IValidatorModule.class);
      break;
    case R5:
      validatorModule = appCtx.getBean("myInstanceValidatorR5", IValidatorModule.class);
      break;
    // These versions are not supported by HAPI FHIR JPA
    case DSTU2_HL7ORG:
    case DSTU2_1:
    default:
      validatorModule = null;
      break;
    }
    if (validatorModule != null) {
      if (HapiProperties.getValidateRequestsEnabled()) {
        RequestValidatingInterceptor interceptor = new RequestValidatingInterceptor();
        interceptor.setFailOnSeverity(ResultSeverityEnum.ERROR);
        interceptor.setValidatorModules(Collections.singletonList(validatorModule));
        registerInterceptor(interceptor);
      }
      if (HapiProperties.getValidateResponsesEnabled()) {
        ResponseValidatingInterceptor interceptor = new ResponseValidatingInterceptor();
        interceptor.setFailOnSeverity(ResultSeverityEnum.ERROR);
        interceptor.setValidatorModules(Collections.singletonList(validatorModule));
        registerInterceptor(interceptor);
      }
    }

    // GraphQL
    if (HapiProperties.getGraphqlEnabled()) {
      if (fhirVersion.isEqualOrNewerThan(FhirVersionEnum.DSTU3)) {
        registerProvider(appCtx.getBean(GraphQLProvider.class));
      }
    }

    if (!HapiProperties.getAllowedBundleTypes().isEmpty()) {
      String allowedBundleTypesString = HapiProperties.getAllowedBundleTypes();
      Set<String> allowedBundleTypes = new HashSet<>();
      Arrays.stream(allowedBundleTypesString.split(",")).forEach(o -> {
        BundleType type = BundleType.valueOf(o);
        allowedBundleTypes.add(type.toCode());
      });
      DaoConfig config = appCtx.getBean(DaoConfig.class);
      config.setBundleTypesAllowedForStorage(Collections.unmodifiableSet(new TreeSet<>(allowedBundleTypes)));
    }

    // Bulk Export
    if (HapiProperties.getBulkExportEnabled()) {
      registerProvider(appCtx.getBean(BulkDataExportProvider.class));
    }

  }

}
