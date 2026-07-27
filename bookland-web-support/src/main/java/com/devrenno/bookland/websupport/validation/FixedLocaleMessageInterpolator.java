package com.devrenno.bookland.websupport.validation;

import jakarta.validation.MessageInterpolator;

import java.util.Locale;

/**
 * Pins constraint messages to one language.
 *
 * <p>Hibernate Validator resolves its built-in messages against {@code Locale.getDefault()}, so
 * without this the API answers in whatever language the host JVM happens to run in — Portuguese on
 * a developer's Windows machine, English in the container — while messages written inline on the
 * annotations stay English either way. The result is a single response mixing both.
 */
class FixedLocaleMessageInterpolator implements MessageInterpolator {

    private final MessageInterpolator delegate;
    private final Locale locale;

    FixedLocaleMessageInterpolator(MessageInterpolator delegate, Locale locale) {
        this.delegate = delegate;
        this.locale = locale;
    }

    @Override
    public String interpolate(String messageTemplate, Context context) {
        return delegate.interpolate(messageTemplate, context, locale);
    }

    @Override
    public String interpolate(String messageTemplate, Context context, Locale requested) {
        return delegate.interpolate(messageTemplate, context, locale);
    }
}
