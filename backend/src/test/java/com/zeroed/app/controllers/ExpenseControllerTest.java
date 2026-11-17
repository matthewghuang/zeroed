package com.zeroed.app.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.zeroed.app.models.Group;
import com.zeroed.app.models.GroupMembership;
import com.zeroed.app.models.User;
import com.zeroed.app.repositories.ExpenseRepository;
import com.zeroed.app.repositories.GroupMembershipRepository;
import com.zeroed.app.repositories.GroupRepository;
import com.zeroed.app.repositories.UserRepository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:expense-tests;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false",
    "spring.session.jdbc.initialize-schema=always"
})
class ExpenseControllerTest {
    private static final String PASSWORD = "expense-test-password";
    private static final String HASH = new BCryptPasswordEncoder().encode(PASSWORD);

    @LocalServerPort private int port;
    @Autowired private ObjectMapper json;
    @Autowired private UserRepository users;
    @Autowired private GroupRepository groups;
    @Autowired private GroupMembershipRepository memberships;
    @Autowired private ExpenseRepository expenses;
    @Autowired private JdbcTemplate jdbc;

    private User creator;
    private User payer;
    private User third;
    private User outsider;
    private Group group;
    private HttpClient client;

    @BeforeEach
    void setUp() throws Exception {
        expenses.deleteAll();
        memberships.deleteAll();
        groups.deleteAll();
        users.deleteAll();
        creator = users.saveAndFlush(new User("creator@example.com", HASH));
        payer = users.saveAndFlush(new User("payer@example.com", HASH));
        third = users.saveAndFlush(new User("third@example.com", HASH));
        outsider = users.saveAndFlush(new User("outsider@example.com", HASH));
        group = groups.saveAndFlush(new Group("Trip", creator));
        for (User member : new User[] {creator, payer, third}) {
            memberships.saveAndFlush(new GroupMembership(member, group));
        }
        client = signedIn(creator);
    }

    @Test
    void createsAndListsExpenseWithSelectedParticipantsAndExactSplits() throws Exception {
        ObjectNode request = validRequest();
        request.put("description", "  Dinner  ");
        request.put("amount", "10.01");
        request.put("paidByUserId", payer.getId());
        request.put("date", "2030-02-28");
        request.putArray("participantIds").add(third.getId()).add(creator.getId());

        var response = post(client, expensePath(group.getId()), json.writeValueAsString(request));
        assertEquals(201, response.statusCode(), response.body());
        JsonNode body = json.readTree(response.body());
        long id = body.get("id").longValue();
        assertEquals("/api/expenses/" + id, response.headers().firstValue("Location").orElseThrow());
        assertEquals(group.getId().longValue(), body.get("groupId").longValue());
        assertEquals("Dinner", body.get("description").asString());
        assertEquals("10.01", body.get("amount").asString());
        assertEquals("CAD", body.get("currency").asString());
        assertEquals(payer.getId().longValue(), body.at("/paidBy/id").longValue());
        assertEquals(creator.getId().longValue(), body.at("/createdBy/id").longValue());
        assertEquals(json.readTree("""
                {"id": %d, "email": "payer@example.com"}
                """.formatted(payer.getId())), body.get("paidBy"));
        assertEquals(json.readTree("""
                {"id": %d, "email": "creator@example.com"}
                """.formatted(creator.getId())), body.get("createdBy"));
        assertEquals("equal", body.get("splitType").asString());
        assertEquals("2030-02-28", body.get("date").asString());
        assertNotNull(body.get("createdAt").asString());
        assertEquals(body.get("createdAt"), body.get("updatedAt"));
        JsonNode splits = json.readTree("""
                [
                  {"userId": %d, "amount": "5.01"},
                  {"userId": %d, "amount": "5.00"}
                ]
                """.formatted(creator.getId(), third.getId()));
        assertEquals(splits, body.get("splits"));
        assertFalse(response.body().contains("password"));
        assertEquals(1001L, jdbc.queryForObject("select amount_minor from expenses where id = ?", Long.class, id));
        assertEquals(2, jdbc.queryForObject("select count(*) from expense_splits where expense_id = ?", Integer.class, id));
        assertEquals(1001L, jdbc.queryForObject("select sum(amount_minor) from expense_splits where expense_id = ?", Long.class, id));

        Instant createdAt = Instant.parse("2030-03-01T12:00:00Z");
        Instant updatedAt = Instant.parse("2030-03-02T12:00:00Z");
        jdbc.update("update expenses set created_at = ?, updated_at = ? where id = ?",
                Timestamp.from(createdAt), Timestamp.from(updatedAt), id);
        ObjectNode expected = (ObjectNode) body.deepCopy();
        expected.put("createdAt", createdAt.toString());
        expected.put("updatedAt", updatedAt.toString());
        JsonNode result = listExpenses(client, group.getId());
        assertEquals(1, result.size());
        assertEquals(expected, result.get(0));
    }

    @Test
    void membersCanListExpensesTheyDidNotCreatePayOrParticipateIn() throws Exception {
        long expenseId = createExpense(group.getId(), validRequest());
        HttpClient memberOutsideTheExpense = signedIn(third);

        JsonNode result = listExpenses(memberOutsideTheExpense, group.getId());

        assertEquals(1, result.size());
        assertEquals(expenseId, result.get(0).get("id").longValue());
    }

    @Test
    void listsOnlyExpensesFromTheRequestedGroup() throws Exception {
        Group otherGroup = groups.saveAndFlush(new Group("Other trip", creator));
        memberships.saveAndFlush(new GroupMembership(creator, otherGroup));
        createExpense(otherGroup.getId(), validRequest());
        long requestedExpenseId = createExpense(group.getId(), validRequest());

        JsonNode result = listExpenses(client, group.getId());

        assertEquals(1, result.size());
        assertEquals(requestedExpenseId, result.get(0).get("id").longValue());
    }

    @Test
    void listsExpensesByDescendingDateThenCreationTimeThenId() throws Exception {
        // Creation times deliberately disagree with ID order to exercise every sort key.
        long earlierDate = createExpenseAt("2030-02-27", "2030-03-03T00:00:00Z");
        long laterCreation = createExpenseAt("2030-02-28", "2030-03-02T00:00:00Z");
        long earlierCreation = createExpenseAt("2030-02-28", "2030-03-01T00:00:00Z");
        long higherIdWithSameDateAndCreationTime = createExpenseAt("2030-02-28", "2030-03-02T00:00:00Z");

        JsonNode result = listExpenses(client, group.getId());

        List<Long> actualIds = new ArrayList<>();
        for (JsonNode expense : result) {
            actualIds.add(expense.get("id").longValue());
        }
        assertEquals(List.of(higherIdWithSameDateAndCreationTime, laterCreation, earlierCreation, earlierDate),
                actualIds);
    }

    @Test
    void listsAnEmptyArrayForAGroupWithoutExpenses() throws Exception {
        assertEquals(json.createArrayNode(), listExpenses(client, group.getId()));
    }

    @Test
    void requiresAuthenticationToListExpenses() throws Exception {
        var response = get(newClient(), expensePath(group.getId()));
        assertEquals(401, response.statusCode(), response.body());
    }

    @ParameterizedTest(name = "non-member with role {0} cannot list expenses")
    @ValueSource(strings = {"REGULAR", "ADMIN"})
    void hidesExpenseListsFromNonMembersRegardlessOfRole(String role) throws Exception {
        createExpense(group.getId(), validRequest());
        jdbc.update("update users set role = ? where id = ?", role, outsider.getId());
        HttpClient nonMember = signedIn(outsider);

        var existingGroup = get(nonMember, expensePath(group.getId()));
        var missingGroup = get(nonMember, expensePath(Long.MAX_VALUE));

        assertEquals(404, existingGroup.statusCode(), existingGroup.body());
        assertEquals(404, missingGroup.statusCode(), missingGroup.body());
    }

    @Test
    void returnsNotFoundWhenListingExpensesForAMissingGroup() throws Exception {
        var response = get(client, expensePath(Long.MAX_VALUE));
        assertEquals(404, response.statusCode(), response.body());
    }

    @ParameterizedTest
    @MethodSource("currencies")
    void defaultsToAllCurrentMembersWithExactCurrencySplits(String currency, String amount,
                                                            long minor, int exponent, String first,
                                                            String second) throws Exception {
        ObjectNode request = validRequest().put("currency", currency).put("amount", amount);
        request.remove("participantIds");
        LocalDate before = LocalDate.now(ZoneOffset.UTC);
        var response = post(client, expensePath(group.getId()), json.writeValueAsString(request));
        LocalDate after = LocalDate.now(ZoneOffset.UTC);
        assertEquals(201, response.statusCode(), response.body());
        JsonNode body = json.readTree(response.body());
        assertEquals(amount, body.get("amount").asString());
        assertEquals(currency, body.get("currency").asString());
        assertEquals(creator.getId().longValue(), body.at("/paidBy/id").longValue());
        LocalDate date = LocalDate.parse(body.get("date").asString());
        assertTrue(date.equals(before) || date.equals(after));
        JsonNode splits = json.readTree("""
                [
                  {"userId": %d, "amount": "%s"},
                  {"userId": %d, "amount": "%s"},
                  {"userId": %d, "amount": "%s"}
                ]
                """.formatted(creator.getId(), first, payer.getId(), second, third.getId(), second));
        assertEquals(splits, body.get("splits"));
        long id = body.get("id").longValue();
        assertEquals(minor, jdbc.queryForObject("select amount_minor from expenses where id = ?", Long.class, id));
        assertEquals(exponent, jdbc.queryForObject("select currency_exponent from expenses where id = ?", Integer.class, id));
        assertEquals(minor, jdbc.queryForObject("select sum(amount_minor) from expense_splits where expense_id = ?", Long.class, id));
        JsonNode result = listExpenses(client, group.getId());
        assertEquals(1, result.size());
        assertEquals(id, result.get(0).get("id").longValue());
        assertEquals(splits, result.get(0).get("splits"));
    }

    static Stream<Arguments> currencies() {
        return Stream.of(
                Arguments.of("CAD", "0.01", 1L, 2, "0.01", "0.00"),
                Arguments.of("JPY", "1", 1L, 0, "1", "0"),
                Arguments.of("KWD", "0.001", 1L, 3, "0.001", "0.000"),
                Arguments.of("CLF", "0.0001", 1L, 4, "0.0001", "0.0000"),
                Arguments.of("CAD", "999999999.99", 99999999999L, 2, "333333333.33", "333333333.33"));
    }

    @ParameterizedTest
    @MethodSource("invalidFields")
    void rejectsInvalidFieldsWithoutPersisting(String field, String value) throws Exception {
        ObjectNode request = validRequest();
        if (value == null) request.remove(field);
        else request.set(field, json.readTree(value));
        var response = post(client, expensePath(group.getId()), json.writeValueAsString(request));
        assertEquals(400, response.statusCode(), response.body());
        JsonNode error = json.readTree(response.body());
        assertEquals("validation_failed", error.get("error").asString());
        assertTrue(error.get("fields").has(field), response.body());
        assertNoExpenses();
    }

    static Stream<Arguments> invalidFields() {
        return Stream.of(
                Arguments.of("description", null), Arguments.of("description", "null"),
                Arguments.of("description", "\"   \""), Arguments.of("description", "42"),
                Arguments.of("description", "\"" + "a".repeat(201) + "\""),
                Arguments.of("amount", null), Arguments.of("amount", "null"),
                Arguments.of("amount", "0.01"), Arguments.of("amount", "\"0.00\""),
                Arguments.of("amount", "\"-1.00\""), Arguments.of("amount", "\"1.0\""),
                Arguments.of("amount", "\"1.001\""), Arguments.of("amount", "\"1e2\""),
                Arguments.of("amount", "\"1000000000.00\""),
                Arguments.of("currency", null), Arguments.of("currency", "\"cad\""),
                Arguments.of("currency", "\"ZZZ\""), Arguments.of("currency", "\"XXX\""),
                Arguments.of("date", "null"), Arguments.of("date", "\"2026-02-30\""),
                Arguments.of("date", "\"2026-1-01\""), Arguments.of("date", "\"2026-01-01T00:00:00Z\""),
                Arguments.of("paidByUserId", "null"), Arguments.of("paidByUserId", "1.0"),
                Arguments.of("paidByUserId", "\"1\""), Arguments.of("paidByUserId", "999999999999999999999"),
                Arguments.of("participantIds", "null"), Arguments.of("participantIds", "[]"),
                Arguments.of("participantIds", "[1,1]"), Arguments.of("participantIds", "[null]"),
                Arguments.of("participantIds", "[1.0]"), Arguments.of("participantIds", "[\"1\"]"));
    }

    @Test
    void rejectsNonMembersAsPayerAndParticipantsWithoutLeakingAccounts() throws Exception {
        for (long id : new long[] {outsider.getId(), Long.MAX_VALUE}) {
            ObjectNode request = validRequest().put("paidByUserId", id);
            request.putArray("participantIds").add(id);
            var response = post(client, expensePath(group.getId()), json.writeValueAsString(request));
            assertEquals(400, response.statusCode(), response.body());
            JsonNode error = json.readTree(response.body());
            assertEquals("validation_failed", error.get("error").asString());
            assertTrue(error.get("fields").has("paidByUserId"));
            assertTrue(error.get("fields").has("participantIds"));
        }
        assertNoExpenses();
    }

    @Test
    void hidesGroupsFromNonMembersIncludingAdmins() throws Exception {
        jdbc.update("update users set role = 'ADMIN' where id = ?", outsider.getId());
        HttpClient admin = signedIn(outsider);
        for (long groupId : new long[] {group.getId(), Long.MAX_VALUE}) {
            var response = post(admin, expensePath(groupId), json.writeValueAsString(validRequest()));
            assertEquals(404, response.statusCode(), response.body());
        }
        assertNoExpenses();
    }

    @Test
    void requiresAuthenticationAndCsrf() throws Exception {
        assertEquals(401, post(newClient(), expensePath(group.getId()),
                json.writeValueAsString(validRequest())).statusCode());
        var noCsrf = client.send(HttpRequest.newBuilder(uri(expensePath(group.getId())))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(
                        json.writeValueAsString(validRequest()))).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(403, noCsrf.statusCode());
        assertNoExpenses();
    }

    @Test
    void rejectsImmutableAndUnknownFields() throws Exception {
        for (String field : new String[] {"id", "groupId", "createdBy", "splitType", "splits", "extra"}) {
            ObjectNode request = validRequest().put(field, "injected");
            var response = post(client, expensePath(group.getId()), json.writeValueAsString(request));
            assertEquals(400, response.statusCode(), response.body());
            assertEquals("unsupported_field", json.readTree(response.body()).get("error").asString());
        }
        assertNoExpenses();
    }

    @Test
    void rejectsMalformedAndNonObjectBodies() throws Exception {
        for (String body : new String[] {"", "{", "[]", "null", "42"}) {
            var response = post(client, expensePath(group.getId()), body);
            assertEquals(400, response.statusCode(), response.body());
            assertEquals("validation_failed", json.readTree(response.body()).get("error").asString());
        }
        assertNoExpenses();
    }

    @Test
    void rollsBackExpenseWhenSplitPersistenceFails() throws Exception {
        jdbc.execute("alter table expense_splits add constraint simulated_failure check (amount_minor <> 1)");
        try {
            ObjectNode request = validRequest().put("amount", "0.01");
            var response = post(client, expensePath(group.getId()), json.writeValueAsString(request));
            assertEquals(500, response.statusCode(), response.body());
            assertNoExpenses();
        } finally {
            jdbc.execute("alter table expense_splits drop constraint simulated_failure");
        }
    }

    private long createExpense(long groupId, ObjectNode request) throws Exception {
        var response = post(client, expensePath(groupId), json.writeValueAsString(request));
        assertEquals(201, response.statusCode(), response.body());
        return json.readTree(response.body()).get("id").longValue();
    }

    private long createExpenseAt(String date, String createdAt) throws Exception {
        long expenseId = createExpense(group.getId(), validRequest().put("date", date));
        jdbc.update("update expenses set created_at = ? where id = ?",
                Timestamp.from(Instant.parse(createdAt)), expenseId);
        return expenseId;
    }

    private JsonNode listExpenses(HttpClient http, long groupId) throws Exception {
        var response = get(http, expensePath(groupId));
        assertEquals(200, response.statusCode(), response.body());
        JsonNode result = json.readTree(response.body());
        assertTrue(result.isArray(), response.body());
        return result;
    }

    private ObjectNode validRequest() {
        ObjectNode request = json.createObjectNode().put("description", "Dinner")
                .put("amount", "10.00").put("currency", "CAD");
        request.putArray("participantIds").add(creator.getId());
        return request;
    }

    private void assertNoExpenses() {
        assertEquals(0, expenses.count());
        assertEquals(0, jdbc.queryForObject("select count(*) from expense_splits", Integer.class));
    }

    private HttpClient signedIn(User user) throws Exception {
        HttpClient authenticated = newClient();
        var response = post(authenticated, "/api/auth/login", json.writeValueAsString(
                Map.of("email", user.getEmail(), "password", PASSWORD)));
        assertEquals(204, response.statusCode(), response.body());
        return authenticated;
    }

    private HttpClient newClient() {
        return HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
    }

    private HttpResponse<String> post(HttpClient http, String path, String body) throws Exception {
        var csrf = http.send(HttpRequest.newBuilder(uri("/api/auth/csrf")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, csrf.statusCode(), csrf.body());
        JsonNode token = json.readTree(csrf.body());
        CookieManager cookies = (CookieManager) http.cookieHandler().orElseThrow();
        String value = cookies.getCookieStore().getCookies().stream()
                .filter(cookie -> cookie.getName().equals("XSRF-TOKEN"))
                .map(cookie -> cookie.getValue()).findFirst().orElse(token.get("token").asString());
        return http.send(HttpRequest.newBuilder(uri(path)).header("Content-Type", "application/json")
                .header(token.get("headerName").asString(), value)
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(HttpClient http, String path) throws Exception {
        return http.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private String expensePath(long groupId) { return "/api/groups/" + groupId + "/expenses"; }
    private URI uri(String path) { return URI.create("http://localhost:" + port + path); }
}
