package protocol.assigner;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

import crypto.Sha3256Hasher;
import model.codec.EncodedEntity;
import model.crypto.Hash;
import model.lightchain.Account;
import model.lightchain.Assignment;
import model.lightchain.Identifier;
import protocol.Parameters;
import state.Snapshot;

/**
 * Represents the assignment of validators to an entity.
 */
public class LightChainValidatorAssigner implements ValidatorAssigner {
  /**
   * Assigns validators from the given snapshot to the entity with given identifier.
   * Identifier of the ith validator is chosen as the staked account with the greatest identifier that
   * is less than or equal to hash(id || i). Once the ith validator is chosen, it is omitted from the procedure
   * of picking the i+1(th) validator.
   *
   * @param id  identifier of the entity.
   * @param s   snapshot to pick validators from.
   * @param num number of validators to choose.
   * @return list of validators.
   */
  @Override
  public Assignment assign(Identifier id, Snapshot s, short num) throws IllegalArgumentException {
    if (s == null) {
      throw new IllegalArgumentException("snapshot cannot be null");
    }
    if (id == null) {
      throw new IllegalArgumentException("identifier cannot be null");
    }
    ArrayList<Account> accounts = s.all();
    ArrayList<Identifier> validatorHashes = new ArrayList<>();
    ArrayList<Account> selectedAccounts = new ArrayList<>();
    Assignment assignment = new Assignment();

    /*
    Computes hash of validators.
     */
    for (int i = 1; i <= num; i++) {
      byte[] bytesId = id.getBytes();
      byte bytesIterator = Integer.valueOf(i).byteValue();
      ByteArrayOutputStream output = new ByteArrayOutputStream();

      output.write(bytesId, 0, 32);
      output.write(bytesIterator);
      byte[] bytesIdArray = output.toByteArray();
      EncodedEntity ee = new EncodedEntity(bytesIdArray, "assignment");
      Hash validatorHash = new Sha3256Hasher().computeHash(ee);
      validatorHashes.add(validatorHash.toIdentifier());
    }

    // picks the greatest staked account id less than validator hash
    for (int j = 0; j < num; j++) {
      // TODO: this and next for loop are going through all accounts causing a linear search, which
      // can be improved later.
      for (int k = accounts.size() - 1; k >= 0; k--) {
        if (validatorHashes.get(j).comparedTo(accounts.get(k).getIdentifier()) >= 0
            && accounts.get(k).getStake() >= Parameters.MINIMUM_STAKE
            && !selectedAccounts.contains(accounts.get(k))) {
          assignment.add(accounts.get(k).getIdentifier());
          selectedAccounts.add(accounts.get(k));
          break;
        }
      }

      // when validator hash is less than all accounts, the staked account with maximum
      // identifier that has not already been selected is picked.
      if (selectedAccounts.size() != j + 1) {
        for (int k = accounts.size() - 1; k >= 0; k--) {
          if (accounts.get(k).getStake() >= Parameters.MINIMUM_STAKE
              && !selectedAccounts.contains(accounts.get(k))) {
            assignment.add(accounts.get(k).getIdentifier());
            selectedAccounts.add(accounts.get(k));
            break;
          }
        }
      }
    }
    if (selectedAccounts.size() < num) {
      throw new IllegalArgumentException("not enough accounts in the snapshot");
    }
    return assignment;
  }
}
