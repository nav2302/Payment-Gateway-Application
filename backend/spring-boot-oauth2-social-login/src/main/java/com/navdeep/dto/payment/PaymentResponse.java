package com.navdeep.dto.payment;

import lombok.Data;

@Data
public class PaymentResponse {
	private String razorpayOrderId;
	private String razorpayPaymentId;
	private String razorpaySignature;
}
