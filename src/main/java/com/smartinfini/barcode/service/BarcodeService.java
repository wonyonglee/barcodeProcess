package com.smartinfini.barcode.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartinfini.barcode.dao.mongo.GetMongoDAO;
import com.smartinfini.barcode.dao.mongo.SetMongoDAO;
import com.smartinfini.barcode.dto.mongo.barcodeDTO;
import com.smartinfini.barcode.dto.mongo.barcodeLogDTO;
import com.smartinfini.barcode.log.LogService;
import com.smartinfini.barcode.repository.SmartPlusBarcodeMapper;

/**
 * @author WONYONG
 * @description 바코드를 꺼내옴.
 */
@Component
public class BarcodeService {
	private static final Logger logger = LoggerFactory.getLogger(BarcodeService.class);
	
	@Resource(name="smartPlusSqlSession")
	private SqlSession smartPlusSqlSession;
	
	@Resource(name="smartPlusTransactionManager")
	private PlatformTransactionManager smartPlusTransactionManager;

	@Autowired
	private LogService logService;
	
	@Autowired
	private SetMongoDAO setMongoDAO;	
	
	@Autowired
	private GetMongoDAO getMongoDAO;	
	
	public Map<String, Object> getBarcodeProcess(Map<String, Object> param, barcodeDTO barcodeDTO) throws Exception {
		Map<String, Object> rtnMap = new HashMap<String, Object>();
		Map<String, Object> barcodeMap = this.BarcodeProcess(param.get("barcodeType").toString(), param);
		
		rtnMap.put("result", false);
		
		if ( param.containsKey("barcodeSelectionCount") ) {
			if ( (int)param.get("barcodeSelectionCount") > 5 ) {
				barcodeMap.put("result", false);
			} else {
				param.put("barcodeSelectionCount", ((int)param.get("barcodeSelectionCount") + 1));
			}
		} else {
			param.put("barcodeSelectionCount", 1);
		}
		
		if ( (boolean) barcodeMap.get("result") ) {
			String barcode = barcodeMap.get("barcode").toString();

			barcodeDTO.setBarcode(barcode);
			barcodeDTO.setOrderNum(param.get("orderNum").toString());
			
			if ( !getMongoDAO.isExistBarcode(barcodeDTO) ) {
				// mongo에 데이터 없음
				setMongoDAO.putBarcodeInfo(barcodeDTO);
				rtnMap.put("result", true);
				rtnMap.put("barcode", barcodeDTO.getBarcode());
			} else {
				// 데이터 존재 바코드 재생성				
				this.getBarcodeProcess(param, new barcodeDTO());
			}					
		}
		
		return rtnMap;
	}
	
	// 요청받은 타입별 바코드 생성
	private Map<String, Object> BarcodeProcess(String barcodeType, Map<String, Object> param) throws Exception {
		String barcode = "";
		Map<String, Object> rtn = new HashMap<String, Object>();
		rtn.put("result", false);
		
		try {
			switch (barcodeType) {
				case "main":
					barcode = this.getMainBarcode(param.get("orderNum").toString());
					break;
				case "multi":
					barcode = this.getMultiBarcode(param.get("orderNum").toString());
					break;
				case "aqmis":
					barcode = this.getAqmisBarcode(param.get("orderNum").toString());
					break;
				case "kamis":
					barcode = this.getKamisBarcode(param.get("orderNum").toString());
					break;
				case "normal":
					barcode = this.getNormalBarcode(param.get("orderNum").toString());
					break;
				case "coupon":
					barcode = this.getCouponBarcode(param.get("orderNum").toString(), (int) param.get("couponIdx"), param.get("couponTicketType").toString());
					break;
			}
			
			if ( barcode == null ) {
				if ( barcodeType.equals("coupon") ) throw new Exception("쿠폰 수량 미존재 [ CouponIdx : "+ param.get("couponIdx") +", CouponTicketType : "+ param.get("couponTicketType") +" ]");
			} else {
				if ( !barcode.equals("") ) {
					rtn.put("result", true);
					rtn.put("barcode", barcode);				
				}
			}
		} catch (Exception e) {
			logger.info("[Barcode selection failed] > " + param.get("orderNum").toString());
			setLog(barcodeType, param, e);
		}
		
		return rtn;
	}
	
	// 통합로그 및 자체(바코드)로그 처리
	private void setLog(String barcodeType, Map<String, Object> param, Exception e) throws Exception {
		barcodeLogDTO barcodeLogDTO = new barcodeLogDTO();
		Map<String, Object> logContents = new HashMap<String, Object>();
		
		logContents.put("code", "9999");
		logContents.put("type", barcodeType);
		logContents.put("title", "[Barcode Selection failed]");
		logContents.put("body", e.toString());
		
		String errorContent = new ObjectMapper().writeValueAsString(logContents);
		
		barcodeLogDTO.setContents(errorContent);
		barcodeLogDTO.setOrderNum(param.get("orderNum").toString());
		
		setMongoDAO.putBarcodeLog(barcodeLogDTO);
		
		logService.setLogMap(param.get("orderNum").toString(), "", "API", "바코드", "생성", errorContent, "E");
	}
	
	private String getMainBarcode(String orderNum) throws Exception {
		return this.smartPlusSqlSession.getMapper(SmartPlusBarcodeMapper.class).getMainBarcode(orderNum);
	}
	
	private String getMultiBarcode(String orderNum) throws Exception {
		return this.smartPlusSqlSession.getMapper(SmartPlusBarcodeMapper.class).getMultiBarcode(orderNum);
	}	
	
	private String getAqmisBarcode(String orderNum) throws Exception {
		return this.smartPlusSqlSession.getMapper(SmartPlusBarcodeMapper.class).getAqmisBarcode(orderNum);
	}
	
	private String getKamisBarcode(String orderNum) throws Exception {
		return this.smartPlusSqlSession.getMapper(SmartPlusBarcodeMapper.class).getKamisBarcode(orderNum);
	}	
	
	private String getNormalBarcode(String orderNum) throws Exception {
		return this.smartPlusSqlSession.getMapper(SmartPlusBarcodeMapper.class).getNormalBarcode(orderNum);
	}	
	
	private String getCouponBarcode(String orderNum, Integer couponIdx, String ticketType) throws Exception {
		Map<String, Object> param = new HashMap<String, Object>();
		
		param.put("orderNum", orderNum);
		param.put("couponIdx", couponIdx);
		param.put("ticketType", ticketType);
		
		return this.smartPlusSqlSession.getMapper(SmartPlusBarcodeMapper.class).getCouponBarcode(param);
	}	
}
