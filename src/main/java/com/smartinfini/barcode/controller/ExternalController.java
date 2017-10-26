package com.smartinfini.barcode.controller;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartinfini.barcode.log.LogService;
import com.smartinfini.barcode.rabbitmq.MQProcessService;
import com.smartinfini.barcode.service.ValidationService;
import com.smartinfini.barcode.test.CheckBarcodeService;

/**
 * @author WONYONG
 * @description 바코드 컨트롤러
 * @return 
 * @throws Exception
 */
@RestController
public class ExternalController {
	private static final Logger logger = LoggerFactory.getLogger(ExternalController.class);
	
	@Autowired
	private LogService logService;
	
	@Autowired
	private ValidationService validationService;
	
	@Autowired
	private MQProcessService mqProcessService;
	
	@Autowired
	private CheckBarcodeService checkBarcodeService;

	// 바코드 생성로직
    @RequestMapping(value = "/system/barcode", method = RequestMethod.POST, consumes = "application/json; charset=utf-8", produces = "application/json; charset=utf-8")
    @ResponseBody
    public String SetResellerCd(HttpServletRequest request, HttpServletResponse response, InputStream is, OAuth2Authentication authentication) throws Exception {
    	Map<String, Object> rtn = new HashMap<String, Object>();

    	try {
    		Map<String, Object> paramMap = (new ObjectMapper()).readValue(is, new TypeReference<Map<String, Object>>() {});
    		rtn.put("code", "0000");
    		validationService.dataValidation(rtn, paramMap);
    		
    		if ( rtn.get("code").equals("0000") ) {
    			rtn.put("barcode", mqProcessService.executeBarcodeProcess(paramMap));
    		}
		} catch (Exception e) {
			e.printStackTrace();
			rtn.put("code", "9999");
			rtn.put("message", "데이터 처리도중 에러발생");
		}
    	
    	return new ObjectMapper().writeValueAsString(rtn);
    }	      
}
