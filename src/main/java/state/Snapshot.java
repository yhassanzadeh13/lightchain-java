package state;

import model.lightchain.Account;
import model.lightchain.Identifier;

public interface Snapshot {
  Identifier GetBlockId();
  long GetBlockHeight();
  Account GetAccount(Identifier identifier);
}
