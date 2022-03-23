package protocol.assigner;

import java.util.ArrayList;

import model.crypto.Sha3256Hash;
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
    ArrayList<Account> accounts = s.all();
    Assignment assignment = new Assignment();
    for (int i = 1; i <= num; i++) {
      String idI = id.toString().concat(Integer.toString(i));
      //TODO: 32 byte issue in concatted string
      Identifier validatorHash = new Sha3256Hash(id).toIdentifier();
      for (int j = 0; j <= num; j++) {
        int validatorIndex = j > 0 ? j - 1 : accounts.size() - 1;
        if (validatorIndex == -1) {
          throw new IllegalArgumentException("not enough accounts in the snapshot");
        }
        //TODO: burda bir kontrol yapılmalı da ne ben de bilmiyorum
        if (validatorHash.comparedTo(accounts.get(j).getIdentifier()) < 0
            && accounts.get(validatorIndex).getStake() >= Parameters.MINIMUM_STAKE) {
          assignment.add(accounts.get(validatorIndex).getIdentifier());
          accounts.remove(validatorIndex);
        }
      }
    }
    return assignment;
  }
}
