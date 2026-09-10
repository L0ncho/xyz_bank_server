package cl.duoc.xyzbank.bffmobile.shared.infrastructure.adapters;

import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.function.Supplier;

public final class CoreServiceCalls {

    private CoreServiceCalls() {
    }

    public static <T> T fetch(Supplier<T> request) {
        try {
            T response = request.get();
            if (response == null) {
                throw new CoreServiceCallException(502, "The upstream service returned an empty response");
            }
            return response;
        } catch (RestClientResponseException exception) {
            throw mapResponse(exception);
        } catch (RestClientException exception) {
            throw new CoreServiceCallException(504, "The upstream service did not respond in time");
        }
    }

    private static CoreServiceCallException mapResponse(RestClientResponseException exception) {
        int status = exception.getStatusCode().value();
        if (status == 404) {
            return new CoreServiceCallException(404, "The requested resource was not found");
        }
        if (status == 409) {
            return new CoreServiceCallException(409, "The request conflicts with the current state");
        }
        if (status == 422) {
            return new CoreServiceCallException(422, "The request could not be processed");
        }
        if (status >= 500) {
            return new CoreServiceCallException(502, "The upstream service failed");
        }
        if (status >= 400) {
            return new CoreServiceCallException(status, "The request was rejected");
        }
        return new CoreServiceCallException(502, "The upstream service failed");
    }
}
