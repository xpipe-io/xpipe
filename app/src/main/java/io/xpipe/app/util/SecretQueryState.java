package io.xpipe.app.util;

public enum SecretQueryState {
    NORMAL,
    CANCELLED,
    NON_INTERACTIVE,
    FIXED_SECRET_WRONG,
    RETRIEVAL_FAILURE;

    public static String toErrorMessage(SecretQueryState s) {
        if (s == null) {
            return "None";
        }

        return switch (s) {
            case NORMAL -> {
                yield "None";
            }
            case CANCELLED -> {
                yield "Operation was cancelled";
            }
            case NON_INTERACTIVE -> {
                yield "Session is not interactive but required user input";
            }
            case FIXED_SECRET_WRONG -> {
                yield "Provided secret is wrong";
            }
            case RETRIEVAL_FAILURE -> {
                yield "Failed to retrieve secret";
            }
        };
    }
}
