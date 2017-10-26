package com.smartinfini.barcode.rabbitmq;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartinfini.barcode.dao.mongo.GetMongoDAO;
import com.smartinfini.barcode.dto.mongo.barcodeDTO;
import com.smartinfini.barcode.repository.SmartPlusBarcodeMapper;
import com.smartinfini.barcode.service.BarcodeService;
import com.smartinfini.barcode.service.RabbitMQService;

@Service
public class MQProcessService {
	private static final Logger logger = LoggerFactory.getLogger(MQProcessService.class);

	@Resource(name="smartPlusSqlSession")
	private SqlSession smartPlusSqlSession;
	
	@Autowired
	private RabbitMQService rabbitMQService;
	
	@Autowired
	private BarcodeService barcodeService;	
	
	// queue에 하나씩 적재
    public String executeBarcodeProcess(Map<String, Object> paramMap) throws Exception {
    	String barcode = "";
    	
    	Map<String, Object> data = new HashMap<String, Object>();
    	data.put("orderNum", paramMap.get("orderNum"));
    	data.put("barcodeType", paramMap.get("barcodeType"));
    	
    	if ( paramMap.containsKey("couponIdx") ) {
    		data.put("couponIdx", paramMap.get("couponIdx"));
    		data.put("couponTicketType", paramMap.get("couponTicketType"));
    	}
    	
    	try {
    		Object rtn = rabbitMQService.send("barcode_queue", data);
    		
    		if ( rtn.getClass().getName().equals("java.util.HashMap") ) {
    			Map<String, Object> rtnMap = (Map<String, Object>) rtn;
    			
    			if ( (boolean) rtnMap.get("result") ) {
    				barcode = rtnMap.get("barcode").toString();
    				
    				if ( barcode.equals("") ) {
    					throw new Exception("바코드 생성실패");
    				} else {
    					logger.info("["+ paramMap.get("orderNum").toString() +"] > 바코드 생성성공 > " + new ObjectMapper().writeValueAsString(paramMap));
    				}
    			}
    		}
		} catch (Exception e) {
			logger.info("["+ paramMap.get("orderNum").toString() +"] > " + e.toString() + " > " + new ObjectMapper().writeValueAsString(paramMap));
		}
    	
    	return barcode;
    }
	
    // 바코드 처리 (순차처리)
	@RabbitListener(queues="barcode_queue")
    public Object barcodeReceiver(String message) {
		Object data = new Object();

		try {
			Map<String, Object> dataMap = (new ObjectMapper()).readValue(message, new TypeReference<Map<String, Object>>() {});
			data = barcodeService.getBarcodeProcess(dataMap, new barcodeDTO());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return data;
    }
}
