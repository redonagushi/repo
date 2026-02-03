package com.eazybytes.eazyschool;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenHash {
    public static void main(String[] args) {
        String raw = "12345678"; // password i thjeshtë
        String hash = new BCryptPasswordEncoder().encode(raw);
        System.out.println(hash);
    }
}
