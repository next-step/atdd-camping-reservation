package com.camping.legacy.acceptance.fixtures;

import java.time.LocalDate;

public class ReservationRequest {
        private final String customerName;
        private final String startDate;
        private final String endDate;
        private final String siteNumber;
        private final String phoneNumber;
        private final Integer numberOfPeople;
        private final String carNumber;
        private final String requests;

        private ReservationRequest(Builder builder) {
            this.customerName = builder.customerName;
            this.startDate = builder.startDate;
            this.endDate = builder.endDate;
            this.siteNumber = builder.siteNumber;
            this.phoneNumber = builder.phoneNumber;
            this.numberOfPeople = builder.numberOfPeople;
            this.carNumber = builder.carNumber;
            this.requests = builder.requests;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private String customerName;
            private String startDate;
            private String endDate;
            private String siteNumber;

            // 기본값들
            private String phoneNumber = "010-0000-0000";
            private Integer numberOfPeople = 2;
            private String carNumber = "12가3456";
            private String requests = "요청사항 없음";

            private Builder() {
            }

            public Builder customerName(String customerName) {
                this.customerName = customerName;
                return this;
            }

            public Builder startDate(LocalDate startDate) {
                this.startDate = startDate == null ? null : startDate.toString();
                return this;
            }

            public Builder endDate(LocalDate endDate) {
                this.endDate = endDate == null ? null : endDate.toString();
                return this;
            }

            public Builder siteNumber(String siteNumber) {
                this.siteNumber = siteNumber;
                return this;
            }

            public Builder phoneNumber(String phoneNumber) {
                this.phoneNumber = phoneNumber;
                return this;
            }

            public Builder numberOfPeople(Integer numberOfPeople) {
                this.numberOfPeople = numberOfPeople;
                return this;
            }

            public Builder carNumber(String carNumber) {
                this.carNumber = carNumber;
                return this;
            }

            public Builder requests(String requests) {
                this.requests = requests;
                return this;
            }

            public ReservationRequest build() {
                return new ReservationRequest(this);
            }
        }

        public String getCustomerName() {
            return customerName;
        }

        public String getStartDate() {
            return startDate;
        }

        public String getEndDate() {
            return endDate;
        }

        public String getSiteNumber() {
            return siteNumber;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public Integer getNumberOfPeople() {
            return numberOfPeople;
        }

        public String getCarNumber() {
            return carNumber;
        }

        public String getRequests() {
            return requests;
        }
    }