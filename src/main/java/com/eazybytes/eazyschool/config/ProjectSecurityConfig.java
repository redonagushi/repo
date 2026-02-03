package com.eazybytes.eazyschool.config;

import com.eazybytes.eazyschool.constants.EazySchoolConstants; // ✅ FIX: importi i saktë
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class ProjectSecurityConfig {

    @Bean
    SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/saveMsg", "/public/**", "/api/**", "/data-api/**")
                )
                .authorizeHttpRequests(requests -> requests

                        // ✅ 1) ROLE-based paths (vendosi lart)
                        .requestMatchers("/admin/**").hasRole(EazySchoolConstants.ADMIN_ROLE)
                        .requestMatchers("/lecturer/**").hasRole(EazySchoolConstants.LECTURER_ROLE)
                        .requestMatchers("/student/**").hasRole(EazySchoolConstants.STUDENT_ROLE)

                        // ✅ 2) Admin-only ekstra (nëse i ke këto rrugë jashtë /admin/**)
                        .requestMatchers("/displayMessages/**").hasRole(EazySchoolConstants.ADMIN_ROLE)
                        .requestMatchers("/closeMsg/**").hasRole(EazySchoolConstants.ADMIN_ROLE)

                        // ✅ 3) Authenticated paths
                        .requestMatchers("/dashboard").authenticated()
                        .requestMatchers("/displayProfile", "/updateProfile").authenticated()
                        .requestMatchers("/api/**", "/data-api/**").authenticated()

                        // ✅ 4) shared for roles
                        .requestMatchers("/course-materials/**")
                        .hasAnyRole(EazySchoolConstants.STUDENT_ROLE,
                                EazySchoolConstants.LECTURER_ROLE,
                                EazySchoolConstants.ADMIN_ROLE)

                        // ✅ 5) Public paths
                        .requestMatchers("/", "/home", "/holidays/**", "/contact", "/saveMsg",
                                "/courses", "/about", "/login", "/logout", "/public/**")
                        .permitAll()

                        // ✅ static
                        .requestMatchers("/uploads/**", "/assets/**").permitAll()

                        // ✅ çdo gjë tjetër kërkon login
                        .anyRequest().authenticated()
                )
                .formLogin(login -> login
                        .loginPage("/login")
                        .usernameParameter("email")
                        .passwordParameter("pwd")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
