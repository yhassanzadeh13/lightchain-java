package unittest.fixtures;

import javassist.bytecode.ByteArray;
import model.crypto.PublicKey;
import model.lightchain.Account;
import model.lightchain.Identifier;
import org.mockito.Mockito;

public class AccountFixture extends Account {
    /**
     * Constructor of an Account.
     *
     * @param identifier  unique identifier of the account.
     * @param publicKey   public key of the account owner.
     * @param lastBlockId identifier of the last block id that changed this account (or genesis id at bootstrap time).
     */
    public AccountFixture(Identifier identifier, PublicKey publicKey, Identifier lastBlockId) {
        super(identifier, publicKey, lastBlockId);
    }
    /**
     * Constructor of an Account.
     *
     * @param identifier  unique identifier of the account.
     * Creates an Account using randomly created PublicKey and LastBlockId.
     */
    public AccountFixture(Identifier identifier) {
        super(identifier, Mockito.mock(PublicKey.class, Mockito.withSettings().useConstructor(Bytes.byteArrayFixture(32))), IdentifierFixture.NewIdentifier());
    }
}
