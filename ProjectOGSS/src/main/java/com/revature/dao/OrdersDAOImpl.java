package com.revature.dao;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.revature.model.Invoice;
import com.revature.model.ItemList;
import com.revature.model.Product;
import com.revature.model.UserAccount;
import com.revature.struct.Token;

@Repository
public class OrdersDAOImpl implements OrdersDAO {
	private SessionFactory sf;
	private UserAccountDAO userAccountDAO;
	private DriverDAO driverDAO;

	@Autowired
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sf = sessionFactory;
	}

	@Autowired
	public void setUserAccountDAO(UserAccountDAO userAccountDAO) {
		this.userAccountDAO = userAccountDAO;
	}

	@Autowired
	public void setDriverDAO(DriverDAO driverDAO) {
		this.driverDAO = driverDAO;
	}

	@Override
	public Boolean checkout(Invoice invoice) {
		try {

			Session session = sf.openSession();
			Transaction tx = session.beginTransaction();

			UserAccount userAccount = userAccountDAO.getUserAccount(new Token(invoice.getCustomer().getSessionToken()));
			if (userAccount == null) {
				return null;
			}
			// session.persist(userAccount);
			invoice.setCustomer(userAccount);
			UserAccount tempUser = driverDAO.getAvailableDriver();
			if (tempUser != null) {
				invoice.setDriver(tempUser);
			}

			List<ItemList> iList = new ArrayList<ItemList>();
			for (ItemList p : invoice.getItemList()) {
				String getProductHQL = "SELECT p FROM Product p WHERE p.id = :productID";
				Query query = session.createQuery(getProductHQL);
				query.setParameter("productID", p.getProduct().getId());
				Product productResult = (Product) query.uniqueResult();
				productResult.setInventoryQuantity(productResult.getInventoryQuantity() - p.getQuantity());

				ItemList item = new ItemList();
				item.setOrder(invoice);
				item.setProduct(productResult);
				item.setQuantity(p.getQuantity());
				iList.add(item);
			}
			invoice.setItemList(iList);
			session.save(invoice);
			session.flush();
			tx.commit();
			session.close();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public List<Invoice> getInvoices(Token token) {
		Session session = sf.openSession();
		Transaction tx = session.beginTransaction();

		UserAccount userAccount = userAccountDAO.getUserAccount(token);
		if (userAccount == null) {
			return null;
		}

		String getInvoiceHQL = "SELECT invoice FROM Invoice invoice WHERE invoice.customer.id = :customerID";
		Query query = session.createQuery(getInvoiceHQL);
		query.setParameter("customerID", userAccount.getId());
		List<Invoice> listresults = (List<Invoice>) query.list();

		tx.commit();
		session.close();
		return listresults;
	}

	@Override
	public List<Invoice> getDriverOrders(Token token) {
		Session session = sf.openSession();
		Transaction tx = session.beginTransaction();

		UserAccount userAccount = userAccountDAO.getUserAccount(token);
		if (userAccount == null) {
			return null;
		}

		String getInvoiceHQL = "SELECT invoice FROM Invoice invoice WHERE invoice.driver.id = :driverID";
		Query query = session.createQuery(getInvoiceHQL);
		query.setParameter("driverID", userAccount.getId());
		List<Invoice> listresults = (List<Invoice>) query.list();

		tx.commit();
		session.close();
		return listresults;
	}

	@Override
	public Invoice updateOrderDriver(Invoice invoice) {
		Session session = sf.openSession();
		Transaction tx = session.beginTransaction();

		UserAccount userAccount = userAccountDAO.getUserAccount(new Token(invoice.getCustomer().getSessionToken()));
		if (userAccount == null) {
			return null;
		} else if (userAccount.getAccountType().getType().equals("customer")
				|| userAccount.getAccountType().getType().equals("")) {
			return null;
		}

		String getInvoiceHQL = "SELECT invoice FROM Invoice invoice WHERE invoice.id = :invoiceID";
		Query query = session.createQuery(getInvoiceHQL);
		query.setParameter("invoiceID", invoice.getId());
		Invoice listresult = (Invoice) query.uniqueResult();
		listresult.setStatus(invoice.getStatus());
		listresult.setDeliveryComments(invoice.getDeliveryComments());
		tx.commit();
		session.close();
		return listresult;
	}

}
