package com.wipro.fraud.aiassistant.service.impl;


import com.fasterxml.jackson.databind.ObjectMapper;

import com.wipro.fraud.aiassistant.repository.ConversationMemoryRepository;
import com.wipro.fraud.aiassistant.security.PromptSecurityFilter;
import org.springframework.jdbc.core.JdbcTemplate;

import com.wipro.fraud.aiassistant.client.BedrockKnowledgeBaseClient;
import com.wipro.fraud.aiassistant.client.OllamaClient;
import com.wipro.fraud.aiassistant.client.UserClient;
import com.wipro.fraud.aiassistant.dto.AssistantRequest;
import com.wipro.fraud.aiassistant.dto.AssistantResponse;
import com.wipro.fraud.aiassistant.entity.ConversationMemory;
import com.wipro.fraud.aiassistant.security.TableAccessManager;
import com.wipro.fraud.aiassistant.service.ConversationMemoryService;
import com.wipro.fraud.aiassistant.service.FraudAssistantService;
import com.wipro.fraud.aiassistant.util.PromptTemplateBuilder;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FraudAssistantServiceImpl implements FraudAssistantService {

    private static final String INTENT_DB = "DB";
    private static final String INTENT_KB = "KB";
    private static final String INTENT_HYBRID = "HYBRID";

    private final BedrockKnowledgeBaseClient bedrockClient;
    private final ConversationMemoryService memoryService;
    private final OllamaClient ollamaClient;
    private final DatabaseQueryService databaseQueryService;

    @Autowired
    private TableAccessManager tableAccessManager;

    @Autowired
    private PromptSecurityFilter securityFilter;

    @Autowired
    private UserClient userClient;

    @Autowired
    private JdbcTemplate jdbcTemplate;


    @Autowired
    private ConversationMemoryRepository memoryRepository;

    private void saveConversation(String sessionId,
                                  String userId,
                                  String message,
                                  String role) {

        try {

            ConversationMemory cm = new ConversationMemory();
            cm.setSessionId(sessionId);
            cm.setUserId(userId);
            cm.setMessage(message);
            cm.setRole(role);

            memoryRepository.save(cm);

            System.out.println("✅ Conversation saved: " + role);

        } catch (Exception e) {

            System.out.println("❌ MEMORY SAVE FAILED: " + e.getMessage());
        }
    }





    @Override
    public AssistantResponse processQuery(AssistantRequest request) {

        try {


            String sessionId = request.getSessionId();
            String question = request.getQuestion();
            String userId = request.getAnalystId();
            saveConversation(sessionId, userId, question, "USER");

            String normalizedQ = normalize(question);

            String intent = detectIntent(normalizedQ);
            System.out.println("Detected Intent: " + intent);

            // ===============================
            // ROLE-BASED ACCESS CHECK
            // ===============================
            Set<String> roles = userClient.getUserRoles(userId);

            if (roles == null || roles.isEmpty()) {
                return buildResponse("ERROR", "Authorization", null,
                        "❌ Unable to determine user roles",
                        "Authorization");
            }

            var securityResult = securityFilter.checkInput(question);

            if (securityResult.getLevel() == PromptSecurityFilter.SecurityLevel.BLOCK
                    && !intent.equals("GENERAL")) {

                return buildResponse(
                        "ERROR",
                        "Security",
                        null,
                        securityResult.getMessage(),
                        "Security Filter"
                );
            }

            String table = tableAccessManager.getTableFromIntent(intent);

            if (table.equals("unknown")) {
                table = tableAccessManager.detectTableFromQuery(normalizedQ);
            }

            if (!tableAccessManager.hasAccess(roles, table)) {
                return buildResponse("ERROR", "Authorization", null,
                        "❌ Access Denied: You do not have permission to access " + table,
                        "Authorization");
            }

            if (intent.equals("GENERAL") && !isValidQuestion(question)) {
                return buildResponse("ERROR", "Validation", null,
                        "Please ask a fraud-related question.",
                        "Validation");
            }

            List<ConversationMemory> history =
                    memoryService.getConversation(sessionId, userId);


            if (!intent.equals("GENERAL")
                    && !Set.of("USERS","FRAUD_INSIGHTS","FRAUD_PATTERNS").contains(intent)
                    && !isDbQuery(normalizedQ)) {
                return buildResponse("ERROR", "System", null,
                        "No relevant information found.",
                        "System");
            }

            // ===============================
            // HANDLE INTENTS
            // ===============================
            switch (intent) {

                case "NO_OP" -> {
                    return buildResponse("INFO", "System", null,
                            "👍 Let me know if you need anything else.",
                            "System");
                }

                case "FRAUD_CHECK" -> {
                    return handleFraudCheck(sessionId, userId, question);
                }

                case "ACCOUNTS" -> {

                    List<Map<String, Object>> data = getAllAccounts();

                    if (data == null || data.isEmpty()) {
                        return buildResponse(
                                "ERROR",
                                "Accounts",
                                null,
                                "No accounts found.",
                                "PostgreSQL Database"
                        );
                    }

                    return buildResponse(
                            "ACCOUNTS",
                            "🏦 Accounts",
                            data,
                            null,
                            "PostgreSQL Database"
                    );
                }

                case "HYBRID_FRAUD" -> {
                    return handleFraudExplanation(sessionId, userId, question);
                }


                case "ACCOUNT_LIMIT" -> {

                    String accNo = extractAccountNumber(normalizedQ);

                    if (accNo == null) {
                        return buildResponse("ERROR", "Validation", null,
                                "Please provide a valid account number.",
                                "Validation");
                    }

                    List<Map<String, Object>> data = getAccountLimit(accNo);

                    if (data == null || data.isEmpty()) {
                        return buildResponse("ERROR", "Account Limit", null,
                                "No account limit found.",
                                "PostgreSQL Database");
                    }

                    return buildResponse(
                            "ACCOUNT_LIMIT",
                            "Account Limits",
                            data,
                            null,
                            "PostgreSQL Database"
                    );
                }

                case "FRAUD_INSIGHTS" -> {

                    List<Map<String, Object>> data = getFraudInsights();

                    if (data == null || data.isEmpty()) {
                        return buildResponse(
                                "ERROR",
                                "Fraud Insights",
                                null,
                                "No fraud insights found.",
                                "PostgreSQL Database"
                        );
                    }

                    return buildResponse(
                            "FRAUD_INSIGHTS",   // ✅ VERY IMPORTANT
                            "📊 Fraud Insights",
                            data,               // ✅ MUST NOT BE NULL
                            null,
                            "PostgreSQL Database"
                    );

                }

                case "FRAUD_PATTERNS" -> {

                    List<Map<String, Object>> data = getTopFraudReasons();

                    if (data == null || data.isEmpty()) {
                        return buildResponse(
                                "ERROR",
                                "Fraud Patterns",
                                null,
                                "No fraud patterns found.",
                                "PostgreSQL Database"
                        );
                    }

                    return buildResponse(
                            "FRAUD_PATTERNS",   // ✅ CRITICAL
                            "Fraud Patterns",
                            data,               // ✅ structured data
                            null,
                            "PostgreSQL Database"
                    );
                }

                case "FAILED_TRANSACTIONS" -> {

                    int limit = extractLimit(normalizedQ, request.isAgentMode());

                    List<Map<String, Object>> data =
                            getTransactionsByStatus("FAILED", limit);

                    if (data == null || data.isEmpty()) {
                        return buildResponse("ERROR", "Failed Transactions", null,
                                "No failed transactions found.",
                                "PostgreSQL Database");
                    }

                    data = sanitizeData(data);

                    List<Map<String, Object>> finalData =
                            data.stream().limit(limit).toList();


                    if (isAnalysisQuery(normalizedQ) && !isExplicitDbQuery(normalizedQ)) {

                        return buildResponse(
                                "LOADING",
                                "Analysis",
                                null,
                                "⏳ Analyzing... please wait",
                                "System"
                        );
                    }

                    // ✅ NORMAL TABLE RESPONSE
                    AssistantResponse response = buildResponse(
                            "FAILED_TRANSACTIONS",
                            "Failed Transactions",
                            finalData,
                            null,
                            "PostgreSQL Database"
                    );

                    saveConversation(sessionId, userId, finalData.toString(), "ASSISTANT");

                    return response;
                }

                case "SUCCESS_TRANSACTIONS" -> {

                    int limit = extractLimit(normalizedQ, request.isAgentMode());
                    List<Map<String, Object>> data = getTransactionsByStatus("SUCCESSFUL", limit);

                    if (data == null || data.isEmpty()) {
                        return buildResponse("ERROR", "Successful Transactions", null,
                                "No successful transactions found.",
                                "PostgreSQL Database");
                    }

                    return buildResponse("SUCCESS_TRANSACTIONS", "Successful Transactions",
                            data, null,
                            "PostgreSQL Database");
                }

                case "TRANSACTIONS" -> {

                    int limit = extractLimit(normalizedQ, request.isAgentMode());

                    boolean hasUserKeyword = normalizedQ.matches(".*\\buser\\b.*");
                    boolean hasLastKeyword = normalizedQ.contains("last") || normalizedQ.contains("recent");

                    List<Map<String, Object>> data;

                    // ===============================
                    // 🔥 RANGE QUERY FIX (ADDED)
                    // ===============================
                    if (normalizedQ.contains("between") && normalizedQ.matches(".*\\d+.*")) {

                        List<Long> numbers = extractAllNumbers(normalizedQ);

                        if (numbers.size() >= 2) {
                            long min = numbers.get(0);
                            long max = numbers.get(1);

                            data = databaseQueryService.executeQuery("""
                SELECT accnofrom, accnoto, amount, status,timestamp
                FROM transactions
                WHERE amount BETWEEN ? AND ?
                ORDER BY timestamp DESC
                LIMIT ?
            """, min, max, limit);

                        } else {
                            data = List.of();
                        }
                    }

                    // ===============================
                    // EXISTING LOGIC (UNCHANGED)
                    // ===============================
                    else if (hasLastKeyword && !hasUserKeyword) {
                        data = getRecentTransactions(limit);
                    }
                    else if (hasUserKeyword) {

                        Long userIdNumber = extractNumber(normalizedQ);

                        if (userIdNumber == null || userIdNumber <= 0 || userIdNumber > 1000) {
                            return buildResponse(
                                    "ERROR",
                                    "Validation",
                                    null,
                                    "Please provide a valid user ID.",
                                    "Validation"
                            );
                        }

                        data = getTransactions(userIdNumber, limit);
                    }
                    else {
                        data = getSmartFilteredTransactions(normalizedQ, request.isAgentMode());
                    }

                    // ===============================
                    // COMMON RESPONSE (UNCHANGED)
                    // ===============================
                    if (data == null || data.isEmpty()) {
                        return buildResponse(
                                "ERROR",
                                "Transactions",
                                null,
                                "No transactions found.",
                                "PostgreSQL Database"
                        );
                    }

                    data = sanitizeData(data);

                    List<Map<String, Object>> finalData =
                            data.stream().limit(limit).toList();

                    if (isAnalysisQuery(normalizedQ) && !isExplicitDbQuery(normalizedQ)) {

                        return buildResponse(
                                "LOADING",
                                "Analysis",
                                null,
                                "⏳ Analyzing... please wait",
                                "System"
                        );
                    }

                    AssistantResponse response = buildResponse(
                            "TRANSACTIONS",
                            "Transactions",
                            finalData,
                            null,
                            "PostgreSQL Database"
                    );

                    saveConversation(sessionId, userId, finalData.toString(), "ASSISTANT");

                    return response;
                }
                case "FRAUD_TRANSACTIONS" -> {

                    Long userIdNumber = extractNumber(normalizedQ);

                    List<Map<String, Object>> data =
                            getSmartFilteredTransactions(normalizedQ, request.isAgentMode());

                    if (userIdNumber != null && normalizedQ.contains("user")) {

                        data = data.stream()
                                .filter(row -> row.get("userid") != null &&
                                        row.get("userid").toString().equals(userIdNumber.toString()))
                                .toList();
                    }


                    if (data == null || data.isEmpty()) {
                        return buildResponse("ERROR", "Fraud Transactions", null,
                                "No fraud transactions found.",
                                "PostgreSQL Database");
                    }

                    data = sanitizeData(data);

                    List<Map<String, Object>> finalData =
                            data.stream().limit(5).toList();

                    String title = normalizedQ.contains("alert")
                            ? "⚠️ Alert Transactions"
                            : "🚨 Fraud Transactions";

                    if (isAnalysisQuery(normalizedQ) && !isExplicitDbQuery(normalizedQ)) {

                        return buildResponse(
                                "LOADING",
                                "Analysis",
                                null,
                                "⏳ Analyzing... please wait",
                                "System"
                        );
                    }
                    // ✅ NORMAL TABLE
                    AssistantResponse response = buildResponse(
                            "FRAUD_TRANSACTIONS",
                            title,
                            finalData,
                            null,
                            "PostgreSQL Database"
                    );

                    saveConversation(sessionId, userId, finalData.toString(), "ASSISTANT");

                    return response;
                }

                case "BLOCKED_ACCOUNT" -> {

                    String accNo = extractAccountNumber(normalizedQ);

                    List<Map<String, Object>> data =
                            (accNo != null)
                                    ? getBlockedAccount(accNo)
                                    : getAllBlockedAccounts();

                    if (data == null || data.isEmpty()) {
                        return buildResponse("ERROR", "Blocked Accounts", null,
                                "No blocked accounts found.",
                                "PostgreSQL Database");
                    }

                    return buildResponse("BLOCKED_ACCOUNT", "Blocked Accounts",
                            data, null,
                            "PostgreSQL Database");
                }

                case "ACCOUNT_BALANCE" -> {

                    Long userIdNumber = extractNumber(normalizedQ);
                    List<Map<String, Object>> data;

                    if (normalizedQ.contains("user") && userIdNumber != null) {
                        data = getBalanceByUserId(userIdNumber);
                    } else {
                        String accNo = extractAccountNumber(normalizedQ);

                        if (accNo == null) {
                            return buildResponse("ERROR", "Validation", null,
                                    "Please provide a valid account number.",
                                    "Validation");
                        }

                        data = getBalance(accNo);
                    }

                    if (data == null || data.isEmpty()) {
                        return buildResponse("ERROR", "Account Balance", null,
                                "No account data found.",
                                "PostgreSQL Database");
                    }

                    return buildResponse("ACCOUNT_BALANCE", "Account Balance",
                            data, null,
                            "PostgreSQL Database");
                }

                case "USERS" -> {

                    List<Map<String, Object>> data = getAllUsers();

                    return buildResponse("USERS", "Users",
                            data, null,
                            "PostgreSQL Database");
                }

                case "GENERAL" -> {

                    if (!isFraudDomainQuestion(normalizedQ)) {
                        return buildResponse("ERROR", "System", null,
                                "No relevant information found.",
                                "System");
                    }

                    String ragAnswer = callKnowledgeBase(question, normalizedQ, history, sessionId);

                    if (ragAnswer == null
                            || ragAnswer.trim().isEmpty()
                            || ragAnswer.toLowerCase().contains("no relevant")
                            || ragAnswer.toLowerCase().contains("not found")) {

                        return buildResponse("ERROR", "System", null,
                                "No relevant information found.",
                                "System");
                    }

                    AssistantResponse response = buildResponse(
                            "KB",
                            "Knowledge Base",
                            null,
                            ragAnswer,
                            "Fraud Policy Knowledge Base"
                    );

                    saveConversation(sessionId, userId, ragAnswer, "ASSISTANT");

                    return response;
                }
            }

            // ===============================
            // FINAL FALLBACK
            // ===============================
            return buildResponse("ERROR", "System", null,
                    "No relevant data found in database or knowledge base.",
                    "System");
        }
        catch (Exception e) {

            e.printStackTrace();

            return buildResponse(
                    "ERROR",
                    "System Error",
                    null,
                    "Something went wrong internally. Please try again.",
                    "System"
            );
        }
    }




    // ===============================
    //  NORMALIZATION
    // ===============================
    private String normalize(String question) {

        if (question == null) return "";

        String q = question.toLowerCase().trim();

        q = q.replace("txn", "transaction");
        q = q.replace("txns", "transactions");
        q = q.replace("frud", "fraud");
       // q = q.replace("acc", "account");
        q = q.replaceAll("\\bacc\\b", "account");
        q = q.replace("fraudulent", "fraud");
        q = q.replace("transaction logs", "transaction");
        q = q.replace("transactions logs", "transaction");
        q = q.replace("alerted", "alert");
        q = q.replace("alerts", "alert");
        q = q.replace("frauds", "fraud");
        q = q.replace("fraudulent", "fraud");

        return q;
    }

    // ===============================
    //  INTENT DETECTION
    // ===============================
    private String detectIntent(String q) {

        if (q == null || q.isBlank()) return "GENERAL";

        q = q.toLowerCase().trim();

        // ===============================
        // BASIC NO-OP
        // ===============================
        if (q.equals("ok") || q.equals("thanks") || q.equals("yes") || q.equals("no")) {
            return "NO_OP";
        }

        // ===============================
        // 🔥 KB OVERRIDE (HIGHEST PRIORITY)
        // ===============================
        if (q.contains("from policy")
                || q.contains("policy")
                || q.contains("knowledge")) {
            return "GENERAL";
        }

        // ===============================
//  KNOWLEDGE QUESTIONS (FORCE KB)
// ===============================
        if (q.startsWith("what is")
                || q.startsWith("explain")
                || q.contains("meaning")
                || q.contains("define")
                || q.contains("why is")
                || q.contains("what is meant")
        ) {
            return "GENERAL";
        }

        // ===============================
        // SMART FLAGS
        // ===============================
        boolean hasAccountNumber = q.matches(".*\\d{10,}.*");

        String[] dbActions = {"show", "list", "get", "fetch", "give", "display"};
        String[] fraudWords = {"fraud", "suspicious", "flagged", "risk"};
        String[] transactionWords = {"transaction", "transfer", "payment"};
        String[] blockedWords = {"blocked", "disabled", "blacklist"};
        String[] balanceWords = {"balance", "amount", "fund"};

        boolean hasDbAction = containsAny(q, dbActions);
        boolean hasFraud = containsAny(q, fraudWords);
        boolean hasTransaction = containsAny(q, transactionWords);
        boolean hasBlocked = containsAny(q, blockedWords);
        boolean hasBalance = containsAny(q, balanceWords);
        boolean hasAccount = q.contains("account");

        // ===============================
        // HYBRID (WHY + ACCOUNT)
        // ===============================
        if ((q.contains("why") || q.contains("reason"))
                && hasAccountNumber) {
            return "HYBRID_FRAUD";
        }





        // ===============================
        // FRAUD INSIGHTS / PATTERNS
        // ===============================
        if (q.contains("fraud insights") || q.contains("fraud summary")) {
            return "FRAUD_INSIGHTS";
        }

        // ===============================
// FRAUD PATTERNS (SMART DB TRIGGER)
// ===============================
        if ((q.contains("fraud pattern") || q.contains("fraud patterns"))
                && (q.contains("show") || q.contains("list") || q.contains("system") || q.contains("top"))) {
            return "FRAUD_PATTERNS";
        }

        // ===============================
// FRAUD PATTERN EXPLANATION (KB)
// ===============================
        if ((q.contains("fraud pattern") || q.contains("fraud patterns"))
                && (q.contains("recent") || q.contains("explain") || q.contains("types"))) {
            return "GENERAL";
        }

        // ===============================
// ACCOUNT LIMIT (CORRECT)
// ===============================
        if (q.contains("account limit") || q.contains("limit of account")) {
            return "ACCOUNT_LIMIT";
        }

        // ===============================
        // 🧠 ANALYSIS QUERIES (WHY / REASON)
        // ===============================
        if ((q.contains("why") || q.contains("reason") || q.contains("analysis"))
                && hasTransaction) {

            // 🔥 PRIORITY: ACCOUNT + WHY → HYBRID
            if (hasAccountNumber) {
                return "HYBRID_FRAUD";
            }

            if (q.contains("fail")) return "FAILED_TRANSACTIONS";
            if (q.contains("fraud") || q.contains("suspicious")) return "FRAUD_TRANSACTIONS";

            return "GENERAL";
        }

        // ===============================
        // 🧠 CONCEPTUAL QUESTIONS (KB)
        // ===============================
        if ((q.contains("what")
                || q.contains("difference")
                || q.contains("meaning")
                || q.contains("explain")
                || q.contains("define"))
                && !hasDbAction) {

            return "GENERAL";
        }

        // ===============================
        // FRAUD TRANSACTIONS (STRICT DB)
        // ===============================
        if ((q.contains("fraud") || q.contains("alert") || q.contains("suspicious"))
                && hasTransaction
                && hasDbAction) {

            return "FRAUD_TRANSACTIONS";
        }

        // ===============================
        // FAILED TRANSACTIONS
        // ===============================
        if ((q.contains("fail") || q.contains("failed") || q.contains("failing"))
                && hasTransaction) {
            return "FAILED_TRANSACTIONS";
        }

        // ===============================
        // SUCCESS TRANSACTIONS
        // ===============================
        if (q.contains("successful transaction") || q.contains("success transaction")) {
            return "SUCCESS_TRANSACTIONS";
        }



        // ===============================
        // FRAUD CHECK
        // ===============================
        if (hasAccountNumber && hasFraud) {
            return "FRAUD_CHECK";
        }

        // ===============================
        // BLOCKED ACCOUNT
        // ===============================
        if (hasBlocked) return "BLOCKED_ACCOUNT";

        // ===============================
        // BALANCE
        // ===============================
        if (q.contains("balance") || (hasBalance && hasAccount)) {
            return "ACCOUNT_BALANCE";
        }

        if (
                (q.contains("accounts") || q.contains("account"))
                        &&
                        (q.contains("present")
                                || q.contains("registered")
                                || q.contains("in system")
                                || q.contains("in bank")
                                || q.contains("all"))
        ) {
            return "ACCOUNTS";
        }

        // ===============================
        // USERS
        // ===============================
        if ((q.contains("user") || q.contains("users") || q.contains("registered"))
                && !q.contains("transaction") && !q.contains("balance")) {
            return "USERS";
        }

        if (q.contains("user") && q.contains("transaction")) {
            return "TRANSACTIONS";
        }


        // ===============================
        // TRANSACTIONS DEFAULT
        // ===============================
        if (hasTransaction) {
            return "TRANSACTIONS";
        }



        // ===============================
        // DEFAULT
        // ===============================
        return "GENERAL";
    }

    private List<Map<String, Object>> getRecentTransactions(int limit) {

        String sql = """
                                SELECT\s
                                    id,
                                    type,
                                    userid,
                                    accnofrom AS from_account,
                                    accnoto AS to_account,
                                    status,
                                    timestamp,
                                    COALESCE(amount, 0) AS amount
                                FROM transactions
                                ORDER BY timestamp DESC
                                LIMIT ?
                """;

        System.out.println("SQL: " + sql + " LIMIT: " + limit);

        return databaseQueryService.executeQuery(sql, limit);
    }



    // ===============================
    //  PARAM EXTRACTION
    // ===============================
    private String extractAccountNumber(String q) {

        if (q == null) return null;

        // detect 10–18 digit numbers (account range)
        Pattern pattern = Pattern.compile("\\b\\d{10,18}\\b");
        Matcher matcher = pattern.matcher(q);

        if (matcher.find()) {
            return matcher.group();
        }

        return null;
    }

    private Long extractNumber(String q) {

        if (q == null || q.isBlank()) return null;

        // ===============================
        // STEP 1: Extract "user <id>"
        // ===============================
        Pattern userPattern = Pattern.compile("user\\s*(\\d+)");
        Matcher userMatcher = userPattern.matcher(q);

        if (userMatcher.find()) {
            return Long.parseLong(userMatcher.group(1));
        }

        // ===============================
        // STEP 2: Extract standalone small numbers (avoid account numbers)
        // ===============================
        Pattern numberPattern = Pattern.compile("\\b\\d{1,6}\\b"); // only small numbers
        Matcher numberMatcher = numberPattern.matcher(q);

        if (numberMatcher.find()) {
            return Long.parseLong(numberMatcher.group());
        }

        return null;
    }
    // ===============================
    //  DB METHODS
    // ===============================
    private List<Map<String, Object>> getAllBlockedAccounts() {

        String sql = """
        SELECT account_number, reason, blocked_at
        FROM blocked_accounts
        ORDER BY blocked_at DESC
        """;

        System.out.println("SQL: " + sql);

        return databaseQueryService.executeQuery(sql);
    }

    private List<Map<String, Object>> getBlockedAccount(String accountNumber) {

        String sql = """
        SELECT account_number, reason, blocked_at
        FROM blocked_accounts
        WHERE account_number = ?
        """;

        System.out.println("SQL: " + sql + " PARAM: " + accountNumber);

        return databaseQueryService.executeQuery(sql, accountNumber);
    }

    private List<Map<String, Object>> getTransactions(Long userId, int limit) {

        String sql = """
                            SELECT\s
                                id,
                                type,
                                userid,
                                accnofrom AS from_account,
                                accnoto AS to_account,
                                status,
                                timestamp,
                                COALESCE(amount, 0) AS amount
                            FROM transactions
                            WHERE userid = ?
                            ORDER BY timestamp DESC
                            LIMIT ?
                """;

        System.out.println("SQL: " + sql + " PARAM userId: " + userId + " LIMIT: " + limit);

        return databaseQueryService.executeQuery(sql, userId, limit);
    }

    private List<Map<String, Object>> getBalance(String accountNumber) {

        String sql = """
        SELECT account_number, balance
        FROM account
        WHERE account_number = ?
        """;

        System.out.println("SQL: " + sql + " PARAM: " + accountNumber);

        return databaseQueryService.executeQuery(sql, accountNumber);
    }

    // ===============================
    //  FRAUD ANALYSIS
    // ===============================
    private AssistantResponse handleFraudCheck(String sessionId, String userId, String question) {

        // ===============================
        // 🔐 INPUT SECURITY
        // ===============================
        var inputCheck = securityFilter.checkInput(question);

        if (inputCheck.getLevel() == PromptSecurityFilter.SecurityLevel.BLOCK) {
            return buildResponse(
                    "ERROR",
                    "Security",
                    null,
                    inputCheck.getMessage(),
                    "Security Filter"
            );
        }

        String accNo = extractAccountNumber(normalize(question));

        if (accNo == null) {
            return buildResponse(
                    "ERROR",
                    "Validation",
                    null,
                    "Please provide a valid account number for fraud analysis.",
                    "Validation"
            );
        }

        // ===============================
        // STEP 1: GET FRAUD DATA
        // ===============================
        List<Map<String, Object>> fraudLogs = getFraudLogsByAccount(accNo);

        if (fraudLogs == null || fraudLogs.isEmpty()) {
            return buildResponse(
                    "FRAUD_ANALYSIS",
                    "Fraud Analysis",
                    null,
                    "No fraud detected for this account.",
                    "PostgreSQL Database"
            );
        }

        // ===============================
        // STEP 2: HISTORY
        // ===============================
        List<ConversationMemory> history =
                memoryService.getConversation(sessionId, userId);

        if (history != null && history.size() > 2) {
            history = history.subList(history.size() - 2, history.size());
        }

        // ===============================
        // STEP 3: PROMPT
        // ===============================
        AssistantRequest req = new AssistantRequest();
        req.setSessionId(sessionId);
        req.setQuestion("""
Analyze fraud for account: %s

Transaction Logs:
%s
""".formatted(accNo, fraudLogs.toString()));

        String prompt = PromptTemplateBuilder.buildFraudPrompt(req, history);

        // ===============================
        // STEP 4: LLM CALL
        // ===============================
//        String analysis = ollamaClient.generateResponse(prompt);

        String analysis;

        try {
            analysis = ollamaClient.generateResponse(prompt);
            if (analysis == null || analysis.isBlank()) {
                return buildResponse(
                        "FRAUD_TRANSACTIONS",
                        "Fraud Transactions",
                        fraudLogs,
                        "No AI insights available. Showing transaction data.",
                        "PostgreSQL Database"
                );
            }
        } catch (Exception e) {

            System.out.println("LLM ERROR: " + e.getMessage());

            // 🔥 FALLBACK TO DB DATA
            return buildResponse(
                    "FRAUD_TRANSACTIONS",
                    "Fraud Transactions",
                    fraudLogs,
                    "AI service unavailable. Showing transaction data instead.",
                    "PostgreSQL Database"
            );
        }

// ===============================
// 🔐 RESPONSE SECURITY (REFINED)
// ===============================
        var resCheck = securityFilter.checkResponse(analysis);

        if (resCheck.getLevel() == PromptSecurityFilter.SecurityLevel.BLOCK) {

            System.out.println("⚠️ Security BLOCK → sanitizing response");

            // ✅ Never fully block — trim instead
            if (analysis != null && analysis.length() > 400) {
                analysis = analysis.substring(0, 400);
            }

            // Optional soft note (not scary)
            analysis = analysis + "\n\n(Note: Some sensitive details were omitted.)";
        }

        else if (resCheck.getLevel() == PromptSecurityFilter.SecurityLevel.FALLBACK) {

            System.out.println("⚠️ Security FALLBACK → partial response");

            // ✅ Keep content but add light disclaimer
            analysis = "ℹ️ Partial insights:\n\n" + analysis;
        }
        // ===============================
        // FINAL RESPONSE
        // ===============================
        String finalAnswer = """
Fraud Analysis:
%s
""".formatted(analysis);

        return buildResponse(
                "FRAUD_ANALYSIS",
                "Fraud Analysis",
                fraudLogs,
                finalAnswer,
                "Hybrid"
        );
    }


    private AssistantResponse buildResponse(
            String type,
            String title,
            Object data,
            String answer,
            String source) {

        try {

            return AssistantResponse.builder()
                    .type(type)
                    .title(title)
                    .data(data != null ? data : List.of())
                    .answer(answer != null ? answer : "")
                    .source(source)
                    .suggestions(getSuggestions(type))
                    .build();

        } catch (Exception e) {

            System.out.println("❌ RESPONSE BUILD FAILED: " + e.getMessage());

            return AssistantResponse.builder()
                    .type("ERROR")
                    .title("System Error")
                    .data(null)
                    .answer("Response generation failed")
                    .source("System")
                    .build();
        }
    }

    private List<String> getSuggestions(String type) {

        if (type == null) {
            return List.of(
                    "Show fraud transactions",
                    "View fraud insights",
                    "Check blocked accounts"
            );
        }

        return switch (type) {

            // 🚨 FRAUD ANALYSIS
            case "FRAUD_ANALYSIS" -> List.of(
                    "Why is this fraud?",
                    "Show transactions for this account",
                    "Check blocked accounts",
                    "View fraud insights"
            );

            // 💳 TRANSACTIONS
            case "TRANSACTIONS" -> List.of(
                    "Show failed transactions",
                    "Show successful transactions",
                    "Check fraud for this account",
                    "View fraud patterns"
            );

            case "FAILED_TRANSACTIONS" -> List.of(
                    "Show successful transactions",
                    "Show fraud transactions",
                    "Check fraud for this account"
            );

            case "SUCCESS_TRANSACTIONS" -> List.of(
                    "Show failed transactions",
                    "Show fraud transactions",
                    "View fraud insights"
            );

            // 🚫 BLOCKED
            case "BLOCKED_ACCOUNT", "BLOCKED_ACCOUNTS" -> List.of(
                    "Why is this account blocked?",
                    "Show transactions for this account",
                    "Check fraud status",
                    "View fraud insights"
            );

            // 📊 INSIGHTS
            case "FRAUD_INSIGHTS" -> List.of(
                    "Show fraud transactions",
                    "Show top fraud patterns",
                    "Check blocked accounts"
            );

            // 🔍 PATTERNS
            case "FRAUD_PATTERNS" -> List.of(
                    "Show fraud transactions",
                    "Check similar fraud cases",
                    "View fraud insights"
            );

            // 📘 KB
            case "KB" -> List.of(
                    "Show fraud transactions",
                    "Explain fraud rules",
                    "What is AML?",
                    "Show suspicious patterns"
            );

            // ❌ ERROR / EMPTY
            case "ERROR" -> List.of(
                    "Show fraud transactions",
                    "View fraud insights",
                    "Check blocked accounts"
            );

            // 🌟 DEFAULT
            default -> List.of(
                    "Show fraud transactions",
                    "Show failed transactions",
                    "View fraud insights"
            );
        };
    }



    private String convertToReadable(List<Map<String, Object>> result) {

        if (result == null || result.isEmpty()) {
            return "No data available.";
        }

        StringBuilder structured = new StringBuilder();

        int count = 1;

        for (Map<String, Object> row : result) {

            structured.append("Record ").append(count++).append(":\n");

            for (Map.Entry<String, Object> entry : row.entrySet()) {

                if (entry.getValue() == null) continue;

                structured.append(entry.getKey())
                        .append(": ")
                        .append(entry.getValue())
                        .append("\n");
            }

            structured.append("\n");
        }

        // 🔥 Send structured data to LLM for polishing
        String prompt = """
Convert the following structured data into clean, professional bullet points for a fraud analyst.

Rules:
- Keep it short
- No repetition
- No null values
- Use bullet points

DATA:
%s
""".formatted(structured.toString());

        return ollamaClient.generateResponse(prompt);
    }


    // ===============================
// 🤖 COMMON AI ANALYSIS METHOD (NEW)
// ===============================
    private String generateAIAnalysis(List<Map<String, Object>> data, String question) {

        try {

            if (data == null || data.isEmpty()) {
                return "No data available for analysis.";
            }

            String prompt = """
You are a banking fraud detection expert.

User Question:
%s

Transaction Data:
%s

Analyze and provide:
- Key insights
- Possible fraud patterns
- Reasons for failures (if any)
- Risk indicators

Keep it short and clear.
""".formatted(question, data.toString());

            String response = ollamaClient.generateResponse(prompt);

            if (response == null || response.isBlank()) {
                return "AI analysis not available.";
            }

            return response;

        } catch (Exception e) {

            System.out.println("❌ LLM FAILED: " + e.getMessage());

            // 🔥 VERY IMPORTANT: NEVER RETURN NULL
            return "AI service unavailable. Showing transaction data only.";
        }
    }

    private boolean containsAny(String text, String[] keywords) {
        for (String word : keywords) {
            if (text.contains(word)) return true;
        }
        return false;
    }

    private boolean isValidQuestion(String q) {
        if (q == null) return false;
        q = q.toLowerCase().trim();
        return q.length() >= 8;
    }

    private List<Map<String, Object>> getBalanceByUserId(Long userId) {

        String sql = """
    SELECT account_number, balance
    FROM account
    WHERE user_id = ?
    """;

        System.out.println("SQL: " + sql + " PARAM userId: " + userId);

        return databaseQueryService.executeQuery(sql, userId);
    }

    private boolean isFollowUpQuestion(String q) {

        if (q == null || q.isBlank()) return false;

        q = q.toLowerCase().trim();

        if (q.contains("this")
                || q.contains("that")
                || q.contains("these")
                || q.contains("those")
                || q.contains("above")
                || q.contains("previous")
                || q.contains("earlier")
                || q.contains("it")) {
            return true;
        }

        if (q.contains("explain this")
                || q.contains("more details")
                || q.contains("tell me more")
                || q.contains("why is this")
                || q.contains("how is this")
                || q.contains("reason for this")) {
            return true;
        }

        if (q.length() < 15 &&
                (q.equals("why")
                        || q.equals("how")
                        || q.equals("reason")
                        || q.equals("why?")
                        || q.equals("how?")
                        || q.equals("explain")
                        || q.equals("details"))) {
            return true;
        }

        // Avoid false positives
        if (q.contains("fraud")
                || q.contains("transaction")
                || q.contains("aml")
                || q.contains("kyc")
                || q.contains("account")
                || q.contains("rbi")
                || q.contains("bank")) {
            return false;
        }

        return false;
    }

    private int extractLimit(String q, boolean isAgentMode) {

        Pattern pattern = Pattern.compile("(last|recent|latest)\\s*(\\d+)");
        Matcher matcher = pattern.matcher(q);

        if (matcher.find()) {
            return Integer.parseInt(matcher.group(2)); // group(2) = number
        }

        //  AGENT MODE OVERRIDE
        if (isAgentMode && !q.contains("last")) {
            return 1000; // or Integer.MAX_VALUE
        }

        return 5; // default
    }

    private boolean isEmptyResponse(String answer) {
        return answer == null
                || answer.trim().isEmpty()
                || answer.toLowerCase().contains("no relevant data")
                || answer.toLowerCase().contains("not found");
    }

    private String callKnowledgeBase(String question,
                                     String normalizedQ,
                                     List<ConversationMemory> history,
                                     String sessionId) {

        String cleanQuery = question;

        // Normalize
        cleanQuery = cleanQuery.replace("ATO", "Account Takeover");
        cleanQuery = cleanQuery.replace("aml", "anti money laundering");
        cleanQuery = cleanQuery.replace("kyc", "know your customer");

        // ===============================
        // FOLLOW-UP CONTEXT
        // ===============================
        if (isFollowUpQuestion(normalizedQ) && history != null && !history.isEmpty()) {

            String lastContext = history.stream()
                    .skip(Math.max(0, history.size() - 2))
                    .map(ConversationMemory::getMessage)
                    .reduce("", (a, b) -> a + "\n" + b);

            if (!lastContext.isBlank()) {
                cleanQuery = """
Context:
%s

Question:
%s
""".formatted(lastContext, question);
            }
        }

        System.out.println("KB Query: " + cleanQuery);

        // ===============================
        // 🔐 INPUT SECURITY
        // ===============================
        var inputCheck = securityFilter.checkInput(cleanQuery);

        if (inputCheck.getLevel() == PromptSecurityFilter.SecurityLevel.BLOCK) {
            return inputCheck.getMessage();
        }

        // ===============================
        // KB CALL
        // ===============================
        String ragAnswer = bedrockClient.queryKnowledgeBase(cleanQuery);

        // ===============================
        // EMPTY CHECK
        // ===============================
        if (ragAnswer == null
                || ragAnswer.trim().isEmpty()
                || ragAnswer.toLowerCase().contains("no relevant")
                || ragAnswer.toLowerCase().contains("not found")) {

            return "No relevant information found.";
        }

        // ===============================
        // 🔐 RESPONSE SECURITY
        // ===============================
        var resCheck = securityFilter.checkResponse(ragAnswer);

        if (resCheck.getLevel() == PromptSecurityFilter.SecurityLevel.BLOCK) {

            System.out.println("⚠️ KB BLOCK → sanitizing instead of blocking");

            if (ragAnswer != null && ragAnswer.length() > 400) {
                ragAnswer = ragAnswer.substring(0, 400);
            }

            ragAnswer = ragAnswer + "\n\n(Note: Some sensitive details were omitted.)";
        }

        else if (resCheck.getLevel() == PromptSecurityFilter.SecurityLevel.FALLBACK) {

            System.out.println("⚠️ KB FALLBACK → partial response");

            ragAnswer = "ℹ️ Partial information:\n\n" + ragAnswer;
        }

        return ragAnswer;
    }



    @Override
    public List<Map<String, Object>> getAllUsers() {
        return jdbcTemplate.queryForList(
                "SELECT id, username, email, enabled FROM users"
        );
    }

    private List<Map<String, Object>> getFraudLogsByAccount(String accountNumber) {

        String sql = """
    SELECT *
    FROM transaction_logs
    WHERE account_to = ?
       OR account_from = ?
    """;

        return databaseQueryService.executeQuery(sql, accountNumber, accountNumber);
    }

    private List<Map<String, Object>> getFraudTransactions(int limit, String type) {

        String sql;

        if ("FRAUD".equals(type)) {

            sql = """
        SELECT transaction_id, account_from, account_to, amount, status, reason,timestamp
        FROM transaction_logs
        WHERE status = 'FRAUD'
        ORDER BY amount DESC
        LIMIT ?
        """;

            return databaseQueryService.executeQuery(sql, limit);
        }

        if ("ALERT".equals(type)) {

            sql = """
        SELECT transaction_id, account_from, account_to, amount, status, reason,timestamp
        FROM transaction_logs
        WHERE status = 'ALERT'
        ORDER BY amount DESC
        LIMIT ?
        """;

            return databaseQueryService.executeQuery(sql, limit);
        }

        // 🔥 DEFAULT (BOTH)
        sql = """
    SELECT transaction_id, account_from, account_to, amount, status, reason,timestamp
    FROM transaction_logs
    WHERE status IN ('FRAUD', 'ALERT')
    ORDER BY amount DESC
    LIMIT ?
    """;

        return databaseQueryService.executeQuery(sql, limit);
    }

    private AssistantResponse handleFraudExplanation(String sessionId, String userId, String question) {

        String accNo = extractAccountNumber(normalize(question));






        if (accNo == null) {
            return buildResponse(
                    "ERROR",
                    "Validation",
                    null,
                    "Please provide a valid account number.",
                    "Validation"
            );
        }

        List<Map<String, Object>> logs = getFraudLogsByAccount(accNo);

        if (logs == null || logs.isEmpty()) {
            return buildResponse(
                    "ERROR",
                    "Fraud Analysis",
                    null,
                    "No fraud-related transactions found for this account.",
                    "PostgreSQL Database"
            );
        }

        // ===============================
        // EXTRACT TOP REASONS
        // ===============================
        Map<String, Long> reasonCount = logs.stream()
                .filter(log -> log.get("reason") != null)
                .collect(Collectors.groupingBy(
                        log -> log.get("reason").toString(),
                        Collectors.counting()
                ));

        List<Map<String, Object>> topReasons = reasonCount.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(5)
                .map(e -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("reason", e.getKey());
                    map.put("count", e.getValue());
                    return map;
                })
                .toList();

        // ===============================
        // BUILD READABLE TEXT
        // ===============================
        StringBuilder reasonsText = new StringBuilder();

        topReasons.forEach(r -> {
            reasonsText.append("• ")
                    .append(r.get("reason"))
                    .append(" (")
                    .append(r.get("count"))
                    .append(" times)\n");
        });

        String finalAnswer = """
🚨 Fraud detected for this account.

Top reasons:
%s
""".formatted(reasonsText.toString());

        // ===============================
        // FINAL RESPONSE
        // ===============================
        return buildResponse(
                "FRAUD_ANALYSIS",
                "🚨 Fraud Analysis",
                topReasons,   // 👈 structured data for UI
                finalAnswer,  // 👈 readable summary
                "PostgreSQL Database"
        );
    }

    private List<Map<String, Object>> getTransactionsByStatus(String status, int limit) {

        String sql = """
        SELECT 
            id,
            type,
            userid,
            accnofrom AS from_account,
            accnoto AS to_account,
            status,
            timestamp,
            COALESCE(amount, 0) AS amount
        FROM transactions
        WHERE status = ?
        ORDER BY timestamp DESC
        LIMIT ?
    """;

        return databaseQueryService.executeQuery(sql, status, limit);
    }

    private boolean isDbQuery(String q) {

        return q.contains("transaction")
                || q.contains("account")
                || q.contains("balance")
                || q.contains("blocked")
                || q.contains("fraud")
                || q.contains("user ");
    }

    private boolean isFraudDomainQuestion(String q) {

        if (q == null || q.isBlank()) return false;

        q = q.toLowerCase();

        return q.contains("fraud")
                || q.contains("fraudulent")
                || q.contains("transaction")
                || q.contains("transfer")
                || q.contains("payment")
                || q.contains("aml")
                || q.contains("anti money laundering")
                || q.contains("kyc")
                || q.contains("know your customer")
                || q.contains("account")
                || q.contains("bank")
                || q.contains("banking")
                || q.contains("rbi")
                || q.contains("compliance")
                || q.contains("guideline")
                || q.contains("policy")
                || q.contains("rule")
                || q.contains("risk")
                || q.contains("risk score")
                || q.contains("suspicious")
                || q.contains("alert")
                || q.contains("flagged")
                || q.contains("blocked")
                || q.contains("blacklist")
                || q.contains("money laundering")
                || q.contains("ato")
                || q.contains("account takeover")
                || q.contains("phishing")
                || q.contains("otp")
                || q.contains("unauthorized")
                || q.contains("login")
                || q.contains("device")
                || q.contains("location")
                || q.contains("ip")
                || q.contains("velocity")
                || q.contains("pattern")
                || q.contains("anomaly")
                || q.contains("detection")
                || q.contains("monitoring")
                || q.contains("investigation")
                || q.contains("case")
                || q.contains("report")
                || q.contains("fiu")
                || q.contains("compliance")
                || q.contains("regulation");
    }

    private List<Map<String, Object>> getFraudInsights() {

        String sql = """
        SELECT 
            status,
            COUNT(*) AS count
        FROM transaction_logs
        GROUP BY status
    """;

        return databaseQueryService.executeQuery(sql);
    }

    private List<Map<String, Object>> getTopFraudReasons() {

        String sql = """
        SELECT 
            reason,
            COUNT(*) AS count
        FROM transaction_logs
        WHERE status IN ('FRAUD', 'ALERT')
        GROUP BY reason
        ORDER BY count DESC
        LIMIT 5
    """;

        return databaseQueryService.executeQuery(sql);
    }

    private String detectFraudType(String q) {

        if (q.contains("alert")) return "ALERT";
        if (q.contains("fraud")) return "FRAUD";

        return "ALL"; // fallback
    }

    private Double extractAmount(String q) {

        if (q == null) return null;

        q = q.toLowerCase();

        // lakh support
        if (q.contains("lakh")) {
            Pattern p = Pattern.compile("(\\d+(\\.\\d+)?)\\s*lakh");
            Matcher m = p.matcher(q);
            if (m.find()) {
                return Double.parseDouble(m.group(1)) * 100000;
            }
        }

        // numeric amount
        Pattern p = Pattern.compile("(\\d{3,})");
        Matcher m = p.matcher(q);

        if (m.find()) {
            return Double.parseDouble(m.group(1));
        }

        return null;
    }

    private List<Map<String, Object>> getSmartFilteredTransactions(String normalizedQ,boolean isAgentMode) {

        int limit = extractLimit(normalizedQ, isAgentMode);

        boolean hasBetween = normalizedQ.contains("between");
        boolean hasAmountFilter = normalizedQ.contains("above")
                || normalizedQ.contains("below")
                || normalizedQ.contains("less than")
                || normalizedQ.contains("greater than");

        List<Map<String, Object>> data;

        // ===============================
        // FRAUD / ALERT (logs table)
        // ===============================
        if (normalizedQ.contains("fraud") || normalizedQ.contains("alert")) {

            data = databaseQueryService.executeQuery("""
        SELECT 
            transaction_id,
            account_from,
            account_to,
            amount,
            status,
            reason,
            timestamp
        FROM transaction_logs
        WHERE status = ?
        ORDER BY amount DESC
    """,
                    normalizedQ.contains("fraud") ? "FRAUD" : "ALERT");

        } else {

            // ===============================
            // 🔥 ACCOUNT FILTER (TOP PRIORITY FIX)
            // ===============================
            String accNo = extractAccountNumber(normalizedQ);

            if (accNo != null) {

                data = databaseQueryService.executeQuery("""
            SELECT 
                accnofrom AS from_account,
                accnoto AS to_account,
                amount,
                status,
                timestamp
            FROM transactions
            WHERE accnofrom = ? OR accnoto = ?
            ORDER BY amount DESC
        """, accNo, accNo);

            } else {

                // ===============================
                // ⚡ FAST PATH (NO LLM)
                // ===============================
                double[] range = extractBetweenRange(normalizedQ);

                // ===============================
                // BETWEEN
                // ===============================
                if (range != null) {

                    data = databaseQueryService.executeQuery("""
                SELECT 
                    accnofrom AS from_account, 
                    accnoto AS to_account, 
                    amount, 
                    status,
                    timestamp
                FROM transactions
                WHERE amount BETWEEN ? AND ?
                ORDER BY amount DESC
            """, range[0], range[1]);
                }

                else {

                    Double amount = extractAmount(normalizedQ);
                    String op = extractAmountOperator(normalizedQ);

                    // ===============================
                    // ABOVE / BELOW
                    // ===============================
                    if (amount != null) {

                        if ("LT".equals(op)) {

                            data = databaseQueryService.executeQuery("""
                        SELECT 
                            accnofrom AS from_account, 
                            accnoto AS to_account, 
                            amount, 
                            status,timestamp
                        FROM transactions
                        WHERE amount < ?
                        ORDER BY amount DESC
                    """, amount);

                        } else {

                            data = databaseQueryService.executeQuery("""
                        SELECT 
                            accnofrom AS from_account, 
                            accnoto AS to_account, 
                            amount, 
                            status,timestamp
                        FROM transactions
                        WHERE amount > ?
                        ORDER BY amount DESC
                    """, amount);
                        }
                    }

                    // ===============================
                    // DEFAULT (no filter)
                    // ===============================
                    else {

                        if (hasBetween || hasAmountFilter) {

                            // 🔥 Fetch full data (NO LIMIT)
                            data = databaseQueryService.executeQuery("""
            SELECT 
                accnofrom AS from_account, 
                accnoto AS to_account, 
                amount, 
                status,timestamp
            FROM transactions
            ORDER BY timestamp DESC
        """);

                        } else {

                            // ✅ Keep existing behavior
                            data = getRecentTransactions(limit);
                        }
                    }
                }
            }
        }

        // ===============================
        // APPLY FILTERS (user only now)
        // ===============================
        if (!normalizedQ.contains("account")) {
            data = applyFilters(data, normalizedQ);
        }

        // ===============================
        // SORTING
        // ===============================
        if (isTopQuery(normalizedQ)) {
            data = data.stream()
                    .sorted((a, b) -> Double.compare(
                            Double.valueOf(b.get("amount").toString()),
                            Double.valueOf(a.get("amount").toString())
                    ))
                    .toList();
        }

        if (isLowestQuery(normalizedQ)) {
            data = data.stream()
                    .sorted((a, b) -> Double.compare(
                            Double.valueOf(a.get("amount").toString()),
                            Double.valueOf(b.get("amount").toString())
                    ))
                    .toList();
        }

        // ===============================
        // FINAL LIMIT
        // ===============================
        data = data.stream().limit(limit).toList();

        return data;
    }

    private List<Map<String, Object>> applyFilters(
            List<Map<String, Object>> data,
            String normalizedQ
    ) {

        if (data == null || data.isEmpty()) return data;

        Double amount = extractAmount(normalizedQ);

        final Long userId = isUserQuery(normalizedQ)
                ? extractNumber(normalizedQ)
                : null;

        String accNo = extractAccountNumber(normalizedQ);

        // ===============================
        // 🔥 FIX: CALCULATE ONCE (NOT PER ROW)
        // ===============================
        double[] range = extractBetweenRange(normalizedQ);

        return data.stream().filter(row -> {

            // ===============================
            // AMOUNT FILTER
            // ===============================
            if (amount != null && row.get("amount") != null) {

                Double amt = Double.valueOf(row.get("amount").toString());
                String op = extractAmountOperator(normalizedQ);

                if ("GT".equals(op) && amt <= amount) return false;
                if ("LT".equals(op) && amt >= amount) return false;
            }

            // ===============================
            // USER FILTER
            // ===============================
            if (normalizedQ.contains("user") && userId != null && row.get("userid") != null) {
                if (!row.get("userid").toString().equals(userId.toString())) {
                    return false;
                }
            }

            // ===============================
            // BETWEEN RANGE FILTER (FIXED)
            // ===============================
            if (range != null && row.get("amount") != null) {

                Double amt = Double.valueOf(row.get("amount").toString());

                if (amt < range[0] || amt > range[1]) {
                    return false;
                }
            }

            // ===============================
            // ACCOUNT FILTER (FINAL FIX)
            // ===============================
            if (accNo != null) {

                String from = row.containsKey("from_account")
                        ? String.valueOf(row.get("from_account"))
                        : row.containsKey("accnofrom")
                        ? String.valueOf(row.get("accnofrom"))
                        : null;

                String to = row.containsKey("to_account")
                        ? String.valueOf(row.get("to_account"))
                        : row.containsKey("accnoto")
                        ? String.valueOf(row.get("accnoto"))
                        : null;

                if ((from == null || !accNo.equals(from)) &&
                        (to == null || !accNo.equals(to))) {
                    return false;
                }
            }

            return true;

        }).toList();
    }

    private String extractAmountOperator(String q) {

        if (q == null) return "GT"; // default

        q = q.toLowerCase();

        if (q.contains("below") || q.contains("less than") || q.contains("under")) {
            return "LT";
        }

        if (q.contains("above") || q.contains("greater than") || q.contains("more than")) {
            return "GT";
        }

        return "GT"; // default
    }

    private double[] extractBetweenRange(String q) {

        if (q == null) return null;

        q = q.toLowerCase().trim();

        // normalize variations
        q = q.replace("in range", "between");
        q = q.replace("from", "between");

        // 🔥 flexible regex
        Pattern p = Pattern.compile(
                "between\\s+(?:amount\\s*)?(\\d+(?:\\.\\d+)?)\\s*(k|lakh)?\\s*(?:and|to)\\s*(\\d+(?:\\.\\d+)?)\\s*(k|lakh)?"
        );

        Matcher m = p.matcher(q);

        if (m.find()) {

            double min = parseAmount(m.group(1), m.group(2));
            double max = parseAmount(m.group(3), m.group(4));

            System.out.println("✅ BETWEEN detected: " + min + " - " + max);

            return new double[]{min, max};
        }



        return null;
    }

    private double parseAmount(String value, String unit) {

        double val = Double.parseDouble(value);

        if ("k".equals(unit)) return val * 1000;
        if ("lakh".equals(unit)) return val * 100000;

        return val;
    }

    private boolean isTopQuery(String q) {
        return q.contains("top") || q.contains("highest") || q.contains("largest");
    }

    private boolean isLowestQuery(String q) {
        return q.contains("lowest") || q.contains("smallest") || q.contains("minimum");
    }

    private boolean isUserQuery(String q) {
        return q != null && q.toLowerCase().contains("user");
    }


    private Map<String, Object> extractQueryMetadata(String question) {

        String prompt = """
Extract structured query info from user input.

Return JSON ONLY. No explanation.

Supported types:
- between → { "type": "between", "min": number, "max": number }
- greater → { "type": "greater", "amount": number }
- less → { "type": "less", "amount": number }
- none → { "type": "none" }

User Query:
%s
""".formatted(question);

        try {
            String response = ollamaClient.generateResponse(prompt);

            System.out.println("LLM RAW: " + response);

// 🔥 CLEAN RESPONSE (VERY IMPORTANT)
            response = response
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();

            System.out.println("LLM CLEAN: " + response);

            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(response, Map.class);

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("type", "none");
        }
    }

    private boolean isAnalysisQuery(String q) {
        return q.contains("why")
                || q.contains("reason")
                || q.contains("analyze")
                || q.contains("analysis")
                || q.contains("explain");
    }

    private List<Map<String, Object>> sanitizeData(List<Map<String, Object>> data) {

        if (data == null) return null;

        return data.stream().map(row -> {

            Map<String, Object> clean = new HashMap<>();

            for (Map.Entry<String, Object> e : row.entrySet()) {

                Object v = e.getValue();

                if (v == null) {
                    clean.put(e.getKey(), null);
                } else if (v instanceof java.sql.Timestamp ts) {
                    clean.put(e.getKey(), ts.toLocalDateTime().toString());
                } else if (v instanceof java.time.LocalDateTime ldt) {
                    clean.put(e.getKey(), ldt.toString());
                } else if (v instanceof java.math.BigDecimal bd) {
                    clean.put(e.getKey(), bd.doubleValue());
                } else {
                    clean.put(e.getKey(), v);
                }
            }

            return clean;

        }).toList();
    }

    private boolean isExplicitDbQuery(String q) {
        return q.contains("show")
                || q.contains("list")
                || q.contains("get")
                || q.contains("fetch")
                || q.contains("display");
    }

    public AssistantResponse processAnalysis(AssistantRequest request) {

        String sessionId = request.getSessionId();
        String question = request.getQuestion();
        String userId = request.getAnalystId();

        String normalizedQ = normalize(question);
        String intent = detectIntent(normalizedQ);

        List<Map<String, Object>> data;

        // 🔥 LIGHT DATA ONLY
        if ("FAILED_TRANSACTIONS".equals(intent)) {
            data = getTransactionsByStatus("FAILED", 5);
        } else if ("FRAUD_TRANSACTIONS".equals(intent)) {
            data = getSmartFilteredTransactions(normalizedQ, request.isAgentMode())
                    .stream().limit(5).toList();
        } else {
            data = getRecentTransactions(5);
        }

        data = sanitizeData(data);

        String aiAnswer;

        try {
            aiAnswer = generateAIAnalysis(data, question);
        } catch (Exception e) {
            aiAnswer = convertToReadable(data);
        }

        if (aiAnswer == null || aiAnswer.isBlank()) {
            aiAnswer = convertToReadable(data);
        }

        saveConversation(sessionId, userId, aiAnswer, "ASSISTANT");

        return buildResponse(
                "ANALYSIS",
                "Analysis Result",
                null,
                aiAnswer,
                "AI"
        );
    }

    private List<Long> extractAllNumbers(String text) {
        Matcher m = Pattern.compile("\\d+").matcher(text);
        List<Long> numbers = new ArrayList<>();

        while (m.find()) {
            numbers.add(Long.parseLong(m.group()));
        }

        return numbers;
    }

    private List<Map<String, Object>> getAccountLimit(String accNo) {

        String sql = """
        SELECT account_number, daily_limit, transaction_limit
        FROM account_limits
        WHERE account_number = ?
    """;

        return databaseQueryService.executeQuery(sql, accNo);
    }

    private List<Map<String, Object>> getAllAccounts() {

        String sql = """
        SELECT 
            account_number,
            balance,
            ifsc_code,
            user_id,
            account_enabled,
            currency
        FROM account
        ORDER BY account_number
    """;

        return databaseQueryService.executeQuery(sql);
    }









}