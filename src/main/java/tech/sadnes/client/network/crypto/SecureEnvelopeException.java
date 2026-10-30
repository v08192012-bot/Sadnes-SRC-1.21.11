package tech.sadnes.client.network.crypto;

public final class SecureEnvelopeException extends RuntimeException {
    public SecureEnvelopeException(String string) {
        super(string);
    }

    public SecureEnvelopeException(String string, Throwable throwable) {
        super(string, throwable);
    }
}
