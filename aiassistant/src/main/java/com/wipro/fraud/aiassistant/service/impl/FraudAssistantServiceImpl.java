package com.wipro.fraud.aiassistant.service.impl;

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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class FraudAssistantServiceImpl implements FraudAssistantService {

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

    @Override
    public AssistantResponse processQuery(AssistantRequest request) {

        String sessionId = request.getSessionId();
        String question = request.getQuestion();
        String userId = request.getAnalystId();

        String normalizedQ = normalize(question);

        String intent = detectIntent(normalizedQ);
        System.out.println("Detected Intent: " + intent);

        // ===============================
//  ROLE-BASED ACCESS CHECK
// ===============================

        Set<String> roles = userClient.getUserRoles(userId);

        if (roles == null || roles.isEmpty()) {
            return buildResponse(sessionId, userId, question,
                    "❌ Unable to determine user roles",
                    "Authorization");
        }

//get table from intent
        String table = tableAccessManager.getTableFromIntent(intent);

// fallback if unknown
        if (table.equals("unknown")) {
            table = tableAccessManager.detectTableFromQuery(normalizedQ);
        }

//check access
        if (!tableAccessManager.hasAccess(roles, table)) {
            return buildResponse(sessionId, userId, question,
                    "❌ Access Denied: You do not have permission to access " + table,
                    "Authorization");
        }

        if (intent.equals("GENERAL") && !isValidQuestion(question)) {
            return buildResponse(sessionId, userId, question,
                    "Please ask a fraud-related question.",
                    "Validation");
        }

        List<ConversationMemory> history =
                memoryService.getConversation(sessionId, userId);

        List<Map<String, Object>> result = null;
        String answer;

        // ===============================
        //  HANDLE INTENTS
        // ===============================
        switch (intent) {

            case "NO_OP" -> {
                return buildResponse(sessionId, userId, question,
                        "👍 Let me know if you need anything else.",
                        "System");
            }

            case "FRAUD_CHECK" -> {
                return handleFraudCheck(sessionId, userId, question);
            }

            case "BLOCKED_ACCOUNT" -> {
                String accNo = extractAccountNumber(normalizedQ);
                result = (accNo != null)
                        ? getBlockedAccount(accNo)
                        : getAllBlockedAccounts();
            }

            case "TRANSACTIONS" -> {

                Long userIdNumber = extractNumber(normalizedQ);

                if (userIdNumber == null) {
                    return buildResponse(sessionId, userId, question,
                            "Please provide a valid user ID.",
                            "Validation");
                }

                int limit = extractLimit(normalizedQ);

                result = getTransactions(userIdNumber, limit);
            }

            case "ACCOUNT_BALANCE" -> {
                Long userIdNumber = extractNumber(normalizedQ);

                if (normalizedQ.contains("user") && userIdNumber != null) {
                    result = getBalanceByUserId(userIdNumber);
                } else {
                    String accNo = extractAccountNumber(normalizedQ);

                    if (accNo == null) {
                        return buildResponse(sessionId, userId, question,
                                "Please provide a valid account number.",
                                "Validation");
                    }

                    result = getBalance(accNo);
                }
            }

            case "USERS" -> {

                result = getAllUsers(); // you create this method
            }

            case "GENERAL" -> {

                //  STEP 1: KB FIRST
                String ragAnswer = callKnowledgeBase(question, normalizedQ, history, sessionId);

                if (!isEmptyResponse(ragAnswer)) {
                    return buildResponse(sessionId, userId, question,
                            ragAnswer,
                            "Fraud Policy Knowledge Base");
                }

                //  STEP 2: FALLBACK TO DB (safe generic)
                List<Map<String, Object>> fallbackDb = getAllBlockedAccounts();

                if (fallbackDb != null && !fallbackDb.isEmpty()) {
                    String dbAnswer = convertToReadable(fallbackDb);

                    return buildResponse(sessionId, userId, question,
                            dbAnswer,
                            "PostgreSQL Database");
                }

                //  FINAL
                return buildResponse(sessionId, userId, question,
                        "No relevant data found in database or knowledge base.",
                        "System");
            }
        }

        // ===============================
        //  DB FLOW (PRIMARY DB INTENTS)
        // ===============================

        // ===============================
//  DB RESULT CHECK
// ===============================
        boolean hasDb = result != null && !result.isEmpty();

        if (hasDb) {

            answer = convertToReadable(result);

            return buildResponse(sessionId, userId, question,
                    answer,
                    "PostgreSQL Database");
        }

// ===============================
//  DB FAILED → FALLBACK TO KB
// ===============================
        String ragAnswer = callKnowledgeBase(question, normalizedQ, history, sessionId);
        boolean hasKb = !isEmptyResponse(ragAnswer);

// ===============================
//  CASE: DB FAIL → SHOW MESSAGE + KB
// ===============================
        if (hasKb) {

            String finalAnswer = """
No data found in PostgreSQL Database.

%s
""".formatted(ragAnswer);

            return buildResponse(sessionId, userId, question,
                    finalAnswer,
                    "Fraud Policy Knowledge Base");
        }

// ===============================
//  FINAL FALLBACK
// ===============================
        return buildResponse(sessionId, userId, question,
                "No relevant data found in database or knowledge base.",
                "System");
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
        q = q.replace("acc", "account");
        q = q.replace("fraudulent", "fraud");
        q = q.replace("transaction logs", "transaction");
        q = q.replace("transactions logs", "transaction");

        return q;
    }

    // ===============================
    //  INTENT DETECTION
    // ===============================
    private String detectIntent(String q) {

        if (q == null || q.isBlank()) return "GENERAL";

        if (q.equals("ok") || q.equals("thanks") || q.equals("yes") || q.equals("no")) {
            return "NO_OP";
        }

        String[] dbActions = {"show", "list", "get", "fetch", "give", "display"};
        String[] knowledgeWords = {"what", "how", "explain", "define"};
        String[] policyWords = {"policy", "rule", "guideline", "aml"};
        String[] blockedWords = {"blocked", "disabled", "blacklist"};
        String[] balanceWords = {"balance", "amount", "fund"};
        String[] fraudWords = {"fraud", "suspicious", "flagged", "risk"};
        String[] transactionWords = {"transaction", "transfer", "payment"};

        boolean hasDbAction = containsAny(q, dbActions);
        boolean hasFraud = containsAny(q, fraudWords);
        boolean hasKnowledge = containsAny(q, knowledgeWords);
        boolean hasPolicy = containsAny(q, policyWords);
        boolean hasBlocked = containsAny(q, blockedWords);
        boolean hasTransaction = containsAny(q, transactionWords);
        boolean hasBalance = containsAny(q, balanceWords);
        boolean hasAccount = q.contains("account");
        boolean hasNumber = q.matches(".*\\d+.*");

        if (q.contains("from database") || q.contains("from db")) {
            if (hasBlocked) return "BLOCKED_ACCOUNT";
            if (hasTransaction) return "TRANSACTIONS";
            if (hasBalance) return "ACCOUNT_BALANCE";
        }

        if ((hasPolicy || hasKnowledge) && !hasDbAction) {
            return "GENERAL";
        }

        if (hasBlocked) return "BLOCKED_ACCOUNT";

        if (hasTransaction && (hasDbAction || hasNumber)) {
            return "TRANSACTIONS";
        }

        if (hasFraud && hasTransaction) {
            return "TRANSACTIONS";
        }

        if (hasBalance && hasAccount) {
            return "ACCOUNT_BALANCE";
        }

        //  CONTEXTUAL FOLLOW-UP QUESTIONS
        if (q.contains("rule") || q.contains("explain") || q.contains("information")) {
            return "GENERAL";
        }


        if ((hasFraud || q.contains("why")) && hasNumber) {
            return "FRAUD_CHECK";
        }

        if (q.contains("user") || q.contains("users") || q.contains("registered")) {
            return "USERS";
        }

        return "GENERAL";
    }



    // ===============================
    //  PARAM EXTRACTION
    // ===============================
    private String extractAccountNumber(String q) {
        String num = q.replaceAll("\\D+", "");
        return num.isEmpty() ? null : num;
    }

    private Long extractNumber(String q) {

        Pattern pattern = Pattern.compile("user\\s*(\\d+)");
        Matcher matcher = pattern.matcher(q);

        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }

        Pattern fallback = Pattern.compile("(\\d+)");
        Matcher fallbackMatcher = fallback.matcher(q);

        Long lastNumber = null;
        while (fallbackMatcher.find()) {
            lastNumber = Long.parseLong(fallbackMatcher.group(1));
        }

        return lastNumber;
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
    SELECT *
    FROM transactions
    WHERE userId = ?
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

        String accNo = extractAccountNumber(normalize(question));

        if (accNo == null) {
            return buildResponse(sessionId, userId, question,
                    "Please provide a valid account number for fraud analysis.",
                    "Validation");
        }

        List<Map<String, Object>> blockedData = getBlockedAccount(accNo);

        if (blockedData == null || blockedData.isEmpty()) {
            return buildResponse(sessionId, userId, question,
                    "No fraud detected for this account.",
                    "Fraud Analysis");
        }

        String analysisPrompt = """
Fraud: YES or NO
Risk Score: (0-100)
Reason:
- Point 1
- Point 2

DATA:
%s
""".formatted(blockedData.toString());

        String analysis = ollamaClient.generateResponse(analysisPrompt);

        if (securityFilter.isMalicious(question)) {
            return buildResponse(sessionId, userId, question,
                    "⚠️ Your query contains restricted or unsafe instructions.",
                    "Security");
        }

        String policy = bedrockClient.queryKnowledgeBase(question);

        String finalAnswer = """
Fraud Analysis:
%s

Policy Reference:
%s
""".formatted(analysis, policy);

        return buildResponse(sessionId, userId, question, finalAnswer, "Hybrid");
    }

    // ===============================
    //  RESPONSE BUILDER
    // ===============================
    private AssistantResponse buildResponse(String sessionId, String userId, String question, String answer, String source) {

        memoryService.saveMessage(sessionId, userId, "USER", question);
        memoryService.saveMessage(sessionId, userId, "ASSISTANT", answer);

        return AssistantResponse.builder()
                .answer(answer)
                .source(source)
                .build();
    }

    // ===============================
    //  RESULT FORMAT
    // ===============================
    private String convertToReadable(List<Map<String, Object>> result) {

        String prompt = """
Convert DB result into clean bullet points.

%s
""".formatted(result.toString());

        return ollamaClient.generateResponse(prompt);
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
        return q.contains("this rule")
                || q.contains("that rule")
                || q.contains("above")
                || q.contains("explain")
                || q.contains("information");
    }

    private int extractLimit(String q) {

        Pattern pattern = Pattern.compile("last\\s*(\\d+)");
        Matcher matcher = pattern.matcher(q);

        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
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

        String enhancedQuestion = question;

        // ===============================
        // FOLLOW-UP CONTEXT
        // ===============================
        if (isFollowUpQuestion(normalizedQ) && history != null && !history.isEmpty()) {

            String lastResponse = history.stream()
                    .filter(m -> m.getRole().equals("ASSISTANT"))
                    .reduce((first, second) -> second)
                    .map(ConversationMemory::getMessage)
                    .orElse("");

            enhancedQuestion = """
Context:
%s

User Question:
%s
""".formatted(lastResponse, question);
        }

        // ===============================
        //  INPUT SECURITY FILTER
        // ===============================
        if (securityFilter.isMalicious(question)) {
            return "⚠️ Unsafe request detected.";
        }

        AssistantRequest newRequest = new AssistantRequest();
        newRequest.setSessionId(sessionId);
        newRequest.setQuestion(enhancedQuestion);

        String prompt = PromptTemplateBuilder.buildFraudPrompt(newRequest, history);

        String ragAnswer = bedrockClient.queryKnowledgeBase(prompt);

        // ===============================
        //  OUTPUT SECURITY FILTER
        // ===============================
        if (securityFilter.isUnsafeResponse(ragAnswer)) {
            return "⚠️ Response blocked due to security policy.";
        }

        return ragAnswer;
    }



    @Override
    public List<Map<String, Object>> getAllUsers() {
        return jdbcTemplate.queryForList(
                "SELECT id, username, email, enabled FROM users"
        );
    }


}