package com.maan.whatsapp.repository.whatsapp;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.stereotype.Repository;

import com.maan.whatsapp.entity.whatsapp.WADataDetail;
import com.maan.whatsapp.entity.whatsapp.WADataDetailPK;

@Repository
@QuerydslPredicate
public interface WADataDetailRepo
		extends JpaRepository<WADataDetail, WADataDetailPK>, QuerydslPredicateExecutor<WADataDetail> {

	List<WADataDetail> findByWaddPk_WaidOrderByEntrydateDesc(Long waid);

	@Query(value = "SELECT WDD.WHATSAPPID, WDD.PRIOR_ID AS PARENTMESSAGEID, WDD.ENTRYDATE,WDD.ISINPUT,WDD.INPUT_VALUE, WDD.REQUEST_KEY,WDD.USERMESSAGEID FROM( SELECT REQ_DATA.WHATSAPPID, REQ_DATA.PARENTMESSAGEID, REQ_DATA.ENTRYDATE, LAG(REQ_DATA.PARENTMESSAGEID) OVER (ORDER BY REQ_DATA.ENTRYDATE DESC) AS PRIOR_ID,REQ_DATA.ISINPUT,REQ_DATA.INPUT_VALUE, REQ_DATA.REQUEST_KEY,REQ_DATA.USERMESSAGEID FROM ( SELECT MAIN_DATA.WHATSAPPID, MAIN_DATA.PARENTMESSAGEID, MAIN_DATA.ENTRYDATE,MAIN_DATA.ISINPUT,MAIN_DATA.INPUT_VALUE, MAIN_DATA.REQUEST_KEY,MAIN_DATA.USERMESSAGEID FROM ( SELECT ENTRYDATE, WHATSAPPID, PARENTMESSAGEID,ISINPUT,INPUT_VALUE, REQUEST_KEY, ROW_NUMBER() OVER (ORDER BY ENTRYDATE DESC) AS RN,USERMESSAGEID FROM whatsapp_data_detail WHERE WHATSAPPID=?1 AND STATUS='Y') AS MAIN_DATA ) AS REQ_DATA WHERE REQ_DATA.ENTRYDATE BETWEEN (SELECT MAX(ENTRYDATE) FROM whatsapp_data_detail WHERE WHATSAPPID=?1 AND STATUS='Y' AND PARENTMESSAGEID = 'COM001' ) AND REQ_DATA.ENTRYDATE) AS WDD", nativeQuery = true)
	List<Map<String, Object>> getChatInputData(String waid);

	@Query(value = "SELECT MESSAGECONTENT MESSAGE, CAST (USERREPLY_MSG AS NVARCHAR2(500)) USERREPLY,CAST('' AS NVARCHAR2(1)) VALMSG,ISJOBYN, 'N' FILE_YN,'' FILE_PATH, 'N' ISDOCUPLYN, '' LOCWA_USERFILEPATH, 'Y' ISREPLYYN,ENTRYDATE, 'Y' ISCHAT "
			+ "FROM WHATSAPP_DATA_DETAIL WDD WHERE WDD.WHATSAPPID = ?1 AND TRUNC(WDD.ENTRYDATE)=TO_DATE(?2,'DD/MM/YYYY') "
			+ "UNION ALL SELECT MESSAGE, USERREPLY, VALIDATIONMESSAGE VALMSG, ISJOBYN,FILE_YN,FILE_PATH, ISDOCUPLYN, LOCWA_USERFILEPATH,ISREPLYYN,ENTRY_DATE ENTRYDATE,'N' ISCHAT "
			+ "FROM WHATSAPP_REQUEST_DETAIL WRD WHERE WRD.WHATSAPPID = ?1 AND TRUNC(WRD.ENTRY_DATE)=TO_DATE(?2,'DD/MM/YYYY') ORDER BY ENTRYDATE", nativeQuery = true)
	List<Map<String, Object>> getMsgDet(String whatsappid, String entryDate);

	List<WADataDetail> findByWaddPk_WaidAndEntrydateBetweenOrderByEntrydateDesc(Long waid, Date startDate, Date endDate);

}
