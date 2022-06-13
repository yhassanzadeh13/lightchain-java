package unittest.fixtures;

import java.util.ArrayList;

import model.crypto.KeyGen;
import model.crypto.PrivateKey;
import model.crypto.PublicKey;
import model.lightchain.Account;
import model.lightchain.Identifier;
import model.lightchain.ValidatedBlock;
import model.local.Local;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import protocol.Parameters;
import protocol.block.BlockValidator;
import state.table.TableSnapshot;
import state.table.TableState;

/**
 * Encapsulates tests for validating blockchain fixture.
 */
public class BlockchainFixtureTest {
  // TODO: implement following test WITHOUT mocking anything
  // Create a snapshot of 100 accounts, and then use that to create a chain of 1000 blocks using newValidChain method.
  // Then examine that each block is passing validation.
  @Test
  public void testValidChain() {
    Local[] locals = new Local[100];
    TableSnapshot tableSnapshot = new TableSnapshot(IdentifierFixture.newIdentifier(), 0);
    for (int i = 0; i < 100; i++) {
      KeyGen keygen = KeyGenFixture.newKeyGen();
      PublicKey publicKey = keygen.getPublicKey();
      PrivateKey privateKey = keygen.getPrivateKey();
      Identifier accountIdentifier = IdentifierFixture.newIdentifier();
      locals[i] = new Local(accountIdentifier, privateKey);
      Account account = new Account(accountIdentifier,
          publicKey,
          tableSnapshot.getReferenceBlockId(),
          Parameters.MINIMUM_STAKE);
      account.setBalance(9999999999L);
      tableSnapshot.addAccount(accountIdentifier, account);
    }
    ArrayList<ValidatedBlock> chain = BlockchainFixture.newValidChain(tableSnapshot, 1000);
    TableState tableState = new TableState();
    for (ValidatedBlock vBlock:chain){
      tableState.execute(vBlock);
    }
    BlockValidator blockValidator = new BlockValidator(tableState);
    for (ValidatedBlock block : chain) {
      Assertions.assertTrue(blockValidator.allTransactionsSound(block));
      Assertions.assertTrue(blockValidator.allTransactionsValidated(block));
      //Assertions.assertTrue(blockValidator.isAuthenticated(block));
      Assertions.assertTrue(blockValidator.isConsistent(block));
      Assertions.assertTrue(blockValidator.isCorrect(block));
      Assertions.assertTrue(blockValidator.noDuplicateSender(block));
      Assertions.assertTrue(blockValidator.proposerHasEnoughStake(block));
    }
  }
}
