package com.transaction.management.service.impl;

import com.transaction.management.constants.Constants;
import com.transaction.management.entity.AccountUserAuth;
import com.transaction.management.entity.Permission;
import com.transaction.management.entity.Transactions;
import com.transaction.management.enums.Permissions;
import com.transaction.management.kafka.producer.NotificationServiceProducer;
import com.transaction.management.repository.AccountsRepository;
import com.transaction.management.repository.TransactionRepository;
import com.transaction.management.request.TransactionRequest;
import com.transaction.management.entity.Accounts;
import com.transaction.management.response.FraudResponse;
import com.transaction.management.response.NotificationResponse;
import com.transaction.management.service.TransactionService;
import com.transaction.management.validations.TransactionRequestValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.UUID;


import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TransactionServiceImpl implements TransactionService {

    @Autowired
    private AccountsRepository accountsRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private TransactionRequestValidator transactionRequestValidator;

    @Autowired
    private NotificationServiceProducer notificationServiceProducer;

    @Autowired
    private TransactionRepository transactionRepository;

    @Value("${user.account.url}")
    private String userAccountUrl;

    @Value("${kafka.transaction.topic}")
    private String topic;

//    @Override
//    public void processTransaction(Long userId, TransactionRequest transactionRequest) throws Exception {
//        // Make REST call to fetch user details to User Service
//        AccountUserAuth accountUserAuth = getAccountByUserId(userId);
//
//        if (!accountUserAuth.getAccountEnabled()) {
//            throw new Exception("User Account is Blocked, no Transaction Possible!!");
//        }
//
//        Permissions type = Permissions.valueOf(transactionRequest.getTransactionType());
//        transactionRequestValidator.validateTransactionRequest(transactionRequest);
//
//        Set<String> allowedPermissionNames = accountUserAuth.getUser()
//                .getPermissions()
//                .stream()
//                .map(Permission::getName)
//                .collect(Collectors.toSet());
//
//        if (allowedPermissionNames.contains(transactionRequest.getTransactionType())) {
//            switch (type) {
//                case DEPOSIT:
//                    Optional<Accounts> accountsDepositOptional = accountsRepository.findByAccountNumber(transactionRequest.getAccNoFrom());
//                    if(transactionRequest.getAccNoTo() != null){
//                        throw new Exception("No receiver account possible in DEPOSIT!");
//                    }
//                    FraudResponse fraudResponse = makeRestCallToFraud(transactionRequest);
//
//                    if (Objects.equals(fraudResponse.getStatus(), Constants.VALID)
//                            || Objects.equals(fraudResponse.getStatus(), Constants.ALERT) ) {
//
//                        notificationServiceProducer.send(topic, String.valueOf(userId), getNotificationResponse(userId, Constants.SUCCESSFUL));
//                    } else {
//                        notificationServiceProducer.send(topic, String.valueOf(userId), getNotificationResponse(userId, Constants.FAILED));
//                        throw new Exception("Transaction FAILED!!!");
//                    }
//                    if (accountsDepositOptional.isPresent()) {
//                        Accounts accounts = accountsDepositOptional.get();
//                        accounts.setBalance(accounts.getBalance() + transactionRequest.getAmount());
//                        accountsRepository.save(accounts);
//                    }
//                    else{
//                        throw new Exception("Account does not exist");
//                    }
//
//                    saveTransaction(userId, transactionRequest);
//                    break;
//
//                case TRANSFER:
//                    Optional<Accounts> senderOptional = accountsRepository.findByAccountNumber(transactionRequest.getAccNoTo());
//                    Optional<Accounts> receiverOptional = accountsRepository.findByAccountNumber(transactionRequest.getAccNoFrom());
//                    if (senderOptional.isPresent() && receiverOptional.isPresent()) {
//                        Accounts senderAccount = senderOptional.get();
//                        Accounts receiverAccount = receiverOptional.get();
//                        FraudResponse fraudResponse1 = makeRestCallToFraud(transactionRequest);
//                        if (Objects.equals(fraudResponse1.getStatus(), Constants.VALID)
//                                || Objects.equals(fraudResponse1.getStatus(), Constants.ALERT)  ) {
//                            notificationServiceProducer.send(topic, String.valueOf(userId), getNotificationResponse(userId, Constants.SUCCESSFUL));
//                        } else {
//                            notificationServiceProducer.send(topic, String.valueOf(userId), getNotificationResponse(userId, Constants.FAILED));
//                            throw new Exception("Transaction FAILED!!!");
//                        }
//                        if (checkBalance(senderAccount.getBalance(), transactionRequest.getAmount())) {
//                            senderAccount.setBalance(senderAccount.getBalance() - transactionRequest.getAmount());
//                            receiverAccount.setBalance(receiverAccount.getBalance() + transactionRequest.getAmount());
//                        } else {
//                            notificationServiceProducer.send(topic, String.valueOf(userId), getNotificationResponse(userId, Constants.FAILED));
//                            throw new Exception("Transfer not possible with this amount!!");
//                        }
//                        saveTransaction(userId, transactionRequest);
//                        notificationServiceProducer.send(topic, "key", getNotificationResponse(userId, Constants.SUCCESSFUL));
//                        accountsRepository.save(senderAccount);
//                        accountsRepository.save(receiverAccount);
//                    }
//
//                    else{
//                        throw new Exception("Account does not exist");
//                    }
//                    break;
//
//
//                case WITHDRAWAL:
//                    Optional<Accounts> accountsWithdrawalOptional = accountsRepository.findByAccountNumber(transactionRequest.getAccNoFrom());
//                    if(transactionRequest.getAccNoTo() != null){
//                        throw new Exception("No receiver account possible in WITHDRAWAL!");
//                    }
//                    if (accountsWithdrawalOptional.isPresent()) {
//                        Accounts accounts = accountsWithdrawalOptional.get();
//                        FraudResponse fraudResponse2 = makeRestCallToFraud(transactionRequest);
//
//                        if (Objects.equals(fraudResponse2.getStatus(), Constants.VALID)
//                                || Objects.equals(fraudResponse2.getStatus(), Constants.ALERT) ) {
//                            notificationServiceProducer.send(topic, String.valueOf(userId), getNotificationResponse(userId, Constants.SUCCESSFUL));
//                        } else {
//                            notificationServiceProducer.send(topic, String.valueOf(userId), getNotificationResponse(userId, Constants.FAILED));
//                            throw new Exception("Transaction FAILED!!!");
//                        }
//                        if (checkBalance(accounts.getBalance(), transactionRequest.getAmount())) {
//                            accounts.setBalance(accounts.getBalance() - transactionRequest.getAmount());
//                        } else {
//                            notificationServiceProducer.send(topic, "key", getNotificationResponse(userId, Constants.FAILED));
//                            throw new Exception("Withdrawal not possible with this amount!!");
//                        }
//                        notificationServiceProducer.send(topic, "key", getNotificationResponse(userId, Constants.SUCCESSFUL));
//                        saveTransaction(userId, transactionRequest);
//                        accountsRepository.save(accounts);
//                    }
//
//                    else{
//                        throw new Exception("Account does not exist");
//                    }
//                    break;
//
//                default:
//                    throw new Exception("No Other Permission possible!!");
//            }
//        }
//        else{
//            throw new Exception("Permission is not there for  requested transaction type!!");
//        }
//    }


    @Override
    public void processTransaction(Long userId, TransactionRequest transactionRequest) throws Exception {

        if (transactionRequest.getTimestamp() == null) {
            transactionRequest.setTimestamp(LocalDateTime.now());
        }

        // : Generate transactionId ONCE
        String transactionId = UUID.randomUUID().toString();
        transactionRequest.setTransactionId(transactionId);

        // Fetch user account
        AccountUserAuth accountUserAuth = getAccountByUserId(userId);

        if (accountUserAuth == null || !accountUserAuth.getAccountEnabled()) {
            throw new RuntimeException("User Account is Blocked or Not Found!!");
        }

        Permissions type = Permissions.valueOf(transactionRequest.getTransactionType());
        transactionRequestValidator.validateTransactionRequest(transactionRequest);

        Set<String> allowedPermissionNames = accountUserAuth.getUser()
                .getPermissions()
                .stream()
                .map(Permission::getName)
                .collect(Collectors.toSet());

        if (!allowedPermissionNames.contains(transactionRequest.getTransactionType())) {
            throw new RuntimeException("Permission is not there for requested transaction type!!");
        }

        switch (type) {

            case DEPOSIT:

                Optional<Accounts> depositAccountOpt =
                        accountsRepository.findByAccountNumber(transactionRequest.getAccNoFrom());

                if (transactionRequest.getAccNoTo() != null) {
                    throw new RuntimeException("No receiver account possible in DEPOSIT!");
                }

                if (depositAccountOpt.isEmpty()) {
                    throw new RuntimeException("Account does not exist");
                }

                FraudResponse depositFraud = makeRestCallToFraud(transactionRequest);

                if (depositFraud == null ||
                        (!Constants.VALID.equals(depositFraud.getStatus())
                                && !Constants.ALERT.equals(depositFraud.getStatus()))) {

                    saveTransaction(userId, transactionRequest, Constants.FAILED);

                    notificationServiceProducer.send(topic, String.valueOf(userId),
                            getNotificationResponse(userId, Constants.FAILED));

                    throw new RuntimeException("Transaction FAILED!!!");
                }

                Accounts depositAccount = depositAccountOpt.get();
                depositAccount.setBalance(depositAccount.getBalance() + transactionRequest.getAmount());
                accountsRepository.save(depositAccount);

                saveTransaction(userId, transactionRequest, Constants.SUCCESSFUL);

                notificationServiceProducer.send(topic, String.valueOf(userId),
                        getNotificationResponse(userId, Constants.SUCCESSFUL));

                break;


            case TRANSFER:

                Optional<Accounts> senderOpt =
                        accountsRepository.findByAccountNumber(transactionRequest.getAccNoFrom());

                Optional<Accounts> receiverOpt =
                        accountsRepository.findByAccountNumber(transactionRequest.getAccNoTo());

                if (senderOpt.isEmpty() || receiverOpt.isEmpty()) {
                    throw new RuntimeException("Account does not exist");
                }

                Accounts sender = senderOpt.get();
                Accounts receiver = receiverOpt.get();

                FraudResponse transferFraud = makeRestCallToFraud(transactionRequest);

                if (transferFraud == null ||
                        (!Constants.VALID.equals(transferFraud.getStatus())
                                && !Constants.ALERT.equals(transferFraud.getStatus()))) {

                    saveTransaction(userId, transactionRequest, Constants.FAILED);

                    notificationServiceProducer.send(topic, String.valueOf(userId),
                            getNotificationResponse(userId, Constants.FAILED));

                    throw new RuntimeException("Transaction FAILED!!!");
                }

                if (!checkBalance(sender.getBalance(), transactionRequest.getAmount())) {
                    saveTransaction(userId, transactionRequest, Constants.FAILED);
                    throw new RuntimeException("Transfer not possible with this amount!!");
                }

                sender.setBalance(sender.getBalance() - transactionRequest.getAmount());
                receiver.setBalance(receiver.getBalance() + transactionRequest.getAmount());

                accountsRepository.save(sender);
                accountsRepository.save(receiver);

                saveTransaction(userId, transactionRequest, Constants.SUCCESSFUL);

                notificationServiceProducer.send(topic, String.valueOf(userId),
                        getNotificationResponse(userId, Constants.SUCCESSFUL));

                break;


            case WITHDRAWAL:

                Optional<Accounts> withdrawalOpt =
                        accountsRepository.findByAccountNumber(transactionRequest.getAccNoFrom());

                if (transactionRequest.getAccNoTo() != null) {
                    throw new RuntimeException("No receiver account possible in WITHDRAWAL!");
                }

                if (withdrawalOpt.isEmpty()) {
                    throw new RuntimeException("Account does not exist");
                }

                Accounts withdrawalAccount = withdrawalOpt.get();

                FraudResponse withdrawalFraud = makeRestCallToFraud(transactionRequest);

                if (withdrawalFraud == null ||
                        (!Constants.VALID.equals(withdrawalFraud.getStatus())
                                && !Constants.ALERT.equals(withdrawalFraud.getStatus()))) {

                    saveTransaction(userId, transactionRequest, Constants.FAILED);

                    notificationServiceProducer.send(topic, String.valueOf(userId),
                            getNotificationResponse(userId, Constants.FAILED));

                    throw new RuntimeException("Transaction FAILED!!!");
                }

                if (!checkBalance(withdrawalAccount.getBalance(), transactionRequest.getAmount())) {
                    saveTransaction(userId, transactionRequest, Constants.FAILED);
                    throw new RuntimeException("Withdrawal not possible with this amount!!");
                }

                withdrawalAccount.setBalance(withdrawalAccount.getBalance() - transactionRequest.getAmount());
                accountsRepository.save(withdrawalAccount);

                saveTransaction(userId, transactionRequest, Constants.SUCCESSFUL);

                notificationServiceProducer.send(topic, String.valueOf(userId),
                        getNotificationResponse(userId, Constants.SUCCESSFUL));

                break;

            default:
                throw new RuntimeException("No Other Permission possible!!");
        }
    }
    private AccountUserAuth getAccountByUserId(Long userId) {
        String url = userAccountUrl + userId;
        try {
            ResponseEntity<AccountUserAuth> response = restTemplate.getForEntity(url, AccountUserAuth.class);
            return response.getBody();
        }
        catch(Exception e){
            System.out.println("ERROR while calling user-auth service!"+e);
            return null;
        }
    }

    private boolean checkBalance(Double accountBalance, Double transactionBalance) {
        return accountBalance > transactionBalance;
    }

//    private void saveTransaction(Long userId, TransactionRequest transactionRequest) {
//        Transactions transactions = new Transactions();
//        transactions.setType(transactionRequest.getTransactionType());
//        transactions.setStatus(Constants.SUCCESSFUL);
//        transactions.setTimestamp(LocalDateTime.now());
//        transactions.setAccNoFrom(transactionRequest.getAccNoFrom());
//        transactions.setAccNoTo(transactionRequest.getAccNoTo());
//        transactions.setUserId(userId);
//        transactions.setId(transactionRequest.getTransactionId());
//        transactionRepository.save(transactions);
//    }
private void saveTransaction(Long userId, TransactionRequest transactionRequest, String status) {
    Transactions transactions = new Transactions();
    transactions.setType(transactionRequest.getTransactionType());
    transactions.setStatus(status);
    transactions.setTimestamp(LocalDateTime.now());
    transactions.setAccNoFrom(transactionRequest.getAccNoFrom());
    transactions.setAccNoTo(transactionRequest.getAccNoTo());
    transactions.setAmount(transactionRequest.getAmount());
    transactions.setCurrency(transactionRequest.getCurrency());
    transactions.setUserId(userId);
    transactions.setId(transactionRequest.getTransactionId());
    transactionRepository.save(transactions);
}

    private NotificationResponse getNotificationResponse(Long userId, String status) {
        NotificationResponse notificationResponse = new NotificationResponse();
        notificationResponse.setUser_id(userId);
        notificationResponse.setStatus(status);
        return notificationResponse;
    }

    private FraudResponse makeRestCallToFraud(TransactionRequest transactionRequest) {
        if (transactionRequest.getTransactionId() == null || transactionRequest.getTransactionId().isBlank()) {
            transactionRequest.setTransactionId(UUID.randomUUID().toString());
        }
        String fraudApiUrl = "http://localhost:8082/api/fraud/check";

        try {
            ResponseEntity<FraudResponse> fraudResponse = restTemplate.postForEntity(
                    fraudApiUrl,
                    transactionRequest,
                    FraudResponse.class
            );
            return fraudResponse.getBody();
        }
        catch(Exception e){
            System.out.println("ERROR while calling fraud management service!"+e);
            return null;
        }
    }
}