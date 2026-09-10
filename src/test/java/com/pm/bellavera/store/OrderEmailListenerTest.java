package com.pm.bellavera.store;

import com.pm.bellavera.email.EmailGateway;
import com.pm.bellavera.email.EmailMessage;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderEmailListenerTest {

    private final CustomerOrderRepository customerOrderRepository = mock(CustomerOrderRepository.class);
    private final EmailGateway emailGateway = mock(EmailGateway.class);
    private final OrderEmailListener listener = new OrderEmailListener(customerOrderRepository, emailGateway);

    @Test
    void onOrderPaidSendsAConfirmationListingTheItemsAndTotal() {
        UUID orderId = UUID.randomUUID();
        CustomerOrder order = orderWithOneItem(orderId, "buyer@example.com", 5000);
        when(customerOrderRepository.findWithItemsById(orderId)).thenReturn(Optional.of(order));

        listener.onOrderPaid(new OrderPaidEvent(orderId));

        var captor = org.mockito.ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailGateway).send(captor.capture());
        EmailMessage message = captor.getValue();
        assertThat(message.to()).isEqualTo("buyer@example.com");
        assertThat(message.subject()).contains("confirmed");
        assertThat(message.body()).contains("2x Tonic").contains("50.00");
    }

    @Test
    void onOrderFulfilledIncludesTrackingInfoWhenPresent() {
        UUID orderId = UUID.randomUUID();
        CustomerOrder order = orderWithOneItem(orderId, "buyer@example.com", 5000);
        order.setCarrier("USPS");
        order.setTrackingNumber("9400111899223");
        when(customerOrderRepository.findWithItemsById(orderId)).thenReturn(Optional.of(order));

        listener.onOrderFulfilled(new OrderFulfilledEvent(orderId));

        var captor = org.mockito.ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailGateway).send(captor.capture());
        EmailMessage message = captor.getValue();
        assertThat(message.subject()).contains("shipped");
        assertThat(message.body()).contains("USPS").contains("9400111899223");
    }

    @Test
    void anOrderWithNoEmailOnFileIsSkippedRatherThanSendingToNull() {
        UUID orderId = UUID.randomUUID();
        CustomerOrder order = orderWithOneItem(orderId, null, 5000);
        when(customerOrderRepository.findWithItemsById(orderId)).thenReturn(Optional.of(order));

        listener.onOrderPaid(new OrderPaidEvent(orderId));

        verify(emailGateway, never()).send(any());
    }

    @Test
    void aMissingOrderIsSkippedWithoutThrowing() {
        UUID orderId = UUID.randomUUID();
        when(customerOrderRepository.findWithItemsById(orderId)).thenReturn(Optional.empty());

        assertThatCode(() -> listener.onOrderPaid(new OrderPaidEvent(orderId))).doesNotThrowAnyException();
        verify(emailGateway, never()).send(any());
    }

    /**
     * By the time this listener runs, the order transition already committed. A gateway failure
     * must not surface as an exception - it would turn a successful webhook delivery or fulfillment
     * request into a failure the caller retries for no reason.
     */
    @Test
    void aGatewayFailureIsSwallowedNotPropagated() {
        UUID orderId = UUID.randomUUID();
        CustomerOrder order = orderWithOneItem(orderId, "buyer@example.com", 5000);
        when(customerOrderRepository.findWithItemsById(orderId)).thenReturn(Optional.of(order));
        org.mockito.Mockito.doThrow(new RuntimeException("provider down")).when(emailGateway).send(any());

        assertThatCode(() -> listener.onOrderPaid(new OrderPaidEvent(orderId))).doesNotThrowAnyException();
    }

    private static CustomerOrder orderWithOneItem(UUID orderId, String email, int lineTotalCents) {
        CustomerOrder order = CustomerOrder.builder()
                .id(orderId)
                .email(email)
                .subtotalCents(lineTotalCents)
                .build();
        order.addItem(OrderItem.builder()
                .productCode("tonic")
                .productName("Tonic")
                .unitPriceCents(lineTotalCents / 2)
                .quantity(2)
                .lineTotalCents(lineTotalCents)
                .build());
        return order;
    }
}
