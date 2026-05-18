package com.maan.whatsapp.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AiFlowService {

    @Autowired
    private AiUserSessionRepository repo;

    public String process(String mobile, String message) {

        AiUserSession session =
                repo.findByMobileNo(mobile)
                .orElse(new AiUserSession());

        session.setMobileNo(mobile);

        if(session.getCurrentStep() == null) {

            session.setCurrentStep("NAME");

            repo.save(session);

			return "Please enter your name";
        }

        else if("NAME".equals(session.getCurrentStep())) {

            session.setCustomerName(message);

            session.setCurrentStep("VEHICLE");

            repo.save(session);

            return "Please enter vehicle number";
        }

        else if("VEHICLE".equals(session.getCurrentStep())) {

            session.setVehicleNo(message);

            session.setCurrentStep("CONFIRM");

            repo.save(session);

            return "Click confirm button";
        }

        return "Completed";
    }
}