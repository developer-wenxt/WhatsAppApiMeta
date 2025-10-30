package com.maan.whatsapp.repository.whatsapp;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.maan.whatsapp.entity.whatsapp.PhoenixUserDataDetails;
import com.maan.whatsapp.entity.whatsapp.PhoenixUserDataDetailsPk;

public interface PhoenixUserDataDetailsRepo extends JpaRepository<PhoenixUserDataDetails,PhoenixUserDataDetailsPk>{

	List<PhoenixUserDataDetails> findByWaidAndParentMessageIdAndUserMessageIdAndCompanyId(Long waid, String string,
			String string2, String string3);

	List<PhoenixUserDataDetails> findTop1ByWaidAndCompanyIdAndUserMessageIdInOrderByEntryDateDesc(Long waid,
			String string, List<String> messageIds);

	PhoenixUserDataDetails findTop1ByWaidAndCompanyId(Long mobileNumber, String string);

}
