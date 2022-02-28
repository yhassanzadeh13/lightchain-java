package model.crypto;

import model.lightchain.Identifier;

public class Sha3256Hash extends Hash{
    private final byte[] bytes;

    public Sha3256Hash(byte[] hashValue) {
        super(hashValue);
        this.bytes = hashValue;
    }

    public Sha3256Hash(Identifier identifier) {
        super(identifier);
        this.bytes = identifier.getBytes();
    }

    @Override
    public int compare(Hash other) {
        return this.toIdentifier().comparedTo(other.toIdentifier());
    }

    @Override
    public Identifier toIdentifier() {
        return new Identifier(this.bytes);
    }
}
