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

        // 🚫 BLOCK NON-FRAUD QUERIES FROM DB
        if (!intent.equals("GENERAL") && !isDbQuery(normalizedQ)) {
            return buildResponse(sessionId, userId, question,
                    "No relevant information found.",
                    "System");
        }

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

            case "FRAUD_INSIGHTS" -> {
                result = getFraudInsights();
            }

            case "FRAUD_PATTERNS" -> {
                result = getTopFraudReasons();
            }

            case "HYBRID_FRAUD" -> {
                return handleFraudExplanation(sessionId, userId, question);
            }

            case "FAILED_TRANSACTIONS" -> {
                int limit = extractLimit(normalizedQ);
                result = getTransactionsByStatus("FAILED", limit);
            }

            case "SUCCESS_TRANSACTIONS" -> {
                int limit = extractLimit(normalizedQ);
                result = getTransactionsByStatus("SUCCESSFUL", limit);
            }

            case "BLOCKED_ACCOUNT" -> {
                String accNo = extractAccountNumber(normalizedQ);
                result = (accNo != null)
                        ? getBlockedAccount(accNo)
                        : getAllBlockedAccounts();
            }

            case "TRANSACTIONS" -> {

                int limit = extractLimit(normalizedQ);

                boolean hasUserKeyword = normalizedQ.contains("user");
                boolean hasLastKeyword = normalizedQ.contains("last");

                //  CASE 1: "last 5 transactions" → system-wide
                if (hasLastKeyword && !hasUserKeyword) {
                    result = getRecentTransactions(limit);
                }

                //  CASE 2: "transactions for user 6"
                else if (hasUserKeyword) {

                    Long userIdNumber = extractNumber(normalizedQ);

                    if (userIdNumber == null) {
                        return buildResponse(sessionId, userId, question,
                                "Please provide a valid user ID.",
                                "Validation");
                    }

                    result = getTransactions(userIdNumber, limit);
                }

                //  CASE 3: generic → system-wide fallback
                else {
                    result = getRecentTransactions(limit);
                }
            }

            case "FRAUD_TRANSACTIONS" -> {
                result = getFraudTransactions();
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

                // 🚫 Block non-fraud domain questions
                if (!isFraudDomainQuestion(normalizedQ)) {
                    return buildResponse(sessionId, userId, question,
                            "No relevant information found.",
                            "System");
                }

                // ✅ Call KB (clean)
                String ragAnswer = callKnowledgeBase(question, normalizedQ, history, sessionId);

                // 🚫 Prevent hallucination
                if (ragAnswer == null
                        || ragAnswer.trim().isEmpty()
                        || ragAnswer.toLowerCase().contains("no relevant")
                        || ragAnswer.length() < 20) {

                    return buildResponse(sessionId, userId, question,
                            "No relevant information found.",
                            "System");
                }

                return buildResponse(sessionId, userId, question,
                        ragAnswer,
                        "Fraud Policy Knowledge Base");
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
//  DB FAILED → SMART SUGGESTION (NO KB CALL)
// ===============================
        // ===============================
//  DB FAILED → SMART SUGGESTION (NO KB CALL)
// ===============================
        if (!intent.equals("GENERAL")) {

            String suggestion;

            switch (intent) {

                case "BLOCKED_ACCOUNT":
                    suggestion = "No blocked accounts found. You can check recent fraud transactions.";
                    break;

                case "TRANSACTIONS":
                    if (normalizedQ.contains("last")) {
                        suggestion = """
No recent transactions found.

Try:
• Increase the limit (e.g., last 10 or 20)
• Check fraud transactions instead
""";
                    } else {
                        suggestion = """
No transactions found.

Try:
• Specify a user ID
• View recent transactions
• Check fraud summary
""";
                    }
                    break;

                case "ACCOUNT_BALANCE":
                    suggestion = "No account details found. Please verify the account number.";
                    break;

                case "FRAUD_CHECK":
                    suggestion = "No fraud detected for this account.";
                    break;

                case "USERS":
                    suggestion = "No users found in the system.";
                    break;

                default:
                    suggestion = "No relevant data found.";
            }

            return buildResponse(sessionId, userId, question,
                    suggestion,
                    "PostgreSQL Database");
        }

// ===============================
//  GENERAL → KNOWLEDGE BASE ONLY
// ===============================
        if (intent.equals("GENERAL")) {

            String ragAnswer = callKnowledgeBase(question, normalizedQ, history, sessionId);

            if (!isEmptyResponse(ragAnswer)) {
                return buildResponse(sessionId, userId, question,
                        ragAnswer,
                        "Fraud Policy Knowledge Base");
            }
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

        q = q.toLowerCase().trim();

        // ===============================
        // BASIC NO-OP
        // ===============================
        if (q.equals("ok") || q.equals("thanks") || q.equals("yes") || q.equals("no")) {
            return "NO_OP";
        }

        if (q.contains("fraud insights") || q.contains("fraud summary")) {
            return "FRAUD_INSIGHTS";
        }

        if (q.contains("fraud reasons") || q.contains("top fraud patterns")) {
            return "FRAUD_PATTERNS";
        }

        // ===============================
        // SMART ENTITY DETECTION (NEW)
        // ===============================
        boolean hasAccountNumber = q.matches(".*\\d{10,}.*"); // account-like number
        boolean hasUserIdPattern = q.matches(".*user\\s*\\d+.*");

        // ===============================
        // KEYWORD GROUPS
        // ===============================
        String[] dbActions = {"show", "list", "get", "fetch", "give", "display"};
        String[] knowledgeWords = {"what", "how", "define"};
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

        // ===============================
        // EXPLICIT DB REQUEST
        // ===============================
        if (q.contains("from database") || q.contains("from db")) {
            if (hasBlocked) return "BLOCKED_ACCOUNT";
            if (hasTransaction) return "TRANSACTIONS";
            if (hasBalance) return "ACCOUNT_BALANCE";
        }

        // ===============================
        // HYBRID (WHY / EXPLANATION)
        // ===============================
        if ((q.contains("why") || q.contains("reason") || q.contains("explain"))
                && hasAccountNumber) {
            return "HYBRID_FRAUD";
        }

        // ===============================
        // FRAUD CHECK (DIRECT DATA QUERY)
        // ===============================
        if (hasAccountNumber && hasFraud) {
            return "FRAUD_CHECK";
        }

        // ===============================
        // KNOWLEDGE BASE (POLICY)
        // ===============================
        if ((hasPolicy || hasKnowledge) && !hasDbAction) {
            return "GENERAL";
        }

        // ===============================
        // BLOCKED ACCOUNT
        // ===============================
        if (hasBlocked) return "BLOCKED_ACCOUNT";

        if (q.contains("fraud transaction") || q.contains("alert transaction")) {
            return "FRAUD_TRANSACTIONS";
        }

        //  KNOWLEDGE QUESTIONS (VERY IMPORTANT)
        if (q.startsWith("what") || q.startsWith("how") || q.startsWith("when") || q.startsWith("explain")) {
            return "GENERAL";
        }

        if (q.contains("failed transaction")) return "FAILED_TRANSACTIONS";
        if (q.contains("successful transaction")) return "SUCCESS_TRANSACTIONS";



        // ===============================
        // TRANSACTIONS (IMPROVED)
        // ===============================
        if (hasTransaction) {
            return "TRANSACTIONS";
        }

        // ===============================
        // BALANCE
        // ===============================
        if (hasBalance && hasAccount) {
            return "ACCOUNT_BALANCE";
        }

        // ===============================
        // FOLLOW-UP QUESTIONS
        // ===============================
        if (q.contains("rule") || q.contains("information")) {
            return "GENERAL";
        }

        // ===============================
        // USERS
        // ===============================
        if (q.contains("user") || q.contains("users") || q.contains("registered")) {
            return "USERS";
        }

        // ===============================
        // DEFAULT
        // ===============================
        // FINAL STRICT CHECK
        if (!isDbQuery(q)) {
            return "GENERAL";
        }

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

        String accNo = extractAccountNumber(normalize(question));

        if (accNo == null) {
            return buildResponse(sessionId, userId, question,
                    "Please provide a valid account number for fraud analysis.",
                    "Validation");
        }

        List<Map<String, Object>> fraudLogs = getFraudLogsByAccount(accNo);

        if (fraudLogs == null || fraudLogs.isEmpty()) {
            return buildResponse(sessionId, userId, question,
                    "No fraud detected for this account.",
                    "Fraud Analysis");
        }

        String analysisPrompt = """
Analyze the fraud based on data.

Give output in this format:

Fraud: YES or NO
Risk Score: (0-100)
Reason:
- short reason 1
- short reason 2

DATA:
%s
""".formatted(fraudLogs.toString());

        String analysis = ollamaClient.generateResponse(analysisPrompt);

        if (securityFilter.isMalicious(question)) {
            return buildResponse(sessionId, userId, question,
                    "⚠️ Your query contains restricted or unsafe instructions.",
                    "Security");
        }

        String policy = bedrockClient.queryKnowledgeBase(question);

//        String finalAnswer = """
//Fraud Analysis:
//%s
//
//Policy Reference:
//%s
//""".formatted(analysis, policy);

        String finalAnswer = """
Fraud Analysis:
%s
""".formatted(analysis);

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
                .suggestions(getSuggestions(answer))
                .build();
    }

    private List<String> getSuggestions(String answer) {

        if (answer == null || answer.isBlank()) {
            return List.of(
                    "Show fraud transactions",
                    "View fraud insights",
                    "Check blocked accounts"
            );
        }

        String a = answer.toLowerCase();

        // 🚨 FRAUD RELATED RESPONSE
        if (a.contains("fraud") || a.contains("risk score")) {
            return List.of(
                    "Why is this marked as fraud?",
                    "Show transactions for this account",
                    "Check blocked accounts",
                    "View fraud insights"
            );
        }

        // 💳 TRANSACTION RESPONSE
        if (a.contains("transaction") || a.contains("amount")) {
            return List.of(
                    "Show failed transactions",
                    "Show successful transactions",
                    "Check fraud for this account",
                    "View fraud patterns"
            );
        }

        // 🚫 BLOCKED ACCOUNT RESPONSE
        if (a.contains("blocked")) {
            return List.of(
                    "Why is this account blocked?",
                    "Show transactions for this account",
                    "Check fraud status",
                    "View fraud insights"
            );
        }

        // 📊 INSIGHTS RESPONSE
        if (a.contains("fraud insights") || a.contains("distribution")) {
            return List.of(
                    "Show fraud transactions",
                    "Show top fraud patterns",
                    "Check blocked accounts",
                    "View detailed transactions"
            );
        }

        // 🔍 PATTERNS RESPONSE
        if (a.contains("reason") || a.contains("pattern")) {
            return List.of(
                    "Show fraud transactions",
                    "Check similar fraud cases",
                    "View fraud insights",
                    "Show blocked accounts"
            );
        }

        // 🧠 KNOWLEDGE BASE RESPONSE
        if (a.contains("aml") || a.contains("rule") || a.contains("guideline")) {
            return List.of(
                    "Show fraud transactions",
                    "Give fraud examples",
                    "Explain risk scoring",
                    "Show suspicious patterns"
            );
        }

        // ❌ NO DATA / FALLBACK RESPONSE
        if (a.contains("no data") || a.contains("not found") || a.contains("no relevant")) {
            return List.of(
                    "Show fraud transactions",
                    "View fraud insights",
                    "Check blocked accounts",
                    "Show recent transactions"
            );
        }

        // 🌟 DEFAULT SMART SUGGESTIONS
        return List.of(
                "Show fraud transactions",
                "Show failed transactions",
                "View fraud insights",
                "Check blocked accounts"
        );
    }

    // ===============================
    //  RESULT FORMAT
    // ===============================
//    private String convertToReadable(List<Map<String, Object>> result) {
//
//        String prompt = """
//Convert database result into simple readable lines.
//Do not use "Answer:" or "Details:".
//Keep output clean and short.
//
//%s
//""".formatted(result.toString());
//
//        return ollamaClient.generateResponse(prompt);
//    }

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

        // ===============================
        // 🔥 STEP 1: CLEAN QUERY (NO HISTORY)
        // ===============================
        String cleanQuery = question;

        // Normalize important keywords
        cleanQuery = cleanQuery.replace("ATO", "Account Takeover");
        cleanQuery = cleanQuery.replace("aml", "anti money laundering");
        cleanQuery = cleanQuery.replace("kyc", "know your customer");

        System.out.println("KB Query: " + cleanQuery);

        // ===============================
        // 🔒 STEP 2: SECURITY CHECK
        // ===============================
        if (securityFilter.isMalicious(cleanQuery)) {
            return "⚠️ Unsafe request detected.";
        }

        // ===============================
        // 🚀 STEP 3: DIRECT KB CALL (NO PROMPT)
        // ===============================
        String ragAnswer = bedrockClient.queryKnowledgeBase(cleanQuery);

        // ===============================
        // 🛑 STEP 4: EMPTY / BAD RESPONSE FILTER
        // ===============================
        if (ragAnswer == null
                || ragAnswer.trim().isEmpty()
                || ragAnswer.toLowerCase().contains("no relevant")
                || ragAnswer.toLowerCase().contains("not found")
                || ragAnswer.length() < 20) {

            return "No relevant information found.";
        }

        // ===============================
        // 🔒 STEP 5: OUTPUT SECURITY
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

    private List<Map<String, Object>> getFraudLogsByAccount(String accountNumber) {

        String sql = """
    SELECT *
    FROM transaction_logs
    WHERE account_to = ?
       OR account_from = ?
    """;

        return databaseQueryService.executeQuery(sql, accountNumber, accountNumber);
    }

    private List<Map<String, Object>> getFraudTransactions() {

        String sql = """
    SELECT 
        transaction_id,
        account_from,
        account_to,
        amount,
        status,
        reason
    FROM transaction_logs
    WHERE status IN ('FRAUD', 'ALERT')
    ORDER BY amount DESC
    LIMIT 5
    """;

        return databaseQueryService.executeQuery(sql);
    }

    private AssistantResponse handleFraudExplanation(String sessionId, String userId, String question) {

        String accNo = extractAccountNumber(normalize(question));

        if (accNo == null) {
            return buildResponse(sessionId, userId, question,
                    "Please provide a valid account number.",
                    "Validation");
        }

        List<Map<String, Object>> logs = getFraudLogsByAccount(accNo);

        if (logs == null || logs.isEmpty()) {
            return buildResponse(sessionId, userId, question,
                    "No fraud-related transactions found for this account.",
                    "Fraud Analysis");
        }

        // 🔥 Extract reasons
        StringBuilder reasons = new StringBuilder();

        logs.stream()
                .limit(5)
                .forEach(log -> {
                    Object reason = log.get("reason");
                    if (reason != null) {
                        reasons.append("• ").append(reason.toString()).append("\n");
                    }
                });

        String finalAnswer = """
Fraud detected due to following reasons:

%s
""".formatted(reasons.toString());

        return buildResponse(sessionId, userId, question, finalAnswer, "PostgreSQL Database");
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





}