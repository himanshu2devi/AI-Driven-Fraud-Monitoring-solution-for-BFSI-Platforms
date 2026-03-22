package com.fraud_detection.Fraud_Management.config.Swagger;



import io.swagger.v3.oas.models.ExternalDocumentation;
        import io.swagger.v3.oas.models.OpenAPI;
        import io.swagger.v3.oas.models.info.Contact;
        import io.swagger.v3.oas.models.info.Info;
        import io.swagger.v3.oas.models.info.License;
        import org.springframework.context.annotation.Bean;
        import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerAPIConfig {

    @Bean
    public OpenAPI transactionServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Fraud Management Service  API Swagger")
                        .description("Backend API documentation for the Fraud management Service")
                        .version("2.0.0")
                        .contact(new Contact()
                                .name("Himanshu Devi")
                                .email("himanshudevi1997@gmail.com"))
                        .license(new License().name("Apache 2.0").url("https://springdoc.org")))
                .externalDocs(new ExternalDocumentation()
                        .description("Project GitHub Repository")
                        .url("https://github.com/Scalable-Service-Group27/Fraud-management"));
    }

}

