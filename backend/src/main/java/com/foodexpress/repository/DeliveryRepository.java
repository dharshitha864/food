package com.foodexpress.repository;

import com.foodexpress.entity.Delivery;
import com.foodexpress.entity.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    Optional<Delivery> findByOrderId(Long orderId);
    List<Delivery> findByDeliveryPartnerIdOrderByAssignedAtDesc(Long deliveryPartnerId);
    List<Delivery> findByStatus(DeliveryStatus status);
}
