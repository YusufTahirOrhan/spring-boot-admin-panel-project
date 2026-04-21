package com.optimaxx.management.application;

import com.optimaxx.management.domain.model.Lead;
import com.optimaxx.management.domain.model.LeadStatus;
import com.optimaxx.management.domain.repository.LeadRepository;
import com.optimaxx.management.interfaces.rest.dto.ContactRequest;
import org.springframework.stereotype.Service;

@Service
public class PublicContactService {

    private final LeadRepository leadRepository;

    public PublicContactService(LeadRepository leadRepository) {
        this.leadRepository = leadRepository;
    }

    public void submitContactForm(ContactRequest request) {
        Lead lead = new Lead();
        lead.setName(request.getName());
        lead.setEmail(request.getEmail());
        lead.setMessage(request.getMessage());
        lead.setServiceInterest(request.getServiceInterest());
        lead.setStatus(LeadStatus.NEW);

        leadRepository.save(lead);
    }
}
