package com.fastcampus.snsproject.configuration;


import com.fastcampus.snsproject.configuration.filter.JwtTokenFilter;
import com.fastcampus.snsproject.exception.CustomAuthenticationEntryPoint;
import com.fastcampus.snsproject.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class AuthenticationConfig extends WebSecurityConfigurerAdapter {

  private final UserService userService;
  @Value("${jwt.secret-key}")
  private String key;

  @Override
  public void configure(WebSecurity web) throws Exception {
    // /api/ 로 시작 하는 경로만 통과
    web.ignoring().regexMatchers("^(?!/api/).*")
        .antMatchers(HttpMethod.POST, "/api/*/users/join", "/api/*/users/login");
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.csrf().disable()
        .authorizeRequests()
        .antMatchers("/api/*/users/join", "api/*/users/login").permitAll()
        .antMatchers("/api/**").authenticated()
        .and()
        .sessionManagement()
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        .and()
        .addFilterBefore(new JwtTokenFilter(key, userService), UsernamePasswordAuthenticationFilter.class)
        .exceptionHandling()
        .authenticationEntryPoint(new CustomAuthenticationEntryPoint());
    ;
  }
}