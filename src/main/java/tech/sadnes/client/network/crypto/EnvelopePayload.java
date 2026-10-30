package tech.sadnes.client.network.crypto;

public record EnvelopePayload(String nonce, String ciphertext, String tag) {
    public String toJson() {
        return "{\"n\":\"" + this.nonce + "\",\"c\":\"" + this.ciphertext + "\",\"t\":\"" + this.tag + "\"}";
    }

    public static EnvelopePayload fromJson(String string) {
        return new EnvelopePayload(extract(string, "n"), extract(string, "c"), extract(string, "t"));
    }

    private static String extract(String string, String string2) {
        String string3 = "\"" + string2 + "\"";
        int i = string.indexOf(string3);
        if (i < 0) {
            throw new SecureEnvelopeException("secure envelope missing field " + string2);
        } else {
            int i2 = string.indexOf(58, i + string3.length());
            if (i2 < 0) {
                throw new SecureEnvelopeException("secure envelope malformed at field " + string2);
            } else {
                int i3 = string.indexOf(34, i2 + 1);
                if (i3 < 0) {
                    throw new SecureEnvelopeException("secure envelope malformed at field " + string2);
                } else {
                    int i4 = string.indexOf(34, i3 + 1);
                    if (i4 < 0) {
                        throw new SecureEnvelopeException("secure envelope malformed at field " + string2);
                    } else {
                        String string4 = string.substring(i3 + 1, i4);
                        if (string4.isEmpty()) {
                            throw new SecureEnvelopeException("secure envelope empty field " + string2);
                        } else {
                            return string4;
                        }
                    }
                }
            }
        }
    }
}
