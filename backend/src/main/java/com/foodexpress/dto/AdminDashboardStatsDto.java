package com.foodexpress.dto;

import java.math.BigDecimal;

public class AdminDashboardStatsDto {
    private long totalCustomers;
    private long totalRestaurants;
    private long totalDeliveryPartners;
    private long totalOrders;
    private long activeOrders;
    private BigDecimal totalRevenue;
    private long totalFoodItems;

    public AdminDashboardStatsDto() {
    }

    public AdminDashboardStatsDto(long totalCustomers, long totalRestaurants, long totalDeliveryPartners, long totalOrders, long activeOrders, BigDecimal totalRevenue, long totalFoodItems) {
        this.totalCustomers = totalCustomers;
        this.totalRestaurants = totalRestaurants;
        this.totalDeliveryPartners = totalDeliveryPartners;
        this.totalOrders = totalOrders;
        this.activeOrders = activeOrders;
        this.totalRevenue = totalRevenue;
        this.totalFoodItems = totalFoodItems;
    }

    public long getTotalCustomers() {
        return totalCustomers;
    }

    public void setTotalCustomers(long totalCustomers) {
        this.totalCustomers = totalCustomers;
    }

    public long getTotalRestaurants() {
        return totalRestaurants;
    }

    public void setTotalRestaurants(long totalRestaurants) {
        this.totalRestaurants = totalRestaurants;
    }

    public long getTotalDeliveryPartners() {
        return totalDeliveryPartners;
    }

    public void setTotalDeliveryPartners(long totalDeliveryPartners) {
        this.totalDeliveryPartners = totalDeliveryPartners;
    }

    public long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public long getActiveOrders() {
        return activeOrders;
    }

    public void setActiveOrders(long activeOrders) {
        this.activeOrders = activeOrders;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public long getTotalFoodItems() {
        return totalFoodItems;
    }

    public void setTotalFoodItems(long totalFoodItems) {
        this.totalFoodItems = totalFoodItems;
    }
}
