package org.xxg.backend.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xxg.backend.backend.dto.CreateOrderRequest;
import org.xxg.backend.backend.entity.Card;
import org.xxg.backend.backend.entity.CardPricing;
import org.xxg.backend.backend.entity.Order;
import org.xxg.backend.backend.mapper.CardPricingMapper;
import org.xxg.backend.backend.mapper.OrderMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderMapper orderMapper;
    private final CardService cardService;
    private final CardPricingMapper cardPricingMapper;

    public OrderService(OrderMapper orderMapper, CardService cardService, CardPricingMapper cardPricingMapper) {
        this.orderMapper = orderMapper;
        this.cardService = cardService;
        this.cardPricingMapper = cardPricingMapper;
    }

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        BigDecimal unitPrice = BigDecimal.ZERO;
        int duration = 0;
        int totalCount = 0;

        if (request.getPricingId() != null) {
            CardPricing pricing = cardPricingMapper.findById(request.getPricingId());
            if (pricing != null) {
                unitPrice = pricing.getPrice();
                if ("time".equals(request.getCardType())) {
                    duration = pricing.getValue();
                } else {
                    totalCount = pricing.getValue();
                }
            } else {
                int specValue = parseSpecValue(request.getCardSpec());
                unitPrice = calculateUnitPrice(request.getCardType(), request.getCardSpec());
                if ("time".equals(request.getCardType())) duration = specValue;
                else totalCount = specValue;
            }
        } else {
            int specValue = parseSpecValue(request.getCardSpec());
            unitPrice = calculateUnitPrice(request.getCardType(), request.getCardSpec());
            if ("time".equals(request.getCardType())) duration = specValue;
            else totalCount = specValue;
        }

        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(request.getQuantity()));

        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(request.getUserId());
        order.setUsername(request.getUsername() != null ? request.getUsername() : "外部商城");
        order.setCardType(request.getCardType());
        order.setCardSpec(request.getCardSpec());
        order.setQuantity(request.getQuantity());
        order.setUnitPrice(unitPrice);
        order.setTotalPrice(totalPrice);
        order.setStatus("completed");
        order.setSource(request.getSource() != null ? request.getSource() : "external_shop");
        order.setExternalOrderNo(request.getExternalOrderNo());
        order.setPackageId(request.getPackageId());
        order.setCreateTime(LocalDateTime.now());
        order.setPayTime(LocalDateTime.now());

        Long creatorId = request.getUserId() != null ? Long.valueOf(request.getUserId()) : 1L;
        String creatorName = order.getUsername();
        List<Card> cards = cardService.createCards(
                request.getQuantity(), request.getCardType(), duration, totalCount,
                "web", "advanced", 1,
                "external_shop", creatorId, creatorName, null
        );
        order.setCardKeys(cards.stream().map(Card::getCardKey).collect(Collectors.joining(",")));

        orderMapper.insert(order);
        return order;
    }

    @Transactional
    public void completeOrder(Order order) {
        if (order == null || "completed".equals(order.getStatus())) {
            return;
        }
        int specValue = parseSpecValue(order.getCardSpec());
        int duration = "time".equals(order.getCardType()) ? specValue : 0;
        int totalCount = "time".equals(order.getCardType()) ? 0 : specValue;
        Long creatorId = order.getUserId() != null ? Long.valueOf(order.getUserId()) : 1L;

        List<Card> cards = cardService.createCards(
                order.getQuantity(), order.getCardType(), duration, totalCount,
                "web", "advanced", 1,
                "external_shop", creatorId, order.getUsername(), null
        );
        order.setCardKeys(cards.stream().map(Card::getCardKey).collect(Collectors.joining(",")));
        order.setStatus("completed");
        order.setPayTime(LocalDateTime.now());
        orderMapper.update(order);
    }

    @Transactional
    public void completeOrder(String orderNo) {
        completeOrder(orderMapper.findByOrderNo(orderNo));
    }

    public List<Order> getUserOrders(Integer userId) {
        return orderMapper.findByUserId(userId);
    }

    public List<Order> getAllOrders() {
        return orderMapper.findAll();
    }

    public List<Order> searchOrders(String orderNo, String username, String status, String cardType, String startTime, String endTime) {
        return orderMapper.search(orderNo, username, status, cardType, startTime, endTime);
    }

    public boolean updateOrderStatus(String orderNo, String status) {
        return orderMapper.updateStatus(orderNo, status) > 0;
    }

    public Order getOrderByNo(String orderNo) {
        return orderMapper.findByOrderNo(orderNo);
    }

    private int parseSpecValue(String spec) {
        if (spec == null) return 0;
        Matcher m = Pattern.compile("\\d+").matcher(spec);
        return m.find() ? Integer.parseInt(m.group()) : 0;
    }

    private String generateOrderNo() {
        return "ISS" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + (int)(Math.random() * 1000);
    }

    private BigDecimal calculateUnitPrice(String type, String spec) {
        if (spec == null) return BigDecimal.ZERO;
        if ("time".equals(type)) {
            if (spec.contains("7")) return new BigDecimal("9.9");
            if (spec.contains("15")) return new BigDecimal("18.8");
            if (spec.contains("30")) return new BigDecimal("35.0");
            if (spec.contains("60")) return new BigDecimal("65.0");
            if (spec.contains("90")) return new BigDecimal("90.0");
            if (spec.contains("180")) return new BigDecimal("168.0");
        } else if ("count".equals(type)) {
            if (spec.contains("50")) return new BigDecimal("12.0");
            if (spec.contains("100")) return new BigDecimal("22.0");
            if (spec.contains("200")) return new BigDecimal("40.0");
            if (spec.contains("500")) return new BigDecimal("95.0");
            if (spec.contains("1000")) return new BigDecimal("180.0");
        }
        return BigDecimal.ZERO;
    }
}