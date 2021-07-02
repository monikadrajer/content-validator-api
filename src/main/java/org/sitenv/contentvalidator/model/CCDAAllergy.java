package org.sitenv.contentvalidator.model;

import org.apache.log4j.Logger;
import org.sitenv.contentvalidator.dto.ContentValidationResult;
import org.sitenv.contentvalidator.dto.enums.ContentValidationResultLevel;
import org.sitenv.contentvalidator.dto.enums.SeverityLevel;
import org.sitenv.contentvalidator.parsers.ParserUtilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CCDAAllergy {
	
	private static Logger log = Logger.getLogger(CCDAAllergy.class.getName());

	private ArrayList<CCDAII>       		sectionTemplateId;
	private CCDACode                 		sectionCode;
	private ArrayList<CCDAAllergyConcern>	allergyConcern;
	
	private CCDAAuthor 						author;
	
	public void log() {
		
		if(sectionCode != null)
			log.info(" Allergy Section Code = " + sectionCode.getCode());
		
		for(int j = 0; j < sectionTemplateId.size(); j++) {
			log.info(" Tempalte Id [" + j + "] = " + sectionTemplateId.get(j).getRootValue());
			log.info(" Tempalte Id Ext [" + j + "] = " + sectionTemplateId.get(j).getExtValue());
		}
		
		for(int k = 0; k < allergyConcern.size(); k++) {
			allergyConcern.get(k).log();
		}
		
		if(author != null)
			author.log();
	}	
	
	public void compare(CCDAAllergy submittedAllergy, ArrayList<ContentValidationResult> results, CCDARefModel submittedCCDA) {
		
		// handle section code.
		ParserUtilities.compareCode(sectionCode, submittedAllergy.getSectionCode(), results, "Allergy Section");
		
		// Handle Section Template Ids
		ParserUtilities.compareTemplateIds(sectionTemplateId, submittedAllergy.getSectionTemplateId(), results, "Allergy Section");
		
		//Compare details
		compareAllergyData(submittedAllergy, results, submittedCCDA);
	}
	
	private void compareAllergyData(CCDAAllergy submittedAllergy, ArrayList<ContentValidationResult> results, CCDARefModel submittedCCDA) {
		
		HashMap<CCDAAllergyObs, CCDAAllergyConcern> allergies = getAllergyObservationsConcernMap();
		
		for(Map.Entry<CCDAAllergyObs, CCDAAllergyConcern> ent: allergies.entrySet()) {
			
			//check to see if the ref data is part of the problem data submitted
			log.info(" Comparing Allergy Data for " + ent.getKey());
			submittedAllergy.validateAllergyData(ent.getKey(), ent.getValue(), results, submittedCCDA);
			
		}
		
	}
	
	private void validateAllergyData(CCDAAllergyObs refAl, CCDAAllergyConcern refCo, ArrayList<ContentValidationResult> results, 
			CCDARefModel submittedCCDA) {
	
		HashMap<CCDAAllergyObs, CCDAAllergyConcern> allergies = getAllergyObservationsConcernMap();
		
		CCDAAllergyObs subObs = null;
		CCDAAllergyConcern conc = null;
		for(Map.Entry<CCDAAllergyObs, CCDAAllergyConcern> ent: allergies.entrySet()) {
			
			// Find the entry which corresponds to the same value as the refPo
			if(ent.getKey().getAllergySubstance() != null && 
			   refAl.getAllergySubstance() != null && 
			   refAl.getAllergySubstance().getCode().equalsIgnoreCase(ent.getKey().getAllergySubstance().getCode())) {
				
				log.info("Found the Allergy Observation in submitted CCDA");
				subObs = ent.getKey();
				conc = ent.getValue();
				
				break;
			}
			else if(refAl.getAllergySubstance() == null && 
					ent.getKey().getAllergySubstance() == null ) {
				
				log.info("Found an empty Allergy Observation in submitted CCDA");
				subObs = ent.getKey();
				conc = ent.getValue();
				
				break;
			}
			
		}
		
		if( (subObs != null) && (conc != null) ) {
			 
			String allergyObsContext = ((refAl.getAllergySubstance() != null)?(refAl.getAllergySubstance().getDisplayName()):" No Known Allergy ");
			refCo.compare(conc, allergyObsContext, results);
			refAl.compare(subObs, allergyObsContext, results, submittedCCDA);
		}
		else {
			
			String error = "The scenario contains Allergy observation for " + 
							((refAl.getAllergySubstance() != null)?((refAl.getAllergySubstance().getDisplayName()) + " Code: " + refAl.getAllergySubstance().getCode()):" No Known Allergies ") +
							" , however there is no matching data in the submitted CCDA. ";
			ContentValidationResult rs = new ContentValidationResult(error, ContentValidationResultLevel.ERROR, "/ClinicalDocument", "0" );
			results.add(rs);
		}
		
	}
	
	private HashMap<CCDAAllergyObs, CCDAAllergyConcern> getAllergyObservationsConcernMap() {
		
		HashMap<CCDAAllergyObs, CCDAAllergyConcern> allergies = new HashMap<CCDAAllergyObs, CCDAAllergyConcern>();
		
		for(CCDAAllergyConcern c: allergyConcern) {
			
			ArrayList<CCDAAllergyObs> obs = c.getAllergyObs();
			
			for(CCDAAllergyObs o: obs) {
				allergies.put(o,  c);
			}
			
		}
		
		log.info("AllergyObservation-Concern Hash Map ize is " + allergies.size());
		return allergies;
	}
	
	public void compareAuthor(CCDAAllergy subAllergy, ArrayList<ContentValidationResult> results, boolean curesUpdate,
			ArrayList<CCDAAuthor> authorsWithLinkedReferenceData) {
		String elName = "Allergy Section";
		
		CCDAAuthor.compareSectionLevelAuthor(elName, author,
				subAllergy != null && subAllergy.getAuthor() != null ? subAllergy.getAuthor() : null, results);

		log.info("Comparing Authors for Allergy Concerns");
		ArrayList<CCDAAuthor> refAllConcAuths = this.getAllergyConcernAuthors();
		ArrayList<CCDAAuthor> subAllConcAuths = subAllergy != null && subAllergy.getAllergyConcernAuthors() != null
				? subAllergy.getAllergyConcernAuthors()
				: null;
		elName += "/AllergyConcern";
		CCDAAuthor.compareAuthors(refAllConcAuths, subAllConcAuths, results, elName, authorsWithLinkedReferenceData);

		log.info("Comparing Authors for Allergy Observations");
		ArrayList<CCDAAuthor> refAllObsAuths = this.getAllergyObsAuthors();
		ArrayList<CCDAAuthor> subAllObsAuths = subAllergy != null && subAllergy.getAllergyObsAuthors() != null
				? subAllergy.getAllergyObsAuthors()
				: null;
		elName += "/AllergyObservation";
		CCDAAuthor.compareAuthors(refAllObsAuths, subAllObsAuths, results, elName, authorsWithLinkedReferenceData);

		log.info("Comparing Authors for Allergy Reactions");
		ArrayList<CCDAAuthor> refAllReactAuths = this.getAllergyReactionAuthors();
		ArrayList<CCDAAuthor> subAllReactAuths = subAllergy != null && subAllergy.getAllergyReactionAuthors() != null
				? subAllergy.getAllergyReactionAuthors() 
				: null;			
		elName += "/AllergyReaction";
		CCDAAuthor.compareAuthors(refAllReactAuths, subAllReactAuths, results, elName, authorsWithLinkedReferenceData);
	}
	
	public ArrayList<CCDAAuthor> getAllergyConcernAuthors() {
		
		ArrayList<CCDAAuthor> authors = new ArrayList<CCDAAuthor>();
		
		for(CCDAAllergyConcern allConc : allergyConcern) {
			
			if(allConc.getAuthor() != null) {
				authors.add(allConc.getAuthor());
			}
		}	
		
		return authors;
	}
	
	public ArrayList<CCDAAuthor> getAllergyObsAuthors() {
		
		ArrayList<CCDAAuthor> authors = new ArrayList<CCDAAuthor>();
		
		for(CCDAAllergyConcern allConc : allergyConcern) {
			
			for(CCDAAllergyObs allObs : allConc.getAllergyObs()) {
				
				if(allObs.getAuthor() != null) {
					authors.add(allObs.getAuthor());
				}
				
			}
		}	
		
		return authors;
	}
	
	public ArrayList<CCDAAuthor> getAllergyReactionAuthors() {
		ArrayList<CCDAAuthor> authors = new ArrayList<CCDAAuthor>();

		for (CCDAAllergyConcern curConcern : allergyConcern) {
			for (CCDAAllergyObs curObs : curConcern.getAllergyObs()) {
				for (CCDAAllergyReaction curReaction : curObs.getReactions()) {
					if (curReaction.getAuthor() != null) {
						authors.add(curReaction.getAuthor());
					}
				}
			}
		}
		return authors;
	}
	
	public ArrayList<CCDAII> getSectionTemplateId() {
		return sectionTemplateId;
	}

	public void setSectionTemplateId(ArrayList<CCDAII> ids) {
		
		if(ids != null)
			this.sectionTemplateId = ids;
	}

	public CCDACode getSectionCode() {
		return sectionCode;
	}

	public void setSectionCode(CCDACode sectionCode) {
		this.sectionCode = sectionCode;
	}

	public ArrayList<CCDAAllergyConcern> getAllergyConcern() {
		return allergyConcern;
	}

	public void setAllergyConcern(ArrayList<CCDAAllergyConcern> acs) {
		
		if(acs != null)
			this.allergyConcern = acs;
	}

	public CCDAAllergy()
	{
		sectionTemplateId = new ArrayList<CCDAII>();
		allergyConcern = new ArrayList<CCDAAllergyConcern>();
	}

	public CCDAAuthor getAuthor() {
		return author;
	}

	public void setAuthor(CCDAAuthor author) {
		this.author = author;
	}
	
}
