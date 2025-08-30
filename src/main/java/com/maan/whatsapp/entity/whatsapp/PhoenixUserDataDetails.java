package com.maan.whatsapp.entity.whatsapp;

import java.util.Date;

import org.hibernate.annotations.Collate;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "wh_phoenix_user_data_detail")
@IdClass(PhoenixUserDataDetailsPk.class)
@DynamicInsert
@DynamicUpdate
public class PhoenixUserDataDetails {

	//@EmbeddedId
	//private PhoenixUserDataDetailsPk waddPk;
	
	@Id
	@Column(name="WHATSAPPID")
	private Long waid;
	
	@Id
	@Column(name="WAMESSAGEID")
	private String wamessageid;
	
	@Column(name="USERREPLY")
	private String userReply;
	
	@Column(name="ENTRYDATE")
	private Date entryDate;
	
	@Column(name="PARENTMESSAGEID")
	private String parentMessageId;
	
	@Column(name="USERMESSAGEID")
	private String userMessageId;
	
	@Column(name="STATUS")
	private String status;
	
	@Column(name="FLOWREQUEST")
	private String flowRequest;
	
	@Column(name="COMPANYID")
	private String companyId;
}
