package com.loanmust.loan.config;

import com.loanmust.loan.access.PermissionInterceptor;
import com.loanmust.loan.common.IdCodec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ForcePasswordChangeInterceptor forcePasswordChangeInterceptor;
    private final PermissionInterceptor permissionInterceptor;
    private final IdCodec idCodec;

    public WebConfig(ForcePasswordChangeInterceptor forcePasswordChangeInterceptor,
                     PermissionInterceptor permissionInterceptor,
                     IdCodec idCodec) {
        this.forcePasswordChangeInterceptor = forcePasswordChangeInterceptor;
        this.permissionInterceptor = permissionInterceptor;
        this.idCodec = idCodec;
    }

    /**
     * Accept opaque id tokens (and plain numeric ids, for redirects/back-compat) wherever a
     * controller binds a Long from the URL. An all-digit value is treated as a raw id; any
     * other value is decoded as an opaque token.
     */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, Long.class, source -> {
            String s = source == null ? "" : source.trim();
            if (s.isEmpty()) return null;
            boolean digits = true;
            for (int i = 0; i < s.length(); i++) {
                if (s.charAt(i) < '0' || s.charAt(i) > '9') { digits = false; break; }
            }
            if (digits) return Long.valueOf(s);
            Long id = idCodec.decode(s);
            if (id == null) throw new IllegalArgumentException("Unrecognised id token");
            return id;
        });
    }

    /** Languages offered in the UI switcher. */
    public static final List<String[]> LANGUAGES = List.of(
            new String[]{"en", "English"},
            new String[]{"hi", "हिन्दी"},
            new String[]{"ta", "தமிழ்"},
            new String[]{"te", "తెలుగు"},
            new String[]{"kn", "ಕನ್ನಡ"},
            new String[]{"ml", "മലയാളം"}
    );

    @Bean
    public ReloadableResourceBundleMessageSource messageSource() {
        ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
        ms.setBasenames("classpath:i18n/messages");
        ms.setDefaultEncoding("UTF-8");
        ms.setFallbackToSystemLocale(false);
        ms.setUseCodeAsDefaultMessage(true);
        ms.setCacheSeconds(5);
        return ms;
    }

    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver("LOANMUST_LANG");
        resolver.setDefaultLocale(Locale.ENGLISH);
        resolver.setCookieMaxAge(Duration.ofDays(365));
        resolver.setCookiePath("/");
        return resolver;
    }

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        return interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(forcePasswordChangeInterceptor);
        registry.addInterceptor(localeChangeInterceptor());
        registry.addInterceptor(permissionInterceptor)
                .excludePathPatterns("/css/**", "/js/**", "/images/**",
                        "/favicon.ico", "/favicon.svg", "/login", "/logout", "/error",
                        "/no-access", "/refer/**", "/p/**", "/actuator/**");
    }
}
