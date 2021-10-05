package ca.uhn.fhir.jpa.starter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hl7.fhir.r4.model.ResourceType;
import org.mitre.hapifhir.model.ResourceTrigger;
import org.mitre.hapifhir.model.SubscriptionTopic;
import org.mitre.hapifhir.model.ResourceTrigger.MethodCriteria;

public class MedmorphSubscriptionTopics {

    /*
     * Singleton instance so the topics are only created once 
     */
    private static MedmorphSubscriptionTopics singletonMedmorphSubscriptionTopics;

    private List<SubscriptionTopic> subscriptionTopics;

    private MedmorphSubscriptionTopics() {
        ResourceTrigger newConditionResourceTrigger = new ResourceTrigger(ResourceType.Condition, Collections.singletonList(MethodCriteria.CREATE));
        ResourceTrigger newMedicationRequestResourceTrigger = new ResourceTrigger(ResourceType.MedicationRequest, Collections.singletonList(MethodCriteria.CREATE));
        ResourceTrigger newMedicationDispenseResourceTrigger = new ResourceTrigger(ResourceType.MedicationDispense, Collections.singletonList(MethodCriteria.CREATE));
        ResourceTrigger newMedicationStatementResourceTrigger = new ResourceTrigger(ResourceType.MedicationStatement, Collections.singletonList(MethodCriteria.CREATE));
        ResourceTrigger newMedicationAdministrationResourceTrigger = new ResourceTrigger(ResourceType.MedicationAdministration, Collections.singletonList(MethodCriteria.CREATE));
        ResourceTrigger newServiveRequestResourceTrigger = new ResourceTrigger(ResourceType.ServiceRequest, Collections.singletonList(MethodCriteria.CREATE));
        ResourceTrigger newProcedureResourceTrigger = new ResourceTrigger(ResourceType.Procedure, Collections.singletonList(MethodCriteria.CREATE));
        ResourceTrigger newImmunizationResourceTrigger = new ResourceTrigger(ResourceType.Immunization, Collections.singletonList(MethodCriteria.CREATE));
        ResourceTrigger newObervationResourceTrigger = new ResourceTrigger(ResourceType.Observation, Collections.singletonList(MethodCriteria.CREATE), "category=laboratory");
        ResourceTrigger updateEncounterResourceTrigger = new ResourceTrigger(ResourceType.Encounter, Collections.singletonList(MethodCriteria.UPDATE));
        ResourceTrigger updateConditionResourceTrigger = new ResourceTrigger(ResourceType.Condition, Collections.singletonList(MethodCriteria.UPDATE));
        ResourceTrigger updateMedicationRequestResourceTrigger = new ResourceTrigger(ResourceType.MedicationRequest, Collections.singletonList(MethodCriteria.UPDATE));
        ResourceTrigger updateMedicationDispenseResourceTrigger = new ResourceTrigger(ResourceType.MedicationDispense, Collections.singletonList(MethodCriteria.UPDATE));
        ResourceTrigger updateMedicationStatementResourceTrigger = new ResourceTrigger(ResourceType.MedicationStatement, Collections.singletonList(MethodCriteria.UPDATE));
        ResourceTrigger updateMedicationAdministrationResourceTrigger = new ResourceTrigger(ResourceType.MedicationAdministration, Collections.singletonList(MethodCriteria.UPDATE));
        ResourceTrigger updateServiveRequestResourceTrigger = new ResourceTrigger(ResourceType.ServiceRequest, Collections.singletonList(MethodCriteria.UPDATE));
        ResourceTrigger updateProcedureResourceTrigger = new ResourceTrigger(ResourceType.Procedure, Collections.singletonList(MethodCriteria.UPDATE));
        ResourceTrigger updateImmunizationResourceTrigger = new ResourceTrigger(ResourceType.Immunization, Collections.singletonList(MethodCriteria.UPDATE));
        ResourceTrigger updateObervationResourceTrigger = new ResourceTrigger(ResourceType.Observation, Collections.singletonList(MethodCriteria.UPDATE), "category=laboratory");
        ResourceTrigger updatePatientResourceTrigger = new ResourceTrigger(ResourceType.Patient, Collections.singletonList(MethodCriteria.UPDATE));

        List<ResourceTrigger> newDiagnosisTriggers = Collections.singletonList(newConditionResourceTrigger);
        List<ResourceTrigger> newLabresultTriggers = Collections.singletonList(newObervationResourceTrigger);
        List<ResourceTrigger> newOrderTriggers = Collections.singletonList(newServiveRequestResourceTrigger);
        List<ResourceTrigger> newProcedureChangeTriggers = Collections.singletonList(newProcedureResourceTrigger);
        List<ResourceTrigger> newImmunizationTriggers = Collections.singletonList(newImmunizationResourceTrigger);

        List<ResourceTrigger> encounterChangeTriggers = Collections.singletonList(updateEncounterResourceTrigger);
        List<ResourceTrigger> diagnosisChangeTriggers = Collections.singletonList(updateConditionResourceTrigger);
        List<ResourceTrigger> labresultChangeTriggers = Collections.singletonList(updateObervationResourceTrigger);
        List<ResourceTrigger> orderChangeTriggers = Collections.singletonList(updateServiveRequestResourceTrigger);
        List<ResourceTrigger> procedureChangeTriggers = Collections.singletonList(updateProcedureResourceTrigger);
        List<ResourceTrigger> immunizationChangeTriggers = Collections.singletonList(updateImmunizationResourceTrigger);
        List<ResourceTrigger> demographicChangeTriggers = Collections.singletonList(updatePatientResourceTrigger);

        List<ResourceTrigger> newMedicationTriggers = new ArrayList<>();
        newMedicationTriggers.add(newMedicationRequestResourceTrigger);
        newMedicationTriggers.add(newMedicationDispenseResourceTrigger);
        newMedicationTriggers.add(newMedicationStatementResourceTrigger);
        newMedicationTriggers.add(newMedicationAdministrationResourceTrigger);

        List<ResourceTrigger> medicationChangeTriggers = new ArrayList<>();
        medicationChangeTriggers.add(updateMedicationRequestResourceTrigger);
        medicationChangeTriggers.add(updateMedicationDispenseResourceTrigger);
        medicationChangeTriggers.add(updateMedicationStatementResourceTrigger);
        medicationChangeTriggers.add(updateMedicationAdministrationResourceTrigger);

        this.subscriptionTopics = new ArrayList<>();
        this.subscriptionTopics.add(new SubscriptionTopic("encounter-change", "encounter-change", "http://hl7.org/fhir/us/medmorph/SubscriptionTopic/encounter-change", encounterChangeTriggers));
        this.subscriptionTopics.add(new SubscriptionTopic("diagnosis-change", "diagnosis-change", "http://hl7.org/fhir/us/medmorph/SubscriptionTopic/diagnosis-change", diagnosisChangeTriggers));
        this.subscriptionTopics.add(new SubscriptionTopic("new-diagnosis", "new-diagnosis", "http://hl7.org/fhir/us/medmorph/SubscriptionTopic/new-diagnosis", newDiagnosisTriggers));
        this.subscriptionTopics.add(new SubscriptionTopic("medication-change", "medication-change", "http://hl7.org/fhir/us/medmorph/SubscriptionTopic/medication-change", medicationChangeTriggers));
        this.subscriptionTopics.add(new SubscriptionTopic("new-medication", "new-medication", "http://hl7.org/fhir/us/medmorph/SubscriptionTopic/new-medication", newMedicationTriggers));
        this.subscriptionTopics.add(new SubscriptionTopic("labresult-change", "labresult-change", "http://hl7.org/fhir/us/medmorph/SubscriptionTopic/labresult-change", labresultChangeTriggers));
        this.subscriptionTopics.add(new SubscriptionTopic("new-labresult", "new-labresult", "http://hl7.org/fhir/us/medmorph/SubscriptionTopic/new-labresult", newLabresultTriggers));
        this.subscriptionTopics.add(new SubscriptionTopic("order-change", "order-change", "http://hl7.org/fhir/us/medmorph/SubscriptionTopic/order-change", orderChangeTriggers));
        this.subscriptionTopics.add(new SubscriptionTopic("new-order", "new-order", "http://hl7.org/fhir/us/medmorph/SubscriptionTopic/new-order", newOrderTriggers));
        this.subscriptionTopics.add(new SubscriptionTopic("procedure-change", "procedure-change", "http://hl7.org/fhir/us/medmorph/SubscriptionTopic/procedure-change", procedureChangeTriggers));
        this.subscriptionTopics.add(new SubscriptionTopic("new-procedure", "new-procedure", "http://hl7.org/fhir/us/medmorph/SubscriptionTopic/new-procedure", newProcedureChangeTriggers));
        this.subscriptionTopics.add(new SubscriptionTopic("immunization-change", "immunization-change", "http://hl7.org/fhir/us/medmorph/SubscriptionTopic/immunization-change", immunizationChangeTriggers));
        this.subscriptionTopics.add(new SubscriptionTopic("new-immunization", "new-immunization", "http://hl7.org/fhir/us/medmorph/SubscriptionTopic/new-immunization", newImmunizationTriggers));
        this.subscriptionTopics.add(new SubscriptionTopic("demographic-change", "demographic-change", "http://hl7.org/fhir/us/medmorph/SubscriptionTopic/demographic-change", demographicChangeTriggers));
    }
    
    public static List<SubscriptionTopic> getAllTopics() {
        if (singletonMedmorphSubscriptionTopics == null) {
            singletonMedmorphSubscriptionTopics = new MedmorphSubscriptionTopics();
        }

        return singletonMedmorphSubscriptionTopics.subscriptionTopics;
    }
}
