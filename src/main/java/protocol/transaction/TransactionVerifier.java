package protocol.transaction;

import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.checks.*;
import com.puppycrawl.tools.checkstyle.checks.indentation.ObjectBlockHandler;
import fixtures.AccountFixture;
import model.crypto.PublicKey;
import model.crypto.Signature;
import model.lightchain.Account;
import model.lightchain.Identifier;
import model.lightchain.Transaction;
import state.Snapshot;
import state.State;

import java.security.SecureRandom;
import java.util.Random;


import static org.mockito.Mockito.*;

public class TransactionVerifier implements Validator {
    @Override
    public boolean isCorrect(Transaction transaction) {
        Identifier sender = transaction.getSender();
        Identifier receiver = transaction.getReceiver();
        Identifier referenceBlockID = transaction.getReferenceBlockId();
        double amount = transaction.getAmount();

        State mockState = mock(State.class);
        Snapshot mockSnapshot = mock(Snapshot.class);
        Account mockSenderAccount = mock(Account.class);
        Account mockReceiverAccount = mock(Account.class);
        when(mockSenderAccount.getIdentifier()).thenReturn(sender);
        when(mockReceiverAccount.getIdentifier()).thenReturn(receiver);

        when(mockState.atBlockId(referenceBlockID)).thenReturn(mockSnapshot);
        when(mockSnapshot.getAccount(sender)).thenReturn(mockSenderAccount);
        when(mockSnapshot.getAccount(receiver)).thenReturn(mockReceiverAccount);
        when(mockSnapshot.getReferenceBlockId()).thenReturn(referenceBlockID);
        when(mockSnapshot.getReferenceBlockHeight()).thenReturn(new Random().nextLong());
        // TODO: add checks for whether reference block id is final
        return amount>0 && mockSenderAccount != null && mockReceiverAccount != null ;
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
        return mockSnapshot.getReferenceBlockHeight()> mockSenderSnapshot.getReferenceBlockHeight();
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
        when(mockPublicKey.verifySignature(transaction,signature)).thenReturn(true);
        return mockPublicKey.verifySignature(transaction,signature);
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
