package org.vp.pis.controller;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.vp.pis.domain.Account;
import org.vp.pis.domain.AccountMain;
import org.vp.pis.domain.Accounts;
import org.vp.pis.domain.Amount;
import org.vp.pis.domain.Balance;
import org.vp.pis.domain.BankTransactionCode;
import org.vp.pis.domain.CreditLine;
import org.vp.pis.domain.CreditorAccount;
import org.vp.pis.domain.Data;
import org.vp.pis.domain.DirectDebit;
import org.vp.pis.domain.FinalPaymentAmount;
import org.vp.pis.domain.FirstPaymentAmount;
import org.vp.pis.domain.Links;
import org.vp.pis.domain.MerchantDetails;
import org.vp.pis.domain.Meta;
import org.vp.pis.domain.NextPaymentAmount;
import org.vp.pis.domain.Page;
import org.vp.pis.domain.PreviousPaymentAmount;
import org.vp.pis.domain.ProprietaryBankTransactionCode;
import org.vp.pis.domain.Servicer;
import org.vp.pis.domain.StandingOrder;
import org.vp.pis.domain.Transaction;
import org.vp.pis.model.AccountRequest;
import org.vp.pis.model.AccountRequestHistory;
import org.vp.pis.model.Beneficiary;
import org.vp.pis.model.Branches;
import org.vp.pis.model.DirectDebits;
import org.vp.pis.model.Product;
import org.vp.pis.model.StandingOrders;
import org.vp.pis.repository.AccountOwnersRepository;
import org.vp.pis.repository.AccountRepository;
import org.vp.pis.repository.AccountsRequestHistoryRepository;
import org.vp.pis.repository.AccountsRequestRepository;
import org.vp.pis.repository.BeneficiaryRepository;
import org.vp.pis.repository.DirectDebitRepository;
import org.vp.pis.repository.ProductRepository;
import org.vp.pis.repository.StandingOrderRepository;
import org.vp.pis.repository.TransactionRepository;
import org.vp.pis.service.AISService;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Class contains 17 end points POST , GET and DELETE account request GET
 * account, balance, standing order, beneficiaries ,products, transactions,
 * direct debits as bulk response and based on account id
 *
 */
@RestController
public class Default {

	@Autowired
	AccountRepository accountRepository;

	@Autowired
	BeneficiaryRepository beneficiaryRepository;

	@Autowired
	DirectDebitRepository directDebitRepository;

	@Autowired
	ProductRepository productRepository;

	@Autowired
	StandingOrderRepository standingOrderRepository;

	@Autowired
	TransactionRepository transactionRepository;

	@Autowired
	AccountsRequestRepository accountsRequestRepository;

	@Autowired
	AccountOwnersRepository accountOwnersRepository;

	@Autowired
	AISService aisService;

	@Autowired
	AccountsRequestHistoryRepository accountsRequestHistoryRepository;

	Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	SimpleDateFormat inFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

	Locale indiaLocale = new Locale("en", "IN");
	NumberFormat newFormat = NumberFormat.getCurrencyInstance();
	String pattern = ((DecimalFormat) newFormat).toPattern();
	String newPattern = pattern.replace("\u00A4", "").trim();
	NumberFormat defaultformat = new DecimalFormat(newPattern);

	final static Logger LOG = LoggerFactory.getLogger(Default.class);

	/**
	 * Method save the account Request in Account Request Table after validation
	 * 
	 * @param body
	 * @param response
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/account-requests", method = RequestMethod.POST)
	public String postAccountRequest(@RequestBody AccountMain body, HttpServletResponse response,
			HttpServletRequest request) throws ParseException {
		LOG.info("Method postAccountRequest starts");
		String responseResult = "";
		String status = "";
		String logInDate = request.getHeader("x-fapi-customer-last-logged-time");
		LOG.info(logInDate);
		try {// TODO JSOn check
			if (logInDate != null && logInDate != "") {
				LOG.info(logInDate);
				if (!(logInDate.matches(
						"[A-Z]{1}[a-z]{2},\\s[0-9]{2}\\s[A-Z]{1}[a-z]{2}\\s[0-9]{4}\\s[0-9]{2}:[0-9]{2}:[0-9]{2}\\s[A-Z]{3}"))) {

					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					responseResult = "{\"Status\" : \" HTTP headers should be represented as RFC 7231 Full Dates \"}";
				}
			}
			if (response.getStatus() != HttpServletResponse.SC_BAD_REQUEST) {
				responseResult = aisService.getRequestHeaderValidation(request, response);

				if (response.getStatus() != HttpServletResponse.SC_BAD_REQUEST
						&& response.getStatus() != HttpServletResponse.SC_NOT_ACCEPTABLE) {
					String expirationDate = body.getData().getExpirationDateTime();
					String trxnFromDate = body.getData().getTransactionFromDateTime();
					String trxnToDate = body.getData().getTransactionToDateTime();
					String[] permission = body.getData().getPermissions();
					List<String> permissionArray = Arrays.asList("ReadAccountsBasic", "ReadAccountsDetail",
							"ReadBalances", "ReadBeneficiariesBasic", "ReadBeneficiariesDetail", "ReadDirectDebits",
							"ReadProducts", "ReadStandingOrdersBasic", "ReadStandingOrdersDetail",
							"ReadTransactionsBasic", "ReadTransactionsCredits", "ReadTransactionsDebits",
							"ReadTransactionsDetail");
					List<String> permissionRequest = Arrays.asList(body.getData().getPermissions());
					SimpleDateFormat inFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss-00:00");
					Date date1 = new Date();
					Date frompDt = inFormat.parse(trxnFromDate);
					Date expDt = inFormat.parse(expirationDate);
					int difference = aisService.getTimeDiff(frompDt, expDt);
					LOG.info("date" + date1);
					if (!expirationDate
							.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}[+|-][0-9]{2}:[0-9]{2}")) {
						responseResult = "{\"Status\" : \"Expiration Date is not on ISODateTime Format\" }";
						response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						status = "Rejected";
					} else if (date1.compareTo(inFormat.parse(expirationDate)) > 0) {
						responseResult = "{\"Status\" : \"The ExpirationDateTime is the current date or a future date\" }";
						response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						status = "Rejected";
					} else if (!trxnFromDate
							.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}[+|-][0-9]{2}:[0-9]{2}")) {
						responseResult = "{\"Status\" : \"Transaction From Date is not on ISODateTime Format\" }";
						response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						status = "Rejected";
					} else if (!trxnToDate
							.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}[+|-][0-9]{2}:[0-9]{2}")) {
						responseResult = "{\"Status\" : \"Transaction To Date is not on ISODateTime Format\" }";
						response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						status = "Rejected";
					} else if (permission == null || permission.length == 0 || Arrays.asList(permission).isEmpty()) {
						responseResult = "{\"Status\" : \"No Permissions are set\" }";
						response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						status = "Rejected";
					} else if (difference > 90) {
						responseResult = "{\"Status\" : \"Difference between TransactionFromDateTime and ExpirationDateTime is less than or equal to 90 days\" }";
						response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						status = "Rejected";
					}
					LOG.info(responseResult);
					if (!("Rejected".equalsIgnoreCase(status))) {
						boolean flag = true;
						if (!permissionRequest.isEmpty()) {
							for (int i = 0; i < permissionRequest.size(); i++) {
								if (permissionArray.contains(permissionRequest.get(i))) {
									flag = true;
								} else {
									flag = false;
									break;
								}
							}
							if (!flag) {
								response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
								responseResult = "{\"Status\" : \"Not a Valid Permission\" }";
								status = "Rejected";
							}
						}
						LOG.info(responseResult);
					}

					if (!("Rejected".equalsIgnoreCase(status))) {
						boolean flag = true;
						if (!permissionRequest.isEmpty()) {
							if (permissionRequest.contains("ReadTransactionsBasic")) {
								if ((permissionRequest.contains("ReadTransactionsCredits"))
										|| (permissionRequest.contains("ReadTransactionsDebits"))) {
									flag = true;
								} else {
									flag = false;
								}
							}
							if (!flag) {
								response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
								responseResult = "{\"Status\" : \"Permission disallowed - Account requests with a Permissions array that contains ReadTransactionBasic but does not contain at least one of ReadTransactionCredits and ReadTransactionDebits.\" }";
								status = "Rejected";
							}
						}
						LOG.info(responseResult);
					}

					if (!("Rejected".equalsIgnoreCase(status))) {
						boolean flag = true;
						if (!permissionRequest.isEmpty()) {
							if (permissionRequest.contains("ReadTransactionsDetail")) {
								if ((permissionRequest.contains("ReadTransactionsCredits"))
										|| (permissionRequest.contains("ReadTransactionsDebits"))) {
									flag = true;
								} else {
									flag = false;
								}
							}
							if (!flag) {
								response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
								responseResult = "{\"Status\" : \"Permission disallowed - Account requests with a Permissions array that contains ReadTransactionDetail but does not contain at least one of ReadTransactionCredits and ReadTransactionDebits.\" }";
								status = "Rejected";
							}
						}
						LOG.info(responseResult);
					}

					if (!("Rejected".equalsIgnoreCase(status))) {
						boolean flag = true;
						if (!permissionRequest.isEmpty()) {
							if (permissionRequest.contains("ReadTransactionsCredits")) {
								if ((permissionRequest.contains("ReadTransactionsBasic"))
										|| (permissionRequest.contains("ReadTransactionsDetail"))) {
									flag = true;
								} else {
									flag = false;
								}
							}
							if (!flag) {
								response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
								responseResult = "{\"Status\" : \"Permission disallowed - Account requests with a Permissions array that contains ReadTransactionCredits but does not contain at least one of ReadTransactionBasic and ReadTransactionDetails.\" }";
								status = "Rejected";
							}
						}
						LOG.info(responseResult);
					}

					if (!("Rejected".equalsIgnoreCase(status))) {
						boolean flag = true;
						if (!permissionRequest.isEmpty()) {
							if (permissionRequest.contains("ReadTransactionsDebits")) {
								if ((permissionRequest.contains("ReadTransactionsBasic"))
										|| (permissionRequest.contains("ReadTransactionsDetail"))) {
									flag = true;
								} else {
									flag = false;
								}
							}
							if (!flag) {
								response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
								responseResult = "{\"Status\" : \"Permission disallowed - Account requests with a Permissions array that contains ReadTransactionDebits but does not contain at least one of ReadTransactionBasic and ReadTransactionDetails.\" }";
								status = "Rejected";
							}
						}
						LOG.info(responseResult);
					}

					if (!("Rejected".equalsIgnoreCase(status))) {
						response.setStatus(HttpServletResponse.SC_CREATED);
						status = "AwaitingAuthorisation";
					}
					String aisID = aisService.generateID("AIS");
					String responseStatus = aisService.saveAccountRequest(body, status, aisID);
					LOG.info(" Save Account Request --> Default Class");
					if ("Success".equalsIgnoreCase(responseStatus)) {
						responseResult = aisService.getAccountRequest(aisID);
					} else {
						response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						responseResult = "{\"Status\" : \"" + responseStatus + "\"}";
					}
				}
			}
		} catch (Exception e) {
			LOG.info(aisService.getStackTrace(e));
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			responseResult = "{\"Status\" : \"" + e.getCause() + "' - '" + aisService.getStackTrace(e) + "\"}";
		}
		LOG.info("Method postAccountRequest Ends");
		response.addHeader("x-jws-signature", request.getHeader("x-jws-signature"));
		response.addHeader("x-fapi-interaction-id", request.getHeader("x-fapi-interaction-id"));
		return responseResult;
	}

	/**
	 * GET Account Request based on the Account Request ID
	 * 
	 * @param AccountRequestId
	 * @param response
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/account-requests/{AccountRequestId}", method = RequestMethod.GET)
	public String getAccountRequest(@PathVariable(value = "AccountRequestId") String AccountRequestId,
			HttpServletResponse response, HttpServletRequest request) {
		LOG.info("Method getAccountRequest starts");
		String responseResult = "";
		try {
			responseResult = aisService.getRequestHeaderValidation(request, response);
			if (response.getStatus() != HttpServletResponse.SC_BAD_REQUEST
					&& response.getStatus() != HttpServletResponse.SC_NOT_ACCEPTABLE) {
				responseResult = aisService.getAccountRequest(AccountRequestId);
				if ("{\"Status\" : \"Requested Account Request ID is not available\"}"
						.equalsIgnoreCase(responseResult)) {
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				}
			}
		} catch (Exception e) {
			LOG.info(aisService.getStackTrace(e));
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			responseResult = "{\"Status\" : \"" + e.getCause() + "' - '" + aisService.getStackTrace(e) + "\"}";
		}
		LOG.info("Method getAccountRequest Ends");
		response.addHeader("x-jws-signature", request.getHeader("x-jws-signature"));
		response.addHeader("x-fapi-interaction-id", request.getHeader("x-fapi-interaction-id"));
		return responseResult;
	}

	/**
	 * Delete Account Request and move to Account Request history
	 * 
	 * @param AccountRequestId
	 * @param response
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/account-requests/{AccountRequestId}", method = RequestMethod.DELETE)
	public String deleteAccountRequest(@PathVariable(value = "AccountRequestId") String AccountRequestId,
			HttpServletResponse response, HttpServletRequest request) {
		LOG.info("Method postAccountRequest starts");
		String responseResult = "";
		try {
			responseResult = aisService.getRequestHeaderValidation(request, response);
			if (response.getStatus() != HttpServletResponse.SC_BAD_REQUEST
					&& response.getStatus() != HttpServletResponse.SC_NOT_ACCEPTABLE) {
				AccountRequestHistory accountRequestHistory = new AccountRequestHistory();
				List<AccountRequest> listAR = new ArrayList<AccountRequest>();
				listAR = accountsRequestRepository.getDetailsByAccountRequestId(AccountRequestId);
				if (!listAR.isEmpty()) {
					for (int i = 0; i < listAR.size(); i++) {
						LOG.info(listAR.get(i).getAccount_request_id());
						accountRequestHistory.setAccount_id(listAR.get(i).getAccount_id());
						accountRequestHistory.setAccount_request_history_id(listAR.get(i).getAccount_request_id());
						accountRequestHistory.setAccount_id_ref(listAR.get(i).getAccount_id_ref());
						accountRequestHistory
								.setAccount_request_reference(listAR.get(i).getAccount_request_reference());
						// accountRequestHistory.setHistory_created_date(new
						// Date());
						accountRequestHistory.setScheme_name(listAR.get(i).getScheme_name());
						accountRequestHistory.setAccount_identification(listAR.get(i).getAccount_identification());
						accountRequestHistory.setName(listAR.get(i).getName());
						accountRequestHistory.setStatus("Revoked");
						accountRequestHistory.setRead_accounts_basic(listAR.get(i).getRead_accounts_basic());
						accountRequestHistory.setRead_accounts_detail(listAR.get(i).getRead_accounts_detail());
						accountRequestHistory.setRead_balances(listAR.get(i).getRead_balances());
						accountRequestHistory.setRead_beneficiaries_basic(listAR.get(i).getRead_beneficiaries_basic());
						accountRequestHistory
								.setRead_beneficiaries_detail(listAR.get(i).getRead_beneficiaries_detail());
						accountRequestHistory.setRead_direct_debits(listAR.get(i).getRead_direct_debits());
						accountRequestHistory.setRead_products(listAR.get(i).getRead_products());
						accountRequestHistory
								.setRead_standing_orders_basic(listAR.get(i).getRead_standing_orders_basic());
						accountRequestHistory
								.setRead_standing_orders_detail(listAR.get(i).getRead_standing_orders_detail());
						accountRequestHistory.setRead_transactions_basic(listAR.get(i).getRead_transactions_basic());
						accountRequestHistory
								.setRead_transactions_credits(listAR.get(i).getRead_transactions_credits());
						accountRequestHistory.setRead_transactions_debits(listAR.get(i).getRead_transactions_debits());
						accountRequestHistory.setRead_transactions_detail(listAR.get(i).getRead_transactions_detail());
						accountRequestHistory.setExpiration_Date_Time(listAR.get(i).getExpiration_Date_Time());
						accountRequestHistory
								.setTransaction_from_date_time(listAR.get(i).getTransaction_from_date_time());
						accountRequestHistory.setTransaction_to_date_time(listAR.get(i).getTransaction_to_date_time());
						accountRequestHistory.setChecker_date(listAR.get(i).getChecker_date());
						accountRequestHistory.setChecker_id(listAR.get(i).getChecker_id());
						accountRequestHistory.setMaker_date(listAR.get(i).getMaker_date());
						accountRequestHistory.setMaker_id(listAR.get(i).getMaker_id());
						accountRequestHistory.setModified_by(listAR.get(i).getModified_by());
						accountRequestHistory.setModified_date(listAR.get(i).getModified_date());
						LOG.info("Save Request on Account Request Table");
						accountsRequestHistoryRepository.save(accountRequestHistory);
						LOG.info("Delete Account Request from Account Request Table");
						accountsRequestRepository.deleteDetailsByAccountRequestId(AccountRequestId,
								listAR.get(i).getAccount_id_ref());
					}
					response.setStatus(HttpServletResponse.SC_NO_CONTENT);
				} else {
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					responseResult = "{\"Status\" : \"Requested Account Request ID is not available\"}";
				}
			}
		} catch (Exception e) {
			LOG.info(aisService.getStackTrace(e));
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			responseResult = "{\"Status\" : \"" + e.getCause() + " - " + aisService.getStackTrace(e) + "\"}";
		}
		LOG.info("Method deleteAccountRequest Ends");
		response.addHeader("x-jws-signature", request.getHeader("x-jws-signature"));
		response.addHeader("x-fapi-interaction-id", request.getHeader("x-fapi-interaction-id"));
		return responseResult;
	}

	@RequestMapping(value = "/accounts", method = RequestMethod.GET)
	public String getAccounts(@RequestParam(value = "pg", required = false) Integer pageNo,
			HttpServletResponse response, HttpServletRequest request) {
		LOG.info(" Method getAccounts Starts");
		String responseResult = "";
		AccountMain accountMain = new AccountMain();
		boolean execute = false;
		int count = 0;
		List<AccountRequest> listAR = new ArrayList<AccountRequest>();
		List<Branches> listBU = new ArrayList<Branches>();
		List<org.vp.pis.model.Account> listAccount = new ArrayList<org.vp.pis.model.Account>();
		Page page = new Page();
		try {
			responseResult = aisService.getRequestHeaderValidation(request, response);
			if (response.getStatus() != HttpServletResponse.SC_BAD_REQUEST
					&& response.getStatus() != HttpServletResponse.SC_NOT_ACCEPTABLE) {
				/**
				 * To get Account Request from application.properties Removed in
				 * Real Life Scenario
				 */
				String accountRequestID = aisService.getAccountRequestId();
				LOG.info("accountRequestID" + accountRequestID);
				listAR = accountsRequestRepository.getDetailsByAccountRequestId(accountRequestID);
				ArrayList<Accounts> listAcc = new ArrayList<Accounts>();
				if (!listAR.isEmpty()) {
					count = (int) (Math.ceil(listAR.size() / 100 + 0.4));
					Data data = new Data();
					if ((listAR.get(0).getRead_accounts_basic().equalsIgnoreCase("Yes")
							|| listAR.get(0).getRead_accounts_detail().equalsIgnoreCase("Yes"))) {

						Date date1 = new Date();
						Date date2 = listAR.get(0).getExpiration_Date_Time();
						if (date1.compareTo(date2) < 0) {
							if (pageNo != null) {
								if (pageNo > listAR.size()) {
									page = aisService.pagingWithQueryParam(pageNo, listAR.size());
								} else {
									page.setInitialpage(0);
									page.setEndPage(listAR.size());
								}
							} else {
								page.setInitialpage(0);
								page.setEndPage(listAR.size());
							}
							for (int i = page.getInitialpage(); i < page.getEndPage(); i++) {
								execute = aisService.checkAccountIsValid(listAR.get(i).getScheme_name(),
										listAR.get(i).getAccount_identification());
								if (execute && "Authorised".equalsIgnoreCase(listAR.get(i).getStatus())) {
									execute = true;
									if (execute) {
										listAccount = accountRepository.getDetailsByAccountIdentification(
												listAR.get(i).getScheme_name(),
												listAR.get(i).getAccount_identification());
										if (!listAccount.isEmpty()) {
											Accounts accounts = new Accounts();
											Account account = new Account();
											Servicer servicer = new Servicer();
											accounts.setAccountId(listAR.get(i).getAccount_id_ref());
											accounts.setCurrency(listAccount.get(0).getAccount_currency());
											accounts.setNickname(listAccount.get(0).getNickname());
											account.setSchemeName(listAccount.get(0).getScheme_name());
											account.setIdentification(
													String.valueOf(listAccount.get(0).getAccount_identification()));
											account.setName(listAccount.get(0).getAccount_name());

											account.setSecondaryIdentification(
													String.valueOf(listAccount.get(0).getSecondary_identification()));
											accounts.setAccount(account);

											listBU = accountRepository.getDetailsByBranchID(
													listAccount.get(0).getBranch_id(), listAccount.get(0).getBank_id());
											if (!listBU.isEmpty()) {
												servicer.setIdentification(listBU.get(0).getServicer_identification());
												servicer.setSchemeName(listBU.get(0).getServicer_schemename());
											}
											accounts.setServicer(servicer);
											listAcc.add(accounts);
										}
									}
								} else {
									execute = false;
								}
							}
						} else {
							response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
							accountMain.setStatus("Date got expired");
							LOG.info("Date got expired");
						}
					} else {
						response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						accountMain.setStatus("Permission are not available");
						LOG.info("Permission are not available");
					}
					data.setAccount(listAcc);
					if (listAcc.size() >= 1) {
						accountMain.setStatus(null);
					}
					Links links = new Links();
					links.setSelf("/accounts/");

					if (listAR.size() > 100) {
						links.setFirst("/accounts/");
						links.setLast("/accounts?pg=" + count);
						links.setNext("/accounts?pg=" + (Math.ceil(page.getEndPage() / 100 + 0.4) + 1));
					}
					if (Math.ceil(page.getEndPage() / 100) >= 2) {
						links.setPrev("/accounts?pg=" + (Math.ceil(page.getEndPage() / 100 + 0.4) - 1));
					}
					accountMain.setLinks(links);

					Meta meta = new Meta();
					meta.setTotalPages(count);
					meta.setFirstAvailableDateTime(listAR.size() <= 0 ? ""
							: inFormat.format(listAR.get(0).getMaker_date()).replace("Z", "-00:00"));
					meta.setLastAvailableDateTime(inFormat.format(new Date()).replace("Z", "-00:00"));
					accountMain.setMeta(meta);
					accountMain.setData(data);
				} else {
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					accountMain.setStatus("Requested Account Request ID is not available");
					LOG.info("Requested Account Request ID is not available");
				}
				responseResult = gson.toJson(accountMain);
			}
		} catch (Exception e) {
			LOG.info(aisService.getStackTrace(e));
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			accountMain.setStatus("Exception " + e.getCause() + "  -  " + aisService.getStackTrace(e));
		}
		response.addHeader("x-jws-signature", request.getHeader("x-jws-signature"));
		response.addHeader("x-fapi-interaction-id", request.getHeader("x-fapi-interaction-id"));
		LOG.info(" Method getAccounts Ends");

		return responseResult;
	}

	/**
	 * GET account based on the account id
	 * 
	 * @param AccountId
	 * @param response
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "/accounts/{AccountId}", method = RequestMethod.GET)
	public String getAccountsByID(@PathVariable(value = "AccountId") String AccountId,
			@RequestParam(value = "pg", required = false) Integer pageNo, HttpServletResponse response,
			HttpServletRequest request) {
		LOG.info(" Method getAccountsByID Starts");
		AccountMain accountMain = new AccountMain();
		boolean execute = false;
		int count = 0;
		List<AccountRequest> listAR = new ArrayList<AccountRequest>();
		List<Branches> listBU = new ArrayList<Branches>();
		List<org.vp.pis.model.Account> listAccount = new ArrayList<org.vp.pis.model.Account>();
		String responseResult = "";
		Page page = new Page();

		try {
			responseResult = aisService.getRequestHeaderValidation(request, response);
			if (response.getStatus() != HttpServletResponse.SC_BAD_REQUEST
					&& response.getStatus() != HttpServletResponse.SC_NOT_ACCEPTABLE) {
				listAR = accountsRequestRepository.getDetailsByAccountId(AccountId);
				ArrayList<Accounts> listAcc = new ArrayList<Accounts>();
				if (!listAR.isEmpty()) {
					count = (int) (Math.ceil(listAR.size() / 100 + 0.4));
					Data data = new Data();
					if ((listAR.get(0).getRead_accounts_basic().equalsIgnoreCase("Yes")
							|| listAR.get(0).getRead_accounts_detail().equalsIgnoreCase("Yes"))) {

						Date date1 = new Date();
						Date date2 = listAR.get(0).getExpiration_Date_Time();
						if (date1.compareTo(date2) < 0) {

							for (int i = 0; i < listAR.size(); i++) {
								execute = aisService.checkAccountIsValid(listAR.get(i).getScheme_name(),
										listAR.get(i).getAccount_identification());
								if (execute && "Authorised".equalsIgnoreCase(listAR.get(i).getStatus())) {
									execute = true;
									if (execute) {
										listAccount = accountRepository.getDetailsByAccountIdentification(
												listAR.get(i).getScheme_name(),
												listAR.get(i).getAccount_identification());
										if (!listAccount.isEmpty()) {
											if (pageNo != null) {
												if (pageNo > listAccount.size()) {
													page = aisService.pagingWithQueryParam(pageNo, listAccount.size());
												} else {
													page.setInitialpage(0);
													page.setEndPage(listAccount.size());
												}
											} else {
												page.setInitialpage(0);
												page.setEndPage(listAccount.size());
											}
											for (int k = page.getInitialpage(); k < page.getEndPage(); k++) {
												Accounts accounts = new Accounts();
												Account account = new Account();
												Servicer servicer = new Servicer();
												accounts.setAccountId(listAR.get(i).getAccount_id_ref());
												accounts.setCurrency(listAccount.get(k).getAccount_currency());
												accounts.setNickname(listAccount.get(k).getNickname());
												account.setSchemeName(listAccount.get(k).getScheme_name());
												account.setIdentification(
														String.valueOf(listAccount.get(k).getAccount_identification()));
												account.setName(listAccount.get(k).getAccount_name());

												account.setSecondaryIdentification(String
														.valueOf(listAccount.get(k).getSecondary_identification()));
												accounts.setAccount(account);

												listBU = accountRepository.getDetailsByBranchID(
														listAccount.get(k).getBranch_id(),
														listAccount.get(k).getBank_id());
												if (!listBU.isEmpty()) {
													servicer.setIdentification(
															listBU.get(0).getServicer_identification());
													servicer.setSchemeName(listBU.get(0).getServicer_schemename());
												}

												accounts.setServicer(servicer);
												listAcc.add(accounts);
											}
										}
									}
								} else {
									execute = false;
								}
							}
						} else {
							response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
							accountMain.setStatus("Date got expired");
							LOG.info("Date got expired");
						}
					} else {
						response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						accountMain.setStatus("Permission are not available");
						LOG.info("Permission are not available");
					}
					data.setAccount(listAcc);
					if (listAcc.size() >= 1) {
						accountMain.setStatus(null);
					}
					Links links = new Links();
					links.setSelf("/accounts/" + AccountId);
					if (listAccount.size() > 100) {
						links.setFirst("/accounts/");
						links.setLast("/accounts?pg=" + count);
						links.setNext("/accounts?pg=" + (Math.ceil(page.getEndPage() / 100 + 0.4) + 1));
					}
					if (Math.ceil(page.getEndPage() / 100) >= 2) {
						links.setPrev("/accounts?pg=" + (Math.ceil(page.getEndPage() / 100 + 0.4) - 1));
					}
					accountMain.setLinks(links);

					Meta meta = new Meta();
					meta.setTotalPages(count);
					meta.setFirstAvailableDateTime(listAR.size() <= 0 ? ""
							: inFormat.format(listAR.get(0).getMaker_date()).replace("Z", "-00:00"));
					meta.setLastAvailableDateTime(inFormat.format(new Date()).replace("Z", "-00:00"));
					accountMain.setMeta(meta);
					accountMain.setData(data);
				} else {
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					accountMain.setStatus("Requested Account ID is not available");
					LOG.info("Requested Account ID is not available");
				}
				responseResult = gson.toJson(accountMain);
			}
		} catch (Exception e) {
			LOG.info(aisService.getStackTrace(e));
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			accountMain.setStatus("Exception " + e.getCause() + "  -  " + aisService.getStackTrace(e));
		}
		response.addHeader("x-jws-signature", request.getHeader("x-jws-signature"));
		response.addHeader("x-fapi-interaction-id", request.getHeader("x-fapi-interaction-id"));
		LOG.info(" Method getAccountsByID Ends");
		return responseResult;
	}

	@RequestMapping(value = "/balances", method = RequestMethod.GET)
	public String getBalance(@RequestParam(value = "pg", required = false) Integer pageNo, HttpServletResponse response,
			HttpServletRequest request) {
		AccountMain accountMain = new AccountMain();
		List<org.vp.pis.model.Account> listAccount = new ArrayList<org.vp.pis.model.Account>();
		List<AccountRequest> listAR = new ArrayList<AccountRequest>();
		ArrayList<CreditLine> lstCL = new ArrayList<CreditLine>();
		ArrayList<Balance> listBal = new ArrayList<Balance>();
		int count = 0;
		boolean execute = false;
		String responseResult = "";
		Page page = new Page();
		try {
			responseResult = aisService.getRequestHeaderValidation(request, response);
			if (response.getStatus() != HttpServletResponse.SC_BAD_REQUEST
					&& response.getStatus() != HttpServletResponse.SC_NOT_ACCEPTABLE) {
				/**
				 * To get Account Request from application.properties Removed in
				 * Real Life Scenario
				 */
				String accountRequestID = aisService.getAccountRequestId();
				LOG.info("accountRequestID" + accountRequestID);
				listAR = accountsRequestRepository.getDetailsByAccountRequestId(accountRequestID);
				if (!listAR.isEmpty()) {
					count = (int) (Math.ceil(listAR.size() / 100 + 0.4));
					Data data = new Data();
					if (listAR.get(0).getRead_balances().equalsIgnoreCase("Yes")) {

						Date date1 = new Date();
						Date date2 = listAR.get(0).getExpiration_Date_Time();
						if (date1.compareTo(date2) < 0) {
							if (pageNo != null) {
								if (pageNo > listAR.size()) {
									page = aisService.pagingWithQueryParam(pageNo, listAR.size());
								} else {
									page.setInitialpage(0);
									page.setEndPage(listAR.size());
								}
							} else {
								page.setInitialpage(0);
								page.setEndPage(listAR.size());
							}
							for (int i = page.getInitialpage(); i < page.getEndPage(); i++) {
								execute = aisService.checkAccountIsValid(listAR.get(i).getScheme_name(),
										listAR.get(i).getAccount_identification());
								if (execute && "Authorised".equalsIgnoreCase(listAR.get(i).getStatus())) {
									execute = true;
									if (execute) {
										LOG.info("ID" + listAR.get(i).getAccount_id());
										listAccount = accountRepository.getDetailsByAccountIdentification(
												listAR.get(i).getScheme_name(),
												listAR.get(i).getAccount_identification());
										if (!listAccount.isEmpty()) {
											LOG.info("Inside The JSON set");
											String dateTime = inFormat.format(listAccount.get(0).getModified_date());
											Balance balance = new Balance();
											CreditLine creditLine = new CreditLine();
											Amount amount = new Amount();
											balance.setAccountId(String.valueOf(listAR.get(i).getAccount_id_ref()));
											amount.setAmount(defaultformat.format(listAccount.get(0).getBalance()));
											amount.setCurrency(listAccount.get(0).getAccount_currency());
											balance.setAmount(amount);
											String CDI = listAccount.get(0).getCredit_debit_indicator();
											balance.setCreditDebitIndicator(
													CDI.substring(0, 1).toUpperCase() + CDI.substring(1).toLowerCase());
											balance.setType(listAccount.get(0).getType_of_balance());
											balance.setDateTime(dateTime);
											String included = "";
											if ("Yes".equalsIgnoreCase(listAccount.get(0).getCredit_line_included())) {
												included = "True";
											} else {
												included = "False";
											}
											creditLine.setIncluded(included);
											Amount amount1 = new Amount();
											amount1.setAmount(
													defaultformat.format(listAccount.get(0).getCredit_line_amount()));
											amount1.setCurrency(listAccount.get(0).getAccount_currency());
											creditLine.setAmount(amount1);
											creditLine.setType(listAccount.get(0).getCredit_line_type());
											lstCL.add(creditLine);
											balance.setCreditLine(lstCL);
											listBal.add(balance);
										}
									}
								} else {
									execute = false;
								}
							}
						} else {
							response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
							accountMain.setStatus("Date got expired");
							LOG.info("Date got expired");
						}
					} else {
						response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						accountMain.setStatus("Permission are not available");
						LOG.info("Permission are not available");
					}
					data.setBalance(listBal);
					if (listBal.size() >= 1) {
						accountMain.setStatus(null);
					}
					Links links = new Links();
					links.setSelf("/balances/");
					if (listAR.size() > 100) {
						links.setFirst("/balances/");
						links.setLast("/balances?pg=" + count);
						links.setNext("/balances?pg=" + (Math.ceil(page.getEndPage() / 100 + 0.4) + 1));
					}
					if (Math.ceil(page.getEndPage() / 100) >= 2) {
						links.setPrev("/balances?pg=" + (Math.ceil(page.getEndPage() / 100 + 0.4) - 1));
					}
					accountMain.setLinks(links);

					Meta meta = new Meta();
					meta.setTotalPages(count);
					meta.setFirstAvailableDateTime(listAR.size() <= 0 ? ""
							: inFormat.format(listAR.get(0).getMaker_date()).replace("Z", "-00:00"));
					meta.setLastAvailableDateTime(inFormat.format(new Date()).replace("Z", "-00:00"));
					accountMain.setMeta(meta);
					accountMain.setData(data);
				} else {
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					accountMain.setStatus("Requested Account Request ID is not available");
					LOG.info("Requested Account Request ID is not available");
				}
				responseResult = gson.toJson(accountMain);
			}
		} catch (Exception e) {
			LOG.info(aisService.getStackTrace(e));
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			accountMain.setStatus(e.getCause() + "  -  " + aisService.getStackTrace(e));
		}
		response.addHeader("x-jws-signature", request.getHeader("x-jws-signature"));
		response.addHeader("x-fapi-interaction-id", request.getHeader("x-fapi-interaction-id"));
		return responseResult;
	}

	@RequestMapping(value = "/accounts/{AccountId}/balances", method = RequestMethod.GET)
	public String getBalanceByAccountsID(@PathVariable(value = "AccountId") String AccountId,
			@RequestParam(value = "pg", required = false) Integer pageNo, HttpServletResponse response,
			HttpServletRequest request) {

		LOG.info(" Method getBalanceByAccountsID Starts");
		AccountMain accountMain = new AccountMain();
		boolean execute = false;
		int count = 0;
		List<AccountRequest> listAR = new ArrayList<AccountRequest>();
		List<org.vp.pis.model.Account> listAccount = new ArrayList<org.vp.pis.model.Account>();
		ArrayList<CreditLine> lstCrdLine = new ArrayList<CreditLine>();
		ArrayList<Balance> listBal = new ArrayList<Balance>();
		String responseResult = "";
		Page page = new Page();

		try {
			responseResult = aisService.getRequestHeaderValidation(request, response);
			if (response.getStatus() != HttpServletResponse.SC_BAD_REQUEST
					&& response.getStatus() != HttpServletResponse.SC_NOT_ACCEPTABLE) {
				listAR = accountsRequestRepository.getDetailsByAccountId(AccountId);
				if (!listAR.isEmpty()) {
					count = (int) (Math.ceil(listAR.size() / 100 + 0.4));
					Data data = new Data();
					if (listAR.get(0).getRead_balances().equalsIgnoreCase("Yes")) {

						Date date1 = new Date();
						Date date2 = listAR.get(0).getExpiration_Date_Time();
						if (date1.compareTo(date2) < 0) {

							for (int i = 0; i < listAR.size(); i++) {
								execute = aisService.checkAccountIsValid(listAR.get(i).getScheme_name(),
										listAR.get(i).getAccount_identification());
								if (execute && "Authorised".equalsIgnoreCase(listAR.get(i).getStatus())) {
									execute = true;
									if (execute) {
										listAccount = accountRepository.getDetailsByAccountIdentification(
												listAR.get(i).getScheme_name(),
												listAR.get(i).getAccount_identification());
										if (!listAccount.isEmpty()) {
											Balance balance = new Balance();
											CreditLine creditLine = new CreditLine();
											Amount amount = new Amount();
											if (pageNo != null) {
												if (pageNo > listAccount.size()) {
													page = aisService.pagingWithQueryParam(pageNo, listAccount.size());
												} else {
													page.setInitialpage(0);
													page.setEndPage(listAccount.size());
												}
											} else {
												page.setInitialpage(0);
												page.setEndPage(listAccount.size());
											}
											for (int k = page.getInitialpage(); k < page.getEndPage(); k++) {

												SimpleDateFormat formatter = new SimpleDateFormat(
														"yyyy-MM-dd'T'HH:mm:ss'Z'");
												formatter.setTimeZone(TimeZone.getTimeZone("Europe/London"));
												String dateTime = formatter
														.format(listAccount.get(k).getModified_date());
												balance.setAccountId(listAR.get(i).getAccount_id_ref());
												amount.setAmount(defaultformat.format(listAccount.get(k).getBalance()));
												amount.setCurrency(listAccount.get(k).getAccount_currency());
												balance.setAmount(amount);
												String CDI = listAccount.get(k).getCredit_debit_indicator();
												balance.setCreditDebitIndicator(CDI.substring(0, 1).toUpperCase()
														+ CDI.substring(1).toLowerCase());
												balance.setType(listAccount.get(k).getType_of_balance());
												balance.setDateTime(dateTime);
												String included = "";
												if ("Yes".equalsIgnoreCase(
														listAccount.get(k).getCredit_line_included())) {
													included = "True";
												} else {
													included = "False";
												}
												creditLine.setIncluded(included);
												Amount amount1 = new Amount();
												amount1.setAmount(defaultformat
														.format(listAccount.get(k).getCredit_line_amount()));
												amount1.setCurrency(listAccount.get(k).getAccount_currency());
												creditLine.setAmount(amount1);
												creditLine.setType(listAccount.get(k).getCredit_line_type());
												lstCrdLine.add(creditLine);
												balance.setCreditLine(lstCrdLine);
												listBal.add(balance);
											}
										}
									}
								} else {
									execute = false;
								}
							}
						} else {
							response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
							accountMain.setStatus("Date got expired");
							LOG.info("Date got expired");
						}
					} else {
						response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						accountMain.setStatus("Permission are not available");
						LOG.info("Permission are not available");
					}
					data.setBalance(listBal);
					if (listBal.size() >= 1) {
						accountMain.setStatus(null);
					}
					Links links = new Links();
					links.setSelf("/accounts/" + AccountId + "/balances");
					if (listAccount.size() > 100) {
						links.setFirst("/accounts/" + AccountId + "/balances");
						links.setLast("/accounts/" + AccountId + "/balances?pg=" + count);
						links.setNext("/accounts/" + AccountId + "/balances?pg="
								+ (Math.ceil(page.getEndPage() / 100 + 0.4) + 1));
					}
					if (Math.ceil(page.getEndPage() / 100) >= 2) {
						links.setPrev("/accounts/" + AccountId + "/balances?pg="
								+ (Math.ceil(page.getEndPage() / 100 + 0.4) - 1));
					}
					accountMain.setLinks(links);

					Meta meta = new Meta();
					meta.setTotalPages(count);
					meta.setFirstAvailableDateTime(listAR.size() <= 0 ? ""
							: inFormat.format(listAR.get(0).getMaker_date()).replace("Z", "-00:00"));
					meta.setLastAvailableDateTime(inFormat.format(new Date()).replace("Z", "-00:00"));
					accountMain.setMeta(meta);
					accountMain.setData(data);
				} else {
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					accountMain.setStatus("Requested Account ID is not available");
					LOG.info("Requested Account ID is not available");
				}
				responseResult = gson.toJson(accountMain);
			}
		} catch (Exception e) {
			LOG.info(aisService.getStackTrace(e));
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			accountMain.setStatus("Exception " + e.getCause() + "  -  " + aisService.getStackTrace(e));
		}
		response.addHeader("x-jws-signature", request.getHeader("x-jws-signature"));
		response.addHeader("x-fapi-interaction-id", request.getHeader("x-fapi-interaction-id"));
		LOG.info(" Method getBalanceByAccountsID Ends");
		return responseResult;
	}

	@RequestMapping(value = "/beneficiaries", method = RequestMethod.GET)
	public String getBeneficiary(@RequestParam(value = "pg", required = false) Integer pageNo,
			HttpServletResponse response, HttpServletRequest request) {
		List<Beneficiary> list = new ArrayList<Beneficiary>();
		List<AccountRequest> listAR = new ArrayList<AccountRequest>();
		int count = 0;
		ArrayList<org.vp.pis.domain.Beneficiary> listBeneficiary = new ArrayList<org.vp.pis.domain.Beneficiary>();
		AccountMain accountMain = new AccountMain();
		boolean execute = false;
		String responseResult = "";
		Page page = new Page();

		try {
			responseResult = aisService.getRequestHeaderValidation(request, response);
			if (response.getStatus() != HttpServletResponse.SC_BAD_REQUEST
					&& response.getStatus() != HttpServletResponse.SC_NOT_ACCEPTABLE) {
				/**
				 * To get Account Request from application.properties Removed in
				 * Real Life Scenario
				 */
				String accountRequestID = aisService.getAccountRequestId();
				listAR = accountsRequestRepository.getDetailsByAccountRequestId(accountRequestID);
				if (!listAR.isEmpty()) {
					count = (int) (Math.ceil(listAR.size() / 100 + 0.4));
					Data data = new Data();
					if ((listAR.get(0).getRead_beneficiaries_basic().equalsIgnoreCase("Yes")
							|| listAR.get(0).getRead_beneficiaries_detail().equalsIgnoreCase("Yes"))) {

						Date date1 = new Date();
						Date date2 = listAR.get(0).getExpiration_Date_Time();
						if (date1.compareTo(date2) < 0) {
							if (pageNo != null) {
								if (pageNo > listAR.size()) {
									page = aisService.pagingWithQueryParam(pageNo, listAR.size());
								} else {
									page.setInitialpage(0);
									page.setEndPage(listAR.size());
								}
							} else {
								page.setInitialpage(0);
								page.setEndPage(listAR.size());
							}
							for (int i = page.getInitialpage(); i < page.getEndPage(); i++) {
								execute = aisService.checkAccountIsValid(listAR.get(i).getScheme_name(),
										listAR.get(i).getAccount_identification());
								if (execute && "Authorised".equalsIgnoreCase(listAR.get(i).getStatus())) {
									execute = true;
									if (execute) {
										List<org.vp.pis.model.Account> accounts = accountRepository
												.getDetailsByAccountIdentification(listAR.get(i).getScheme_name(),
														listAR.get(i).getAccount_identification());
										if (!accounts.isEmpty()) {
											list = beneficiaryRepository
													.getbeneficiaryByAccountId(accounts.get(0).getAccount_id());
											if (!list.isEmpty()) {

												for (int k = 0; k < list.size(); k++) {
													org.vp.pis.domain.Beneficiary beneficiary = new org.vp.pis.domain.Beneficiary();
													Servicer servicer = new Servicer();
													beneficiary.setAccountId(listAR.get(i).getAccount_id_ref());
													beneficiary.setBeneficiaryId(
															String.valueOf(list.get(k).getBeneficiary_id()));
													beneficiary.setReference(list.get(k).getBeneficiary_ref_id());
													CreditorAccount creditorAccount = new CreditorAccount();
													creditorAccount.setIdentification(String.valueOf(list.get(k)
															.getBeneficiary_creditor_account_identification()));
													creditorAccount.setSchemeName(
															list.get(k).getBeneficiary_creditor_account_schemename());
													creditorAccount.setName(
															list.get(k).getBeneficiary_creditor_account_name());
													creditorAccount.setSecondaryIdentification(String.valueOf(list
															.get(k)
															.getBeneficiary_creditor_account_secondary_identification()));
													beneficiary.setCreditorAccount(creditorAccount);
													servicer.setIdentification(
															list.get(k).getBeneficiary_servicer_identification());
													servicer.setSchemeName(
															list.get(k).getBeneficiary_servicer_schemename());
													beneficiary.setServicer(servicer);
													listBeneficiary.add(beneficiary);
												}
											}
										}
									}
								} else {
									execute = false;
								}
							}
						} else {
							response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
							accountMain.setStatus("Date got expired");
							LOG.info("Date got expired");
						}
					} else {
						response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						accountMain.setStatus("Permission are not available");
						LOG.info("Permission are not available");
					}
					data.setBeneficiary(listBeneficiary);
					if (listBeneficiary.size() >= 1) {
						accountMain.setStatus(null);
					}
					Links links = new Links();
					links.setSelf("/beneficiaries/");
					if (listAR.size() > 100) {
						links.setFirst("/accounts/");
						links.setLast("/accounts?pg=" + count);
						links.setNext("/accounts?pg=" + (Math.ceil(page.getEndPage() / 100 + 0.4) + 1));
					}
					if (Math.ceil(page.getEndPage() / 100) >= 2) {
						links.setPrev("/accounts?pg=" + (Math.ceil(page.getEndPage() / 100 + 0.4) - 1));
					}
					accountMain.setLinks(links);

					Meta meta = new Meta();
					meta.setTotalPages(count);
					meta.setFirstAvailableDateTime(listAR.size() <= 0 ? ""
							: inFormat.format(listAR.get(0).getMaker_date()).replace("Z", "-00:00"));
					meta.setLastAvailableDateTime(inFormat.format(new Date()).replace("Z", "-00:00"));
					accountMain.setMeta(meta);
					accountMain.setData(data);
				} else {
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					accountMain.setStatus("Requested Account Request ID is not available");
					LOG.info("Requested Account Request ID is not available");
				}
				responseResult = gson.toJson(accountMain);
			}
		} catch (Exception e) {
			LOG.info(aisService.getStackTrace(e));
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			accountMain.setStatus(e.getCause() + "  -  " + aisService.getStackTrace(e));
		}
		response.addHeader("x-jws-signature", request.getHeader("x-jws-signature"));
		response.addHeader("x-fapi-interaction-id", request.getHeader("x-fapi-interaction-id"));
		return responseResult;
	}

	@RequestMapping(value = "/accounts/{AccountId}/beneficiaries", method = RequestMethod.GET)
	public String getBeneficiaryByAccountsID(@RequestParam(value = "pg", required = false) Integer pageNo,
			@PathVariable(value = "AccountId") String AccountId, HttpServletResponse response,
			HttpServletRequest request) {

		List<Beneficiary> list = new ArrayList<Beneficiary>();
		List<AccountRequest> listAR = new ArrayList<AccountRequest>();
		int count = 0;
		boolean execute = false;
		ArrayList<org.vp.pis.domain.Beneficiary> listBeneficiary = new ArrayList<org.vp.pis.domain.Beneficiary>();
		AccountMain accountMain = new AccountMain();
		String responseResult = "";
		Page page = new Page();

		try {
			responseResult = aisService.getRequestHeaderValidation(request, response);
			if (response.getStatus() != HttpServletResponse.SC_BAD_REQUEST
					&& response.getStatus() != HttpServletResponse.SC_NOT_ACCEPTABLE) {
				listAR = accountsRequestRepository.getDetailsByAccountId(AccountId);
				if (!listAR.isEmpty()) {
					count = (int) (Math.ceil(listAR.size() / 100 + 0.4));
					Data data = new Data();
					if ((listAR.get(0).getRead_beneficiaries_basic().equalsIgnoreCase("Yes")
							|| listAR.get(0).getRead_beneficiaries_detail().equalsIgnoreCase("Yes"))) {

						Date date1 = new Date();
						Date date2 = listAR.get(0).getExpiration_Date_Time();
						if (date1.compareTo(date2) < 0) {

							for (int i = 0; i < listAR.size(); i++) {
								execute = aisService.checkAccountIsValid(listAR.get(i).getScheme_name(),
										listAR.get(i).getAccount_identification());
								if (execute && "Authorised".equalsIgnoreCase(listAR.get(i).getStatus())) {
									execute = true;
									if (execute) {
										List<org.vp.pis.model.Account> accounts = accountRepository
												.getDetailsByAccountIdentification(listAR.get(i).getScheme_name(),
														listAR.get(i).getAccount_identification());
										if (!accounts.isEmpty()) {
											list = beneficiaryRepository
													.getbeneficiaryByAccountId(accounts.get(0).getAccount_id());
											LOG.info("Retrieve from Beneficiary" + AccountId + " Size " + list.size());
											if (list.isEmpty()) {
												response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
												accountMain.setStatus("No Beneficiary are available");
											} else {
												if (pageNo != null) {
													if (pageNo > list.size()) {
														page = aisService.pagingWithQueryParam(pageNo, list.size());
													} else {
														page.setInitialpage(0);
														page.setEndPage(list.size());
													}
												} else {
													page.setInitialpage(0);
													page.setEndPage(list.size());
												}
												for (int k = page.getInitialpage(); k < page.getEndPage(); k++) {
													org.vp.pis.domain.Beneficiary beneficiary = new org.vp.pis.domain.Beneficiary();
													Servicer servicer = new Servicer();
													beneficiary.setAccountId(listAR.get(i).getAccount_id_ref());
													beneficiary.setBeneficiaryId(
															String.valueOf(list.get(k).getBeneficiary_id()));
													beneficiary.setReference(list.get(k).getBeneficiary_ref_id());
													CreditorAccount creditorAccount = new CreditorAccount();
													creditorAccount.setIdentification(String.valueOf(list.get(k)
															.getBeneficiary_creditor_account_identification()));
													creditorAccount.setSchemeName(
															list.get(k).getBeneficiary_creditor_account_schemename());
													creditorAccount.setName(
															list.get(k).getBeneficiary_creditor_account_name());
													creditorAccount.setSecondaryIdentification(String.valueOf(list
															.get(k)
															.getBeneficiary_creditor_account_secondary_identification()));
													beneficiary.setCreditorAccount(creditorAccount);
													servicer.setIdentification(
															list.get(k).getBeneficiary_servicer_identification());
													servicer.setSchemeName(
															list.get(k).getBeneficiary_servicer_schemename());
													beneficiary.setServicer(servicer);
													listBeneficiary.add(beneficiary);
												}
											}
										}
									}
								} else {
									execute = false;
								}
							}
						} else {
							response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
							accountMain.setStatus("Date got expired");
							LOG.info("Date got expired");
						}
					} else {
						response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						accountMain.setStatus("Permission are not available");
						LOG.info("Permission are not available");
					}
					data.setBeneficiary(listBeneficiary);
					if (listBeneficiary.size() >= 1) {
						accountMain.setStatus(null);
					}
					Links links = new Links();
					links.setSelf("/accounts/" + AccountId + "/beneficiaries/");
					if (list.size() > 100) {
						links.setFirst("/accounts/" + AccountId + "/beneficiaries/");
						links.setLast("/accounts/" + AccountId + "/beneficiaries?pg=" + count);
						links.setNext("/accounts/" + AccountId + "/beneficiaries?pg="
								+ (Math.ceil(page.getEndPage() / 100 + 0.4) + 1));
					}
					if (Math.ceil(page.getEndPage() / 100) >= 2) {
						links.setPrev("/accounts/" + AccountId + "/beneficiaries?pg="
								+ (Math.ceil(page.getEndPage() / 100 + 0.4) - 1));
					}
					accountMain.setLinks(links);

					Meta meta = new Meta();
					meta.setTotalPages(count);
					meta.setFirstAvailableDateTime(listAR.size() <= 0 ? ""
							: inFormat.format(listAR.get(0).getMaker_date()).replace("Z", "-00:00"));
					meta.setLastAvailableDateTime(inFormat.format(new Date()).replace("Z", "-00:00"));
					accountMain.setMeta(meta);
					accountMain.setData(data);
				} else {
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					accountMain.setStatus("Requested Account ID is not available");
					LOG.info("Requested Account ID is not available");
				}
				responseResult = gson.toJson(accountMain);
			}
		} catch (Exception e) {
			LOG.info(aisService.getStackTrace(e));
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			accountMain.setStatus("Exception " + e.getCause() + "  -  " + aisService.getStackTrace(e));
		}
		response.addHeader("x-jws-signature", request.getHeader("x-jws-signature"));
		response.addHeader("x-fapi-interaction-id", request.getHeader("x-fapi-interaction-id"));
		return responseResult;
	}

	@RequestMapping(value = "/direct-debits", method = RequestMethod.GET)
	public String getDirectDebits(@RequestParam(value = "pg", required = false) Integer pageNo,
			HttpServletResponse response, HttpServletRequest request) {
		List<DirectDebits> list = new ArrayList<DirectDebits>();
		List<AccountRequest> listAR = new ArrayList<AccountRequest>();
		int count = 0;
		AccountMain accountMain = new AccountMain();
		ArrayList<DirectDebit> listDD = new ArrayList<DirectDebit>();
		boolean execute = false;
		String responseResult = "";
		Page page = new Page();

		try {
			responseResult = aisService.getRequestHeaderValidation(request, response);
			if (response.getStatus() != HttpServletResponse.SC_BAD_REQUEST
					&& response.getStatus() != HttpServletResponse.SC_NOT_ACCEPTABLE) {
				/**
				 * To get Account Request from application.properties Removed in
				 * Real Life Scenario
				 */
				String accountRequestID = aisService.getAccountRequestId();
				LOG.info("accountRequestID" + accountRequestID);
				listAR = accountsRequestRepository.getDetailsByAccountRequestId(accountRequestID);
				if (!listAR.isEmpty()) {
					count = (int) (Math.ceil(listAR.size() / 100 + 0.4));
					Data data = new Data();
					if (listAR.get(0).getRead_direct_debits().equalsIgnoreCase("Yes")) {

						Date date1 = new Date();
						Date date2 = listAR.get(0).getExpiration_Date_Time();
						if (date1.compareTo(date2) < 0) {
							if (pageNo != null) {
								if (pageNo > listAR.size()) {
									page = aisService.pagingWithQueryParam(pageNo, listAR.size());
								} else {
									page.setInitialpage(0);
									page.setEndPage(listAR.size());
								}
							} else {
								page.setInitialpage(0);
								page.setEndPage(listAR.size());
							}
							for (int i = page.getInitialpage(); i < page.getEndPage(); i++) {
								execute = aisService.checkAccountIsValid(listAR.get(i).getScheme_name(),
										listAR.get(i).getAccount_identification());
								if (execute && "Authorised".equalsIgnoreCase(listAR.get(i).getStatus())) {
									execute = true;
									if (execute) {
										List<org.vp.pis.model.Account> accounts = accountRepository
												.getDetailsByAccountIdentification(listAR.get(i).getScheme_name(),
														listAR.get(i).getAccount_identification());
										if (!accounts.isEmpty()) {
											list = directDebitRepository
													.getDirectDebitsByAccountId(accounts.get(0).getAccount_id());
											if (!list.isEmpty()) {
												for (int k = 0; k < list.size(); k++) {
													LOG.info(String.valueOf(list.get(0).getDebtor_accountid_id()));
													DirectDebit directDebit = new DirectDebit();
													directDebit.setAccountId(listAR.get(i).getAccount_id_ref());
													directDebit.setDirectDebitId(
															String.valueOf(list.get(k).getDirect_debit_id()));
													directDebit.setDirectDebitStatusCode(
															list.get(k).getDirectdebit_status_code());
													directDebit.setMandateIdentification(
															list.get(k).getMandate_identification());
													directDebit.setName(list.get(k).getName_service_user());
													directDebit.setPreviousPaymentDateTime(
															inFormat.format(list.get(k).getPrevious_payment_datetime())
																	.replace("Z", "-00:00"));
													PreviousPaymentAmount previousPaymentAmount = new PreviousPaymentAmount();
													previousPaymentAmount.setAmount(defaultformat
															.format(list.get(k).getPrevious_payment_amount()));
													previousPaymentAmount.setCurrency(list.get(k).getCurrency());
													directDebit.setPreviousPaymentAmount(previousPaymentAmount);
													listDD.add(directDebit);
												}
											}
										}
									}
								} else {
									execute = false;
								}
							}
						} else {
							response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
							accountMain.setStatus("Date got expired");
							LOG.info("Date got expired");
						}
					} else {
						response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						accountMain.setStatus("Permission are not available");
						LOG.info("Permission are not available");
					}
					data.setDirectDebit(listDD);
					if (listDD.size() >= 1) {
						accountMain.setStatus(null);
					}
					Links links = new Links();
					links.setSelf("/direct-debits/");
					if (listAR.size() > 100) {
						links.setFirst("/direct-debits/");
						links.setLast("/direct-debits?pg=" + count);
						links.setNext("/direct-debits?pg=" + (Math.ceil(page.getEndPage() / 100 + 0.4) + 1));
					}
					if (Math.ceil(page.getEndPage() / 100) >= 2) {
						links.setPrev("/direct-debits?pg=" + (Math.ceil(page.getEndPage() / 100 + 0.4) - 1));
					}
					accountMain.setLinks(links);

					Meta meta = new Meta();
					meta.setTotalPages(count);
					meta.setFirstAvailableDateTime(listAR.size() <= 0 ? ""
							: inFormat.format(listAR.get(0).getMaker_date()).replace("Z", "-00:00"));
					meta.setLastAvailableDateTime(inFormat.format(new Date()).replace("Z", "-00:00"));
					accountMain.setMeta(meta);
					accountMain.setData(data);
				} else {
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					accountMain.setStatus("Requested Account Request ID is not available");
					LOG.info("Requested Account Request ID is not available");
				}
				responseResult = gson.toJson(accountMain);
			}
		} catch (Exception e) {
			LOG.info(aisService.getStackTrace(e));
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			accountMain.setStatus(e.getCause() + "  -  " + aisService.getStackTrace(e));
		}
		response.addHeader("x-jws-signature", request.getHeader("x-jws-signature"));
		response.addHeader("x-fapi-interaction-id", request.getHeader("x-fapi-interaction-id"));
		return responseResult;
	}

	@RequestMapping(value = "/accounts/{AccountId}/direct-debits", method = RequestMethod.GET)
	public String getDDByAccountId(@PathVariable(value = "AccountId") String AccountId,
			@RequestParam(value = "pg", required = false) Integer pageNo, HttpServletResponse response,
			HttpServletRequest request) {
		List<DirectDebits> list = new ArrayList<DirectDebits>();
		List<AccountRequest> listAR = new ArrayList<AccountRequest>();
		int count = 0;
		ArrayList<DirectDebit> listDD = new ArrayList<DirectDebit>();
		AccountMain accountMain = new AccountMain();
		boolean execute = false;
		String responseResult = "";
		Page page = new Page();

		try {
			responseResult = aisService.getRequestHeaderValidation(request, response);
			if (response.getStatus() != HttpServletResponse.SC_BAD_REQUEST
					&& response.getStatus() != HttpServletResponse.SC_NOT_ACCEPTABLE) {
				listAR = accountsRequestRepository.getDetailsByAccountId(AccountId);
				if (!listAR.isEmpty()) {
					count = (int) (Math.ceil(listAR.size() / 100 + 0.4));
					Data data = new Data();
					if (listAR.get(0).getRead_direct_debits().equalsIgnoreCase("Yes")) {

						Date date1 = new Date();
						Date date2 = listAR.get(0).getExpiration_Date_Time();
						if (date1.compareTo(date2) < 0) {

							for (int i = 0; i < listAR.size(); i++) {
								execute = aisService.checkAccountIsValid(listAR.get(i).getScheme_name(),
										listAR.get(i).getAccount_identification());
								if (execute && "Authorised".equalsIgnoreCase(listAR.get(i).getStatus())) {
									execute = true;
									if (execute) {
										List<org.vp.pis.model.Account> accounts = accountRepository
												.getDetailsByAccountIdentification(listAR.get(i).getScheme_name(),
														listAR.get(i).getAccount_identification());
										if (!accounts.isEmpty()) {
											list = directDebitRepository
													.getDirectDebitsByAccountId(accounts.get(0).getAccount_id());
											if (!list.isEmpty()) {
												if (pageNo != null) {
													if (pageNo > list.size()) {
														page = aisService.pagingWithQueryParam(pageNo, list.size());
													} else {
														page.setInitialpage(0);
														page.setEndPage(list.size());
													}
												} else {
													page.setInitialpage(0);
													page.setEndPage(list.size());
												}
												for (int k = page.getInitialpage(); k < page.getEndPage(); k++) {
													LOG.info(String.valueOf(list.get(0).getDebtor_accountid_id()));
													DirectDebit directDebit = new DirectDebit();
													directDebit.setAccountId(listAR.get(i).getAccount_id_ref());
													directDebit.setDirectDebitId(
															String.valueOf(list.get(k).getDirect_debit_id()));
													directDebit.setDirectDebitStatusCode(
															list.get(k).getDirectdebit_status_code());
													directDebit.setMandateIdentification(
															list.get(k).getMandate_identification());
													directDebit.setName(list.get(k).getName_service_user());
													directDebit.setPreviousPaymentDateTime(inFormat
															.format(list.get(k).getPrevious_payment_datetime()));
													PreviousPaymentAmount previousPaymentAmount = new PreviousPaymentAmount();
													previousPaymentAmount.setAmount(defaultformat
															.format(list.get(k).getPrevious_payment_amount()));
													previousPaymentAmount.setCurrency(list.get(k).getCurrency());
													directDebit.setPreviousPaymentAmount(previousPaymentAmount);
													listDD.add(directDebit);
												}
											}
										}
									}
								} else {
									execute = false;
								}
							}
						} else {
							response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
							accountMain.setStatus("Date got expired");
							LOG.info("Date got expired");
						}
					} else {
						response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						accountMain.setStatus("Permission are not available");
						LOG.info("Permission are not available");
					}
					data.setDirectDebit(listDD);
					if (listDD.size() >= 1) {
						accountMain.setStatus(null);
					}
					Links links = new Links();
					links.setSelf("/accounts/" + AccountId + "/direct-debits");
					if (listAR.size() > 100) {
						links.setFirst("/accounts/" + AccountId + "/direct-debits");
						links.setLast("/accounts/" + AccountId + "/direct-debits?pg=" + count);
						links.setNext("/accounts/" + AccountId + "/direct-debits?pg="
								+ (Math.ceil(page.getEndPage() / 100 + 0.4) + 1));
					}
					if (Math.ceil(page.getEndPage() / 100) >= 2) {
						links.setPrev("/accounts/" + AccountId + "/direct-debits?pg="
								+ (Math.ceil(page.getEndPage() / 100 + 0.4) - 1));
					}
					accountMain.setLinks(links);

					Meta meta = new Meta();
					meta.setTotalPages(count);
					meta.setFirstAvailableDateTime(listAR.size() <= 0 ? ""
							: inFormat.format(listAR.get(0).getMaker_date()).replace("Z", "-00:00"));
					meta.setLastAvailableDateTime(inFormat.format(new Date()).replace("Z", "-00:00"));
					accountMain.setMeta(meta);
					accountMain.setData(data);
				} else {
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					accountMain.setStatus("Requested Account ID is not available");
					LOG.info("Requested Account ID is not available");
				}
				responseResult = gson.toJson(accountMain);
			}
		} catch (Exception e) {
			LOG.info(aisService.getStackTrace(e));
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			accountMain.setStatus("Exception " + e.getCause() + "  -  " + aisService.getStackTrace(e));
		}
		response.addHeader("x-jws-signature", request.getHeader("x-jws-signature"));
		response.addHeader("x-fapi-interaction-id", request.getHeader("x-fapi-interaction-id"));
		return responseResult;
	}

	@RequestMapping(value = "/products", method = RequestMethod.GET)
	public String getProducts(@RequestParam(value = "pg", required = false) Integer pageNo,
			HttpServletResponse response, HttpServletRequest request) {
		List<Product> list = new ArrayList<Product>();
		List<AccountRequest> listAR = new ArrayList<AccountRequest>();
		int count = 0;
		ArrayList<org.vp.pis.domain.Product> listProduct = new ArrayList<org.vp.pis.domain.Product>();
		AccountMain accountMain = new AccountMain();
		boolean execute = false;
		String responseResult = "";
		Page page = new Page();

		try {
			responseResult = aisService.getRequestHeaderValidation(request, response);
			if (response.getStatus() != HttpServletResponse.SC_BAD_REQUEST
					&& response.getStatus() != HttpServletResponse.SC_NOT_ACCEPTABLE) {
				/**
				 * To get Account Request from application.properties Removed in
				 * Real Life Scenario
				 */
				String accountRequestID = aisService.getAccountRequestId();
				listAR = accountsRequestRepository.getDetailsByAccountRequestId(accountRequestID);
				if (!listAR.isEmpty()) {
					count = (int) (Math.ceil(listAR.size() / 100 + 0.4));
					Data data = new Data();
					if (listAR.get(0).getRead_products().equalsIgnoreCase("Yes")) {

						Date date1 = new Date();
						Date date2 = listAR.get(0).getExpiration_Date_Time();
						if (date1.compareTo(date2) < 0) {
							if (pageNo != null) {
								if (pageNo > listAR.size()) {
									page = aisService.pagingWithQueryParam(pageNo, listAR.size());
								} else {
									page.setInitialpage(0);
									page.setEndPage(listAR.size());
								}
							} else {
								page.setInitialpage(0);
								page.setEndPage(listAR.size());
							}
							for (int i = page.getInitialpage(); i < page.getEndPage(); i++) {
								execute = aisService.checkAccountIsValid(listAR.get(i).getScheme_name(),
										listAR.get(i).getAccount_identification());
								if (execute && "Authorised".equalsIgnoreCase(listAR.get(i).getStatus())) {
									execute = true;
									if (execute) {
										List<org.vp.pis.model.Account> accounts = accountRepository
												.getDetailsByAccountIdentification(listAR.get(i).getScheme_name(),
														listAR.get(i).getAccount_identification());
										if (!accounts.isEmpty()) {
											Integer productID = productRepository
													.getProductIdByAccountId(accounts.get(0).getAccount_id());
											LOG.debug("Productid : " + productID);
											if (productID == null) {
												response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
												accountMain.setStatus("No Product Available for the Account ID");
											} else {
												list = productRepository.getProductByID(productID);
												if (!list.isEmpty()) {
													for (int k = 0; k < list.size(); k++) {
														org.vp.pis.domain.Product product = new org.vp.pis.domain.Product();
														product.setProductIdentifier(
																list.get(0).getProduct_identifier());
														product.setAccountId(listAR.get(i).getAccount_id_ref());
														product.setProductName(list.get(0).getProduct_name());
														product.setProductType(list.get(0).getProduct_type());
														product.setSecondaryProductIdentifier(
																list.get(0).getProduct_group());
														listProduct.add(product);
													}
												}
											}
										}
									}
								} else {
									execute = false;
								}
							}
						} else {
							response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
							accountMain.setStatus("Date got expired");
							LOG.info("Date got expired");
						}
					} else {
						response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						accountMain.setStatus("Permission are not available");
						LOG.info("Permission are not available");
					}
					data.setProduct(listProduct);
					if (listProduct.size() >= 1) {
						accountMain.setStatus(null);
					}
					Links links = new Links();
					links.setSelf("/products/");
					if (listAR.size() > 100) {
						links.setFirst("/products/");
						links.setLast("/products?pg=" + count);
						links.setNext("/products?pg=" + (Math.ceil(page.getEndPage() / 100 + 0.4) + 1));
					}
					if (Math.ceil(page.getEndPage() / 100) >= 2) {
						links.setPrev("/products?pg=" + (Math.ceil(page.getEndPage() / 100 + 0.4) - 1));
					}
					accountMain.setLinks(links);

					Meta meta = new Meta();
					meta.setTotalPages(count);
					meta.setFirstAvailableDateTime(listAR.size() <= 0 ? ""
							: inFormat.format(listAR.get(0).getMaker_date()).replace("Z", "-00:00"));
					meta.setLastAvailableDateTime(inFormat.format(new Date()).replace("Z", "-00:00"));
					accountMain.setMeta(meta);
					accountMain.setData(data);
				} else {
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					accountMain.setStatus("Requested Account Request ID is not available");
					LOG.info("Requested Account Request ID is not available");
				}
				responseResult = gson.toJson(accountMain);
			}
		} catch (Exception e) {
			LOG.info(aisService.getStackTrace(e));
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			accountMain.setStatus(e.getCause() + "  -  " + aisService.getStackTrace(e));
		}
		response.addHeader("x-jws-signature", request.getHeader("x-jws-signature"));
		response.addHeader("x-fapi-interaction-id", request.getHeader("x-fapi-interaction-id"));
		return responseResult;
	}

	@RequestMapping(value = "/accounts/{AccountId}/product", method = RequestMethod.GET)
	public String getProductByAccountID(@PathVariable(value = "AccountId") String AccountId,
			@RequestParam(value = "pg", required = false) Integer pageNo, HttpServletResponse response,
			HttpServletRequest request) {
		List<Product> list = new ArrayList<Product>();
		List<AccountRequest> listAR = new ArrayList<AccountRequest>();
		int count = 0;
		ArrayList<org.vp.pis.domain.Product> listProduct = new ArrayList<org.vp.pis.domain.Product>();
		AccountMain accountMain = new AccountMain();
		boolean execute = false;
		String responseResult = "";
		Page page = new Page();

		try {
			responseResult = aisService.getRequestHeaderValidation(request, response);
			if (response.getStatus() != HttpServletResponse.SC_BAD_REQUEST
					&& response.getStatus() != HttpServletResponse.SC_NOT_ACCEPTABLE) {
				listAR = accountsRequestRepository.getDetailsByAccountId(AccountId);
				if (!listAR.isEmpty()) {
					count = (int) (Math.ceil(listAR.size() / 100 + 0.4));
					Data data = new Data();
					if (listAR.get(0).getRead_products().equalsIgnoreCase("Yes")) {
						Date date1 = new Date();
						Date date2 = listAR.get(0).getExpiration_Date_Time();
						if (date1.compareTo(date2) < 0) {

							for (int i = 0; i < listAR.size(); i++) {
								execute = aisService.checkAccountIsValid(listAR.get(i).getScheme_name(),
										listAR.get(i).getAccount_identification());
								if (execute && "Authorised".equalsIgnoreCase(listAR.get(i).getStatus())) {
									execute = true;
									if (execute) {
										List<org.vp.pis.model.Account> accounts = accountRepository
												.getDetailsByAccountIdentification(listAR.get(i).getScheme_name(),
														listAR.get(i).getAccount_identification());
										if (!accounts.isEmpty()) {
											Integer productID = productRepository
													.getProductIdByAccountId(accounts.get(0).getAccount_id());
											LOG.debug("Productid : " + productID);
											if (productID == null) {
												response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
												accountMain.setStatus("No Product Available for the Account ID");
											} else {
												list = productRepository.getProductByID(productID);
												if (!list.isEmpty()) {
													if (pageNo != null) {
														if (pageNo > list.size()) {
															page = aisService.pagingWithQueryParam(pageNo, list.size());
														} else {
															page.setInitialpage(0);
															page.setEndPage(list.size());
														}
													} else {
														page.setInitialpage(0);
														page.setEndPage(list.size());
													}
													for (int k = page.getInitialpage(); k < page.getEndPage(); k++) {
														org.vp.pis.domain.Product product = new org.vp.pis.domain.Product();
														product.setProductIdentifier(
																list.get(0).getProduct_identifier());
														product.setAccountId(listAR.get(i).getAccount_id_ref());
														product.setProductName(list.get(0).getProduct_name());
														product.setProductType(list.get(0).getProduct_type());
														product.setSecondaryProductIdentifier(
																list.get(0).getProduct_group());
														listProduct.add(product);
													}
												}
											}
										}
									}
								} else {
									execute = false;
								}
							}
						} else {
							response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
							accountMain.setStatus("Date got expired");
							LOG.info("Date got expired");
						}
					} else {
						response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						accountMain.setStatus("Permission are not available");
						LOG.info("Permission are not available");
					}
					data.setProduct(listProduct);
					if (listProduct.size() >= 1) {
						accountMain.setStatus(null);
					}
					Links links = new Links();
					links.setSelf("/accounts/" + AccountId + "/product/");
					if (listAR.size() > 100) {
						links.setFirst("/accounts/" + AccountId + "/product/");
						links.setLast("/accounts/" + AccountId + "/product?pg=" + count);
						links.setNext("/accounts/" + AccountId + "/product?pg="
								+ (Math.ceil(page.getEndPage() / 100 + 0.4) + 1));
					}
					if (Math.ceil(page.getEndPage() / 100) >= 2) {
						links.setPrev("/accounts/" + AccountId + "/product?pg="
								+ (Math.ceil(page.getEndPage() / 100 + 0.4) - 1));
					}
					accountMain.setLinks(links);

					Meta meta = new Meta();
					meta.setTotalPages(count);
					meta.setFirstAvailableDateTime(listAR.size() <= 0 ? ""
							: inFormat.format(listAR.get(0).getMaker_date()).replace("Z", "-00:00"));
					meta.setLastAvailableDateTime(inFormat.format(new Date()).replace("Z", "-00:00"));
					accountMain.setMeta(meta);
					accountMain.setData(data);
				} else {
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					accountMain.setStatus("Requested Account ID is not available");
					LOG.info("Requested Account ID is not available");
				}
				responseResult = gson.toJson(accountMain);
			}
		} catch (Exception e) {
			LOG.info(aisService.getStackTrace(e));
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			accountMain.setStatus("Exception " + e.getCause() + "  -  " + aisService.getStackTrace(e));
		}
		response.addHeader("x-jws-signature", request.getHeader("x-jws-signature"));
		response.addHeader("x-fapi-interaction-id", request.getHeader("x-fapi-interaction-id"));
		return responseResult;
	}

	@RequestMapping(value = "/standing-orders", method = RequestMethod.GET)
	public String getStandingOrder(@RequestParam(value = "pg", required = false) Integer pageNo,
			HttpServletResponse response, HttpServletRequest request) {
		List<StandingOrders> list = new ArrayList<StandingOrders>();
		List<AccountRequest> listAR = new ArrayList<AccountRequest>();
		ArrayList<StandingOrder> listSO = new ArrayList<StandingOrder>();
		AccountMain accountMain = new AccountMain();
		boolean execute = false;
		int count = 0;
		String responseResult = "";
		Page page = new Page();

		try {
			responseResult = aisService.getRequestHeaderValidation(request, response);
			if (response.getStatus() != HttpServletResponse.SC_BAD_REQUEST
					&& response.getStatus() != HttpServletResponse.SC_NOT_ACCEPTABLE) {
				/**
				 * To get Account Request from application.properties Removed in
				 * Real Life Scenario
				 */
				String accountRequestID = aisService.getAccountRequestId();
				listAR = accountsRequestRepository.getDetailsByAccountRequestId(accountRequestID);
				if (!listAR.isEmpty()) {
					count = (int) (Math.ceil(listAR.size() / 100 + 0.4));
					Data data = new Data();
					if ((listAR.get(0).getRead_standing_orders_basic().equalsIgnoreCase("Yes")
							|| listAR.get(0).getRead_standing_orders_detail().equalsIgnoreCase("Yes"))) {

						Date date1 = new Date();
						Date date2 = listAR.get(0).getExpiration_Date_Time();
						if (date1.compareTo(date2) < 0) {
							if (pageNo != null) {
								if (pageNo > listAR.size()) {
									page = aisService.pagingWithQueryParam(pageNo, listAR.size());
								} else {
									page.setInitialpage(0);
									page.setEndPage(listAR.size());
								}
							} else {
								page.setInitialpage(0);
								page.setEndPage(listAR.size());
							}
							for (int i = page.getInitialpage(); i < page.getEndPage(); i++) {
								execute = aisService.checkAccountIsValid(listAR.get(i).getScheme_name(),
										listAR.get(i).getAccount_identification());
								if (execute && "Authorised".equalsIgnoreCase(listAR.get(i).getStatus())) {
									execute = true;
									if (execute) {
										List<org.vp.pis.model.Account> accounts = accountRepository
												.getDetailsByAccountIdentification(listAR.get(i).getScheme_name(),
														listAR.get(i).getAccount_identification());
										if (!accounts.isEmpty()) {
											list = standingOrderRepository
													.getStandingOrderByAccountId(accounts.get(0).getAccount_id());
											if (!list.isEmpty()) {
												for (int k = 0; k < list.size(); k++) {
													SimpleDateFormat formatter = new SimpleDateFormat(
															"yyyy-MM-dd'T'HH:mm:ss'Z'");
													formatter.setTimeZone(TimeZone.getTimeZone("Europe/London"));
													LOG.info(String.valueOf(list.get(k).getStanding_order_id()));
													StandingOrder standingOrder = new StandingOrder();
													Servicer servicer = new Servicer();
													CreditorAccount creditorAccount = new CreditorAccount();
													creditorAccount.setIdentification(String
															.valueOf(list.get(k).getCreditor_account_identification()));
													creditorAccount.setName(list.get(k).getCreditor_account_name());
													creditorAccount.setSchemeName(
															list.get(k).getCreditor_account_scheme_name());
													creditorAccount.setSecondaryIdentification(String.valueOf(list
															.get(k).getCreditor_account_secondary_identification()));
													standingOrder.setCreditorAccount(creditorAccount);
													servicer.setIdentification(
															String.valueOf(list.get(k).getServicer_identification()));
													servicer.setSchemeName(list.get(k).getServicer_scheme_name());
													standingOrder.setServicer(servicer);
													standingOrder.setAccountId(listAR.get(i).getAccount_id_ref());
													standingOrder.setFinalPaymentDateTime(
															formatter.format(list.get(k).getFinal_payment_date_time()));
													FinalPaymentAmount finalPaymentAmount = new FinalPaymentAmount();
													finalPaymentAmount.setAmount(defaultformat
															.format(list.get(k).getFinal_payment_amount()));
													finalPaymentAmount
															.setCurrency(list.get(k).getFinal_payment_currency());
													standingOrder.setFinalPaymentAmount(finalPaymentAmount);
													standingOrder.setFirstPaymentDateTime(
															formatter.format(list.get(k).getFirst_payment_date_time()));
													FirstPaymentAmount firstPaymentAmount = new FirstPaymentAmount();
													firstPaymentAmount.setAmount(defaultformat
															.format(list.get(k).getFirst_payment_amount()));
													firstPaymentAmount
															.setCurrency(list.get(k).getFirst_payment_currency());
													standingOrder.setFirstPaymentAmount(firstPaymentAmount);
													standingOrder.setNextPaymentDateTime(
															formatter.format(list.get(k).getNext_payment_date_time()));
													NextPaymentAmount nextPaymentAmount = new NextPaymentAmount();
													nextPaymentAmount.setAmount(
															defaultformat.format(list.get(k).getNext_payment_amount()));
													nextPaymentAmount
															.setCurrency(list.get(k).getNext_payment_currency());
													standingOrder.setNextPaymentAmount(nextPaymentAmount);
													standingOrder.setFrequency(list.get(k).getFrequency());
													standingOrder.setReference(list.get(k).getReference());

													standingOrder.setStandingOrderId(
															String.valueOf(list.get(k).getStanding_order_id()));
													listSO.add(standingOrder);
												}
											}
										}
									}
								} else {
									execute = false;
								}
							}
						} else {
							response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
							accountMain.setStatus("Date got expired");
							LOG.info("Date got expired");
						}
					} else {
						response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						accountMain.setStatus("Permission are not available");
						LOG.info("Permission are not available");
					}
					data.setStandingOrder(listSO);
					if (listSO.size() >= 1) {
						accountMain.setStatus(null);
					}
					Links links = new Links();
					links.setSelf("/standing-orders/");
					if (listAR.size() > 100) {
						links.setFirst("/standing-orders/");
						links.setLast("/standing-orders?pg=" + count);
						links.setNext("/standing-orders?pg=" + (Math.ceil(page.getEndPage() / 100 + 0.4) + 1));
					}
					if (Math.ceil(page.getEndPage() / 100) >= 2) {
						links.setPrev("/standing-orders?pg=" + (Math.ceil(page.getEndPage() / 100 + 0.4) - 1));
					}
					accountMain.setLinks(links);

					Meta meta = new Meta();
					meta.setTotalPages(count);
					meta.setFirstAvailableDateTime(listAR.size() <= 0 ? ""
							: inFormat.format(listAR.get(0).getMaker_date()).replace("Z", "-00:00"));
					meta.setLastAvailableDateTime(inFormat.format(new Date()).replace("Z", "-00:00"));
					accountMain.setMeta(meta);
					accountMain.setData(data);
				} else {
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					accountMain.setStatus("Requested Account Request ID is not available");
					LOG.info("Requested Account Request ID is not available");
				}
				responseResult = gson.toJson(accountMain);
			}
		} catch (Exception e) {
			LOG.info(aisService.getStackTrace(e));
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			accountMain.setStatus(e.getCause() + "  -  " + aisService.getStackTrace(e));
		}
		response.addHeader("x-jws-signature", request.getHeader("x-jws-signature"));
		response.addHeader("x-fapi-interaction-id", request.getHeader("x-fapi-interaction-id"));
		return responseResult;
	}

	@RequestMapping(value = "/accounts/{AccountId}/standing-orders", method = RequestMethod.GET)
	public String getSOByAccountID(@PathVariable(value = "AccountId") String AccountId,
			@RequestParam(value = "pg", required = false) Integer pageNo, HttpServletResponse response,
			HttpServletRequest request) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		formatter.setTimeZone(TimeZone.getTimeZone("Europe/London"));
		List<StandingOrders> list = new ArrayList<StandingOrders>();
		List<AccountRequest> listAR = new ArrayList<AccountRequest>();
		int count = 0;
		ArrayList<StandingOrder> listSO = new ArrayList<StandingOrder>();
		AccountMain accountMain = new AccountMain();
		boolean execute = false;
		String responseResult = "";
		Page page = new Page();

		try {
			responseResult = aisService.getRequestHeaderValidation(request, response);
			if (response.getStatus() != HttpServletResponse.SC_BAD_REQUEST
					&& response.getStatus() != HttpServletResponse.SC_NOT_ACCEPTABLE) {
				listAR = accountsRequestRepository.getDetailsByAccountId(AccountId);
				if (!listAR.isEmpty()) {
					count = (int) (Math.ceil(listAR.size() / 100 + 0.4));

					Data data = new Data();
					if ((listAR.get(0).getRead_standing_orders_basic().equalsIgnoreCase("Yes")
							|| listAR.get(0).getRead_standing_orders_detail().equalsIgnoreCase("Yes"))) {

						Date date1 = new Date();
						Date date2 = listAR.get(0).getExpiration_Date_Time();
						if (date1.compareTo(date2) < 0) {

							for (int i = 0; i < listAR.size(); i++) {
								execute = aisService.checkAccountIsValid(listAR.get(i).getScheme_name(),
										listAR.get(i).getAccount_identification());
								if (execute && "Authorised".equalsIgnoreCase(listAR.get(i).getStatus())) {
									execute = true;
									if (execute) {
										List<org.vp.pis.model.Account> accounts = accountRepository
												.getDetailsByAccountIdentification(listAR.get(i).getScheme_name(),
														listAR.get(i).getAccount_identification());
										if (!accounts.isEmpty()) {
											list = standingOrderRepository
													.getStandingOrderByAccountId(accounts.get(0).getAccount_id());
											if (!list.isEmpty()) {
												if (pageNo != null) {
													if (pageNo > list.size()) {
														page = aisService.pagingWithQueryParam(pageNo, list.size());
													} else {
														page.setInitialpage(0);
														page.setEndPage(list.size());
													}
												} else {
													page.setInitialpage(0);
													page.setEndPage(list.size());
												}
												for (int k = page.getInitialpage(); k < page.getEndPage(); k++) {
													LOG.info(String.valueOf(list.get(k).getStanding_order_id()));
													StandingOrder standingOrder = new StandingOrder();
													Servicer servicer = new Servicer();
													CreditorAccount creditorAccount = new CreditorAccount();
													creditorAccount.setIdentification(String
															.valueOf(list.get(k).getCreditor_account_identification()));
													creditorAccount.setName(list.get(k).getCreditor_account_name());
													creditorAccount.setSchemeName(
															list.get(k).getCreditor_account_scheme_name());
													creditorAccount.setSecondaryIdentification(String.valueOf(list
															.get(k).getCreditor_account_secondary_identification()));
													standingOrder.setCreditorAccount(creditorAccount);
													servicer.setIdentification(
															String.valueOf(list.get(k).getServicer_identification()));
													servicer.setSchemeName(list.get(k).getServicer_scheme_name());
													standingOrder.setServicer(servicer);
													standingOrder.setAccountId(
															String.valueOf(list.get(k).getCreditor_account_id()));
													standingOrder.setFinalPaymentDateTime(
															formatter.format(list.get(k).getFinal_payment_date_time()));
													FinalPaymentAmount finalPaymentAmount = new FinalPaymentAmount();
													finalPaymentAmount.setAmount(defaultformat
															.format(list.get(k).getFinal_payment_amount()));
													finalPaymentAmount
															.setCurrency(list.get(k).getFinal_payment_currency());
													standingOrder.setFinalPaymentAmount(finalPaymentAmount);
													standingOrder.setFirstPaymentDateTime(
															formatter.format(list.get(k).getFirst_payment_date_time()));
													FirstPaymentAmount firstPaymentAmount = new FirstPaymentAmount();
													firstPaymentAmount.setAmount(defaultformat
															.format(list.get(k).getFirst_payment_amount()));
													firstPaymentAmount
															.setCurrency(list.get(k).getFirst_payment_currency());
													standingOrder.setFirstPaymentAmount(firstPaymentAmount);
													standingOrder.setNextPaymentDateTime(
															formatter.format(list.get(k).getNext_payment_date_time()));
													NextPaymentAmount nextPaymentAmount = new NextPaymentAmount();
													nextPaymentAmount.setAmount(
															defaultformat.format(list.get(k).getNext_payment_amount()));
													nextPaymentAmount
															.setCurrency(list.get(k).getNext_payment_currency());
													standingOrder.setNextPaymentAmount(nextPaymentAmount);
													standingOrder.setFrequency(list.get(k).getFrequency());
													standingOrder.setReference(list.get(k).getReference());

													standingOrder.setStandingOrderId(
															String.valueOf(list.get(k).getStanding_order_id()));
													listSO.add(standingOrder);
												}
											}
										}
									}
								} else {
									execute = false;
								}
							}
						} else {
							response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
							accountMain.setStatus("Date got expired");
							LOG.info("Date got expired");
						}
					} else {
						response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						accountMain.setStatus("Permission are not available");
						LOG.info("Permission are not available");
					}
					data.setStandingOrder(listSO);
					if (listSO.size() >= 1) {
						accountMain.setStatus(null);
					}
					Links links = new Links();
					links.setSelf("/accounts/" + AccountId + "/standing-orders");
					if (list.size() > 100) {
						links.setFirst("/accounts/" + AccountId + "/standing-orders");
						links.setLast("/accounts/" + AccountId + "/standing-orders?pg=" + count);
						links.setNext("/accounts/" + AccountId + "/standing-orders?pg="
								+ (Math.ceil(page.getEndPage() / 100 + 0.4) + 1));
					}
					if (Math.ceil(page.getEndPage() / 100) >= 2) {
						links.setPrev("/accounts/" + AccountId + "/standing-orders?pg="
								+ (Math.ceil(page.getEndPage() / 100 + 0.4) - 1));
					}
					accountMain.setLinks(links);

					Meta meta = new Meta();
					meta.setTotalPages(count);
					meta.setFirstAvailableDateTime(listAR.size() <= 0 ? ""
							: inFormat.format(listAR.get(0).getMaker_date()).replace("Z", "-00:00"));
					meta.setLastAvailableDateTime(inFormat.format(new Date()).replace("Z", "-00:00"));
					accountMain.setMeta(meta);
					accountMain.setData(data);
				} else {
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					accountMain.setStatus("Requested Account ID is not available");
					LOG.info("Requested Account ID is not available");
				}
				responseResult = gson.toJson(accountMain);
			}
		} catch (Exception e) {
			LOG.info(aisService.getStackTrace(e));
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			accountMain.setStatus("Exception " + e.getCause() + "  -  " + aisService.getStackTrace(e));
		}
		response.addHeader("x-jws-signature", request.getHeader("x-jws-signature"));
		response.addHeader("x-fapi-interaction-id", request.getHeader("x-fapi-interaction-id"));
		return responseResult;
	}

	@RequestMapping(value = "/transactions", method = RequestMethod.GET)
	public String getTransactions(@RequestParam(value = "pg", required = false) Integer pageNo,
			@RequestParam(value = "toBookingDateTime", required = false) String toBookingDateTime,
			@RequestParam(value = "fromBookingDateTime", required = false) String fromBookingDateTime,
			HttpServletResponse response, HttpServletRequest request) throws Exception {
		List<org.vp.pis.model.Transaction> list = new ArrayList<org.vp.pis.model.Transaction>();
		List<AccountRequest> listAR = new ArrayList<AccountRequest>();
		int count = 0;
		ArrayList<Transaction> listTrxn = new ArrayList<Transaction>();
		AccountMain accountMain = new AccountMain();
		boolean execute = false;
		String responseResult = "";
		// String toBookingDateTime = request.getHeader("toBookingDateTime");
		LOG.info("Date" + toBookingDateTime);
		// String fromBookingDateTime =
		// request.getHeader("fromBookingDateTime");
		LOG.info("Date" + fromBookingDateTime);
		SimpleDateFormat convertFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		Page page = new Page();

		try {
			responseResult = aisService.getRequestHeaderValidation(request, response);
			if (response.getStatus() != HttpServletResponse.SC_BAD_REQUEST
					&& response.getStatus() != HttpServletResponse.SC_NOT_ACCEPTABLE) {
				/**
				 * To get Account Request from application.properties Removed in
				 * Real Life Scenario
				 */
				String accountRequestID = aisService.getAccountRequestId();
				listAR = accountsRequestRepository.getDetailsByAccountRequestId(accountRequestID);
				if (!listAR.isEmpty()) {
					count = (int) (Math.ceil(listAR.size() / 100 + 0.4));

					Data data = new Data();
					if ((listAR.get(0).getRead_transactions_basic().equalsIgnoreCase("Yes")
							|| listAR.get(0).getRead_transactions_credits().equalsIgnoreCase("Yes")
							|| listAR.get(0).getRead_transactions_debits().equalsIgnoreCase("Yes")
							|| listAR.get(0).getRead_transactions_detail().equalsIgnoreCase("Yes"))) {

						Date date1 = new Date();
						Date date2 = listAR.get(0).getExpiration_Date_Time();
						if (date1.compareTo(date2) < 0) {
							if (pageNo != null) {
								if (pageNo > listAR.size()) {
									page = aisService.pagingWithQueryParam(pageNo, listAR.size());
								} else {
									page.setInitialpage(0);
									page.setEndPage(listAR.size());
								}
							} else {
								page.setInitialpage(0);
								page.setEndPage(listAR.size());
							}
							for (int i = page.getInitialpage(); i < page.getEndPage(); i++) {
								execute = aisService.checkAccountIsValid(listAR.get(i).getScheme_name(),
										listAR.get(i).getAccount_identification());
								if (execute && "Authorised".equalsIgnoreCase(listAR.get(i).getStatus())) {
									execute = true;
									if (execute) {
										List<org.vp.pis.model.Account> accounts = accountRepository
												.getDetailsByAccountIdentification(listAR.get(i).getScheme_name(),
														listAR.get(i).getAccount_identification());
										if (!accounts.isEmpty()) {
											Date toDate = new Date();
											Date fromDate = new Date();
											boolean excuteTransaction = true;
											if (fromBookingDateTime != null && toBookingDateTime != null) {
												if ((convertFormat.parse(fromBookingDateTime)
														.compareTo(listAR.get(i).getTransaction_from_date_time()) > 0)
														&& (convertFormat.parse(fromBookingDateTime).compareTo(
																listAR.get(i).getTransaction_to_date_time()) < 0)
														&& (convertFormat.parse(toBookingDateTime).compareTo(
																listAR.get(i).getTransaction_from_date_time()) > 0)
														&& (convertFormat.parse(toBookingDateTime).compareTo(
																listAR.get(i).getTransaction_to_date_time()) < 0)
														&& (convertFormat.parse(toBookingDateTime).compareTo(
																convertFormat.parse(fromBookingDateTime)) > 0)) {
													excuteTransaction = true;
												}else{
													excuteTransaction = false;
												}
											}
											if (excuteTransaction) {
												if (toBookingDateTime != null && toBookingDateTime != ""
														&& (fromBookingDateTime == null || fromBookingDateTime == "")) {
													toDate = convertFormat.parse(toBookingDateTime);
													fromDate = listAR.get(i).getTransaction_from_date_time();
												} else if (toBookingDateTime != null && toBookingDateTime != ""
														&& fromBookingDateTime != null && fromBookingDateTime != "") {
													toDate = convertFormat.parse(toBookingDateTime);
													fromDate = convertFormat.parse(fromBookingDateTime);
												} else if (fromBookingDateTime != null && fromBookingDateTime != ""
														&& (toBookingDateTime == null || toBookingDateTime == "")) {
													toDate = listAR.get(i).getTransaction_to_date_time();
													fromDate = convertFormat.parse(fromBookingDateTime);
												} else {
													toDate = listAR.get(i).getTransaction_to_date_time();
													fromDate = listAR.get(i).getTransaction_from_date_time();
												}
												LOG.info(toDate + " - " + fromDate);
												list = transactionRepository.getTransactionByAccountIdToAndFromBooking(
														accounts.get(0).getAccount_id(), toDate, fromDate);
												if (!list.isEmpty()) {
													for (int k = 0; k < list.size(); k++) {
														LOG.info(String.valueOf(list.get(0).getAccount_id()));
														Transaction transaction = new Transaction();
														// count++;
														transaction.setAccountId(listAR.get(i).getAccount_id_ref());
														transaction.setAddressLine(list.get(k).getAddress_line());
														Amount amount = new Amount();
														amount.setAmount(defaultformat
																.format(list.get(k).getTransaction_amount()));
														amount.setCurrency(list.get(k).getCurrency_code());
														transaction.setAmount(amount);
														Balance balance = new Balance();
														Amount amount1 = new Amount();
														amount1.setAmount(
																defaultformat.format(list.get(k).getBalance()));
														amount1.setCurrency(list.get(k).getCurrency_code());
														balance.setAmount(amount1);
														balance.setCreditDebitIndicator(
																list.get(k).getCredit_debit_indicator());
														balance.setType(list.get(k).getBalance_type());
														transaction.setBalance(balance);
														BankTransactionCode bankTransactionCode = new BankTransactionCode();
														bankTransactionCode.setCode(list.get(k).getTransaction_name());
														bankTransactionCode
																.setSubCode(list.get(k).getBank_transaction_subcode());
														transaction.setBankTransactionCode(bankTransactionCode);
														transaction.setBookingDateTime(
																inFormat.format(list.get(k).getBooking_date_time()));
														transaction.setCreditDebitIndicator(
																list.get(k).getCredit_debit_indicator());
														MerchantDetails merchantDetails = new MerchantDetails();
														merchantDetails.setMerchantCategoryCode(
																list.get(k).getMerchant_category_code());
														merchantDetails.setMerchantName(list.get(k).getMerchant_name());
														transaction.setMerchantDetails(merchantDetails);
														ProprietaryBankTransactionCode proprietaryBankTransactionCode = new ProprietaryBankTransactionCode();
														proprietaryBankTransactionCode.setCode(
																list.get(k).getProprietary_bank_transaction_code());
														proprietaryBankTransactionCode
																.setIssuer(list.get(k).getIssuer_bank());
														transaction.setProprietaryBankTransactionCode(
																proprietaryBankTransactionCode);
														transaction.setStatus(list.get(k).getStatus());
														transaction.setTransactionId(
																String.valueOf(list.get(k).getTransaction_id()));
														transaction.setTransactionInformation(list.get(k).getComment());
														transaction.setTransactionReference(
																list.get(k).getPayment_ref_id());
														transaction.setValueDateTime(
																inFormat.format(list.get(k).getValue_date_time()));
														listTrxn.add(transaction);
													}
												}
											} else {
												response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
												accountMain.setStatus("Authorized Period for the Account Expired");
												LOG.info("Authorized Period for the Account Expired");
											}
										}
									}
								} else {
									execute = false;
								}
							}
						} else {
							response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
							accountMain.setStatus("Date got expired");
							LOG.info("Date got expired");
						}
					} else {
						response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						accountMain.setStatus("Permission are not available");
						LOG.info("Permission are not available");
					}
					data.setTransaction(listTrxn);
					if (listTrxn.size() >= 1) {
						accountMain.setStatus(null);
					}
					Links links = new Links();
					links.setSelf("/transactions/");
					if (listAR.size() > 100) {
						links.setFirst("/transactions/");
						links.setLast("/transactions?pg=" + count);
						links.setNext("/transactions?pg=" + (Math.ceil(page.getEndPage() / 100 + 0.4) + 1));
					}
					if (Math.ceil(page.getEndPage() / 100) >= 2) {
						links.setPrev("/transactions?pg=" + (Math.ceil(page.getEndPage() / 100 + 0.4) - 1));
					}
					accountMain.setLinks(links);

					Meta meta = new Meta();
					meta.setTotalPages(count);
					meta.setFirstAvailableDateTime(listAR.size() <= 0 ? ""
							: inFormat.format(listAR.get(0).getMaker_date()).replace("Z", "-00:00"));
					meta.setLastAvailableDateTime(inFormat.format(new Date()).replace("Z", "-00:00"));
					accountMain.setMeta(meta);
					accountMain.setData(data);
				} else {
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					accountMain.setStatus("Requested Account Request ID is not available");
					LOG.info("Requested Account Request ID is not available");
				}
				responseResult = gson.toJson(accountMain);
			}

		} catch (Exception e) {
			LOG.info(aisService.getStackTrace(e));
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			accountMain.setStatus(e.getCause() + "  -  " + aisService.getStackTrace(e));
		}
		response.addHeader("x-jws-signature", request.getHeader("x-jws-signature"));
		response.addHeader("x-fapi-interaction-id", request.getHeader("x-fapi-interaction-id"));
		return responseResult;
	}

	@RequestMapping(value = "/accounts/{AccountId}/transactions", method = RequestMethod.GET)
	public String getTransactionByAccountID(@PathVariable(value = "AccountId") String AccountId,
			@RequestParam(value = "pg", required = false) Integer pageNo, HttpServletResponse response,
			@RequestParam(value = "toBookingDateTime", required = false) String toBookingDateTime,
			@RequestParam(value = "fromBookingDateTime", required = false) String fromBookingDateTime,
			HttpServletRequest request) throws Exception {
		List<org.vp.pis.model.Transaction> list = new ArrayList<org.vp.pis.model.Transaction>();
		List<AccountRequest> listAR = new ArrayList<AccountRequest>();
		int count = 0;
		ArrayList<Transaction> listTrxn = new ArrayList<Transaction>();
		AccountMain accountMain = new AccountMain();
		boolean execute = false;
		String responseResult = "";
		Page page = new Page();
		// String toBookingDateTime = request.getHeader("toBookingDateTime");
		LOG.info(toBookingDateTime);
		// String fromBookingDateTime =
		// request.getHeader("fromBookingDateTime");
		LOG.info(fromBookingDateTime);
		SimpleDateFormat convertFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

		try {
			responseResult = aisService.getRequestHeaderValidation(request, response);
			if (response.getStatus() != HttpServletResponse.SC_BAD_REQUEST
					&& response.getStatus() != HttpServletResponse.SC_NOT_ACCEPTABLE) {
				listAR = accountsRequestRepository.getDetailsByAccountId(AccountId);
				if (!listAR.isEmpty()) {
					count = (int) (Math.ceil(listAR.size() / 100 + 0.4));
					LOG.info("count" + count);
					Data data = new Data();
					if ((listAR.get(0).getRead_transactions_basic().equalsIgnoreCase("Yes")
							|| listAR.get(0).getRead_transactions_credits().equalsIgnoreCase("Yes")
							|| listAR.get(0).getRead_transactions_debits().equalsIgnoreCase("Yes")
							|| listAR.get(0).getRead_transactions_detail().equalsIgnoreCase("Yes"))) {

						Date date1 = new Date();
						Date date2 = listAR.get(0).getExpiration_Date_Time();
						if (date1.compareTo(date2) < 0) {

							for (int i = 0; i < listAR.size(); i++) {
								execute = aisService.checkAccountIsValid(listAR.get(i).getScheme_name(),
										listAR.get(i).getAccount_identification());
								if (execute && "Authorised".equalsIgnoreCase(listAR.get(i).getStatus())) {
									execute = true;
									if (execute) {
										List<org.vp.pis.model.Account> accounts = accountRepository
												.getDetailsByAccountIdentification(listAR.get(i).getScheme_name(),
														listAR.get(i).getAccount_identification());
										if (!accounts.isEmpty()) {
											Date toDate = new Date();
											Date fromDate = new Date();
											boolean excuteTransaction = true;
											if (fromBookingDateTime != null && toBookingDateTime != null) {
												if ((convertFormat.parse(fromBookingDateTime)
														.compareTo(listAR.get(i).getTransaction_from_date_time()) > 0)
														&& (convertFormat.parse(fromBookingDateTime).compareTo(
																listAR.get(i).getTransaction_to_date_time()) < 0)
														&& (convertFormat.parse(toBookingDateTime).compareTo(
																listAR.get(i).getTransaction_from_date_time()) > 0)
														&& (convertFormat.parse(toBookingDateTime).compareTo(
																listAR.get(i).getTransaction_to_date_time()) < 0)
														&& (convertFormat.parse(toBookingDateTime).compareTo(
																convertFormat.parse(fromBookingDateTime)) > 0)) {
													excuteTransaction = true;
												}else{
													excuteTransaction = false;
												}
											}
											if (excuteTransaction) {
												if (toBookingDateTime != null && toBookingDateTime != ""
														&& (fromBookingDateTime == null || fromBookingDateTime == "")) {
													toDate = convertFormat.parse(toBookingDateTime);
													fromDate = listAR.get(i).getTransaction_from_date_time();
												} else if (toBookingDateTime != null && toBookingDateTime != ""
														&& fromBookingDateTime != null && fromBookingDateTime != "") {
													toDate = convertFormat.parse(toBookingDateTime);
													fromDate = convertFormat.parse(fromBookingDateTime);
												} else if (fromBookingDateTime != null && fromBookingDateTime != ""
														&& (toBookingDateTime == null || toBookingDateTime == "")) {
													toDate = listAR.get(i).getTransaction_to_date_time();
													fromDate = convertFormat.parse(fromBookingDateTime);
												} else {
													toDate = listAR.get(i).getTransaction_to_date_time();
													fromDate = listAR.get(i).getTransaction_from_date_time();
												}
												LOG.info(toDate + " - " + fromDate);
												list = transactionRepository.getTransactionByAccountIdToAndFromBooking(
														accounts.get(0).getAccount_id(), toDate, fromDate);
												if (!list.isEmpty()) {
													if (pageNo != null) {
														if (pageNo > list.size()) {
															page = aisService.pagingWithQueryParam(pageNo, list.size());
														} else {
															page.setInitialpage(0);
															page.setEndPage(list.size());
														}
													} else {
														page.setInitialpage(0);
														page.setEndPage(list.size());
													}
													for (int k = page.getInitialpage(); k < page.getEndPage(); k++) {
														LOG.info(String.valueOf(list.get(0).getAccount_id()));
														Transaction transaction = new Transaction();
														transaction.setAccountId(listAR.get(i).getAccount_id_ref());
														transaction.setAddressLine(list.get(k).getAddress_line());
														Amount amount = new Amount();
														amount.setAmount(defaultformat
																.format(list.get(k).getTransaction_amount()));
														amount.setCurrency(list.get(k).getCurrency_code());
														transaction.setAmount(amount);
														Balance balance = new Balance();
														Amount amount1 = new Amount();
														amount1.setAmount(
																defaultformat.format(list.get(k).getBalance()));
														amount1.setCurrency(list.get(k).getCurrency_code());
														balance.setAmount(amount1);
														balance.setCreditDebitIndicator(
																list.get(k).getCredit_debit_indicator());
														balance.setType(list.get(k).getBalance_type());
														transaction.setBalance(balance);
														BankTransactionCode bankTransactionCode = new BankTransactionCode();
														bankTransactionCode.setCode(list.get(k).getTransaction_name());
														bankTransactionCode
																.setSubCode(list.get(k).getBank_transaction_subcode());
														transaction.setBankTransactionCode(bankTransactionCode);
														transaction.setBookingDateTime(
																inFormat.format(list.get(k).getBooking_date_time()));
														transaction.setCreditDebitIndicator(
																list.get(k).getCredit_debit_indicator());
														MerchantDetails merchantDetails = new MerchantDetails();
														merchantDetails.setMerchantCategoryCode(
																list.get(k).getMerchant_category_code());
														merchantDetails.setMerchantName(list.get(k).getMerchant_name());
														transaction.setMerchantDetails(merchantDetails);
														ProprietaryBankTransactionCode proprietaryBankTransactionCode = new ProprietaryBankTransactionCode();
														proprietaryBankTransactionCode.setCode(
																list.get(k).getProprietary_bank_transaction_code());
														proprietaryBankTransactionCode
																.setIssuer(list.get(k).getIssuer_bank());
														transaction.setProprietaryBankTransactionCode(
																proprietaryBankTransactionCode);
														transaction.setStatus(list.get(k).getStatus());
														transaction.setTransactionId(
																String.valueOf(list.get(k).getTransaction_id()));
														transaction.setTransactionInformation(list.get(k).getComment());
														transaction.setTransactionReference(
																list.get(k).getPayment_ref_id());
														transaction.setValueDateTime(
																inFormat.format(list.get(k).getValue_date_time()));
														listTrxn.add(transaction);
													}
												}
											} else {
												response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
												accountMain.setStatus("Authorized Period for the Account Expired");
												LOG.info("Authorized Period for the Account Expired");
											}
										}
									}
								} else {
									execute = false;
								}
							}
						} else {
							response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
							accountMain.setStatus("Date got expired");
							LOG.info("Date got expired");
						}
					} else {
						response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						accountMain.setStatus("Permission are not available");
						LOG.info("Permission are not available");
					}
					data.setTransaction(listTrxn);
					if (listTrxn.size() >= 1) {
						accountMain.setStatus(null);
					}
					Links links = new Links();
					links.setSelf("/accounts/" + AccountId + "/transactions");
					if (listAR.size() > 100) {
						links.setFirst("/accounts/" + AccountId + "/transactions");
						links.setLast("/accounts/" + AccountId + "/transactions?pg=" + count);
						links.setNext("/accounts/" + AccountId + "/transactions?pg="
								+ (Math.ceil(page.getEndPage() / 100 + 0.4) + 1));
					}
					if (Math.ceil(page.getEndPage() / 100) >= 2) {
						links.setPrev("/accounts/" + AccountId + "/transactions?pg="
								+ (Math.ceil(page.getEndPage() / 100 + 0.4) - 1));
					}
					accountMain.setLinks(links);

					Meta meta = new Meta();
					meta.setTotalPages(count);
					meta.setFirstAvailableDateTime(listAR.size() <= 0 ? ""
							: inFormat.format(listAR.get(0).getMaker_date()).replace("Z", "-00:00"));
					meta.setLastAvailableDateTime(inFormat.format(new Date()).replace("Z", "-00:00"));
					accountMain.setMeta(meta);
					accountMain.setData(data);
				} else {
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					accountMain.setStatus("Requested Account ID is not available");
					LOG.info("Requested Account ID is not available");
				}
				responseResult = gson.toJson(accountMain);
			}
		} catch (Exception e) {
			LOG.info(aisService.getStackTrace(e));
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			accountMain.setStatus("Exception " + e.getCause() + "  -  " + aisService.getStackTrace(e));
		}
		response.addHeader("x-jws-signature", request.getHeader("x-jws-signature"));
		response.addHeader("x-fapi-interaction-id", request.getHeader("x-fapi-interaction-id"));

		return responseResult;
	}
}
