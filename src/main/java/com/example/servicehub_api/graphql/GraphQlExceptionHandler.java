package com.example.servicehub_api.graphql;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import jakarta.validation.ConstraintViolationException;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
@Component
public class GraphQlExceptionHandler extends DataFetcherExceptionResolverAdapter {
    @Override
    protected GraphQLError resolveToSingleError(Throwable error, DataFetchingEnvironment environment) {
        if (error instanceof ResponseStatusException ex) {
            ErrorType type = switch (ex.getStatusCode().value()) {
                case 400, 409 -> ErrorType.BAD_REQUEST;
                case 401 -> ErrorType.UNAUTHORIZED;
                case 403 -> ErrorType.FORBIDDEN;
                case 404 -> ErrorType.NOT_FOUND;
                default -> ErrorType.INTERNAL_ERROR;
            };
            return GraphqlErrorBuilder.newError(environment).errorType(type).message(ex.getReason() == null ? "Operação inválida" : ex.getReason()).build();
        }
        if (error instanceof ConstraintViolationException) {
            return GraphqlErrorBuilder.newError(environment).errorType(ErrorType.BAD_REQUEST).message("Dados inválidos").build();
        }
        return null;
    }
}
