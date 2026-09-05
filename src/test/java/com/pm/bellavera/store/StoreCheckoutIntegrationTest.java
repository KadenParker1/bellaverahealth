package com.pm.bellavera.store;

import com.pm.bellavera.admin.api.AdminProductDto;
import com.pm.bellavera.admin.api.CreateProductRequest;
import com.pm.bellavera.store.api.AdminOrderDto;
import com.pm.bellavera.store.api.CheckoutItemRequest;
import com.pm.bellavera.store.api.CheckoutRequest;
import com.pm.bellavera.store.api.CheckoutSessionDto;
import com.pm.bellavera.store.api.FulfillOrderRequest;
import com.pm.bellavera.store.api.OrderDto;
import com.pm.bellavera.store.api.ProductDto;
import com.pm.bellavera.support.AbstractIntegrationTest;
import com.pm.bellavera.support.JwtTestSupport;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The whole store path against the mock payment gateway: catalog, checkout, the payment webhook
 * that is the only thing allowed to mark an order paid, and fulfillment.
 */
class StoreCheckoutIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private final UUID adminId = UUID.randomUUID();

    @Test
    void aProductCanBeBoughtPaidForAndShipped() throws Exception {
        String productCode = uniqueCode("tonic");
        AdminProductDto product = createProduct(productCode, "Evening tonic", 2500);
        assertThat(product.active()).isTrue();

        // Browsing needs no account.
        List<ProductDto> catalog = readList(
                mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/store/products"))
                        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
                ProductDto[].class);
        assertThat(catalog).anyMatch(p -> p.code().equals(productCode));

        UUID userId = UUID.randomUUID();
        RequestPostProcessor user = JwtTestSupport.supabaseUser(userId, userId + "@example.com");

        CheckoutSessionDto session = objectMapper.readValue(
                mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/store/checkout").with(user)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new CheckoutRequest(
                                        List.of(new CheckoutItemRequest(productCode, 2))))))
                        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
                CheckoutSessionDto.class);
        assertThat(session.checkoutUrl()).contains("orderId=" + session.orderId());

        // Checkout only creates the order. Nothing is paid until a webhook says so.
        OrderDto pending = getOrder(user, session.orderId());
        assertThat(pending.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(pending.subtotalCents()).isEqualTo(5000);
        assertThat(pending.items()).singleElement()
                .satisfies(item -> {
                    assertThat(item.quantity()).isEqualTo(2);
                    assertThat(item.unitPriceCents()).isEqualTo(2500);
                });

        String eventId = "evt_" + UUID.randomUUID();
        postWebhook(eventId, "checkout.session.completed", session.orderId()).andExpect(status().isOk());

        OrderDto paid = getOrder(user, session.orderId());
        assertThat(paid.status()).isEqualTo(OrderStatus.PAID);
        assertThat(paid.paidAt()).isNotNull();
        assertThat(paid.shipTo()).isNotNull();
        assertThat(paid.shipTo().city()).isEqualTo("Boulder");

        // It is now on the fulfillment queue.
        List<AdminOrderDto> queue = readList(
                mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/admin/orders?status=PAID").with(admin()))
                        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
                AdminOrderDto[].class);
        assertThat(queue).anyMatch(order -> order.id().equals(session.orderId()));

        AdminOrderDto fulfilled = objectMapper.readValue(
                mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/admin/orders/{id}/fulfill", session.orderId())
                                .with(admin())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        new FulfillOrderRequest("USPS", "9400111899223"))))
                        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
                AdminOrderDto.class);
        assertThat(fulfilled.status()).isEqualTo(OrderStatus.FULFILLED);
        assertThat(fulfilled.fulfilledAt()).isNotNull();
        assertThat(fulfilled.trackingNumber()).isEqualTo("9400111899223");

        // A second click cannot re-ship it or overwrite the shipment.
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/admin/orders/{id}/fulfill", session.orderId())
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FulfillOrderRequest("DHL", "other"))))
                .andExpect(status().isConflict());

        // And it has left the queue.
        List<AdminOrderDto> queueAfter = readList(
                mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/admin/orders?status=PAID").with(admin()))
                        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
                AdminOrderDto[].class);
        assertThat(queueAfter).noneMatch(order -> order.id().equals(session.orderId()));
    }

    @Test
    void aRedeliveredPaymentEventIsAppliedOnlyOnce() throws Exception {
        String productCode = uniqueCode("balm");
        createProduct(productCode, "Recovery balm", 1800);

        UUID userId = UUID.randomUUID();
        RequestPostProcessor user = JwtTestSupport.supabaseUser(userId, userId + "@example.com");
        CheckoutSessionDto session = startCheckout(user, productCode, 1);

        String eventId = "evt_" + UUID.randomUUID();
        postWebhook(eventId, "checkout.session.completed", session.orderId()).andExpect(status().isOk());
        OrderDto afterFirst = getOrder(user, session.orderId());

        // Stripe retries until it gets a 2xx; the same event arriving twice must not pay twice.
        postWebhook(eventId, "checkout.session.completed", session.orderId()).andExpect(status().isOk());
        OrderDto afterSecond = getOrder(user, session.orderId());

        assertThat(afterSecond.status()).isEqualTo(OrderStatus.PAID);
        assertThat(afterSecond.paidAt()).isEqualTo(afterFirst.paidAt());
    }

    @Test
    void anOrderThatWasNeverPaidCannotBeFulfilled() throws Exception {
        String productCode = uniqueCode("salts");
        createProduct(productCode, "Bath salts", 900);

        UUID userId = UUID.randomUUID();
        RequestPostProcessor user = JwtTestSupport.supabaseUser(userId, userId + "@example.com");
        CheckoutSessionDto session = startCheckout(user, productCode, 1);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/admin/orders/{id}/fulfill", session.orderId())
                        .with(admin()))
                .andExpect(status().isConflict());
    }

    @Test
    void checkoutRefusesUnknownAndDeactivatedProducts() throws Exception {
        String productCode = uniqueCode("retired");
        AdminProductDto product = createProduct(productCode, "Retired item", 1200);

        UUID userId = UUID.randomUUID();
        RequestPostProcessor user = JwtTestSupport.supabaseUser(userId, userId + "@example.com");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/store/checkout").with(user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CheckoutRequest(
                                List.of(new CheckoutItemRequest("does_not_exist", 1))))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value("Unknown product: does_not_exist"));

        // Removing a product deactivates it - the row survives so past orders still read correctly.
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/admin/products/{id}", product.id()).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/store/products/{code}", productCode))
                .andExpect(status().isNotFound());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/store/checkout").with(user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CheckoutRequest(
                                List.of(new CheckoutItemRequest(productCode, 1))))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void oneCustomerCannotReadAnotherCustomersOrder() throws Exception {
        String productCode = uniqueCode("mask");
        createProduct(productCode, "Sleep mask", 1500);

        UUID buyerId = UUID.randomUUID();
        RequestPostProcessor buyer = JwtTestSupport.supabaseUser(buyerId, buyerId + "@example.com");
        CheckoutSessionDto session = startCheckout(buyer, productCode, 1);

        UUID snooperId = UUID.randomUUID();
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/store/orders/{id}", session.orderId())
                        .with(JwtTestSupport.supabaseUser(snooperId, snooperId + "@example.com")))
                .andExpect(status().isNotFound());
    }

    @Test
    void stockIsDrawnDownWhenAnOrderShipsNotWhenItIsBought() throws Exception {
        String code = uniqueCode("stocked");
        createProduct(code, "Ten in stock", 1000, 10);

        UUID userId = UUID.randomUUID();
        RequestPostProcessor user = JwtTestSupport.supabaseUser(userId, userId + "@example.com");
        CheckoutSessionDto session = startCheckout(user, code, 3);

        // An unpaid order commits nothing - an abandoned checkout must not hold stock hostage.
        assertThat(adminProduct(code).stockQuantity()).isEqualTo(10);
        assertThat(adminProduct(code).available()).isEqualTo(10);

        payFor(session.orderId());

        // Paid but unshipped: the units are spoken for, so they leave availability while the
        // physical count stays put until the parcel goes.
        assertThat(adminProduct(code).stockQuantity()).isEqualTo(10);
        assertThat(adminProduct(code).available()).isEqualTo(7);
        assertThat(storeProduct(code).available()).isEqualTo(7);

        fulfill(session.orderId());

        assertThat(adminProduct(code).stockQuantity()).isEqualTo(7);
        assertThat(adminProduct(code).available()).isEqualTo(7);
    }

    @Test
    void checkoutWillNotSellMoreThanIsAvailable() throws Exception {
        String code = uniqueCode("scarce");
        createProduct(code, "Two in stock", 500, 2);

        UUID firstId = UUID.randomUUID();
        RequestPostProcessor first = JwtTestSupport.supabaseUser(firstId, firstId + "@example.com");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/store/checkout").with(first)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CheckoutRequest(
                                List.of(new CheckoutItemRequest(code, 3))))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value("Only 2 left of '" + code + "'"));

        // Buy and pay for both, so nothing is left for the next customer.
        payFor(startCheckout(first, code, 2).orderId());

        UUID secondId = UUID.randomUUID();
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/store/checkout")
                        .with(JwtTestSupport.supabaseUser(secondId, secondId + "@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CheckoutRequest(
                                List.of(new CheckoutItemRequest(code, 1))))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value("Out of stock: " + code));
    }

    @Test
    void anUntrackedProductIsAlwaysAvailableAndNeverDecremented() throws Exception {
        String code = uniqueCode("unlimited");
        AdminProductDto product = createProduct(code, "Made to order", 700, null);
        assertThat(product.stockQuantity()).isNull();
        assertThat(product.available()).isNull();

        UUID userId = UUID.randomUUID();
        RequestPostProcessor user = JwtTestSupport.supabaseUser(userId, userId + "@example.com");
        // Comfortably more than any stocked fixture, but under bellavera.store.max-item-quantity.
        CheckoutSessionDto session = startCheckout(user, code, 15);
        payFor(session.orderId());
        fulfill(session.orderId());

        assertThat(adminProduct(code).stockQuantity()).isNull();
        assertThat(storeProduct(code).available()).isNull();
    }

    @Test
    void theAdminOrderListFiltersByStatusSoShippedOrdersStayReadable() throws Exception {
        String code = uniqueCode("history");
        createProduct(code, "History item", 1100, 5);

        UUID userId = UUID.randomUUID();
        RequestPostProcessor user = JwtTestSupport.supabaseUser(userId, userId + "@example.com");
        CheckoutSessionDto shipped = startCheckout(user, code, 1);
        payFor(shipped.orderId());
        fulfill(shipped.orderId());

        CheckoutSessionDto waiting = startCheckout(user, code, 1);
        payFor(waiting.orderId());

        assertThat(orderIds("?status=PAID")).contains(waiting.orderId()).doesNotContain(shipped.orderId());
        assertThat(orderIds("?status=FULFILLED")).contains(shipped.orderId()).doesNotContain(waiting.orderId());
        assertThat(orderIds("")).contains(shipped.orderId(), waiting.orderId());

        // The shipped order still reads back in full, stock drawdown and all.
        AdminOrderDto detail = objectMapper.readValue(
                mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/admin/orders/{id}", shipped.orderId())
                                .with(admin()))
                        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
                AdminOrderDto.class);
        assertThat(detail.status()).isEqualTo(OrderStatus.FULFILLED);
        assertThat(detail.fulfilledAt()).isNotNull();
    }

    @Test
    void aRegularUserCannotReachTheFulfillmentQueue() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/admin/orders")
                        .with(JwtTestSupport.supabaseUser(userId, userId + "@example.com")))
                .andExpect(status().isForbidden());
    }

    // --- helpers -------------------------------------------------------------

    private RequestPostProcessor admin() {
        return JwtTestSupport.supabaseAdmin(adminId, adminId + "@example.com");
    }

    private static String uniqueCode(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /** An untracked product (null stock) - the default fixture, so stock never confuses a test. */
    private AdminProductDto createProduct(String code, String name, int priceCents) throws Exception {
        return createProduct(code, name, priceCents, null);
    }

    private AdminProductDto createProduct(String code, String name, int priceCents, Integer stock)
            throws Exception {
        String json = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/admin/products").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateProductRequest(
                                code, name, "A test fixture", null, priceCents, "usd", null, 0, stock))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(json, AdminProductDto.class);
    }

    private AdminProductDto adminProduct(String code) throws Exception {
        List<AdminProductDto> products = readList(
                mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/admin/products").with(admin()))
                        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
                AdminProductDto[].class);
        return products.stream().filter(p -> p.code().equals(code)).findFirst().orElseThrow();
    }

    private ProductDto storeProduct(String code) throws Exception {
        return objectMapper.readValue(
                mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/store/products/{code}", code))
                        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
                ProductDto.class);
    }

    /**
     * A verified event naming an order we cannot find must not be answered 2xx.
     *
     * <p>The provider retries non-2xx and gives up on a 200, so swallowing this quietly - which is
     * what returning 200 and logging a warning did - turns a race into a charged customer whose
     * order stays PENDING forever, with nothing left to retry.
     */
    @Test
    void aPaymentEventForAnUnknownOrderIsRetryableNotSilentlyAccepted() throws Exception {
        postWebhook("evt_" + UUID.randomUUID(), "checkout.session.completed", UUID.randomUUID())
                .andExpect(status().isServiceUnavailable());
    }

    /** An event type we do not act on is still a successful delivery - there is nothing to retry. */
    @Test
    void anUnrecognisedEventTypeIsAccepted() throws Exception {
        postWebhook("evt_" + UUID.randomUUID(), "payment_intent.created", UUID.randomUUID())
                .andExpect(status().isOk());
    }

    private void fulfill(UUID orderId) throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/admin/orders/{id}/fulfill", orderId)
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FulfillOrderRequest(null, null))))
                .andExpect(status().isOk());
    }

    private void payFor(UUID orderId) throws Exception {
        postWebhook("evt_" + UUID.randomUUID(), "checkout.session.completed", orderId)
                .andExpect(status().isOk());
    }

    private CheckoutSessionDto startCheckout(RequestPostProcessor user, String productCode, int quantity)
            throws Exception {
        String json = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/store/checkout").with(user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CheckoutRequest(
                                List.of(new CheckoutItemRequest(productCode, quantity))))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(json, CheckoutSessionDto.class);
    }

    private OrderDto getOrder(RequestPostProcessor user, UUID orderId) throws Exception {
        String json = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/store/orders/{id}", orderId).with(user))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(json, OrderDto.class);
    }

    /**
     * Posts a payload in the normalized shape {@code MockPaymentGateway} reads. The endpoint is
     * unauthenticated, exactly as a real provider callback is.
     */
    private org.springframework.test.web.servlet.ResultActions postWebhook(String eventId, String type, UUID orderId)
            throws Exception {
        Map<String, Object> payload = Map.of(
                "eventId", eventId,
                "type", type,
                "orderId", orderId.toString(),
                "paymentIntentId", "pi_mock_" + eventId,
                "customerEmail", "buyer@example.com",
                "shipping", Map.of(
                        "name", "Test Buyer",
                        "line1", "1 Test Street",
                        "city", "Boulder",
                        "region", "CO",
                        "postalCode", "80301",
                        "country", "US"));
        return mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/webhooks/stripe")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)));
    }

    private List<UUID> orderIds(String query) throws Exception {
        return readList(mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/admin/orders" + query).with(admin()))
                        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
                AdminOrderDto[].class).stream().map(AdminOrderDto::id).toList();
    }

    private <T> List<T> readList(String json, Class<T[]> arrayType) {
        return List.of(objectMapper.readValue(json, arrayType));
    }
}
