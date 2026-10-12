package vn.cmc.coursemanagement.config;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
@Configuration
public class WebSecurityConfig {
    @Bean
    SecurityFilterChain security(HttpSecurity http) throws Exception {
        // Trang quản trị bài tập không yêu cầu đăng nhập; giữ bảo vệ CSRF cho form POST.
        return http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .formLogin(login -> login.disable()).httpBasic(basic -> basic.disable()).build();
    }
}
