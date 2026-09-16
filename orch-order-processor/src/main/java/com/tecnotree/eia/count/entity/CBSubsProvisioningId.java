package com.tecnotree.eia.count.entity;

import java.io.Serializable;
import java.util.Objects;

public class CBSubsProvisioningId implements Serializable {

    private Long orderId;
    private String sequenceNumber;

    public CBSubsProvisioningId() {}

    public CBSubsProvisioningId(Long orderId, String sequenceNumber) {
        this.orderId = orderId;
        this.sequenceNumber = sequenceNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CBSubsProvisioningId)) return false;
        CBSubsProvisioningId that = (CBSubsProvisioningId) o;
        return Objects.equals(orderId, that.orderId) &&
               Objects.equals(sequenceNumber, that.sequenceNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId, sequenceNumber);
    }
}
