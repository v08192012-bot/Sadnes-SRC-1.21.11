package tech.sadnes.client.network;

import java.util.function.UnaryOperator;
import tech.sadnes.client.network.crypto.FrameCipher;

public final class SocketCipherProvider {
    private final FrameCipher cipher;

    public SocketCipherProvider(String string) {
        this.cipher = new FrameCipher(string);
    }

    public UnaryOperator<String> encryptor() {
        return this.cipher::encrypt;
    }

    public UnaryOperator<String> decryptor() {
        return this.cipher::decrypt;
    }
}
