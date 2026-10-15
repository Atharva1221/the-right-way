package com.examplatform.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdatePhoneNumberRequest {

    @NotBlank(message = "Phone number is required")
    @Size(max = 25, message = "Phone number cannot exceed 25 characters")
    @Pattern(
            regexp = "^(?=(?:\\D*\\d){7,15}\\D*$)\\+?[0-9](?:[0-9 ()-]*[0-9])$",
            message = "Enter a valid international phone number")
    private String phoneNumber;

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber == null ? null : phoneNumber.trim();
    }
}
