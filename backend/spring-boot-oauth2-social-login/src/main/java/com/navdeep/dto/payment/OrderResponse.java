package com.navdeep.dto.payment;

import lombok.Data;

@Data
public class OrderResponse {
	private String applicationFee;
	private String razorpayOrderId;
	private String secretKey;
}
