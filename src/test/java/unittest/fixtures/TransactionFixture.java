package unittest.fixtures;

import model.lightchain.Identifier;
import model.lightchain.Transaction;

import java.util.Random;
public class TransactionFixture extends Transaction {
    /**
     * Random object to create random amounts.
     */
    private static final Random RANDOM = new Random();

    /**
     * Constructor of the transaction.
     *
     * @param referenceBlockId identifier of a finalized block that this transaction refers to its snapshot.
     * @param sender           identifier of the sender of this transaction.
     * @param receiver         identifier of the receiver of this transaction.
     * @param amount           amount of LightChain tokens that this transaction transfers from sender to receiver.
     */
    public TransactionFixture(Identifier referenceBlockId, Identifier sender, Identifier receiver, double amount) {
        super(referenceBlockId, sender, receiver, amount);
    }

    /**
     * Creates a new transaction with randomly created sender identifier, receiver identifier, reference block id,
     * and transaction amount.
     *
     * @return random Transaction object
     */
    public static model.lightchain.Transaction newTransaction() {
        Identifier sender = IdentifierFixture.newIdentifier();
        Identifier receiver = IdentifierFixture.newIdentifier();
        Identifier referenceBlockId = IdentifierFixture.newIdentifier();
        double amount = Math.abs(RANDOM.nextInt()) + 1;
        return new model.lightchain.Transaction(sender, receiver, referenceBlockId, amount);
    }

    /**
     * Creates a new transaction with randomly created sender identifier, receiver identifier, reference block id,
     * and given transaction amount.
     *
     * @param amount Transaction amount for the random Transaction object
     * @return random Transaction object
     */
    public static model.lightchain.Transaction newTransaction(double amount) {
        Identifier sender = IdentifierFixture.newIdentifier();
        Identifier receiver = IdentifierFixture.newIdentifier();
        Identifier referenceBlockId = IdentifierFixture.newIdentifier();
        return new model.lightchain.Transaction(sender, receiver, referenceBlockId, amount);
    }

    /**
     * Creates a new transaction with randomly created transaction amount, receiver identifier, reference block id,
     * and given sender identifier.
     *
     * @param sender sender identifier for the random Transaction object
     * @return random Transaction object
     */
    public static model.lightchain.Transaction newTransaction(Identifier sender) {
        Identifier receiver = IdentifierFixture.newIdentifier();
        Identifier referenceBlockId = IdentifierFixture.newIdentifier();
        double amount = Math.abs(RANDOM.nextInt()) + 1;
        return new model.lightchain.Transaction(sender, receiver, referenceBlockId, amount);
    }

}
