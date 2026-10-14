package com.maruthi.orders;

import com.maruthi.orders.model.Order;
import com.maruthi.orders.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.maruthi.orders.controller.OrderController;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Test
    void createOrder_returns201WithBody() throws Exception {
        Order order = new Order("cust-1", "Keyboard", 1, new BigDecimal("79.99"));
        when(orderService.createOrder(any())).thenReturn(order);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "cust-1",
                                  "product": "Keyboard",
                                  "quantity": 1,
                                  "unitPrice": 79.99
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value("cust-1"))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void createOrder_rejectsInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "",
                                  "product": "Keyboard",
                                  "quantity": 0,
                                  "unitPrice": -5
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
