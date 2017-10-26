package com.smartinfini.barcode.dto.mongo;

import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.mongodb.core.query.Update;

import com.fasterxml.jackson.databind.ObjectMapper;

public class barcodeLogDTO {
	private String contents = "";
	private String orderNum = "";

	public String getContents() {
		return contents;
	}
	
	public void setContents(String contents) {
		this.contents = contents;
	}
	
	public String getOrderNum() {
		return orderNum;
	}
	
	public void setOrderNum(String orderNum) {
		this.orderNum = orderNum;
	}
	
	public Update getUpdateObject() throws Exception {
		Update update = new Update();
		update.set("contents", 		this.getContents())
				.set("orderNum", 	this.getOrderNum());				
				
		return update;
	}
}
