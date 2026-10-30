package tech.sadnes.client.network.crypto;

import java.security.SecureRandom;
import tech.sadnes.client.network.ProtocolException;

public final class FrameCipher {
    private final SecureEnvelope envelope;

    public FrameCipher(String string) {
        this.envelope = new SecureEnvelope(new SecureRandom(), new DryGascon2Hash(), new Shacal2CtrCipher(), SecureEnvelope.decodeBase64(string));
    }

    public String encrypt(String string) {
        return this.envelope.encrypt(string);
    }

    public String decrypt(String string) {
        try {
            return this.envelope.decrypt(string);
        } catch (RuntimeException exception) {
            throw new ProtocolException("failed to open secure frame");
        }
    }
}
