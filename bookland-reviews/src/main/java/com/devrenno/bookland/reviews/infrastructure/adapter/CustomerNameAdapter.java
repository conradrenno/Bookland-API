package com.devrenno.bookland.reviews.infrastructure.adapter;

import com.devrenno.bookland.reviews.application.port.out.CustomerNamePort;
import com.devrenno.bookland.user.application.port.in.GetUserByIdUseCase;
import com.devrenno.bookland.user.domain.exception.UserNotFoundException;
import com.devrenno.bookland.user.domain.valueobject.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CustomerNameAdapter implements CustomerNamePort {

    private final GetUserByIdUseCase getUserByIdUseCase;

    @Override
    public String getCustomerName(UUID customerId) {
        try {
            return getUserByIdUseCase.execute(UserId.of(customerId)).getName();
        } catch (UserNotFoundException e) {
            return null;
        }
    }
}
