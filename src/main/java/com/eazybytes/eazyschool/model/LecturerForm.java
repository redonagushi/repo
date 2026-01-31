package com.eazybytes.eazyschool.model;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class LecturerForm {

    @NotBlank(message="Name must not be blank")
    @Size(min=3, message="Name must be at least 3 characters long")
    private String name;

    @NotBlank(message="Mobile number must not be blank")
    @Pattern(regexp="(^$|[0-9]{10})", message="Mobile number must be 10 digits")
    private String mobileNumber;

    @NotBlank(message="Email must not be blank")
    @Email(message="Please provide a valid email address")
    private String email;

    @NotBlank(message="Confirm Email must not be blank")
    @Email(message="Please provide a valid confirm email address")
    private String confirmEmail;

    @NotBlank(message="Password must not be blank")
    @Size(min=5, message="Password must be at least 5 characters long")
    private String pwd;

    @NotBlank(message="Confirm Password must not be blank")
    @Size(min=5, message="Confirm Password must be at least 5 characters long")
    private String confirmPwd;
}
