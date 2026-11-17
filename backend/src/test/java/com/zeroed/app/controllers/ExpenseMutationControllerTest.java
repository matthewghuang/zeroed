package com.zeroed.app.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.zeroed.app.models.Group;
import com.zeroed.app.models.GroupMembership;
import com.zeroed.app.models.GroupMembershipId;
import com.zeroed.app.models.User;
import com.zeroed.app.repositories.ExpenseRepository;
import com.zeroed.app.repositories.GroupMembershipRepository;
import com.zeroed.app.repositories.GroupRepository;
import com.zeroed.app.repositories.UserRepository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:expense-mutation-tests;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false",
    "spring.session.jdbc.initialize-schema=always"
})
class ExpenseMutationControllerTest {
    private static final String PASSWORD = "expense-mutation-password";
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
    void updatesAllEditableFieldsAndRecalculatesThePersistedLedger() throws Exception {
        JsonNode original = createExpense(validRequest().put("amount", "10.01"));
        long id = original.get("id").longValue();
        Instant previousUpdate = Instant.parse("2020-01-01T00:00:00Z");
        jdbc.update("update expenses set updated_at = ? where id = ?", Timestamp.from(previousUpdate), id);
        assertEquals(-501L, net(creator, "CAD"));
        assertEquals(501L, net(payer, "CAD"));

        ObjectNode patch = json.createObjectNode().put("description", "  Corrected tickets  ")
                .put("amount", "12.01").put("currency", "EUR").put("date", "2030-02-28")
                .put("paidByUserId", third.getId());
        patch.putArray("participantIds").add(third.getId()).add(creator.getId());
        var response = mutate(client, "PATCH", path(original), json.writeValueAsString(patch));
        assertEquals(200, response.statusCode(), response.body());
        JsonNode updated = json.readTree(response.body());
        assertEquals(original.get("id"), updated.get("id"));
        assertEquals(original.get("groupId"), updated.get("groupId"));
        assertEquals(original.get("createdBy"), updated.get("createdBy"));
        assertEquals(original.get("createdAt"), updated.get("createdAt"));
        assertTrue(Instant.parse(updated.get("updatedAt").asString()).isAfter(previousUpdate));
        assertEquals("Corrected tickets", updated.get("description").asString());
        assertEquals("12.01", updated.get("amount").asString());
        assertEquals("EUR", updated.get("currency").asString());
        assertEquals("2030-02-28", updated.get("date").asString());
        assertEquals(third.getId().longValue(), updated.at("/paidBy/id").longValue());
        assertEquals(json.readTree("""
                [{"userId": %d, "amount": "6.01"}, {"userId": %d, "amount": "6.00"}]
                """.formatted(creator.getId(), third.getId())), updated.get("splits"));
        assertEquals(updated, read(client, path(original)));
        assertEquals(updated, read(client, expensePath()).get(0));
        assertEquals(2, jdbc.queryForObject("select count(*) from expense_splits where expense_id = ?", Integer.class, id));
        assertEquals(1201L, jdbc.queryForObject("select sum(amount_minor) from expense_splits where expense_id = ?", Long.class, id));
        assertEquals(0L, net(creator, "CAD"));
        assertEquals(0L, net(payer, "CAD"));
        assertEquals(-601L, net(creator, "EUR"));
        assertEquals(0L, net(payer, "EUR"));
        assertEquals(601L, net(third, "EUR"));
    }

    @Test
    void partialUpdatesPreserveOmittedFormerPayerParticipantsAndDate() throws Exception {
        JsonNode original = createExpense(validRequest());
        memberships.deleteById(new GroupMembershipId(payer.getId(), group.getId()));
        var response = mutate(client, "PATCH", path(original), "{\"description\":\"Corrected dinner\"}");
        assertEquals(200, response.statusCode(), response.body());
        JsonNode updated = json.readTree(response.body());
        for (String field : new String[] {"amount", "currency", "date", "paidBy", "splits", "createdBy", "createdAt"}) {
            assertEquals(original.get(field), updated.get(field), field);
        }
        response = mutate(client, "PATCH", path(original), "{\"amount\":\"12.01\"}");
        assertEquals(200, response.statusCode(), response.body());
        updated = json.readTree(response.body());
        assertEquals(original.get("paidBy"), updated.get("paidBy"));
        assertEquals("6.01", updated.at("/splits/0/amount").asString());
        assertEquals("6.00", updated.at("/splits/1/amount").asString());
        assertEquals(601L, net(payer, "CAD"));
        assertEquals(-601L, net(creator, "CAD"));
    }

    @Test
    void currencyOnlyUpdatesRetainNumericAmountAndNeverRoundIt() throws Exception {
        JsonNode original = createExpense(validRequest());
        var response = mutate(client, "PATCH", path(original), "{\"currency\":\"JPY\"}");
        assertEquals(200, response.statusCode(), response.body());
        assertEquals("10", json.readTree(response.body()).get("amount").asString());
        response = mutate(client, "PATCH", path(original), "{\"currency\":\"KWD\"}");
        assertEquals(200, response.statusCode(), response.body());
        assertEquals("10.000", json.readTree(response.body()).get("amount").asString());

        JsonNode fractional = createExpense(validRequest().put("amount", "10.01"));
        response = mutate(client, "PATCH", path(fractional), "{\"currency\":\"JPY\"}");
        assertEquals(400, response.statusCode(), response.body());
        assertTrue(json.readTree(response.body()).get("fields").has("amount"));
        assertEquals(fractional, read(client, path(fractional)));
    }

    @Test
    void onlyCreatorCanModifyAnExpenseEvenWhenOtherUsersAreAdmins() throws Exception {
        JsonNode original = createExpense(validRequest());
        jdbc.update("update users set role = 'ADMIN' where id in (?, ?)", payer.getId(), outsider.getId());
        HttpClient member = signedIn(payer);
        assertEquals(200, get(member, path(original)).statusCode());
        assertEquals(403, mutate(member, "PATCH", path(original), "{\"amount\":\"12.00\"}").statusCode());
        assertEquals(403, mutate(member, "DELETE", path(original), null).statusCode());

        HttpClient nonMember = signedIn(outsider);
        assertEquals(404, get(nonMember, path(original)).statusCode());
        assertEquals(404, mutate(nonMember, "PATCH", path(original), "{\"description\":\"Hidden\"}").statusCode());
        assertEquals(404, mutate(nonMember, "DELETE", path(original), null).statusCode());
        assertEquals(original, read(client, path(original)));
    }

    @Test
    void requiresAuthenticationAndCsrfForMutations() throws Exception {
        JsonNode original = createExpense(validRequest());
        HttpClient anonymous = newClient();
        assertEquals(401, get(anonymous, path(original)).statusCode());
        for (String method : new String[] {"PATCH", "DELETE"}) {
            String body = method.equals("PATCH") ? "{\"description\":\"Unauthenticated\"}" : null;
            assertEquals(401, mutate(anonymous, method, path(original), body).statusCode());
            var response = client.send(HttpRequest.newBuilder(uri(path(original)))
                    .header("Content-Type", "application/json")
                    .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body))
                    .build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(403, response.statusCode(), response.body());
        }
        assertEquals(original, read(client, path(original)));
    }

    @Test
    void rejectsEmptyPatchAndExplicitNullRatherThanTreatingThemAsOmittedFields() throws Exception {
        JsonNode original = createExpense(validRequest());
        var empty = mutate(client, "PATCH", path(original), "{}");
        assertEquals(400, empty.statusCode(), empty.body());
        assertTrue(json.readTree(empty.body()).get("fields").has("body"));

        var explicitNull = mutate(client, "PATCH", path(original), "{\"description\":null}");
        assertEquals(400, explicitNull.statusCode(), explicitNull.body());
        assertTrue(json.readTree(explicitNull.body()).get("fields").has("description"));
        assertEquals(original, read(client, path(original)));
    }

    @Test
    void failedSplitPersistenceRollsBackParentAmountsSharesAndTimestamps() throws Exception {
        JsonNode original = createExpense(validRequest());
        jdbc.execute("alter table expense_splits add constraint mutation_failure check (amount_minor <> 601)");
        try {
            var response = mutate(client, "PATCH", path(original), "{\"description\":\"Must roll back\",\"amount\":\"12.01\"}");
            assertEquals(500, response.statusCode(), response.body());
            assertEquals(original, read(client, path(original)));
            assertEquals(-500L, net(creator, "CAD"));
            assertEquals(500L, net(payer, "CAD"));
        } finally {
            jdbc.execute("alter table expense_splits drop constraint mutation_failure");
        }
    }

    @Test
    void deletionRemovesOnlyItsExpenseSplitsAndBalanceContributionPermanently() throws Exception {
        JsonNode deleted = createExpense(validRequest());
        ObjectNode remainingRequest = validRequest().put("amount", "3.00").put("paidByUserId", third.getId());
        remainingRequest.putArray("participantIds").add(creator.getId());
        JsonNode remaining = createExpense(remainingRequest);
        assertEquals(-800L, net(creator, "CAD"));
        assertEquals(500L, net(payer, "CAD"));
        assertEquals(300L, net(third, "CAD"));

        var response = mutate(client, "DELETE", path(deleted), null);
        assertEquals(204, response.statusCode(), response.body());
        assertEquals("", response.body());
        long id = deleted.get("id").longValue();
        assertEquals(0, jdbc.queryForObject("select count(*) from expenses where id = ?", Integer.class, id));
        assertEquals(0, jdbc.queryForObject("select count(*) from expense_splits where expense_id = ?", Integer.class, id));
        assertEquals(json.createArrayNode().add(remaining), read(client, expensePath()));
        assertEquals(-300L, net(creator, "CAD"));
        assertEquals(0L, net(payer, "CAD"));
        assertEquals(300L, net(third, "CAD"));
        assertEquals(404, get(client, path(deleted)).statusCode());
        assertEquals(404, mutate(client, "PATCH", path(deleted), "{\"amount\":\"1.00\"}").statusCode());
        assertEquals(404, mutate(client, "DELETE", path(deleted), null).statusCode());
    }

    private ObjectNode validRequest() {
        ObjectNode request = json.createObjectNode().put("description", "Dinner").put("amount", "10.00")
                .put("currency", "CAD").put("paidByUserId", payer.getId()).put("date", "2030-01-01");
        request.putArray("participantIds").add(creator.getId()).add(payer.getId());
        return request;
    }

    private JsonNode createExpense(ObjectNode request) throws Exception {
        var response = mutate(client, "POST", expensePath(), json.writeValueAsString(request));
        assertEquals(201, response.statusCode(), response.body());
        return json.readTree(response.body());
    }

    private String path(JsonNode expense) { return "/api/expenses/" + expense.get("id").longValue(); }
    private String expensePath() { return "/api/groups/" + group.getId() + "/expenses"; }
    private URI uri(String path) { return URI.create("http://localhost:" + port + path); }

    // Balances are derived, not stored: assert the credit/debit inputs in integer minor units.
    private long net(User user, String currency) {
        long paid = jdbc.queryForObject("select coalesce(sum(amount_minor), 0) from expenses where group_id = ? and paid_by_user_id = ? and currency = ?",
                Long.class, group.getId(), user.getId(), currency);
        long owed = jdbc.queryForObject("select coalesce(sum(s.amount_minor), 0) from expense_splits s join expenses e on e.id = s.expense_id where e.group_id = ? and s.user_id = ? and e.currency = ?",
                Long.class, group.getId(), user.getId(), currency);
        return paid - owed;
    }

    private HttpClient newClient() {
        return HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
    }

    private HttpClient signedIn(User user) throws Exception {
        HttpClient authenticated = newClient();
        var response = mutate(authenticated, "POST", "/api/auth/login",
                json.writeValueAsString(Map.of("email", user.getEmail(), "password", PASSWORD)));
        assertEquals(204, response.statusCode(), response.body());
        return authenticated;
    }

    private JsonNode read(HttpClient http, String path) throws Exception {
        var response = get(http, path);
        assertEquals(200, response.statusCode(), response.body());
        return json.readTree(response.body());
    }

    private HttpResponse<String> get(HttpClient http, String path) throws Exception {
        return http.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> mutate(HttpClient http, String method, String path, String body) throws Exception {
        var csrf = get(http, "/api/auth/csrf");
        assertEquals(200, csrf.statusCode(), csrf.body());
        JsonNode token = json.readTree(csrf.body());
        CookieManager cookies = (CookieManager) http.cookieHandler().orElseThrow();
        String value = cookies.getCookieStore().getCookies().stream()
                .filter(cookie -> cookie.getName().equals("XSRF-TOKEN"))
                .map(cookie -> cookie.getValue()).findFirst().orElse(token.get("token").asString());
        return http.send(HttpRequest.newBuilder(uri(path)).header("Content-Type", "application/json")
                .header(token.get("headerName").asString(), value)
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body))
                .build(), HttpResponse.BodyHandlers.ofString());
    }
}
