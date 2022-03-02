package unittest.fixtures;

import model.lightchain.Identifier;
import model.lightchain.Transaction;

import java.util.Random;
public class TransactionFixture extends Transaction {
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
    public static model.lightchain.Transaction NewTransaction() {
        Identifier sender = IdentifierFixture.NewIdentifier();
        Identifier receiver = IdentifierFixture.NewIdentifier();
        Identifier referenceBlockId = IdentifierFixture.NewIdentifier();
        double amount = new Random().nextDouble()+0.01;
        return new model.lightchain.Transaction(sender, receiver, referenceBlockId, amount);
    }
}