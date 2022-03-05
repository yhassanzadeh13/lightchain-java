package protocol.transaction;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fixtures.AccountFixture;
import model.crypto.PublicKey;
import model.crypto.Signature;
import model.lightchain.Account;
import model.lightchain.Identifier;
import model.lightchain.Transaction;
import state.Snapshot;
import state.State;

public class TransactionVerifier implements Validator {
  private final State state;

  public TransactionVerifier(State state) {
    this.state = state;
  }

  @Override
  public boolean isCorrect(Transaction transaction) {
    Identifier refBlock = transaction.getReferenceBlockId();
    Snapshot snapshot = state.atBlockId(refBlock);
    if (snapshot == null) {
      // reference block id does not represent a valid snapshot.
      return false;
    }

    Account sender = snapshot.getAccount(transaction.getSender());
    if (sender == null) {
      return false;
    }

    Account receiver = snapshot.getAccount(transaction.getReceiver());
    if (receiver == null) {
      return false;
    }

    return !(transaction.getAmount() <= 0);
  }

  @Override
  public boolean isSound(Transaction transaction) {
    Identifier sender = transaction.getSender();
    Identifier referenceBlockID = transaction.getReferenceBlockId();

    State mockState = mock(State.class);
    Snapshot mockSnapshot = mock(Snapshot.class);
    Account mockSenderAccount = mock(Account.class);
    Identifier mockLastBlockID = mock(Identifier.class);
    Snapshot mockSenderSnapshot = mock(Snapshot.class);
    byte[] mockBytes = new byte[32];
    //long mockHeight = new Random().nextLong();
    // long mockLastHeight = new Random().nextLong();
    when(mockLastBlockID.getBytes()).thenReturn(mockBytes);
    when(mockSenderAccount.getIdentifier()).thenReturn(sender);
    when(mockSenderAccount.getLastBlockId()).thenReturn(mockLastBlockID);
    when(mockState.atBlockId(referenceBlockID)).thenReturn(mockSnapshot);
    when(mockSnapshot.getAccount(sender)).thenReturn(mockSenderAccount);
    when(mockSnapshot.getReferenceBlockId()).thenReturn(referenceBlockID);
    //when(mockSnapshot.getReferenceBlockHeight()).thenReturn(mockHeight);
    when(mockSenderSnapshot.getReferenceBlockId()).thenReturn(mockLastBlockID);
    // when(mockSenderSnapshot.getReferenceBlockHeight()).thenReturn(mockLastHeight);
    // TODO: should mockByte be created new everytime ?
    return mockSnapshot.getReferenceBlockHeight() > mockSenderSnapshot.getReferenceBlockHeight();
  }

  @Override
  public boolean isAuthenticated(Transaction transaction) {
    Identifier sender = transaction.getSender();
    Signature signature = transaction.getSignature();
    Identifier referenceBlockID = transaction.getReferenceBlockId();

    State mockState = mock(State.class);
    Snapshot mockSnapshot = mock(Snapshot.class);
    Account mockSenderAccount = mock(Account.class);
    PublicKey mockPublicKey = mock(PublicKey.class);
    when(mockState.atBlockId(referenceBlockID)).thenReturn(mockSnapshot);
    when(mockSnapshot.getAccount(sender)).thenReturn(mockSenderAccount);
    when(mockSnapshot.getReferenceBlockId()).thenReturn(referenceBlockID);
    when(mockSenderAccount.getPublicKey()).thenReturn(mockPublicKey);
    when(mockPublicKey.verifySignature(transaction, signature)).thenReturn(true);
    return mockPublicKey.verifySignature(transaction, signature);
  }

  @Override
  public boolean senderHasEnoughBalance(Transaction transaction) {
    Identifier sender = transaction.getSender();
    Identifier referenceBlockID = transaction.getReferenceBlockId();
    double amount = transaction.getAmount();

    State mockState = mock(State.class);
    Snapshot mockSnapshot = mock(Snapshot.class);
    Account senderAccount = new AccountFixture(sender);
    when(mockState.atBlockId(referenceBlockID)).thenReturn(mockSnapshot);
    when(mockSnapshot.getAccount(sender)).thenReturn(senderAccount);

    return mockSnapshot.getAccount(sender).getBalance() >= amount;
  }
}
