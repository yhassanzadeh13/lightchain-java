package unittest.fixtures;

import javassist.bytecode.ByteArray;
import model.crypto.PublicKey;
import model.lightchain.Account;
import model.lightchain.Identifier;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;
import java.util.Random;

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
    public AccountFixture(Identifier identifier) {
        super(identifier,
                Mockito.mock(PublicKey.class, Mockito.withSettings().useConstructor(Bytes.ByteArrayFixture(32))),
                IdentifierFixture.NewIdentifier());
    }

    public static Account NewAccount() {
        Identifier identifier = IdentifierFixture.NewIdentifier();
        PublicKey publicKey = Mockito.mock(PublicKey.class, Mockito.withSettings().useConstructor(Bytes.ByteArrayFixture(32)));
        Identifier lastBlockId = IdentifierFixture.NewIdentifier();
        return new AccountFixture(identifier, publicKey, lastBlockId);
    }
}
