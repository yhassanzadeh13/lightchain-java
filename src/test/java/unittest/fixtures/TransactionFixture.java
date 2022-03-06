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
     * Creates a new transaction with randomly created sender identifier, receiver identifier, reference block id,
     * and transaction amount.
     *
     * @return random Transaction object
     */
    public static model.lightchain.Transaction newTransaction() {
        Identifier sender = IdentifierFixture.newIdentifier();
        Identifier receiver = IdentifierFixture.newIdentifier();
        Identifier referenceBlockId = IdentifierFixture.newIdentifier();
        double amount = RANDOM.nextDouble()+0.01;
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
}
