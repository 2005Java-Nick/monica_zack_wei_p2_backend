package com.revature.service;

import java.util.List;

import com.revature.model.Invoice;
import com.revature.struct.Token;

public interface OrdersService {

	Boolean checkout(Invoice invoice);

	List<Invoice> getInvoices(Token token);

}