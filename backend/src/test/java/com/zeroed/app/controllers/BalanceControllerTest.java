package com.zeroed.app.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.zeroed.app.models.Group;
import com.zeroed.app.models.GroupMembership;
import com.zeroed.app.models.GroupMembershipId;
import com.zeroed.app.models.User;
import com.zeroed.app.models.UserRole;
import com.zeroed.app.repositories.ExpenseRepository;
import com.zeroed.app.repositories.GroupMembershipRepository;
import com.zeroed.app.repositories.GroupRepository;
import com.zeroed.app.repositories.UserRepository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:balance-tests;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false",
    "spring.session.jdbc.initialize-schema=always"
})
class BalanceControllerTest {
    private static final FrankfurterStub RATES = new FrankfurterStub();

    @DynamicPropertySource
    static void rateProperties(DynamicPropertyRegistry registry) {
        registry.add("zeroed.exchange-rates.base-url", RATES::baseUrl);
    }

    @AfterAll
    static void stopRates() { RATES.close(); }

    private static final String PASSWORD = "balance-test-password";
    private static final String HASH = new BCryptPasswordEncoder().encode(PASSWORD);

    @LocalServerPort private int port;
    @Autowired private ObjectMapper json;
    @Autowired private UserRepository users;
    @Autowired private GroupRepository groups;
    @Autowired private GroupMembershipRepository memberships;
    @Autowired private ExpenseRepository expenses;
    @Autowired private JdbcTemplate jdbc;

    private User[] people;
    private Group group;
    private HttpClient client;

    @BeforeEach
    void setUp() throws Exception {
        expenses.deleteAll();
        memberships.deleteAll();
        groups.deleteAll();
        users.deleteAll();
        people = new User[6];
        for (int i = 0; i < people.length; i++) {
            char name = (char) ('a' + i);
            people[i] = users.saveAndFlush(new User(name + "@example.com", HASH));
        }
        group = groups.saveAndFlush(new Group("Trip", people[0], Currency.getInstance("CAD")));
        for (int i = 0; i < 5; i++) memberships.saveAndFlush(new GroupMembership(people[i], group));
        client = signedIn(people[0]);
    }

    @Test
    void emptyGroupReturnsCurrentPublicRosterAndZeroBaseCurrencyLedger() throws Exception {
        JsonNode actual = balances(signedIn(people[4]), group);
        assertEquals(group.getId().longValue(), actual.get("groupId").longValue());
        assertEquals(rosterProfiles(0, 1, 2, 3, 4), profiles(actual.get("members")));
        assertEquals(List.of("CAD"), currencyCodes(actual));
        assertCurrency(actual, "CAD", new long[5], new long[5], 2, new long[][]{});
        assertTimestamp(actual);
    }

    @Test
    void convertsCurrenciesAndKeepsHistoricalParticipantsIsolatedByGroup() throws Exception {
        createExpense(group, "CAD", "10.01", people[2], people[1], people[0]);
        createExpense(group, "JPY", "3", people[0], people[2]);
        Group other = groups.saveAndFlush(new Group("Other", people[0], Currency.getInstance("CAD")));
        memberships.saveAndFlush(new GroupMembership(people[0], other));
        memberships.saveAndFlush(new GroupMembership(people[5], other));
        createExpense(other, "EUR", "99.00", people[5], people[0]);
        memberships.deleteById(new GroupMembershipId(people[1].getId(), group.getId()));
        memberships.deleteById(new GroupMembershipId(people[2].getId(), group.getId()));

        JsonNode actual = balances(signedIn(people[4]), group);
        assertEquals(rosterProfiles(0, 1, 2, 3, 4), profiles(actual.get("members")));
        assertCurrency(actual, "CAD", new long[]{3, 0, 1001, 0, 0}, new long[]{501, 500, 3, 0, 0}, 2,
                new long[][]{{1, 2, 500}, {0, 2, 498}});
        assertEquals(List.of("CAD"), currencyCodes(actual));
        assertFalse(actual.toString().contains("f@example.com"));
        assertTimestamp(actual);
    }

    @Test
    void reprioritizesLargestRemainingBalancesAndUsesIdTiesDeterministically() throws Exception {
        createExpense(group, "CAD", "10.00", people[2], people[0]);
        createExpense(group, "CAD", "8.00", people[3], people[1]);
        createExpense(group, "CAD", "1.00", people[0], people[4]);
        createExpense(group, "CAD", "2.00", people[1], people[4]);
        JsonNode first = balances(client, group);
        assertCurrency(first, "CAD", new long[]{100, 200, 1000, 800, 0}, new long[]{1000, 800, 0, 0, 300}, 2,
                new long[][]{{0, 2, 900}, {1, 3, 600}, {4, 3, 200}, {4, 2, 100}});
        JsonNode repeated = balances(client, group);
        assertEquals(withoutTimestamp(first), withoutTimestamp(repeated));

        setUp();
        createExpense(group, "CAD", "5.00", people[2], people[0]);
        createExpense(group, "CAD", "5.00", people[3], people[1]);
        JsonNode ties = balances(client, group);
        assertCurrency(ties, "CAD", new long[]{0, 0, 500, 500, 0}, new long[]{500, 500, 0, 0, 0}, 2,
                new long[][]{{0, 2, 500}, {1, 3, 500}});
    }

    @ParameterizedTest
    @CsvSource({"CAD,0.01,2", "JPY,1,0", "KWD,0.001,3", "CLF,0.0001,4"})
    void formatsPrecisionAndIncludesZeroShareParticipants(String currency, String amount, int exponent) throws Exception {
        group = groups.saveAndFlush(new Group("Precision", people[0], Currency.getInstance(currency)));
        for (int i = 0; i < 5; i++) memberships.saveAndFlush(new GroupMembership(people[i], group));
        createExpense(group, currency, amount, people[2], people[1], people[0]);
        long minor = 1;
        JsonNode actual = balances(client, group);
        assertCurrency(actual, currency, new long[]{0, 0, minor, 0, 0}, new long[]{minor, 0, 0, 0, 0}, exponent,
                new long[][]{{0, 2, minor}});
        assertEquals(rosterProfiles(0, 1, 2, 3, 4), profiles(actual.get("members")));
    }

    @Test
    void supportsTotalsBeyondExpenseLimitAndRetainsSettledCurrency() throws Exception {
        for (int i = 0; i < 2; i++) createExpense(group, "CAD", "999999999.99", people[0], people[1]);
        JsonNode outstanding = balances(client, group);
        assertCurrency(outstanding, "CAD", new long[]{199999999998L, 0, 0, 0, 0},
                new long[]{0, 199999999998L, 0, 0, 0}, 2,
                new long[][]{{1, 0, 199999999998L}});
        for (int i = 0; i < 2; i++) createExpense(group, "CAD", "999999999.99", people[1], people[0]);
        JsonNode settled = balances(client, group);
        assertCurrency(settled, "CAD", new long[]{199999999998L, 199999999998L, 0, 0, 0},
                new long[]{199999999998L, 199999999998L, 0, 0, 0}, 2, new long[][]{});
    }

    @Test
    void reflectsSuccessfulAndRejectedEditsAndDeletionOnNextRead() throws Exception {
        JsonNode created = createExpense(group, "CAD", "10.01", people[1], people[0], people[1]);
        long expenseId = created.get("id").longValue();
        JsonNode original = balances(client, group);
        assertCurrency(original, "CAD", new long[]{0, 1001, 0, 0, 0}, new long[]{501, 500, 0, 0, 0}, 2,
                new long[][]{{0, 1, 501}});

        ObjectNode patch = json.createObjectNode().put("amount", "12.01").put("currency", "EUR")
                .put("paidByUserId", people[2].getId());
        patch.putArray("participantIds").add(people[2].getId()).add(people[0].getId());
        var updated = mutate(client, "PATCH", "/api/expenses/" + expenseId, json.writeValueAsString(patch));
        assertEquals(200, updated.statusCode(), updated.body());
        JsonNode afterEdit = balances(client, group);
        assertCurrency(afterEdit, "CAD", new long[]{0, 0, 1922, 0, 0}, new long[]{961, 0, 961, 0, 0}, 2,
                new long[][]{{0, 2, 961}});
        var rejected = mutate(client, "PATCH", "/api/expenses/" + expenseId, "{\"amount\":\"0.00\"}");
        assertEquals(400, rejected.statusCode(), rejected.body());
        assertEquals(withoutTimestamp(afterEdit), withoutTimestamp(balances(client, group)));
        var deleted = mutate(client, "DELETE", "/api/expenses/" + expenseId, null);
        assertEquals(204, deleted.statusCode(), deleted.body());
        JsonNode empty = balances(client, group);
        assertEquals(List.of("CAD"), currencyCodes(empty));
        assertEquals(rosterProfiles(0, 1, 2, 3, 4), profiles(empty.get("members")));
    }

    @Test
    void enforcesMembershipWithoutAdminBypassAndDoesNotRequireCsrfForGet() throws Exception {
        HttpClient anonymous = newClient();
        assertEquals(401, get(anonymous, balancePath(group)).statusCode());
        for (UserRole role : new UserRole[]{UserRole.REGULAR, UserRole.ADMIN}) {
            jdbc.update("update users set role = ? where id = ?", role.name(), people[5].getId());
            HttpClient outsider = signedIn(people[5]);
            assertEquals(404, get(outsider, balancePath(group)).statusCode());
            assertEquals(404, get(outsider, "/api/groups/9223372036854775807/balances").statusCode());
        }
        memberships.deleteById(new GroupMembershipId(people[4].getId(), group.getId()));
        assertEquals(404, get(signedIn(people[4]), balancePath(group)).statusCode());
        assertEquals(200, get(client, balancePath(group)).statusCode());
    }

    private JsonNode createExpense(Group target, String currency, String amount, User payer, User... participants) throws Exception {
        ObjectNode request = json.createObjectNode().put("description", "Expense").put("amount", amount)
                .put("currency", currency).put("paidByUserId", payer.getId()).put("date", "2030-01-01");
        var participantIds = request.putArray("participantIds");
        for (User participant : participants) participantIds.add(participant.getId());
        var response = mutate(client, "POST", expensePath(target), json.writeValueAsString(request));
        assertEquals(201, response.statusCode(), response.body());
        return json.readTree(response.body());
    }

    private void assertCurrency(JsonNode actual, String code, long[] paid, long[] owed, int exponent, long[][] transfers) throws Exception {
        JsonNode section = actual.get("currencies").get(currencyCodes(actual).indexOf(code));
        assertEquals(code, section.get("currency").asString());
        List<Object> expectedMembers = new ArrayList<>();
        BigInteger sum = BigInteger.ZERO;
        for (int i = 0; i < 5; i++) {
            BigInteger p = BigInteger.valueOf(paid[i]);
            BigInteger o = BigInteger.valueOf(owed[i]);
            BigInteger net = p.subtract(o);
            sum = sum.add(net);
            expectedMembers.add(Map.of("userId", people[i].getId(), "paid", format(p, exponent),
                    "owed", format(o, exponent), "net", format(net, exponent)));
        }
        assertEquals(json.readTree(json.writeValueAsString(expectedMembers)), section.get("members"));
        assertEquals("0", sum.toString());
        List<Object> expectedTransfers = new ArrayList<>();
        for (long[] transfer : transfers) {
            expectedTransfers.add(Map.of("from", profile((int) transfer[0]), "to", profile((int) transfer[1]),
                    "amount", format(BigInteger.valueOf(transfer[2]), exponent)));
        }
        assertEquals(json.readTree(json.writeValueAsString(expectedTransfers)), section.get("transfers"));
        // Applying emitted transfers to the requested ledger must settle every member.
        BigInteger[] remaining = new BigInteger[5];
        for (int i = 0; i < 5; i++) remaining[i] = BigInteger.valueOf(paid[i]).subtract(BigInteger.valueOf(owed[i]));
        for (long[] transfer : transfers) {
            BigInteger amount = BigInteger.valueOf(transfer[2]);
            remaining[(int) transfer[0]] = remaining[(int) transfer[0]].add(amount);
            remaining[(int) transfer[1]] = remaining[(int) transfer[1]].subtract(amount);
        }
        for (BigInteger balance : remaining) assertEquals(BigInteger.ZERO, balance);
    }

    private static String format(BigInteger amount, int exponent) {
        return new BigDecimal(amount, exponent).toPlainString();
    }

    private List<Map<String, Object>> rosterProfiles(int... indices) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (int index : indices) result.add(profile(index));
        return result;
    }

    private Map<String, Object> profile(int index) {
        return Map.of("id", people[index].getId(), "email", people[index].getEmail());
    }

    private List<Map<String, Object>> profiles(JsonNode node) throws Exception {
        List<Map<String, Object>> result = new ArrayList<>();
        for (JsonNode profile : node) result.add(Map.of("id", profile.get("id").longValue(), "email", profile.get("email").asString()));
        return result;
    }

    private List<String> currencyCodes(JsonNode body) {
        List<String> result = new ArrayList<>();
        for (JsonNode currency : body.get("currencies")) result.add(currency.get("currency").asString());
        return result;
    }

    private JsonNode balances(HttpClient http, Group target) throws Exception {
        var before = Instant.now();
        var response = get(http, balancePath(target));
        var after = Instant.now();
        assertEquals(200, response.statusCode(), response.body());
        JsonNode result = json.readTree(response.body());
        Instant generatedAt = Instant.parse(result.get("generatedAt").asString());
        assertFalse(generatedAt.isBefore(before));
        assertFalse(generatedAt.isAfter(after));
        return result;
    }

    private JsonNode withoutTimestamp(JsonNode body) {
        ObjectNode copy = ((ObjectNode) body).deepCopy();
        copy.remove("generatedAt");
        return copy;
    }

    private void assertTimestamp(JsonNode body) {
        assertTrue(Instant.parse(body.get("generatedAt").asString()) != null);
    }

    private String balancePath(Group target) { return "/api/groups/" + target.getId() + "/balances"; }
    private String expensePath(Group target) { return "/api/groups/" + target.getId() + "/expenses"; }
    private URI uri(String path) { return URI.create("http://localhost:" + port + path); }

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
