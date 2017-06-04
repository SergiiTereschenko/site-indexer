package ua.devchallenge.context;

import java.util.Collection;
import java.util.List;

import com.fasterxml.classmate.TypeResolver;
import com.google.common.collect.ImmutableList;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMethod;
import rx.Single;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.builders.ResponseMessageBuilder;
import springfox.documentation.schema.WildcardType;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ResponseMessage;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import static com.google.common.collect.ImmutableList.of;
import static springfox.documentation.schema.AlternateTypeRules.newRule;

@Configuration
@EnableSwagger2
public class SwaggerContext {

    private static final String BASE_PACKAGE = "ua.devchallenge.api";

    private static final String MESSAGE_400 = "Missing or invalid request parameters with internal response code -3";
    private static final String MESSAGE_403 = "Forbidden!";
    private static final String MESSAGE_500 = "Internal server error with internal response code -1";

    @Bean
    Docket api(TypeResolver typeResolver) {
        return new Docket(DocumentationType.SWAGGER_2)
            .globalResponseMessage(RequestMethod.GET, getResponseMessage())
            .globalResponseMessage(RequestMethod.DELETE, getResponseMessage())
            .globalResponseMessage(RequestMethod.POST, getResponseMessage())
            .alternateTypeRules(
                newRule(
                    typeResolver.resolve(Collection.class, WildcardType.class),
                    typeResolver.resolve(List.class, WildcardType.class)),
                newRule(
                    typeResolver.resolve(Single.class, WildcardType.class),
                    WildcardType.class))
            .select()
            .apis(RequestHandlerSelectors.basePackage(BASE_PACKAGE))
            .build()
            .apiInfo(apiInfo());
    }

    private ImmutableList<ResponseMessage> getResponseMessage() {
        return of(
            new ResponseMessageBuilder()
                .code(400)
                .message(MESSAGE_400)
                .build(),
            new ResponseMessageBuilder()
                .code(403)
                .message(MESSAGE_403)
                .build(),
            new ResponseMessageBuilder()
                .code(500)
                .message(MESSAGE_500)
                .build());
    }

    private ApiInfo apiInfo() {
        return new ApiInfo("", "API Documentation",
            "1.0", "", ApiInfo.DEFAULT_CONTACT, "", "");
    }
}