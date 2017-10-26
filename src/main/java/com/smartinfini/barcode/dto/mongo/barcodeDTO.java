package com.smartinfini.barcode.dto.mongo;

import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.mongodb.core.query.Update;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author WONYONG
 * @description 바코드설정 관련 객체
 */
public class barcodeDTO {
	private Map<String, Object> barcodeMap = new HashMap<String, Object>();
	private String barcode = "";
	private String orderNum = "";

	public String getBarcode() {
		return barcode;
	}
	public void setBarcode(String barcode) {
		this.barcode = barcode;
	}
	public String getOrderNum() {
		return orderNum;
	}
	public void setOrderNum(String orderNum) {
		this.orderNum = orderNum;
	}

	public Update getUpdateObject() throws Exception {
		Update update = new Update();
		update.set("barcode", 		this.getBarcode())
				.set("orderNum", 	this.getOrderNum());
				
		return update;
	}
	
	public Map<String, Object> getExistBarcode() throws Exception {
		barcodeMap.put("barcode", this.getBarcode());
		
		return barcodeMap;
	}
}
