package state.merkle;

import java.util.ArrayList;

import model.Entity;
import model.lightchain.Account;
import model.lightchain.Identifier;
import modules.ads.merkletree.MerkleTree;

/**
 * Implements a simplified merkle tree based model of the protocol snapshot at a given block.
 */
public class MerkleSnapshot implements state.Snapshot {
  private final Identifier rootBlockId;
  private final long rootBlockHeight;
  private MerkleTree merkleTree;

  /**
   * Constructor of MerkleSnapShot.
   *
   * @param rootBlockId     root block id representing this snapshot.
   * @param rootBlockHeight root block height of this snapshot.
   */
  public MerkleSnapshot(Identifier rootBlockId, long rootBlockHeight) {
    this.rootBlockId = rootBlockId;
    this.rootBlockHeight = rootBlockHeight;
    this.merkleTree = new MerkleTree();
  }

  /**
   * The identifier of finalized block that this snapshot represents.
   *
   * @return the identifier of finalized block that this snapshot represents.
   */
  @Override
  public Identifier getReferenceBlockId() {
    return rootBlockId;
  }

  /**
   * The height of the reference block that this snapshot represents.
   *
   * @return height of the reference block that this snapshot represents.
   */
  @Override
  public long getReferenceBlockHeight() {
    return rootBlockHeight;
  }

  /**
   * Fetches account corresponding to an identifier at the given snapshot.
   *
   * @param identifier identifier of an account of interest.
   * @return account corresponding to the given identifier at this snapshot, or null if such an account
   * does not exist.
   */
  @Override
  public Account getAccount(Identifier identifier) {
    return (Account) merkleTree.get(identifier).getEntity();
  }

  /**
   * Adds an account to the snapshot.
   *
   * @param identifier Identifier of the account to add.
   * @param account Account to add.
   */
  public void addAccount(Identifier identifier, Account account) {
    this.merkleTree.put(account);
  }

  /**
   * The list of accounts in this snapshot.
   *
   * @return the list of accounts in this snapshot.
   */
  @Override
  public ArrayList<Account> all() {
    ArrayList<Account> accounts = new ArrayList<>();
    for (Entity entity : merkleTree.all()) {
      accounts.add((Account) entity);
    }
    return accounts;
  }
}
