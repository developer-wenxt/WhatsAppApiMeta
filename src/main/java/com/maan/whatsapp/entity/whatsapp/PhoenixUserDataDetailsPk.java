package com.maan.whatsapp.entity.whatsapp;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
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
@Embeddable
public class PhoenixUserDataDetailsPk implements Serializable{/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Long waid;
	private String wamessageid;

}
